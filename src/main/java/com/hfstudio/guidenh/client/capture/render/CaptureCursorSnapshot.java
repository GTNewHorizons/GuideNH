package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;

public class CaptureCursorSnapshot implements CaptureRenderSnapshot {

    private final CaptureRenderSnapshot delegate;
    private final int x;
    private final int y;

    public CaptureCursorSnapshot(CaptureRenderSnapshot delegate, int x, int y) {
        this.delegate = delegate;
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw() {
        delegate.draw();
        CaptureCursorRenderer.draw(x, y);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
