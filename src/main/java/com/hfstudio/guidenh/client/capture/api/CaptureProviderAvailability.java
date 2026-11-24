package com.hfstudio.guidenh.client.capture.api;

public record CaptureProviderAvailability(boolean available, String reason) {

    public static CaptureProviderAvailability ready() {
        return new CaptureProviderAvailability(true, "");
    }

    public static CaptureProviderAvailability unavailable(String reason) {
        return new CaptureProviderAvailability(false, reason);
    }
}
