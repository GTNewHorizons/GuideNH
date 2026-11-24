package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.cursor.CaptureCursorImage;

public class CaptureCursorSnapshot implements CaptureRenderSnapshot {

    private final CaptureRenderSnapshot delegate;
    private final CaptureCursorRenderer cursor;
    private final CaptureViewport viewport;
    private final double x;
    private final double y;

    public CaptureCursorSnapshot(CaptureRenderSnapshot delegate, CaptureCursorImage image, CaptureViewport viewport,
        double x, double y) {
        this.delegate = delegate;
        cursor = new CaptureCursorRenderer(image);
        this.viewport = viewport;
        this.x = x;
        this.y = y;
    }

    @Override
    public void draw() {
        delegate.draw();
        cursor.draw(viewport, x, y);
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } finally {
            cursor.close();
        }
    }

    @Override
    public boolean includesHud() {
        return delegate.includesHud();
    }

    @Override
    public boolean filtersContent() {
        return delegate.filtersContent();
    }
}
