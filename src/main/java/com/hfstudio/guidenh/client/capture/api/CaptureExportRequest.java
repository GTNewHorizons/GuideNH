package com.hfstudio.guidenh.client.capture.api;

import java.util.List;
import java.util.UUID;

public record CaptureExportRequest(UUID taskId, String sourceId, CaptureViewport viewport, List<CaptureRegion> regions,
    CaptureOptions options) {

    public CaptureExportRequest {
        regions = List.copyOf(regions);
        if (regions.isEmpty()) throw new IllegalArgumentException("Select an area before saving");
    }
}
