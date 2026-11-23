package com.hfstudio.guidenh.client.capture.render;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderProbe;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderState;

public class CaptureTracingPixelReader implements CapturePixelReader {

    private final CapturePixelReader delegate;
    private final CaptureRenderProbe probe;
    private final CaptureContext context;
    private final long budget;
    private CaptureRenderState target;

    public CaptureTracingPixelReader(CapturePixelReader delegate, CaptureRenderProbe probe, CaptureContext context,
        long budget) {
        this.delegate = delegate;
        this.probe = probe;
        this.context = context;
        this.budget = budget;
    }

    @Override
    public CaptureFrame read(int x, int y, int width, int height, long timestamp, CaptureAlphaMode alphaMode) {
        target = probe.capture(context, budget);
        return delegate.read(x, y, width, height, timestamp, alphaMode);
    }

    public CaptureRenderState target() {
        return target;
    }

    @Override
    public void close() {}
}
