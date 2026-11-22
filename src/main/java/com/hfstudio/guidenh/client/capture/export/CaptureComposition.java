package com.hfstudio.guidenh.client.capture.export;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;

public record CaptureComposition(CaptureOutputSize size, List<CapturePlacement> placements) {

    public CaptureComposition {
        placements = List.copyOf(placements);
    }
}
