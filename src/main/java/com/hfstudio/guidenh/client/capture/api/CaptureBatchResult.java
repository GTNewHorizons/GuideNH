package com.hfstudio.guidenh.client.capture.api;

import java.util.Map;

public record CaptureBatchResult(String targetId, String providerId, String scope, String label, String status,
    String file, int width, int height, int frameCount, String reason, Map<String, String> data) {

    public CaptureBatchResult {
        data = Map.copyOf(data);
    }
}
