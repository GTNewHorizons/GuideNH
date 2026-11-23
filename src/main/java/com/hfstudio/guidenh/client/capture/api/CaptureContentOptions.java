package com.hfstudio.guidenh.client.capture.api;

public record CaptureContentOptions(boolean includeWorld, boolean includeGuiBackdrop, boolean includeTooltips,
    boolean includeDebug, boolean includeNei, int backgroundArgb, boolean includeHud, boolean includeCursor) {

    public CaptureContentOptions(boolean includeWorld, boolean includeGuiBackdrop, boolean includeTooltips,
        boolean includeDebug, boolean includeNei, int backgroundArgb) {
        this(includeWorld, includeGuiBackdrop, includeTooltips, includeDebug, includeNei, backgroundArgb, false, false);
    }
}
