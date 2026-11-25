package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class CaptureTemporaryFiles extends SimpleFileVisitor<Path> {

    private CaptureTemporaryFiles() {}

    public static void remove(Path path) throws IOException {
        Files.walkFileTree(path, new CaptureTemporaryFiles());
    }

    @Override
    public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
        if (!attributes.isOther()) return FileVisitResult.CONTINUE;
        // Windows directory junctions are reparse points rather than symbolic links.
        Files.deleteIfExists(directory);
        return FileVisitResult.SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException error) throws IOException {
        if (error instanceof NoSuchFileException) return FileVisitResult.CONTINUE;
        throw error;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
        if (error != null) throw error;
        Files.deleteIfExists(directory);
        return FileVisitResult.CONTINUE;
    }
}
