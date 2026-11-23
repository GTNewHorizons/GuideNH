package com.hfstudio.guidenh.client.capture.api;

public interface CaptureRenderProbe {

    CaptureRenderState capture(CaptureContext context, long memoryBudgetBytes);

    boolean shadowPass();

    void reset();
}
