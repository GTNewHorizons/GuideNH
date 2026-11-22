package com.hfstudio.guidenh.client.capture.api;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class CaptureFrame implements AutoCloseable {

    private final int width;
    private final int height;
    private final long timestampNanos;
    private int[] pixels;

    public CaptureFrame(int width, int height, long timestampNanos, int[] pixels) {
        if (pixels.length != Math.multiplyExact(width, height)) {
            throw new IllegalArgumentException("Pixel count does not match the capture dimensions");
        }
        this.width = width;
        this.height = height;
        this.timestampNanos = timestampNanos;
        this.pixels = pixels;
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
    public void close() {
        pixels = null;
    }
}
