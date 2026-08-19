package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Lightweight content inventory used to decide whether a resource-pack scan can be reused.
 * Metadata is sufficient here because the inventory is only a cache key; a changed file normally
 * changes either its size or modification timestamp, while ZIP entries expose a CRC directly.
 */
final class ResourcePackContentManifest {

    private record Entry(String path, long size, long modified, long crc) {}

    record Pack(File root, boolean directory, List<Entry> entries) {}

    private ResourcePackContentManifest() {}

    static List<Pack> capture(List<File> roots, String guideFolder) {
        var result = new ArrayList<Pack>(roots.size());
        for (File root : roots) {
            result.add(root.isDirectory() ? captureDirectory(root, guideFolder) : captureZip(root));
        }
        return List.copyOf(result);
    }

    private static Pack captureDirectory(File root, String guideFolder) {
        Path absoluteRoot = root.toPath()
            .toAbsolutePath()
            .normalize();
        var entries = new ArrayList<Entry>();

        collectFiles(absoluteRoot.resolve("assets"), absoluteRoot, entries);
        try (var children = Files.list(absoluteRoot)) {
            children.filter(Files::isDirectory)
                .filter(
                    path -> !"assets".equals(
                        path.getFileName()
                            .toString()))
                .forEach(path -> collectFiles(path.resolve(guideFolder), absoluteRoot, entries));
        } catch (IOException e) {
            entries.add(new Entry("<unreadable-root>", -1L, -1L, -1L));
        }

        entries.sort(Comparator.comparing(Entry::path));
        return new Pack(root, true, List.copyOf(entries));
    }

    private static void collectFiles(Path directory, Path root, List<Entry> entries) {
        if (!Files.isDirectory(directory)) return;
        try (var files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        var attributes = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
                        entries.add(
                            new Entry(
                                root.relativize(path)
                                    .toString()
                                    .replace(File.separatorChar, '/'),
                                attributes.size(),
                                attributes.lastModifiedTime()
                                    .toMillis(),
                                -1L));
                    } catch (IOException e) {
                        entries.add(
                            new Entry(
                                root.relativize(path)
                                    .toString()
                                    .replace(File.separatorChar, '/'),
                                -1L,
                                -1L,
                                -1L));
                    }
                });
        } catch (IOException e) {
            entries.add(
                new Entry(
                    root.relativize(directory)
                        .toString()
                        .replace(File.separatorChar, '/') + "/<unreadable>",
                    -1L,
                    -1L,
                    -1L));
        }
    }

    private static Pack captureZip(File root) {
        var entries = new ArrayList<Entry>();
        try (var zip = new ZipFile(root)) {
            var zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                var entry = zipEntries.nextElement();
                if (!entry.isDirectory() && entry.getName()
                    .startsWith("assets/")) {
                    entries.add(new Entry(entry.getName(), entry.getSize(), entry.getTime(), entry.getCrc()));
                }
            }
        } catch (IOException e) {
            entries.add(new Entry("<unreadable-zip>", -1L, -1L, -1L));
        }
        entries.sort(Comparator.comparing(Entry::path));
        return new Pack(root, false, List.copyOf(entries));
    }
}
