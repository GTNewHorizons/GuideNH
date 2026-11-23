package com.hfstudio.guidenh.capture.server.api;

import java.util.List;
import java.util.UUID;

public record CaptureServerReply(UUID id, String status, String reason, int windowId, List<CaptureServerTarget> targets,
    boolean complete) {

    public CaptureServerReply {
        targets = List.copyOf(targets);
    }
}
