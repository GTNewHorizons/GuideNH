package com.hfstudio.guidenh.client.capture.api;

public record CaptureFrameIdentity(CaptureBackendSelection backend, CaptureBackendSelection requestedBackend,
    CaptureContentOptions content, CaptureReadbackMetadata readback) {

    public CaptureFrameIdentity(CaptureBackendSelection backend, CaptureBackendSelection requestedBackend,
        CaptureContentOptions content) {
        this(backend, requestedBackend, content, null);
    }
}
