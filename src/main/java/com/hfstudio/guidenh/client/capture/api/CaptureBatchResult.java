package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchResult(String targetId, String providerId, String scope, String label, String status,
    String file, int width, int height, int frameCount, String reason) {}
