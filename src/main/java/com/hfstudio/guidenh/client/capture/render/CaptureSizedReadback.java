package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureSizedReadback implements CaptureReadback {

    private final CaptureReadback delegate;
    private final int width;
    private final int height;

    public CaptureSizedReadback(CaptureReadback delegate, CaptureViewport viewport) {
        this.delegate = delegate;
        width = viewport.pixelWidth();
        height = viewport.pixelHeight();
    }

    @Override
    public boolean ready() {
        return delegate.ready();
    }

    @Override
    public CaptureFrame take() {
        CaptureFrame frame = delegate.take();
        if (frame.width() != width || frame.height() != height) {
            frame.close();
            throw new IllegalStateException("The readback dimensions differ from the submitted viewport");
        }
        return frame;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
