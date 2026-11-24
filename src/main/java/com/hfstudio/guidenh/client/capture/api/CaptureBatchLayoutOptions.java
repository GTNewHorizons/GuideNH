package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchLayoutOptions(boolean contactSheet, boolean groupByProvider, int columns, int thumbnailSize,
    int pageSize, boolean scrollImages, CaptureRecordingOptions pageRecording, int pageDelayMillis,
    int pageMaximumDimension) {

    public CaptureBatchLayoutOptions(boolean contactSheet, boolean groupByProvider, int columns, int thumbnailSize,
        int pageSize, boolean scrollImages, CaptureRecordingOptions pageRecording, int pageDelayMillis) {
        this(
            contactSheet,
            groupByProvider,
            columns,
            thumbnailSize,
            pageSize,
            scrollImages,
            pageRecording,
            pageDelayMillis,
            0);
    }

    public CaptureBatchLayoutOptions(boolean contactSheet, boolean groupByProvider, int columns, int thumbnailSize,
        int pageSize, boolean scrollImages) {
        this(contactSheet, groupByProvider, columns, thumbnailSize, pageSize, scrollImages, null, 1000);
    }

    public CaptureBatchLayoutOptions(boolean contactSheet, boolean groupByProvider, int columns, int thumbnailSize,
        int pageSize) {
        this(contactSheet, groupByProvider, columns, thumbnailSize, pageSize, false);
    }

    public CaptureBatchLayoutOptions {
        if (columns < 1 || thumbnailSize < 1
            || pageSize < thumbnailSize + 36
            || pageDelayMillis < 10
            || pageMaximumDimension < 0) throw new IllegalArgumentException("Invalid contact sheet dimensions");
    }

    public static CaptureBatchLayoutOptions filesOnly() {
        return new CaptureBatchLayoutOptions(false, true, 4, 256, 4096);
    }
}
