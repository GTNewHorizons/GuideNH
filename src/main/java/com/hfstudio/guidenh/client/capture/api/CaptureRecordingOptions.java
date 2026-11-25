package com.hfstudio.guidenh.client.capture.api;

public record CaptureRecordingOptions(String format, int fps, int durationSeconds, int frameLimit, int loopCount,
    int alphaThreshold, boolean matteTransparency, boolean dither, int spriteColumns, int spritePadding,
    int spritePageSize, int queueFrames, long memoryBudgetBytes, boolean strictQueue,
    CaptureTargetChangePolicy targetChangePolicy, CaptureSpriteLayout spriteLayout) {

    public CaptureRecordingOptions(String format, int fps, int durationSeconds, int frameLimit, int loopCount,
        int alphaThreshold, boolean matteTransparency, boolean dither, int spriteColumns, int spritePadding,
        int spritePageSize, int queueFrames, long memoryBudgetBytes, boolean strictQueue,
        CaptureTargetChangePolicy targetChangePolicy) {
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
            targetChangePolicy,
            CaptureSpriteLayout.NATIVE);
    }

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
        if (format == null || format.isBlank()
            || fps < 1
            || fps > 60
            || durationSeconds < 0
            || frameLimit < 0
            || loopCount < 0
            || loopCount > 65535
            || alphaThreshold < 0
            || alphaThreshold > 255
            || spriteColumns < 1
            || spritePadding < 0
            || spritePageSize < 1
            || queueFrames < 1
            || memoryBudgetBytes < 1)
            throw new IllegalArgumentException(
                "Invalid capture recording dimensions, timing, format, or resource limits");
        if (targetChangePolicy == null) throw new IllegalArgumentException("A target change policy is required");
        spriteLayout = spriteLayout == null ? CaptureSpriteLayout.NATIVE : spriteLayout;
    }
}
