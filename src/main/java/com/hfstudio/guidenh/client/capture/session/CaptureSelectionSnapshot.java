package com.hfstudio.guidenh.client.capture.session;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRegion;

public record CaptureSelectionSnapshot(List<CaptureRegion> regions, long activeId) {

    public CaptureSelectionSnapshot {
        regions = List.copyOf(regions);
    }
}
