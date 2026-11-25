package com.hfstudio.guidenh.client.capture.render;

import java.util.Objects;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;

public class CaptureAccountedPixelReader implements CapturePixelReader {

    private final CapturePixelReader delegate;
    private final CaptureMemoryAccount memory;

    public CaptureAccountedPixelReader(CapturePixelReader delegate, CaptureMemoryAccount memory) {
        this.delegate = Objects.requireNonNull(delegate);
        this.memory = Objects.requireNonNull(memory);
        delegate.memoryAccount(memory);
    }

    @Override
    public void memoryAccount(CaptureMemoryAccount account) {
        if (account != memory) throw new IllegalStateException("The pixel reader already belongs to another budget");
    }

    @Override
    public CaptureMemoryAccount memoryAccount() {
        return memory;
    }

    @Override
    public CaptureFrame read(int x, int y, int width, int height, long timestamp, CaptureAlphaMode alpha) {
        return CaptureAccountedReadback.account(delegate.read(x, y, width, height, timestamp, alpha), memory);
    }

    @Override
    public CaptureReadback enqueue(int x, int y, int width, int height, long timestamp, CaptureAlphaMode alpha,
        long budget) {
        return new CaptureAccountedReadback(delegate.enqueue(x, y, width, height, timestamp, alpha, budget), memory);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
