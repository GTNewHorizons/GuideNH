package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchReadiness(boolean ready, long revision, String reason) {}
