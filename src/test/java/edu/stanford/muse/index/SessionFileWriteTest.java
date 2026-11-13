package edu.stanford.muse.index;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SessionFileWriteTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static Object[] read(Path file) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(file))))) {
            return new Object[]{ois.readObject(), ois.readObject()};
        }
    }

    private List<String> filesIn(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).sorted().collect(Collectors.toList());
        }
    }

    @Test
    public void writesAndReplacesTheFile() throws Exception {
        Path dir = tmp.getRoot().toPath();
        Path target = dir.resolve("default.archive.v2");

        ArchiveReaderWriter.writeSessionFileAtomically(target, "archive", "first");
        assertArrayEquals(new Object[]{"archive", "first"}, read(target));

        ArchiveReaderWriter.writeSessionFileAtomically(target, "archive", "second");
        assertArrayEquals(new Object[]{"archive", "second"}, read(target));

        // no temporary files are left behind
        assertEquals(List.of("default.archive.v2"), filesIn(dir));
    }

    /** Something in the archive that can't be serialized makes the write fail half way through. */
    @Test
    public void failedWriteKeepsThePreviousFile() throws Exception {
        Path dir = tmp.getRoot().toPath();
        Path target = dir.resolve("default.archive.v2");
        ArchiveReaderWriter.writeSessionFileAtomically(target, "archive", "good");
        byte[] before = Files.readAllBytes(target);

        ArrayList<Object> broken = new ArrayList<>();
        broken.add("some data that is written before the failure");
        broken.add(new Object()); // not serializable
        try {
            ArchiveReaderWriter.writeSessionFileAtomically(target, "archive", broken);
            fail("expected the write to fail");
        } catch (IOException e) {
            assertTrue(e instanceof NotSerializableException);
        }

        assertArrayEquals(before, Files.readAllBytes(target));
        assertArrayEquals(new Object[]{"archive", "good"}, read(target));
        assertEquals(List.of("default.archive.v2"), filesIn(dir));
    }

    /** An exception thrown by the archive's own serialization code is reported as an IOException. */
    @Test
    public void runtimeExceptionDuringWriteKeepsThePreviousFile() throws Exception {
        Path dir = tmp.getRoot().toPath();
        Path target = dir.resolve("default.archive.v2");
        ArchiveReaderWriter.writeSessionFileAtomically(target, "archive", "good");

        try {
            ArchiveReaderWriter.writeSessionFileAtomically(target, "archive", new ThrowsWhileWriting());
            fail("expected the write to fail");
        } catch (IOException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        assertArrayEquals(new Object[]{"archive", "good"}, read(target));
        assertEquals(List.of("default.archive.v2"), filesIn(dir));
    }

    private static class ThrowsWhileWriting implements Serializable {
        private void writeObject(java.io.ObjectOutputStream out) throws IOException {
            out.writeUTF("partial");
            throw new IllegalStateException("modified while saving");
        }
    }
}
