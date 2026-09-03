package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;
import java.util.zip.ZipFile;

/**
 * Lightweight content inventory used to decide whether a resource-pack scan can be reused.
 * The inventory is only a cache key. GuideNH entries in directory packs include a streamed
 * content checksum so a same-size edit (or an edit with a preserved timestamp) still invalidates
 * the scan cache. Other assets keep metadata only because their loading is owned by Minecraft's
 * resource manager, while no per-file objects are retained after the scan.
 */
public class ResourcePackContentManifest {

    /**
     * Compact cache key for a resource pack. The scanner still visits every file, but the
     * resulting cache retains only a count and an order-independent fingerprint instead of one
     * object (and path string) per file.
     */
    public record Pack(File root, boolean directory, int entryCount, long fingerprint) {}

    public static List<Pack> capture(List<File> roots, String guideFolder) {
        var result = new ArrayList<Pack>(roots.size());
        for (File root : roots) {
            result.add(root.isDirectory() ? captureDirectory(root, guideFolder) : captureZip(root));
        }
        return List.copyOf(result);
    }

    public static Pack captureDirectory(File root, String guideFolder) {
        Path absoluteRoot = root.toPath()
            .toAbsolutePath()
            .normalize();
        var hashes = new LongHashBuffer();

        collectFiles(absoluteRoot.resolve("assets"), absoluteRoot, hashes);
        try (var children = Files.list(absoluteRoot)) {
            children.filter(Files::isDirectory)
                .filter(
                    path -> !"assets".equals(
                        path.getFileName()
                            .toString()))
                .forEach(path -> collectFiles(path.resolve(guideFolder), absoluteRoot, hashes));
        } catch (IOException e) {
            hashes.add(entryHash("<unreadable-root>", -1L, -1L, -1L));
        }

        return new Pack(root, true, hashes.size(), hashes.fingerprint());
    }

    public static void collectFiles(Path directory, Path root, LongHashBuffer hashes) {
        if (!Files.isDirectory(directory)) return;
        byte[] contentBuffer = new byte[16 * 1024];
        try (var files = Files.walk(directory)) {
            files.filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        var attributes = Files.readAttributes(path, BasicFileAttributes.class);
                        String relativePath = root.relativize(path)
                            .toString()
                            .replace(File.separatorChar, '/');
                        long contentHash = shouldHashContent(relativePath) ? contentChecksum(path, contentBuffer) : -1L;
                        hashes.add(
                            entryHash(
                                relativePath,
                                attributes.size(),
                                attributes.lastModifiedTime()
                                    .toMillis(),
                                contentHash));
                    } catch (IOException e) {
                        String relativePath = root.relativize(path)
                            .toString()
                            .replace(File.separatorChar, '/');
                        hashes.add(entryHash(relativePath, -1L, -1L, -1L));
                    }
                });
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
        var hashes = new LongHashBuffer();
        try (var zip = new ZipFile(root)) {
            var zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                var entry = zipEntries.nextElement();
                if (!entry.isDirectory() && entry.getName()
                    .startsWith("assets/")) {
                    hashes.add(entryHash(entry.getName(), entry.getSize(), entry.getTime(), entry.getCrc()));
                }
            }
        } catch (IOException e) {
            hashes.add(entryHash("<unreadable-zip>", -1L, -1L, -1L));
        }
        return new Pack(root, false, hashes.size(), hashes.fingerprint());
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

    private static boolean isGuideRelevantPath(String path) {
        return path.endsWith(".lang") || path.contains("/guidenh/") || path.endsWith("/guidenh");
    }

    private static boolean shouldHashContent(String path) {
        if (isGuideRelevantPath(path)) {
            return true;
        }
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png") || lower.endsWith(".jpg")
            || lower.endsWith(".jpeg")
            || lower.endsWith(".gif")
            || lower.endsWith(".webp")
            || lower.endsWith(".mcmeta")
            || lower.endsWith(".json")
            || lower.endsWith(".snbt")
            || lower.endsWith(".nbt");
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
