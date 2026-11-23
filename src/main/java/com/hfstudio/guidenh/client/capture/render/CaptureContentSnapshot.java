package com.hfstudio.guidenh.client.capture.render;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderSnapshot;
import com.hfstudio.guidenh.client.capture.geometry.CaptureContentGeometry;

public class CaptureContentSnapshot implements CaptureRenderSnapshot {

    private final CaptureRenderSnapshot delegate;
    private final boolean includeGui;
    private final List<CaptureRect> masks;

    public CaptureContentSnapshot(CaptureRenderSnapshot delegate, CaptureGeometry geometry,
        CaptureContentOptions content) {
        this.delegate = delegate;
        includeGui = content.includeGui();
        masks = CaptureContentGeometry.inventoryMask(geometry, content);
    }

    @Override
    public void draw() {
        if (!includeGui) return;
        if (masks.isEmpty()) {
            delegate.draw();
            return;
        }
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            delegate.draw();
        }
        CaptureLayerMask.clear(masks);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
