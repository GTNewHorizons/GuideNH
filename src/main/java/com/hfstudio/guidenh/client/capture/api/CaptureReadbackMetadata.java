package com.hfstudio.guidenh.client.capture.api;

import java.util.List;

public record CaptureReadbackMetadata(long frameId, CaptureRenderPhase capturedPhase, CaptureRenderState target,
    List<CapturePhaseSnapshot> phases) {

    public CaptureReadbackMetadata {
        phases = List.copyOf(phases);
    }
}
