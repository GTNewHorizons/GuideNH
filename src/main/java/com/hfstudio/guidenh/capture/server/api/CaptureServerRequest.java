package com.hfstudio.guidenh.capture.server.api;

import java.util.UUID;

public record CaptureServerRequest(UUID id, UUID catalogId, String action, String targetId, CaptureServerQuery query) {}
