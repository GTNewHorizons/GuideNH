package com.hfstudio.guidenh.guide.internal.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class GuideLocalPathPolicy {

    private final Path rootDirectory;

    public GuideLocalPathPolicy(Path rootDirectory) {
        this.rootDirectory = rootDirectory.toAbsolutePath()
            .normalize();
        if (isNetworkPath(this.rootDirectory)) {
            throw new IllegalArgumentException("A local root directory is required");
        }
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path resolveExisting(Path path) throws IOException {
        Path candidate = resolveForWrite(path);
        Path realRoot = rootDirectory.toRealPath();
        Path realPath = candidate.toRealPath();
        if (!realPath.startsWith(realRoot)) {
            throw new IOException("Local path leaves " + realRoot);
        }
        return realPath;
    }

    public Path resolveForWrite(Path path) throws IOException {
        if (isNetworkPath(path)) {
            throw new IOException("Network paths are not allowed");
        }
        Path candidate = (path.isAbsolute() ? path : rootDirectory.resolve(path)).toAbsolutePath()
            .normalize();
        if (!candidate.startsWith(rootDirectory)) {
            throw new IOException("Local path leaves " + rootDirectory);
        }
        for (Path component : candidate) {
            if (component.toString()
                .indexOf(':') >= 0) {
                throw new IOException("Device and alternate stream paths are not allowed");
            }
        }

        Path current = candidate.getRoot();
        for (Path component : candidate) {
            current = current.resolve(component);
            BasicFileAttributes attributes;
            try {
                attributes = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException e) {
                break;
            }
            if (attributes.isSymbolicLink() || attributes.isOther()) {
                throw new IOException("Linked local paths are not allowed");
            }
        }
        return candidate;
    }

    private static boolean isNetworkPath(Path path) {
        return path.toString()
            .replace('\\', '/')
            .startsWith("//");
    }
}
