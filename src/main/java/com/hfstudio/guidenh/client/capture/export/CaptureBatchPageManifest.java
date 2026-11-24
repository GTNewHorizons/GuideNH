package com.hfstudio.guidenh.client.capture.export;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public record CaptureBatchPageManifest(String group, String format, int width, int height, long durationNanos,
    List<Entry> pages) {

    public CaptureBatchPageManifest {
        pages = List.copyOf(pages);
    }

    public record Entry(String targetId, String label, String source, CaptureRect sourceBounds,
        CaptureRect destinationBounds, long elapsedNanos, long durationNanos) {

        public Entry(String targetId, String label, String source, CaptureRect sourceBounds, long elapsedNanos,
            long durationNanos) {
            this(targetId, label, source, sourceBounds, null, elapsedNanos, durationNanos);
        }
    }
}
