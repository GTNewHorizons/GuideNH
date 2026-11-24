package com.hfstudio.guidenh.client.capture.api;

public interface CaptureNavigationState extends AutoCloseable {

    void depart();

    void returning();

    @Override
    void close();
}
