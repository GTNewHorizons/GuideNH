package com.hfstudio.guidenh.client.capture.export;

import java.util.List;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public record CaptureScrollManifest(String group, int width, int height, double pixelScaleX, double pixelScaleY,
    boolean complete, long coveredPixels, List<Entry> frames) {

    public CaptureScrollManifest {
        frames = List.copyOf(frames);
    }

    public record Entry(String targetId, String source, CaptureRect sourceCrop, CaptureRect destination) {}
}
