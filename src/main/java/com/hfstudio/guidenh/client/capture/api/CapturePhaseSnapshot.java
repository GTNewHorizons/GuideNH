package com.hfstudio.guidenh.client.capture.api;

public record CapturePhaseSnapshot(long frameId, CaptureRenderPhase phase, long timestampNanos,
    CaptureRenderState renderState) {}
