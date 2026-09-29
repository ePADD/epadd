package edu.stanford.muse.index;

import gov.loc.repository.bagit.domain.Bag;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BagitManifestCleanerTest {
    // BagIt 1.0 writes CR/LF in payload paths as %0D/%0A. A Mac import can produce such names from folded
    // attachment headers; they are legal on macOS but not on Windows.
    private static final String ENCODED_CRLF_PATH = "data/blobs/5282.Stipendium%0D%0A   =_ISO-8859-1_Q_f=FCr_Medienkunst=5FBewerbungsrichtlinien=2Ep_=.pdf";
    private static final String HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path createBag() throws IOException {
        Path root = tmp.newFolder("bag").toPath();
        Files.createDirectories(root.resolve("data/blobs"));
        Files.write(root.resolve("data/blobs/1.ok.pdf"), new byte[0]);
        Files.write(root.resolve("bagit.txt"), Arrays.asList("BagIt-Version: 1.0", "Tag-File-Character-Encoding: UTF-8"), StandardCharsets.UTF_8);
        Files.write(root.resolve("manifest-sha256.txt"), Arrays.asList(HASH + " data/blobs/1.ok.pdf", HASH + " " + ENCODED_CRLF_PATH), StandardCharsets.UTF_8);
        return root;
    }

    @Test
    public void cleanDropsPercentEncodedControlCharacters() throws IOException {
        Path root = createBag();

        BagitManifestCleaner.Result result = BagitManifestCleaner.clean(root);

        assertEquals(1, result.totalKept());
        assertEquals(1, result.totalDropped());
        List<String> manifest = Files.readAllLines(root.resolve("manifest-sha256.txt"), StandardCharsets.UTF_8);
        assertEquals(Arrays.asList(HASH + " data/blobs/1.ok.pdf"), manifest);
    }

    @Test
    public void cleanKeepsPercentEncodedPercentSign() throws IOException {
        Path root = createBag();
        Files.write(root.resolve("manifest-sha256.txt"), Arrays.asList(HASH + " data/blobs/100%25.pdf"), StandardCharsets.UTF_8);

        BagitManifestCleaner.Result result = BagitManifestCleaner.clean(root);

        assertEquals(1, result.totalKept());
        assertEquals(0, result.totalDropped());
    }

    @Test
    public void readArchiveBagRecoversFromWindowsUnsafeNameOnWindows() throws IOException {
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("windows"));
        Path root = createBag();

        Bag bag = Archive.readArchiveBag(root.toString());

        assertNotNull(bag);
        assertEquals(1, bag.getPayLoadManifests().iterator().next().getFileToChecksumMap().size());
        String manifest = String.join("\n", Files.readAllLines(root.resolve("manifest-sha256.txt"), StandardCharsets.UTF_8));
        assertFalse(manifest.contains("Stipendium"));
        assertTrue(manifest.contains("1.ok.pdf"));
    }
}
