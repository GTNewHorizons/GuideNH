package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.export.CaptureRowComposer;

public class CaptureWorldSnapshot implements AutoCloseable {

    private final int width;
    private final int height;
    private int[] pixels;

    // Take ownership of an unpublished pixel buffer for one composition.
    public CaptureWorldSnapshot(int width, int height, int[] pixels) {
        if (width <= 0 || height <= 0 || pixels.length != Math.multiplyExact(width, height))
            throw new IllegalArgumentException("The world pixel count must match its dimensions");
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public CaptureFrame compose(CaptureFrame gui) {
        if (pixels == null) throw new IllegalStateException("The world snapshot has already been consumed");
        if (gui.width() != width || gui.height() != height)
            throw new IllegalArgumentException("The GUI and world dimensions must match");
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++)
                pixels[offset + x] = CaptureRowComposer.composite(gui.argb(x, y), pixels[offset + x]);
        }
        CaptureFrame composed = new CaptureFrame(width, height, gui.timestampNanos(), pixels);
        pixels = null;
        return composed;
    }

    @Override
    public void close() {
        pixels = null;
    }
}
