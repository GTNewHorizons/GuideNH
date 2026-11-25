package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryReservation;
import com.hfstudio.guidenh.client.capture.api.CapturePixelBuffer;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;

public abstract class CaptureBufferPixelReader implements CapturePixelReader {

    private IntBuffer scratch;
    private CaptureMemoryAccount memory;
    private CaptureMemoryReservation scratchReservation;
    private boolean started;

    @Override
    public void memoryAccount(CaptureMemoryAccount account) {
        if (started && memory != account)
            throw new IllegalStateException("A live pixel reader cannot change its allocation budget");
        memory = account;
    }

    @Override
    public CaptureMemoryAccount memoryAccount() {
        return memory;
    }

    protected void beginRead() {
        started = true;
    }

    // Fill the buffer with bottom-up ARGB words; backend state must be restored before returning.
    protected abstract void readPixels(int x, int y, int width, int height, IntBuffer pixels);

    @Override
    public CaptureFrame read(int x, int y, int width, int height, long timestampNanos, CaptureAlphaMode alphaMode) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Capture dimensions must be positive");
        int count = Math.multiplyExact(width, height);
        int bytes = Math.multiplyExact(count, Integer.BYTES);
        beginRead();
        if (scratch == null || scratch.capacity() < count) {
            scratch = null;
            if (scratchReservation != null) scratchReservation.close();
            scratchReservation = null;
            CaptureMemoryReservation reservation = new CaptureMemoryReservation(memory, bytes);
            try {
                scratch = BufferUtils.createIntBuffer(count);
                scratchReservation = reservation;
            } catch (RuntimeException | Error failure) {
                CaptureCleanup cleanup = new CaptureCleanup(failure);
                cleanup.close(reservation);
                throw failure;
            }
        }
        scratch.clear();
        scratch.limit(count);
        CapturePixelBuffer buffer = CapturePixelBuffer.allocate(count, memory);
        try {
            readPixels(x, y, width, height, scratch);
            int[] pixels = buffer.array();
            for (int row = 0; row < height; row++) {
                scratch.position((height - 1 - row) * width);
                scratch.get(pixels, row * width, width);
            }
            CapturePixelConverter.normalize(pixels, alphaMode);
            return CaptureFrame.take(width, height, timestampNanos, buffer);
        } catch (RuntimeException | Error failure) {
            CaptureCleanup cleanup = new CaptureCleanup(failure);
            cleanup.close(buffer);
            throw failure;
        }
    }

    @Override
    public void close() {
        scratch = null;
        started = false;
        CaptureMemoryReservation previous = scratchReservation;
        scratchReservation = null;
        if (previous != null) previous.close();
    }
}
