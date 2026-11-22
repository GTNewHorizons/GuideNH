package com.hfstudio.guidenh.client.capture.api;

public record CaptureContentOptions(boolean includeWorld, boolean includeGuiBackdrop, boolean includeTooltips,
    boolean includeDebug, boolean includeNei, int backgroundArgb) {}
