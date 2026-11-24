package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;

public abstract class CaptureBufferPixelReader implements CapturePixelReader {

    private IntBuffer scratch;

    // Fill the buffer with bottom-up ARGB words; backend state must be restored before returning.
    protected abstract void readPixels(int x, int y, int width, int height, IntBuffer pixels);

    @Override
    public CaptureFrame read(int x, int y, int width, int height, long timestampNanos, CaptureAlphaMode alphaMode) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Capture dimensions must be positive");
        int count = Math.multiplyExact(width, height);
        if (scratch == null || scratch.capacity() < count) scratch = BufferUtils.createIntBuffer(count);
        scratch.clear();
        scratch.limit(count);
        readPixels(x, y, width, height, scratch);
        int[] pixels = new int[count];
        for (int row = 0; row < height; row++) {
            scratch.position((height - 1 - row) * width);
            scratch.get(pixels, row * width, width);
        }
        CapturePixelConverter.normalize(pixels, alphaMode);
        return new CaptureFrame(width, height, timestampNanos, pixels);
    }

    @Override
    public void close() {
        scratch = null;
    }
}
