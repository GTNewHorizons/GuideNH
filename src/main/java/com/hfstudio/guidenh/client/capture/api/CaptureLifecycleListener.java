package com.hfstudio.guidenh.client.capture.api;

public interface CaptureLifecycleListener {

    void opened(CaptureContext context);

    void closed(CaptureContext context);
}
