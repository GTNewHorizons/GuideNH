package com.hfstudio.guidenh.client.capture.api;

public record CaptureBackendSelection(String sourceId, String readerId) {

    public CaptureBackendSelection {
        if (sourceId == null || sourceId.isBlank() || readerId == null || readerId.isBlank())
            throw new IllegalArgumentException("Capture source and reader identifiers cannot be empty");
    }

    public static CaptureBackendSelection automatic() {
        return new CaptureBackendSelection("automatic", "automatic");
    }
}
