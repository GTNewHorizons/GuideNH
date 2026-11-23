package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public record CaptureRecordedFrame(CaptureFrame frame, long elapsedNanos, List<CaptureRegion> regions)
    implements AutoCloseable {

    public CaptureRecordedFrame {
        regions = List.copyOf(regions);
    }

    public CaptureRecordedFrame(CaptureFrame frame, long elapsedNanos) {
        this(frame, elapsedNanos, List.of());
    }

    public CaptureExportRequest request(CaptureExportRequest initial) {
        return regions.isEmpty() ? initial : initial.withRegions(regions);
    }

    @Override
    public void close() {
        frame.close();
    }
}
