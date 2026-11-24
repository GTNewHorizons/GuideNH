package com.hfstudio.guidenh.client.capture.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CaptureOutputPaths {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");

    private CaptureOutputPaths() {}

    public static Path next(Path directory, String source, String extension) throws IOException {
        Files.createDirectories(directory);
        String label = source.replaceAll("[^\\p{L}\\p{N}_-]", "_");
        if (label.length() > 64) label = label.substring(0, 64);
        String stem = TIMESTAMP.format(LocalDateTime.now()) + "_" + label;
        Path candidate = directory.resolve(stem + "." + extension);
        int suffix = 2;
        while (Files.exists(candidate)) candidate = directory.resolve(stem + "_" + suffix++ + "." + extension);
        return candidate;
    }
}
