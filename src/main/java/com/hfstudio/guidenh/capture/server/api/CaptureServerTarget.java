package com.hfstudio.guidenh.capture.server.api;

public record CaptureServerTarget(String id, String providerId, int dimension, int x, int y, int z, String variant,
    String label, String availability, String reason) {

    public boolean available() {
        return availability.equals("ready");
    }
}
