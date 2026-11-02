package edu.stanford.epadd.util;

import edu.stanford.muse.index.Archive;
import edu.stanford.muse.webapp.SimpleSessions;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Tests the path checks and the delete of the DelFolder and SidecarFileDelete servlets. */
public class SafeDeleteTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, "x".getBytes(StandardCharsets.UTF_8));
    }

    private Path createCollection(Path base, String id) throws IOException {
        Path collection = base.resolve(id);
        write(collection.resolve(Archive.BAG_DATA_FOLDER).resolve(Archive.SESSIONS_SUBDIR).resolve("default" + SimpleSessions.getSessionSuffix()));
        write(collection.resolve(Archive.BAG_DATA_FOLDER).resolve("blobs/1.pdf"));
        return collection;
    }

    @Test
    public void sidecarFileAcceptsOnlyFilesDirectlyInTheSidecarFolder() throws Exception {
        Path sidecar = tmp.newFolder("archive", "data", "sidecar").toPath();
        write(sidecar.resolve("notes.txt"));
        write(sidecar.resolve("sub/inner.txt"));
        write(sidecar.getParent().resolve("blobs/1.pdf"));

        assertEquals(sidecar.resolve("notes.txt").toAbsolutePath().normalize(), SidecarFileDelete.sidecarFile(sidecar.toString(), "notes.txt"));
        assertNull(SidecarFileDelete.sidecarFile(sidecar.toString(), "../blobs/1.pdf"));
        assertNull(SidecarFileDelete.sidecarFile(sidecar.toString(), "sub/inner.txt"));
        assertNull(SidecarFileDelete.sidecarFile(sidecar.toString(), "sub"));
        assertNull(SidecarFileDelete.sidecarFile(sidecar.toString(), "missing.txt"));
        assertNull(SidecarFileDelete.sidecarFile(sidecar.toString(), sidecar.getParent().resolve("blobs/1.pdf").toString()));
        assertNull(SidecarFileDelete.sidecarFile(sidecar.toString(), ".."));
    }

    @Test
    public void collectionFolderAcceptsOnlyFoldersDirectlyInTheModeFolder() throws Exception {
        Path base = tmp.newFolder("epadd-processing").toPath();
        Path collection = createCollection(base, "abc");
        Files.createDirectories(collection.resolve("data/sub"));

        assertEquals(collection.toFile().getCanonicalFile(), DelFolder.collectionFolder(collection.toString(), base.toString()));
        // the page sends the path with forward slashes
        assertEquals(collection.toFile().getCanonicalFile(), DelFolder.collectionFolder(collection.toString().replace('\\', '/'), base.toString()));
        assertNull(DelFolder.collectionFolder(base.toString(), base.toString()));
        assertNull(DelFolder.collectionFolder(base.getParent().toString(), base.toString()));
        assertNull(DelFolder.collectionFolder(collection.resolve("data/sub").toString(), base.toString()));
        assertNull(DelFolder.collectionFolder(base.resolve("abc/../..").toString(), base.toString()));
        assertNull(DelFolder.collectionFolder(base.resolve("missing").toString(), base.toString()));
    }

    @Test
    public void deleteCollectionRemovesTheFolder() throws Exception {
        Path base = tmp.newFolder("epadd-processing").toPath();
        Path collection = createCollection(base, "abc");
        Path other = createCollection(base, "other");

        assertNull(DelFolder.deleteCollection(collection.toFile()));

        assertFalse(Files.exists(collection));
        assertTrue(Files.exists(other.resolve("data/blobs/1.pdf")));
        assertEquals(1, base.toFile().list().length);
    }

    @Test
    public void deleteCollectionDeletesNothingWhenAFileIsOpen() throws Exception {
        // an open file prevents renaming its folder on Windows only
        Assume.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"));
        Path base = tmp.newFolder("epadd-processing").toPath();
        Path collection = createCollection(base, "abc");

        try (FileInputStream open = new FileInputStream(collection.resolve("data/blobs/1.pdf").toFile())) {
            DelFolder.deleteCollection(collection.toFile());
            fail("expected IOException");
        } catch (IOException expected) {
        }

        assertTrue(Files.exists(collection.resolve("data/blobs/1.pdf")));
        assertTrue(Files.exists(collection.resolve(Archive.BAG_DATA_FOLDER).resolve(Archive.SESSIONS_SUBDIR).resolve("default" + SimpleSessions.getSessionSuffix())));
        assertEquals(1, base.toFile().list().length);
    }
}
