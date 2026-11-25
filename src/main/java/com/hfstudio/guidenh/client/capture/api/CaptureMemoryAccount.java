package com.hfstudio.guidenh.client.capture.api;

public interface CaptureMemoryAccount {

    void reserve(long bytes);

    void release(long bytes);

    long available();
}
