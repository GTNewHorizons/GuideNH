package com.hfstudio.guidenh.client.capture.api;

public record CaptureBatchReadiness(boolean ready, long revision, String reason) {

    // Readiness tracks the rendered view, while paint geometry can animate independently.
    public static long viewRevision(CaptureContext context) {
        return ((long) System.identityHashCode(context.screen()) << Integer.SIZE) ^ Integer.toUnsignedLong(
            context.viewport()
                .hashCode());
    }
}
