package com.hfstudio.guidenh.client.capture.api;

import java.util.Map;

public record CaptureTargetDescriptor(String id, String providerId, String scope, String label, String availability,
    String reason, Map<String, String> data) {

    public CaptureTargetDescriptor {
        data = Map.copyOf(data);
    }

    public boolean available() {
        return availability.equals("ready");
    }
}
