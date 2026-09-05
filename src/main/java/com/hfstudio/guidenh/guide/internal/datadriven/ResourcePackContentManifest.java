package com.hfstudio.guidenh.guide.internal.datadriven;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * Lightweight content inventory used to decide whether a resource-pack scan can be reused.
 * The inventory is only a cache key. Directory packs include a streamed checksum for GuideNH
 * files and language files, so a same-size edit (or an edit with a preserved timestamp) still
 * invalidates the scan cache. ZIP packs need only immutable archive metadata: their internal
 * entries are never retained or enumerated for this purpose.
 */
public class ResourcePackContentManifest {

    /**
     * Compact cache key for a resource pack. Directory packs retain only an order-independent
     * fingerprint. ZIP packs retain only their normalized path and archive metadata.
     */
    public record Pack(String path, boolean directory, long size, long lastModified, int entryCount, long fingerprint) {

        public boolean matchesZip(File root) {
            return !directory && !root.isDirectory()
                && path.equals(normalizePath(root))
                && size == root.length()
                && lastModified == root.lastModified();
        }
    }

    public static List<Pack> capture(List<File> roots, String guideFolder) {
        return capture(roots, guideFolder, List.of());
    }

    /**
     * Reuses the immutable ZIP metadata from the last scan. Directory packs remain content-aware
     * and are rescanned on every reload because changing a child does not reliably update the
     * directory timestamp across filesystems.
     */
    public static List<Pack> capture(List<File> roots, String guideFolder, List<Pack> previous) {
        var result = new ArrayList<Pack>(roots.size());
        for (int index = 0; index < roots.size(); index++) {
            File root = roots.get(index);
            Pack cached = index < previous.size() ? previous.get(index) : null;
            if (!root.isDirectory() && cached != null && cached.matchesZip(root)) {
                result.add(cached);
            } else {
                result.add(root.isDirectory() ? captureDirectory(root, guideFolder) : captureZip(root));
            }
        }
        return List.copyOf(result);
    }

    public static Pack captureDirectory(File root, String guideFolder) {
        Path absoluteRoot = root.toPath()
            .toAbsolutePath()
            .normalize();
        var hashes = new LongHashBuffer();

        Path assets = absoluteRoot.resolve("assets");
        collectAssets(assets, absoluteRoot, guideFolder, hashes);
        try (var children = Files.list(absoluteRoot)) {
            children.filter(Files::isDirectory)
                .filter(
                    path -> !"assets".equals(
                        path.getFileName()
                            .toString()))
                .forEach(path -> collectNativeNamespace(path, absoluteRoot, guideFolder, hashes));
        } catch (IOException e) {
            hashes.add(entryHash("<unreadable-root>", -1L, -1L, -1L));
        }

        return new Pack(
            normalizePath(root),
            true,
            root.length(),
            root.lastModified(),
            hashes.size(),
            hashes.fingerprint());
    }

    private static void collectAssets(Path assets, Path root, String guideFolder, LongHashBuffer hashes) {
        if (!Files.isDirectory(assets)) return;
        try (var namespaces = Files.list(assets)) {
            namespaces.filter(Files::isDirectory)
                .forEach(namespace -> {
                    collectFiles(namespace.resolve(guideFolder), root, hashes);
                    collectLangFiles(namespace.resolve("lang"), root, hashes);
                });
        } catch (IOException e) {
            hashes.add(entryHash("assets/<unreadable>", -1L, -1L, -1L));
        }
    }

    private static void collectNativeNamespace(Path namespace, Path root, String guideFolder, LongHashBuffer hashes) {
        collectFiles(namespace.resolve(guideFolder), root, hashes);
        if (guideFolder.equals(
            namespace.getFileName()
                .toString())) {
            collectFiles(namespace, root, hashes);
        }
        collectLangFiles(namespace.resolve("lang"), root, hashes);
    }

    private static void collectLangFiles(Path directory, Path root, LongHashBuffer hashes) {
        if (!Files.isDirectory(directory)) return;
        byte[] contentBuffer = new byte[16 * 1024];
        try (var files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                .filter(
                    path -> path.getFileName()
                        .toString()
                        .endsWith(".lang"))
                .forEach(path -> addFile(path, root, hashes, contentBuffer));
        } catch (IOException e) {
            hashes.add(
                entryHash(
                    root.relativize(directory)
                        .toString()
                        .replace(File.separatorChar, '/') + "/<unreadable>",
                    -1L,
                    -1L,
                    -1L));
        }
    }

    public static void collectFiles(Path directory, Path root, LongHashBuffer hashes) {
        if (!Files.isDirectory(directory)) return;
        byte[] contentBuffer = new byte[16 * 1024];
        try (var files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                .forEach(path -> addFile(path, root, hashes, contentBuffer));
        } catch (IOException e) {
            hashes.add(
                entryHash(
                    root.relativize(directory)
                        .toString()
                        .replace(File.separatorChar, '/') + "/<unreadable>",
                    -1L,
                    -1L,
                    -1L));
        }
    }

    public static Pack captureZip(File root) {
        return new Pack(normalizePath(root), false, root.length(), root.lastModified(), 0, 0L);
    }

    public static long entryHash(String path, long size, long modified, long crc) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < path.length(); i++) {
            hash ^= path.charAt(i);
            hash *= 0x100000001b3L;
        }
        hash ^= size;
        hash *= 0x100000001b3L;
        hash ^= modified;
        hash *= 0x100000001b3L;
        hash ^= crc;
        hash *= 0x100000001b3L;
        return mix64(hash);
    }

    public static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static long contentChecksum(Path path, byte[] buffer) {
        CRC32 crc = new CRC32();
        try (var input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    crc.update(buffer, 0, read);
                }
            }
            return crc.getValue();
        } catch (IOException e) {
            return -1L;
        }
    }

    private static void addFile(Path path, Path root, LongHashBuffer hashes, byte[] contentBuffer) {
        String relativePath = root.relativize(path)
            .toString()
            .replace(File.separatorChar, '/');
        try {
            var attributes = Files.readAttributes(path, BasicFileAttributes.class);
            hashes.add(
                entryHash(
                    relativePath,
                    attributes.size(),
                    attributes.lastModifiedTime()
                        .toMillis(),
                    contentChecksum(path, contentBuffer)));
        } catch (IOException e) {
            hashes.add(entryHash(relativePath, -1L, -1L, -1L));
        }
    }

    private static String normalizePath(File root) {
        return root.toPath()
            .toAbsolutePath()
            .normalize()
            .toString();
    }

    /** Small primitive buffer used only during a scan; no per-entry objects survive the scan. */
    public static final class LongHashBuffer {

        public int size;
        public long sum;
        public long xor;

        public void add(long value) {
            size++;
            sum += value;
            xor ^= value;
        }

        public int size() {
            return size;
        }

        public long fingerprint() {
            // The aggregate is independent of filesystem/ZIP enumeration order. Combining both
            // sum and xor keeps accidental collisions unlikely without retaining all entries.
            return mix64(sum ^ Long.rotateLeft(xor, 17) ^ ((long) size * 0x9e3779b97f4a7c15L));
        }
    }
}
