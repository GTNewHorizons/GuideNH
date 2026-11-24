package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchLayoutOptions(boolean contactSheet, boolean groupByProvider, int columns, int thumbnailSize,
    int pageSize, boolean scrollImages) {

    public CaptureBatchLayoutOptions(boolean contactSheet, boolean groupByProvider, int columns, int thumbnailSize,
        int pageSize) {
        this(contactSheet, groupByProvider, columns, thumbnailSize, pageSize, false);
    }

    public CaptureBatchLayoutOptions {
        if (columns < 1 || thumbnailSize < 1 || pageSize < thumbnailSize + 36)
            throw new IllegalArgumentException("Invalid contact sheet dimensions");
    }

    public static CaptureBatchLayoutOptions filesOnly() {
        return new CaptureBatchLayoutOptions(false, true, 4, 256, 4096);
    }
}
