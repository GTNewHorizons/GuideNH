package com.hfstudio.guidenh.client.capture.api;

public record CaptureRecordedFrame(CaptureFrame frame, long elapsedNanos) implements AutoCloseable {

    @Override
    public void close() {
        frame.close();
    }
}
