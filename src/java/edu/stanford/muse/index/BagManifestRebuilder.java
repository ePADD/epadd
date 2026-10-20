package edu.stanford.muse.index;

import gov.loc.repository.bagit.creator.CreatePayloadManifestsVistor;
import gov.loc.repository.bagit.creator.CreateTagManifestsVistor;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.domain.Metadata;
import gov.loc.repository.bagit.domain.Version;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.util.PathUtils;
import gov.loc.repository.bagit.writer.BagitFileWriter;
import gov.loc.repository.bagit.writer.ManifestWriter;
import gov.loc.repository.bagit.writer.MetadataWriter;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Recreates the BagIt tag files (bagit.txt, bag-info.txt, payload and tag manifests) of an archive whose
 * payload is already in bagRoot/data, e.g. after files in the archive have changed.
 *
 * The result is the same as BagCreator.bagInPlace on the payload, but the payload is never moved. Previously
 * the whole data folder was moved out of the archive, the archive folder deleted and the data moved back; if
 * the second move failed (locked file, full disk, filename invalid on Windows) the archive was left in a temp
 * folder. Here, the new tag files are written to a staging folder first and then swapped with the old ones;
 * if the swap fails, the old tag files are restored.
 */
public final class BagManifestRebuilder {
    private static final Logger log = LogManager.getLogger(BagManifestRebuilder.class);

    private BagManifestRebuilder() {
    }

    public static Bag rebuild(Path bagRoot, Collection<? extends SupportedAlgorithm> algorithms, boolean includeHiddenFiles) throws IOException, NoSuchAlgorithmException {
        Path dataDir = bagRoot.resolve(Archive.BAG_DATA_FOLDER);
        if (!Files.isDirectory(dataDir))
            throw new IOException("Cannot update bag, payload directory is missing: " + dataDir);

        Bag bag = new Bag(Version.LATEST_BAGIT_VERSION());
        bag.setRootDir(bagRoot);
        bag.setFileEncoding(StandardCharsets.UTF_8);

        Map<Manifest, MessageDigest> payloadManifests = Hasher.createManifestToMessageDigestMap(new ArrayList<>(algorithms));
        Files.walkFileTree(dataDir, new CreatePayloadManifestsVistor(payloadManifests, includeHiddenFiles));
        bag.getPayLoadManifests().addAll(payloadManifests.keySet());

        Metadata metadata = new Metadata();
        metadata.upsertPayloadOxum(PathUtils.generatePayloadOxum(dataDir));
        metadata.add("Bagging-Date", new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date()));
        bag.setMetadata(metadata);

        Path staging = Files.createTempDirectory(bagRoot, "bag-rebuild-");
        try {
            BagitFileWriter.writeBagitFile(bag.getVersion(), bag.getFileEncoding(), staging);
            ManifestWriter.writePayloadManifests(bag.getPayLoadManifests(), staging, bagRoot, bag.getFileEncoding());
            MetadataWriter.writeBagMetadata(metadata, bag.getVersion(), staging, bag.getFileEncoding());

            Map<Manifest, MessageDigest> tagManifests = Hasher.createManifestToMessageDigestMap(new ArrayList<>(algorithms));
            Files.walkFileTree(staging, new CreateTagManifestsVistor(tagManifests, includeHiddenFiles));
            ManifestWriter.writeTagManifests(tagManifests.keySet(), staging, staging, bag.getFileEncoding());

            replaceTagFiles(bagRoot, staging);
        } finally {
            FileUtils.deleteQuietly(staging.toFile());
        }

        // read the bag back so that its manifests refer to the files in bagRoot, not in the staging folder
        Bag result = Archive.readArchiveBag(bagRoot.toString());
        if (result == null)
            throw new IOException("Bag was updated but could not be read back: " + bagRoot);
        return result;
    }

    /** Moves the old tag files of bagRoot aside, moves the new ones from staging in, and restores the old ones on failure. */
    private static void replaceTagFiles(Path bagRoot, Path staging) throws IOException {
        Path old = Files.createTempDirectory(bagRoot, "bag-old-");
        List<Path> movedOut = new ArrayList<>();
        List<Path> movedIn = new ArrayList<>();
        try {
            for (Path f : tagFiles(bagRoot)) {
                Files.move(f, old.resolve(f.getFileName()));
                movedOut.add(f.getFileName());
            }
            for (Path f : tagFiles(staging)) {
                Files.move(f, bagRoot.resolve(f.getFileName()));
                movedIn.add(f.getFileName());
            }
        } catch (IOException e) {
            log.error("Failed to replace bag tag files in " + bagRoot + ", restoring the previous ones", e);
            for (Path name : movedIn)
                Files.deleteIfExists(bagRoot.resolve(name));
            for (Path name : movedOut)
                Files.move(old.resolve(name), bagRoot.resolve(name));
            FileUtils.deleteQuietly(old.toFile());
            throw e;
        }
        FileUtils.deleteQuietly(old.toFile());
    }

    private static List<Path> tagFiles(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        // filter by name rather than a {a,b} glob, which some Windows filesystem providers don't support
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
            for (Path f : files)
                if (Files.isRegularFile(f) && isTagFileName(f.getFileName().toString()))
                    result.add(f);
        }
        return result;
    }

    private static boolean isTagFileName(String name) {
        return name.equals("bagit.txt") || name.equals("bag-info.txt")
                || ((name.startsWith("manifest-") || name.startsWith("tagmanifest-")) && name.endsWith(".txt"));
    }
}
