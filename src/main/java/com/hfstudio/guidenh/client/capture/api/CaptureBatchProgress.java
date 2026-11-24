package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchProgress(String stage, int completed, int total, String label, String reason) {}
