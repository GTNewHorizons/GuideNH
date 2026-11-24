package com.hfstudio.guidenh.client.capture.api;

public record CaptureContentOptions(boolean includeWorld, boolean includeGuiBackdrop, boolean includeTooltips,
    boolean includeDebug, boolean includeNei, int backgroundArgb, boolean includeHud, boolean includeCursor,
    boolean includeGui, boolean includePlayerInventory, boolean fullBrightness) {

    public boolean sameGuiLayer(CaptureContentOptions other) {
        return includeGuiBackdrop == other.includeGuiBackdrop && includeTooltips == other.includeTooltips
            && includeDebug == other.includeDebug
            && includeNei == other.includeNei
            && includeHud == other.includeHud
            && includeCursor == other.includeCursor
            && includeGui == other.includeGui
            && includePlayerInventory == other.includePlayerInventory
            && fullBrightness == other.fullBrightness;
    }

    public CaptureContentOptions(boolean includeWorld, boolean includeGuiBackdrop, boolean includeTooltips,
        boolean includeDebug, boolean includeNei, int backgroundArgb, boolean includeHud, boolean includeCursor) {
        this(
            includeWorld,
            includeGuiBackdrop,
            includeTooltips,
            includeDebug,
            includeNei,
            backgroundArgb,
            includeHud,
            includeCursor,
            true,
            true,
            false);
    }

    public CaptureContentOptions(boolean includeWorld, boolean includeGuiBackdrop, boolean includeTooltips,
        boolean includeDebug, boolean includeNei, int backgroundArgb) {
        this(includeWorld, includeGuiBackdrop, includeTooltips, includeDebug, includeNei, backgroundArgb, false, false);
    }
}
