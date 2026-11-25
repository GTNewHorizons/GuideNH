package com.hfstudio.guidenh.client.capture.api;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class CaptureFrame implements AutoCloseable {

    private final int width;
    private final int height;
    private final long timestampNanos;
    private final CapturePixelBuffer buffer;
    private int[] pixels;

    public CaptureFrame(int width, int height, long timestampNanos, int[] pixels) {
        if (width < 1 || height < 1 || pixels == null)
            throw new IllegalArgumentException("Capture frames require positive dimensions and pixel data");
        if (pixels.length != Math.multiplyExact(width, height)) {
            throw new IllegalArgumentException("Pixel count does not match the capture dimensions");
        }
        this.width = width;
        this.height = height;
        this.timestampNanos = timestampNanos;
        this.pixels = pixels;
        buffer = new CapturePixelBuffer(pixels);
    }

    private CaptureFrame(int width, int height, long timestampNanos, CapturePixelBuffer buffer) {
        this.width = width;
        this.height = height;
        this.timestampNanos = timestampNanos;
        this.buffer = buffer;
        pixels = buffer.array();
    }

    public static CaptureFrame take(int width, int height, long timestampNanos, CapturePixelBuffer buffer) {
        if (width < 1 || height < 1 || buffer.array().length != Math.multiplyExact(width, height))
            throw new IllegalArgumentException("Pixel count does not match the capture dimensions");
        return new CaptureFrame(width, height, timestampNanos, buffer);
    }

    public synchronized CaptureFrame account(CaptureMemoryAccount account) {
        if (pixels == null) throw new IllegalStateException("A released capture frame cannot be accounted");
        buffer.account(account);
        return this;
    }

    public CaptureMemoryAccount memoryAccount() {
        return buffer.memoryAccount();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public long timestampNanos() {
        return timestampNanos;
    }

    public long byteSize() {
        return (long) width * height * 4;
    }

    public synchronized CaptureFrame retain() {
        if (pixels == null) throw new IllegalStateException("A released capture frame cannot be retained");
        buffer.retain();
        try {
            return new CaptureFrame(width, height, timestampNanos, buffer);
        } catch (RuntimeException | Error failure) {
            buffer.close();
            throw failure;
        }
    }

    public int argb(int x, int y) {
        return pixels[y * width + x];
    }

    public void copyRow(int y, int x, int length, int[] target, int offset) {
        System.arraycopy(pixels, y * width + x, target, offset, length);
    }

    public BufferedImage toImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] target = ((DataBufferInt) image.getRaster()
            .getDataBuffer()).getData();
        System.arraycopy(pixels, 0, target, 0, pixels.length);
        return image;
    }

    @Override
    public synchronized void close() {
        if (pixels == null) return;
        pixels = null;
        buffer.close();
    }
}
