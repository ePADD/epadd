package edu.stanford.muse.index;

import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.verify.BagVerifier;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
