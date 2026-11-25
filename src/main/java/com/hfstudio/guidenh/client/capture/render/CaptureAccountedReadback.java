package com.hfstudio.guidenh.client.capture.render;

import java.util.Objects;

import com.hfstudio.guidenh.client.capture.CaptureCleanup;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureMemoryAccount;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;

public class CaptureAccountedReadback implements CaptureReadback {

    private final CaptureReadback delegate;
    private final CaptureMemoryAccount memory;

    public CaptureAccountedReadback(CaptureReadback delegate, CaptureMemoryAccount memory) {
        this.delegate = Objects.requireNonNull(delegate);
        this.memory = memory;
    }

    @Override
    public boolean ready() {
        return delegate.ready();
    }

    @Override
    public CaptureFrame take() {
        return account(delegate.take(), memory);
    }

    public static CaptureFrame account(CaptureFrame frame, CaptureMemoryAccount memory) {
        Objects.requireNonNull(frame);
        try {
            return frame.account(memory);
        } catch (RuntimeException | Error failure) {
            CaptureCleanup cleanup = new CaptureCleanup(failure);
            cleanup.close(frame);
            throw failure;
        }
    }

    @Override
    public void close() {
        delegate.close();
    }
}
