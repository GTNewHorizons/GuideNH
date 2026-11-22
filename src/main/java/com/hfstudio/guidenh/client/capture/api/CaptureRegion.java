package com.hfstudio.guidenh.client.capture.api;

public record CaptureRegion(long id, CaptureRect bounds, String targetId, boolean followTarget) {

    public CaptureRegion withBounds(CaptureRect rectangle) {
        return new CaptureRegion(id, rectangle, targetId, followTarget);
    }
}
