package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideSiteAssetRegistry implements AutoCloseable {

    private static final int ASYNC_WRITE_LIMIT = 32;
    private static final int PATH_LOCK_COUNT = 256;
    private final Path outDir;
    private final Object[] pathLocks = new Object[PATH_LOCK_COUNT];
    private final Set<Path> pendingPaths = ConcurrentHashMap.newKeySet();
    private final Set<Path> completedPaths = ConcurrentHashMap.newKeySet();
    private final Set<Path> initializedDirectories = ConcurrentHashMap.newKeySet();
    private final Semaphore pendingWrites = new Semaphore(ASYNC_WRITE_LIMIT);
    private final AtomicReference<Throwable> writeFailure = new AtomicReference<>();
    private final ExecutorService writeExecutor = Executors.newFixedThreadPool(
        Math.max(
            2,
            Math.min(
                4,
                Runtime.getRuntime()
                    .availableProcessors())),
        runnable -> {
            Thread thread = new Thread(runnable, "guidenh-site-asset-write");
            thread.setDaemon(true);
            return thread;
        });
    private final AtomicLong scheduledWrites = new AtomicLong();
    private final AtomicLong completedWrites = new AtomicLong();
    private final AtomicLong encodedBytes = new AtomicLong();
    private final AtomicLong producerNanos = new AtomicLong();
    private final AtomicLong fileWriteNanos = new AtomicLong();

    public GuideSiteAssetRegistry(Path outDir) {
        this.outDir = outDir;
        for (int index = 0; index < pathLocks.length; index++) {
            pathLocks[index] = new Object();
        }
    }

    public String writeShared(String bucket, String extension, byte[] content) throws Exception {
        checkWriteFailure();
        Path relative = sharedPath(bucket, extension, sha256(content));
        scheduleWrite(relative, () -> content);
        return relative.toString()
            .replace('\\', '/');
    }

    public String writeSharedCompressed(String bucket, String extension, byte[] content) throws Exception {
        Path relative = sharedPath(bucket, extension + ".gz", sha256(content));
        scheduleWrite(relative, () -> gzip(content));
        return relative.toString()
            .replace('\\', '/');
    }

    public String writePngAsync(String bucket, BufferedImage image) throws Exception {
        Path relative = sharedPath(bucket, ".png", imageHash(image));
        scheduleWrite(relative, () -> encodePng(image));
        return relative.toString()
            .replace('\\', '/');
    }

    public String writeAnimatedPngAsync(String bucket, List<GuideSiteAnimatedPng.Frame> frames) throws Exception {
        List<GuideSiteAnimatedPng.Frame> snapshot = List.copyOf(frames);
        if (snapshot.size() == 1) return writePngAsync(
            bucket,
            snapshot.getFirst()
                .image());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (GuideSiteAnimatedPng.Frame frame : snapshot) {
            digest.update(imageHash(frame.image()).getBytes(StandardCharsets.US_ASCII));
            digest.update(
                ByteBuffer.allocate(Integer.BYTES)
                    .putInt(frame.ticks())
                    .array());
        }
        Path relative = sharedPath(bucket, ".png", hex(digest.digest()));
        scheduleWrite(relative, () -> GuideSiteAnimatedPng.encode(snapshot));
        return relative.toString()
            .replace('\\', '/');
    }

    private Path sharedPath(String bucket, String extension, String hash) {
        return Paths.get("_res", bucket, hash + extension);
    }

    private Object pathLock(Path relative) {
        return pathLocks[relative.hashCode() & (PATH_LOCK_COUNT - 1)];
    }

    private void ensureDirectory(Path directory) throws IOException {
        if (initializedDirectories.contains(directory)) {
            return;
        }
        synchronized (pathLock(directory)) {
            if (!initializedDirectories.contains(directory)) {
                Files.createDirectories(directory);
                initializedDirectories.add(directory);
            }
        }
    }

    private void scheduleWrite(Path relative, Callable<byte[]> producer) throws Exception {
        checkWriteFailure();
        Path absolute = outDir.resolve(relative);
        if (completedPaths.contains(relative) || pendingPaths.contains(relative)) {
            return;
        }
        pendingWrites.acquire();
        try {
            checkWriteFailure();
        } catch (Exception failure) {
            pendingWrites.release();
            throw failure;
        }
        if (completedPaths.contains(relative) || !pendingPaths.add(relative)) {
            pendingWrites.release();
            return;
        }
        try {
            writeExecutor.execute(() -> {
                long startedAt = System.nanoTime();
                try {
                    byte[] content = producer.call();
                    producerNanos.addAndGet(System.nanoTime() - startedAt);
                    encodedBytes.addAndGet(content.length);
                    long writeStartedAt = System.nanoTime();
                    ensureDirectory(absolute.getParent());
                    synchronized (pathLock(relative)) {
                        if (!completedPaths.contains(relative) && !Files.exists(absolute)) {
                            Files.write(absolute, content);
                        }
                        completedPaths.add(relative);
                    }
                    fileWriteNanos.addAndGet(System.nanoTime() - writeStartedAt);
                    completedWrites.incrementAndGet();
                } catch (Throwable failure) {
                    writeFailure
                        .compareAndSet(null, new IOException("Failed to write site asset " + relative, failure));
                } finally {
                    pendingPaths.remove(relative);
                    pendingWrites.release();
                }
            });
            scheduledWrites.incrementAndGet();
        } catch (RejectedExecutionException failure) {
            pendingPaths.remove(relative);
            pendingWrites.release();
            throw failure;
        }
    }

    private byte[] gzip(byte[] content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output) {

            {
                def.setLevel(Deflater.BEST_SPEED);
            }
        }) {
            gzip.write(content);
        }
        return output.toByteArray();
    }

    private byte[] encodePng(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        if (!writers.hasNext()) {
            throw new IOException("No PNG writer is available for site assets");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            if (parameters.canWriteCompressed()) {
                parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // PNG quality selects compression effort; the pixels remain lossless.
                parameters.setCompressionQuality(0f);
            }
            writer.write(null, new IIOImage(image, null, null), parameters);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }

    String imageHash(BufferedImage image) throws Exception {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB || !(image.getRaster()
            .getDataBuffer() instanceof DataBufferInt data)) {
            throw new IllegalArgumentException("Site image must use TYPE_INT_ARGB");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
        bytes.putInt(image.getWidth());
        bytes.putInt(image.getHeight());
        digest.update(bytes.array(), 0, bytes.position());
        bytes.clear();
        IntBuffer integers = bytes.asIntBuffer();
        int[] pixels = data.getData();
        for (int offset = 0; offset < pixels.length;) {
            int count = Math.min(integers.capacity(), pixels.length - offset);
            integers.clear();
            integers.put(pixels, offset, count);
            digest.update(bytes.array(), 0, count * Integer.BYTES);
            offset += count;
        }
        return hex(digest.digest());
    }

    private void checkWriteFailure() throws IOException {
        Throwable failure = writeFailure.get();
        if (failure != null) {
            throw new IOException("Failed to write a site asset", failure);
        }
    }

    @Override
    public void close() throws Exception {
        writeExecutor.shutdown();
        try {
            writeExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException failure) {
            writeExecutor.shutdownNow();
            Thread.currentThread()
                .interrupt();
            throw failure;
        }
        checkWriteFailure();
        GuideDebugLog.infoAlways(
            "[GuideNH] [GuideSiteAssetRegistry] writes scheduled={}, completed={}, encodedBytes={}, producerTimeMs={}, fileWriteTimeMs={}",
            scheduledWrites.get(),
            completedWrites.get(),
            encodedBytes.get(),
            TimeUnit.NANOSECONDS.toMillis(producerNanos.get()),
            TimeUnit.NANOSECONDS.toMillis(fileWriteNanos.get()));
    }

    private String sha256(byte[] content) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(content);
        return hex(digest);
    }

    private String hex(byte[] digest) {
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }
}
