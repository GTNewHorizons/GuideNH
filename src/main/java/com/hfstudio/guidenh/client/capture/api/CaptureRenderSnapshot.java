package com.hfstudio.guidenh.client.capture.api;

public interface CaptureRenderSnapshot extends AutoCloseable {

    default boolean filtersContent() {
        return false;
    }

    void draw();

    default boolean includesHud() {
        return false;
    }

    @Override
    default void close() {}
}
