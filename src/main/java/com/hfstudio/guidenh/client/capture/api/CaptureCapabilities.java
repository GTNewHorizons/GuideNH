package com.hfstudio.guidenh.client.capture.api;

import java.util.List;
import java.util.Set;

public record CaptureCapabilities(String backendId, Set<String> features, int maxTextureSize, int maxRenderbufferSize,
    int maxViewportWidth, int maxViewportHeight, long memoryBudgetBytes, List<String> limitations) {

    public CaptureCapabilities {
        features = Set.copyOf(features);
        limitations = List.copyOf(limitations);
    }
}
