package com.hfstudio.guidenh.client.capture.export;

import java.util.List;
import java.util.UUID;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;

public record CaptureRegionManifest(UUID taskId, String sourceId, long timestampNanos, List<CaptureRegion> selections,
    List<Entry> regions) {

    public CaptureRegionManifest {
        regions = List.copyOf(regions);
        selections = List.copyOf(selections);
    }

    public record Entry(long id, CaptureRect bounds, String file, int width, int height) {}
}
