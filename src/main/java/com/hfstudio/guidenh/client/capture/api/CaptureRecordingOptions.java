package com.hfstudio.guidenh.client.capture.api;

public record CaptureRecordingOptions(String format, int fps, int durationSeconds, int frameLimit, int loopCount,
    int alphaThreshold, boolean matteTransparency, boolean dither, int spriteColumns, int spritePadding,
    int spritePageSize, int queueFrames, long memoryBudgetBytes, boolean strictQueue,
    CaptureTargetChangePolicy targetChangePolicy) {

    public CaptureRecordingOptions(String format, int fps, int durationSeconds, int frameLimit, int loopCount,
        int alphaThreshold, boolean matteTransparency, boolean dither, int spriteColumns, int spritePadding,
        int spritePageSize, int queueFrames, long memoryBudgetBytes, boolean strictQueue) {
        this(
            format,
            fps,
            durationSeconds,
            frameLimit,
            loopCount,
            alphaThreshold,
            matteTransparency,
            dither,
            spriteColumns,
            spritePadding,
            spritePageSize,
            queueFrames,
            memoryBudgetBytes,
            strictQueue,
            CaptureTargetChangePolicy.PAUSE);
    }

    public CaptureRecordingOptions {
        if (targetChangePolicy == null) throw new IllegalArgumentException("A target change policy is required");
    }
}
