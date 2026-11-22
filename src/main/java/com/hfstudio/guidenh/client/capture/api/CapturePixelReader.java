package com.hfstudio.guidenh.client.capture.api;

public interface CapturePixelReader {

    CaptureFrame read(int x, int y, int width, int height, long timestampNanos);
}
