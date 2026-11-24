package com.hfstudio.guidenh.client.capture.export;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRegion;

public record CaptureAnimationManifest(String sourceId, int width, int height, long durationNanos,
    List<CaptureRegion> selections, List<FrameEntry> frames) {

    public record FrameEntry(String file, int x, int y, int width, int height, long startNanos, long durationNanos) {}
}
