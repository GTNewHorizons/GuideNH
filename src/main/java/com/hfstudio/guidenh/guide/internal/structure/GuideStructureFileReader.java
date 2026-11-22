package com.hfstudio.guidenh.guide.internal.structure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import com.hfstudio.guidenh.guide.internal.resource.GuideLocalPathPolicy;

public class GuideStructureFileReader {

    public static final int DEFAULT_MAX_BYTES = 8 * 1024 * 1024;

    private final GuideLocalPathPolicy paths;
    private final int maxBytes;

    public GuideStructureFileReader(Path rootDirectory, int maxBytes) {
        this.paths = new GuideLocalPathPolicy(rootDirectory);
        if (maxBytes <= 0 || maxBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("A local structure directory and a bounded byte limit are required");
        }
        this.maxBytes = maxBytes;
    }

    public static GuideStructureFileReader createDefault() {
        return new GuideStructureFileReader(Paths.get("config", "guidenh", "structures"), DEFAULT_MAX_BYTES);
    }

    public Path getRootDirectory() {
        return paths.getRootDirectory();
    }

    public Path resolve(Path path) throws IOException {
        if (!isStructureFile(path)) {
            throw new IOException("Import requires a .snbt file inside " + getRootDirectory());
        }
        Path realPath = paths.resolveExisting(path);
        if (!isStructureFile(realPath) || !Files.isRegularFile(realPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Import requires a regular .snbt file");
        }
        return realPath;
    }

    public String read(Path path) throws IOException {
        Path resolved = resolve(path);
        byte[] bytes;
        try (InputStream input = Files.newInputStream(resolved, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(maxBytes + 1);
        }
        if (bytes.length > maxBytes) {
            throw new IOException("Structure file exceeds " + maxBytes + " bytes");
        }
        return StandardCharsets.UTF_8.newDecoder()
            .decode(ByteBuffer.wrap(bytes))
            .toString();
    }

    private static boolean isStructureFile(Path path) {
        return path.getFileName() != null && path.getFileName()
            .toString()
            .toLowerCase(Locale.ROOT)
            .endsWith(".snbt");
    }

}
