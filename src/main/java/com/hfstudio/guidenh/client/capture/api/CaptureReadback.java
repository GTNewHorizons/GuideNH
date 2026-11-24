package com.hfstudio.guidenh.client.capture.api;

public interface CaptureReadback extends AutoCloseable {

    // Poll and consume only on the rendering thread which submitted this readback.
    boolean ready();

    CaptureFrame take();

    @Override
    void close();
}
