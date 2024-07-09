package edu.stanford.muse.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class FolderMerger {

    public static void merge(String sourceFolder, String targetFolder) throws IOException {
        merge(sourceFolder,targetFolder, null);
    }

    public static void merge(String sourceFolder, String targetFolder, Set<String> excludedFolders) throws IOException {
        Path sourceRoot = Paths.get(sourceFolder);
        Path targetRoot = Paths.get(targetFolder);

        try {
            Files.walkFileTree(sourceRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = sourceRoot.relativize(dir);
                    Path targetDir = targetRoot.resolve(relativePath);
                    if (excludedFolders != null && excludedFolders.contains(dir.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = sourceRoot.relativize(file);
                    Path targetFile = targetRoot.resolve(relativePath);
                    if (Files.exists(targetFile)) {
                        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-").replace("Z", "");
                        String fileName = targetFile.getFileName().toString();
                        int dotIndex = fileName.lastIndexOf('.');
                        String newFileName;
                        if (dotIndex == -1) {
                            newFileName = fileName + "_" + timestamp;
                        } else {
                            newFileName = fileName.substring(0, dotIndex) + "_" + timestamp + fileName.substring(dotIndex);
                        }
                        targetFile = targetFile.getParent().resolve(newFileName);
                    }
                    Files.copy(file, targetFile);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}