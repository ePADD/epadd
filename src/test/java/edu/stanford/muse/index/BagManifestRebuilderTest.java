package edu.stanford.muse.index;

import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.verify.BagVerifier;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BagManifestRebuilderTest {
    private static final List<SupportedAlgorithm> ALGORITHMS = Arrays.asList(StandardSupportedAlgorithms.MD5, StandardSupportedAlgorithms.SHA256);

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** Creates a bag the way ePADD does when an archive is first saved. */
    private Path createArchiveBag() throws Exception {
        Path root = tmp.newFolder("archive").toPath();
        Files.createDirectories(root.resolve("sessions"));
        Files.write(root.resolve("sessions/default.archive.v2"), "session".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("blobs"));
        Files.write(root.resolve("blobs/1.letter.pdf"), "pdf".getBytes(StandardCharsets.UTF_8));
        BagCreator.bagInPlace(root, ALGORITHMS, false);
        return root;
    }

    private static Set<String> list(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static void assertValid(Bag bag) throws Exception {
        try (BagVerifier verifier = new BagVerifier()) {
            verifier.isValid(bag, false);
        }
    }

    @Test
    public void rebuildUpdatesManifestsWithoutMovingThePayload() throws Exception {
        Path root = createArchiveBag();
        Path blob = root.resolve("data/blobs/1.letter.pdf");
        Files.write(blob, "changed".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("data/blobs/2.new.pdf"), "new".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(root.resolve("_backups"));
        Files.write(root.resolve("_backups/manifest-md5.txt.bak"), "backup".getBytes(StandardCharsets.UTF_8));

        Bag bag = BagManifestRebuilder.rebuild(root, ALGORITHMS, false);

        assertValid(bag);
        assertEquals(2, bag.getPayLoadManifests().iterator().next().getFileToChecksumMap().keySet().stream()
                .filter(p -> p.toString().contains("blobs")).count());
        assertEquals("changed", new String(Files.readAllBytes(blob), StandardCharsets.UTF_8));
        assertTrue(Files.exists(root.resolve("_backups/manifest-md5.txt.bak")));
        // same tag files as BagCreator.bagInPlace, and no staging folders left behind
        assertEquals(new TreeSet<>(Arrays.asList("_backups", "bag-info.txt", "bagit.txt", "data", "manifest-md5.txt",
                "manifest-sha256.txt", "tagmanifest-md5.txt", "tagmanifest-sha256.txt")), list(root));
    }

    private static String checksum(Bag bag, Path file) {
        Manifest md5 = bag.getPayLoadManifests().stream()
                .filter(m -> m.getAlgorithm() == StandardSupportedAlgorithms.MD5).findFirst().get();
        return md5.getFileToChecksumMap().entrySet().stream()
                .filter(e -> e.getKey().toAbsolutePath().normalize().equals(file.toAbsolutePath().normalize()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private static Map<String, byte[]> readTagFiles(Path root) throws IOException {
        Map<String, byte[]> result = new TreeMap<>();
        for (String name : list(root))
            if (name.endsWith(".txt"))
                result.put(name, Files.readAllBytes(root.resolve(name)));
        return result;
    }

    @Test
    public void updateRefreshesChangedFilesAndKeepsTheRest() throws Exception {
        Path root = createArchiveBag();
        Path session = root.resolve("data/sessions/default.archive.v2");
        Path blob = root.resolve("data/blobs/1.letter.pdf");
        Bag bag = Archive.readArchiveBag(root.toString());
        String blobChecksum = checksum(bag, blob);
        String sessionChecksum = checksum(bag, session);

        Files.write(session, "changed".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("data/sessions/new.json"), "{}".getBytes(StandardCharsets.UTF_8));
        BagManifestRebuilder.update(bag, root.resolve("data/sessions"), false);

        assertValid(bag);
        assertValid(Archive.readArchiveBag(root.toString()));
        assertTrue(!sessionChecksum.equals(checksum(bag, session)));
        assertEquals(blobChecksum, checksum(bag, blob));
        assertTrue(checksum(bag, root.resolve("data/sessions/new.json")) != null);
        assertEquals(new TreeSet<>(Arrays.asList("bag-info.txt", "bagit.txt", "data", "manifest-md5.txt",
                "manifest-sha256.txt", "tagmanifest-md5.txt", "tagmanifest-sha256.txt")), list(root));
    }

    @Test
    public void updateRemovesEntriesOfDeletedFiles() throws Exception {
        Path root = createArchiveBag();
        Path blob = root.resolve("data/blobs/1.letter.pdf");
        Bag bag = Archive.readArchiveBag(root.toString());

        Files.delete(blob);
        BagManifestRebuilder.update(bag, blob, false);

        assertValid(bag);
        assertValid(Archive.readArchiveBag(root.toString()));
        assertEquals(null, checksum(bag, blob));
    }

    @Test
    public void updateLeavesBagUntouchedWhenItFails() throws Exception {
        Path root = createArchiveBag();
        Bag bag = Archive.readArchiveBag(root.toString());
        Map<String, byte[]> before = readTagFiles(root);
        Set<Manifest> manifestsBefore = new HashSet<>(bag.getPayLoadManifests());

        Path session = root.resolve("data/sessions/default.archive.v2");
        Files.write(session, "changed".getBytes(StandardCharsets.UTF_8));
        Path moved = root.resolve("moved-away");
        Files.move(root.resolve("data"), moved);
        try {
            BagManifestRebuilder.update(bag, root.resolve("data/sessions"), false);
            fail("expected IOException");
        } catch (IOException expected) {
        }

        assertEquals(manifestsBefore, new HashSet<>(bag.getPayLoadManifests()));
        Map<String, byte[]> after = readTagFiles(root);
        assertEquals(before.keySet(), after.keySet());
        for (String name : before.keySet())
            assertTrue(name, Arrays.equals(before.get(name), after.get(name)));
    }

    @Test
    public void updateRestoresTagFilesWhenTheSwapFails() throws Exception {
        // a file opened with FileInputStream cannot be moved on Windows
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"));
        Path root = createArchiveBag();
        Bag bag = Archive.readArchiveBag(root.toString());
        Map<String, byte[]> before = readTagFiles(root);
        Set<Manifest> manifestsBefore = new HashSet<>(bag.getPayLoadManifests());

        Files.write(root.resolve("data/sessions/default.archive.v2"), "changed".getBytes(StandardCharsets.UTF_8));
        try (FileInputStream locked = new FileInputStream(root.resolve("tagmanifest-sha256.txt").toFile())) {
            BagManifestRebuilder.update(bag, root.resolve("data/sessions"), false);
            fail("expected IOException");
        } catch (IOException expected) {
        }

        assertEquals(manifestsBefore, new HashSet<>(bag.getPayLoadManifests()));
        assertEquals(before.keySet(), readTagFiles(root).keySet());
        Map<String, byte[]> after = readTagFiles(root);
        for (String name : before.keySet())
            assertTrue(name, Arrays.equals(before.get(name), after.get(name)));
        assertEquals(new TreeSet<>(Arrays.asList("bag-info.txt", "bagit.txt", "data", "manifest-md5.txt",
                "manifest-sha256.txt", "tagmanifest-md5.txt", "tagmanifest-sha256.txt")), list(root));
    }

    @Test
    public void rebuildLeavesArchiveUntouchedWhenPayloadIsMissing() throws Exception {
        Path root = tmp.newFolder("notabag").toPath();
        Files.write(root.resolve("file.txt"), "x".getBytes(StandardCharsets.UTF_8));

        try {
            BagManifestRebuilder.rebuild(root, ALGORITHMS, false);
            fail("expected IOException");
        } catch (IOException expected) {
        }
        assertEquals(new TreeSet<>(Arrays.asList("file.txt")), list(root));
    }
}
