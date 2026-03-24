package edu.stanford.muse.index;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class BagitManifestCleaner {

    //Invalid chars for windows filenames (like *,\,... A blank as last char of a filename works in
    //Mac but not on Windows, so is included in the regex.)
    private static final Pattern INVALID_CHARS =
            Pattern.compile("[\\u0000-\\u001F\\u007F\\\\/:*?\"<>|]");

    /**
     * Windows reserved device names (case-insensitive).
     */
    private static final Pattern RESERVED_BASENAMES =
            Pattern.compile("(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$");


    public static final class Result {
        public final Map<Path, Stats> byManifest = new LinkedHashMap<>();

        public int manifestsProcessed() {
            return byManifest.size();
        }

        public int totalKept() {
            return byManifest.values().stream().mapToInt(s -> s.kept).sum();
        }

        public int totalDropped() {
            return byManifest.values().stream().mapToInt(s -> s.droppedPaths.size()).sum();
        }

        /** All filenames that were removed across all manifests, deduplicated. */
        public List<String> allDroppedPaths() {
            return byManifest.values().stream()
                    .flatMap(s -> s.droppedPaths.stream())
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    public static final class Stats {
        public final int kept;
        public final List<String> droppedPaths;

        Stats(int kept, List<String> droppedPaths) {
            this.kept = kept;
            this.droppedPaths = Collections.unmodifiableList(droppedPaths);
        }
    }

    private BagitManifestCleaner() {
    }

    /**
     * Cleans manifest-md5.txt and manifest-sha256.txt by removing any filenames which are not legal in Windows.
     * The removed files will not be accessible from within ePADD.
     * creates a directory _backups (if it doesn't exist) and places a backup .bak
     * in that directory.
     */
    public static Result clean(Path bagRoot) throws IOException {
        Objects.requireNonNull(bagRoot, "bagRoot");
        Result result = new Result();

        // Check both payload manifests explicitly — avoids relying on glob brace-expansion
        // support which can silently return an empty stream on some Windows providers.
        for (String name : new String[]{"manifest-md5.txt", "manifest-sha256.txt"}) {
            Path manifest = bagRoot.resolve(name);
            if (Files.exists(manifest)) {
                Stats stats = cleanSingleManifest(bagRoot, manifest);
                result.byManifest.put(manifest, stats);
            }
        }

        return result;
    }

    private static Stats cleanSingleManifest(Path bagRoot, Path manifest) throws IOException {
        Path backup = uniqueBackupPath(manifest);           // << no overwrite of backups
        Files.copy(manifest, backup);
        List<String> inLines = Files.readAllLines(backup, StandardCharsets.UTF_8);
        List<String> outLines = new ArrayList<>(inLines.size());

        int kept = 0;
        List<String> droppedPaths = new ArrayList<>();

        for (String raw : inLines) {
            String line = raw;
            // Use full trim only for blank/comment detection so we don't accidentally
            // strip a trailing space that is part of the filename.
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                outLines.add(line);
                continue;
            }

            // Strip only leading whitespace so a trailing space in the filename is preserved.
            String stripped = line.stripLeading();

            // BagIt default is: "<checksum><whitespace><relative-path>"
            int space = firstWhitespacePos(stripped);
            if (space <= 0 || space >= stripped.length() - 1) {
                // Malformed line: keep as-is (be conservative)
                outLines.add(line);
                continue;
            }

            String checksum = stripped.substring(0, space);
            // Strip only the separator whitespace on the left; preserve trailing spaces.
            String relPath = stripped.substring(space + 1).stripLeading();

            if (isWindowsUnsafe(relPath)) {
                droppedPaths.add(relPath);
                continue;
            }

            // Final guard: let Windows Path parser validate
            try {
                // DO NOT normalize or resolve against the filesystem; just validate syntax.
                bagRoot.resolve(relPath);
            } catch (InvalidPathException e) {
                droppedPaths.add(relPath);
                continue;
            }

            outLines.add(checksum + " " + relPath);
            kept++;
        }

        Files.write(manifest, outLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        return new Stats(kept, droppedPaths);
    }

    /**
     * Returns a copy of {@code relPath} with invisible but Windows-unsafe characters in each
     * path segment replaced by a bracketed label so they are readable in log files and on the
     * report page.  Examples:
     *   "data/EntityBook "  →  "data/EntityBook [SPACE]"
     *   "data/file."        →  "data/file[DOT]"
     *   "data/file\u0001"   →  "data/file[U+0001]"
     */
    public static String annotateForDisplay(String relPath) {
        String[] segs = relPath.split("/", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segs.length; i++) {
            if (i > 0) sb.append('/');
            String seg = segs[i];
            if (seg.isEmpty()) { sb.append(seg); continue; }

            // Annotate one or more trailing spaces
            int end = seg.length();
            int trailingSpaces = 0;
            while (trailingSpaces < end && seg.charAt(end - 1 - trailingSpaces) == ' ')
                trailingSpaces++;
            if (trailingSpaces > 0) {
                sb.append(seg, 0, end - trailingSpaces);
                for (int k = 0; k < trailingSpaces; k++) sb.append("[SPACE]");
                continue;
            }

            // Annotate a trailing dot
            if (seg.charAt(end - 1) == '.') {
                sb.append(seg, 0, end - 1);
                sb.append("[DOT]");
                continue;
            }

            // Annotate control characters (invisible in any viewer)
            StringBuilder segBuf = new StringBuilder();
            for (int ci = 0; ci < seg.length(); ci++) {
                char c = seg.charAt(ci);
                if (c <= '\u001F' || c == '\u007F') {
                    segBuf.append(String.format("[U+%04X]", (int) c));
                } else {
                    segBuf.append(c);
                }
            }
            sb.append(segBuf);
        }
        return sb.toString();
    }

    private static int firstWhitespacePos(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    /** Returns true if the given plain filename (not a path) is a Windows reserved device name. */
    public static boolean isWindowsReservedName(String filename) {
        return RESERVED_BASENAMES.matcher(filename).matches();
    }

    private static boolean isWindowsUnsafe(String relPath) {
        if (INVALID_CHARS.matcher(relPath.replace('/',' ')).find()) return true;

        // Each segment: no trailing space/dot; no reserved device names
        String[] segs = relPath.split("[/\\\\]");
        for (String seg : segs) {
            if (seg.isEmpty()) continue;
            char last = seg.charAt(seg.length() - 1);
            if (last == ' ' || last == '.') return true;
            if (RESERVED_BASENAMES.matcher(seg).matches()) return true;
        }
        return false;
    }

    // pick a non-existing backup path next to the manifest
    private static Path uniqueBackupPath(Path manifest) throws IOException {
        Path dir = manifest.getParent().resolve("_backups");
        Files.createDirectories(dir);
        String base = manifest.getFileName().toString();
        String ts = java.time.LocalDateTime.now().toString().replace(':','-');
        Path p = dir.resolve(base + "." + ts + ".bak");
        if (Files.notExists(p)) return p;
        for (int i = 1; i < 10_000; i++) {
            p = dir.resolve(base + "." + ts + ".bak." + i);
            if (Files.notExists(p)) return p;
        }
        throw new IOException("Too many backups exist for " + manifest);
    }

    // NOT IN USE. atomically replace the original file with a temp file
    private static void replaceFileAtomically(Path target, byte[] data) throws IOException {
        Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.write(tmp, data, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING); // safe fallback
            }
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignore) {}
        }
    }
}
