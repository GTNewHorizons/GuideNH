package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchOutput(String kind, String group, String status, String file, int width, int height,
    int entries, long byteSize, String reason) {}
