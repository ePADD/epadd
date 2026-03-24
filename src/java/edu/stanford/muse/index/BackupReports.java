package edu.stanford.muse.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BackupReports {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("d_M_yyyy_H_m_s");

    /** Writes content to _backup/report_<d_M_yyyy_H_m_s>.txt, avoiding overwrite. Returns the path. */
    public static Path writeBackupReport(Path bagRoot, CharSequence content) throws IOException {
        Path backupDir = bagRoot.resolve("_backups");
        Files.createDirectories(backupDir);

        String ts = LocalDateTime.now().format(TS);                   // e.g., 1_10_2025_1_20_21
        Path report = uniqueReportPath(backupDir, "report", ts, "txt");

        // CREATE_NEW guarantees we never overwrite anything.
        Files.writeString(report, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        return report;
    }

    private static Path uniqueReportPath(Path dir, String base, String ts, String ext) throws IOException {
        Path p = dir.resolve(base + "_" + ts + "." + ext);
        if (Files.notExists(p)) return p;
        for (int i = 1; i < 10_000; i++) {
            p = dir.resolve(base + "_" + ts + "_" + i + "." + ext);
            if (Files.notExists(p)) return p;
        }
        throw new IOException("Too many reports exist for timestamp " + ts);
    }
}
