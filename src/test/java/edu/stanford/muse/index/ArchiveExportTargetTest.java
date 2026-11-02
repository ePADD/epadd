package edu.stanford.muse.index;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests how Archive.export checks and replaces its target folder. */
public class ArchiveExportTargetTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertRefused(File target, File archiveDir) {
        try {
            Archive.checkExportTarget(target, archiveDir);
            fail("expected export to " + target + " to be refused");
        } catch (IOException expected) {
        }
    }

    private Path createExport(Path dir, String content) throws IOException {
        write(dir.resolve("bagit.txt"), "BagIt-Version: 1.0");
        write(dir.resolve(Archive.BAG_DATA_FOLDER).resolve(Archive.SESSIONS_SUBDIR).resolve("default.archive.v2"), content);
        return dir;
    }

    @Test
    public void exportTargetMayBeNewEmptyOrAnEarlierExport() throws Exception {
        Path archive = createExport(tmp.newFolder("epadd-appraisal", "user").toPath(), "archive");
        Path exports = tmp.newFolder("exports").toPath();
        Path earlier = createExport(exports.resolve("earlier"), "earlier");
        Files.createDirectories(exports.resolve("empty"));

        Archive.checkExportTarget(exports.resolve("new").toFile(), archive.toFile());
        Archive.checkExportTarget(exports.resolve("empty").toFile(), archive.toFile());
        Archive.checkExportTarget(earlier.toFile(), archive.toFile());
    }

    @Test
    public void exportTargetMustNotOverlapTheArchiveOrReplaceOtherFolders() throws Exception {
        Path archive = createExport(tmp.newFolder("epadd-appraisal", "user").toPath(), "archive");
        Path documents = tmp.newFolder("Documents").toPath();
        write(documents.resolve("thesis.docx"), "thesis");
        write(tmp.getRoot().toPath().resolve("file"), "x");

        assertRefused(archive.toFile(), archive.toFile());
        assertRefused(archive.resolve("data/export").toFile(), archive.toFile());
        assertRefused(archive.getParent().toFile(), archive.toFile());
        assertRefused(archive.resolve("../user").toFile(), archive.toFile());
        assertRefused(documents.toFile(), archive.toFile());
        assertRefused(tmp.getRoot().toPath().resolve("file").toFile(), archive.toFile());
    }

    @Test
    public void successfulExportDeletesThePreviousOne() throws Exception {
        Path target = createExport(tmp.getRoot().toPath().resolve("export"), "new");
        Path previous = createExport(tmp.getRoot().toPath().resolve("export.previous-1"), "old");

        Archive.finishExport(target.toFile(), previous, true);

        assertFalse(Files.exists(previous));
        assertEquals("new", new String(Files.readAllBytes(target.resolve("data/sessions/default.archive.v2")), StandardCharsets.UTF_8));
    }

    @Test
    public void failedExportRestoresThePreviousOne() throws Exception {
        Path target = tmp.getRoot().toPath().resolve("export");
        write(target.resolve("data/blobs/partial"), "partial");
        Path previous = createExport(tmp.getRoot().toPath().resolve("export.previous-1"), "old");

        Archive.finishExport(target.toFile(), previous, false);

        assertFalse(Files.exists(previous));
        assertFalse(Files.exists(target.resolve("data/blobs/partial")));
        assertEquals("old", new String(Files.readAllBytes(target.resolve("data/sessions/default.archive.v2")), StandardCharsets.UTF_8));
    }

    @Test
    public void failedFirstExportLeavesNothingBehind() throws Exception {
        Path target = tmp.getRoot().toPath().resolve("export");
        write(target.resolve("data/blobs/partial"), "partial");

        Archive.finishExport(target.toFile(), null, false);

        assertFalse(Files.exists(target));
        assertTrue(Files.isDirectory(tmp.getRoot().toPath()));
    }
}
