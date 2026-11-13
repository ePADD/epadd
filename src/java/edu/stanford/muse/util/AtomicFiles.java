package edu.stanford.muse.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes files so that the target is either left as it was or completely replaced, never left half written.
 *
 * Writing straight to a file empties it first, so an interrupted or failed write (ePADD being closed, a crash, a full
 * disk, a dropped network drive, an exception while serializing) leaves a truncated or zero-filled file behind, and
 * the archive data in it can no longer be read.
 */
public final class AtomicFiles {

    private static final Logger log = LogManager.getLogger(AtomicFiles.class);

    @FunctionalInterface
    public interface StreamWriter {
        /** Writes the content to out. It may close out (e.g. to finish a GZIPOutputStream wrapped around it). */
        void write(OutputStream out) throws IOException;
    }

    private AtomicFiles() {
    }

    /**
     * Writes the content to a temporary file in the target's directory, flushes it to disk and then moves it over
     * the target.
     *
     * @throws IOException if the file could not be written; the target is then unchanged. A RuntimeException thrown
     *                     by the writer is wrapped in an IOException.
     */
    public static void write(Path target, StreamWriter writer) throws IOException {
        Path dir = target.toAbsolutePath().getParent();
        // a unique name, so that two saves running at the same time don't write into the same temporary file
        Path tmp = Files.createTempFile(dir, target.getFileName().toString() + ".", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                writer.write(out);
            }
            // make sure the data is on disk before it replaces the old file
            try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                log.warn("Atomic move not supported for " + target + ", replacing it non-atomically.");
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            log.error("Failed to write " + target + ". The previous version of the file is kept.", e);
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException e2) {
                log.warn("Could not delete temporary file " + tmp, e2);
            }
            throw (e instanceof IOException) ? (IOException) e : new IOException("Failed to write " + target, e);
        }
    }
}
