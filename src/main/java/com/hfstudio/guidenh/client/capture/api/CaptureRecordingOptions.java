package com.hfstudio.guidenh.client.capture.api;

public record CaptureRecordingOptions(String format, int fps, int durationSeconds, int frameLimit, int loopCount,
    int alphaThreshold, boolean matteTransparency, boolean dither, int spriteColumns, int spritePadding,
    int spritePageSize, int queueFrames, long memoryBudgetBytes, boolean strictQueue) {}
