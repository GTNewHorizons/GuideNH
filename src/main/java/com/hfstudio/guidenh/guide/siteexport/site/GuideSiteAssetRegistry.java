package com.hfstudio.guidenh.guide.siteexport.site;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class GuideSiteAssetRegistry implements AutoCloseable {

    private static final int ASYNC_WRITE_LIMIT = 8;
    private static final int PATH_LOCK_COUNT = 256;
    private final Path outDir;
    private final Object[] pathLocks = new Object[PATH_LOCK_COUNT];
    private final Set<Path> pendingPaths = ConcurrentHashMap.newKeySet();
    private final Semaphore pendingWrites = new Semaphore(ASYNC_WRITE_LIMIT);
    private final AtomicReference<Throwable> writeFailure = new AtomicReference<>();
    private final ExecutorService writeExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "guidenh-site-asset-write");
        thread.setDaemon(true);
        return thread;
    });

    public GuideSiteAssetRegistry(Path outDir) {
        this.outDir = outDir;
        for (int index = 0; index < pathLocks.length; index++) {
            pathLocks[index] = new Object();
        }
    }

    public String writeShared(String bucket, String extension, byte[] content) throws Exception {
        Path relative = sharedPath(bucket, extension, sha256(content));
        Path absolute = outDir.resolve(relative);
        synchronized (pathLock(relative)) {
            Files.createDirectories(absolute.getParent());
            if (!Files.exists(absolute)) {
                Files.write(absolute, content);
            }
        }
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
        Path relative = sharedPath(bucket, ".png", sha256Image(image));
        scheduleWrite(relative, () -> encodePng(image));
        return relative.toString()
            .replace('\\', '/');
    }

    private Path sharedPath(String bucket, String extension, String hash) {
        return Paths.get("_res", bucket, hash + extension);
    }

    private Object pathLock(Path relative) {
        return pathLocks[relative.hashCode() & (PATH_LOCK_COUNT - 1)];
    }

    private void scheduleWrite(Path relative, Callable<byte[]> producer) throws Exception {
        checkWriteFailure();
        Path absolute = outDir.resolve(relative);
        if (Files.exists(absolute) || pendingPaths.contains(relative)) {
            return;
        }
        pendingWrites.acquire();
        if (Files.exists(absolute) || !pendingPaths.add(relative)) {
            pendingWrites.release();
            return;
        }
        try {
            writeExecutor.execute(() -> {
                try {
                    byte[] content = producer.call();
                    synchronized (pathLock(relative)) {
                        Files.createDirectories(absolute.getParent());
                        if (!Files.exists(absolute)) {
                            Files.write(absolute, content);
                        }
                    }
                } catch (Throwable failure) {
                    writeFailure
                        .compareAndSet(null, new IOException("Failed to write site asset " + relative, failure));
                } finally {
                    pendingPaths.remove(relative);
                    pendingWrites.release();
                }
            });
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
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream imageOutput = new MemoryCacheImageOutputStream(output)) {
            if (!ImageIO.write(image, "png", imageOutput)) {
                throw new IOException("No PNG writer is available for site assets");
            }
        }
        return output.toByteArray();
    }

    private String sha256Image(BufferedImage image) throws Exception {
        if (image.getType() != BufferedImage.TYPE_INT_ARGB || !(image.getRaster()
            .getDataBuffer() instanceof DataBufferInt data)) {
            throw new IllegalArgumentException("Site image must use TYPE_INT_ARGB");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        ByteBuffer bytes = ByteBuffer.allocate(4096);
        bytes.putInt(image.getWidth());
        bytes.putInt(image.getHeight());
        for (int pixel : data.getData()) {
            if (bytes.remaining() < Integer.BYTES) {
                digest.update(bytes.array(), 0, bytes.position());
                bytes.clear();
            }
            bytes.putInt(pixel);
        }
        digest.update(bytes.array(), 0, bytes.position());
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
