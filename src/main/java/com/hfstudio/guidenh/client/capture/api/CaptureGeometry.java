package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public record CaptureGeometry(long revision, CaptureViewport viewport, List<CaptureTarget> targets) {

    public CaptureGeometry {
        targets = List.copyOf(targets);
    }
}
