package com.hfstudio.guidenh.client.capture.api;

public record CaptureFrameIdentity(CaptureBackendSelection backend, CaptureBackendSelection requestedBackend,
    CaptureContentOptions content) {}
