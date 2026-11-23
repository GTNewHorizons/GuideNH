package com.hfstudio.guidenh.client.capture.api;

public record CaptureRegion(long id, CaptureRect bounds, String targetId, boolean followTarget, boolean erased) {

    public CaptureRegion(long id, CaptureRect bounds, String targetId, boolean followTarget) {
        this(id, bounds, targetId, followTarget, false);
    }

    public CaptureRegion withBounds(CaptureRect rectangle) {
        return new CaptureRegion(id, rectangle, targetId, followTarget, erased);
    }
}
