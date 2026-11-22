package com.hfstudio.guidenh.client.capture.api;

public interface CaptureRenderSnapshot extends AutoCloseable {

    void draw();

    @Override
    default void close() {}
}
