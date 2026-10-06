package com.hfstudio.guidenh.guide.internal.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class DisplayScale {

    public static int cachedDW = -1;
    public static int cachedDH = -1;
    public static int cachedGuiScale = -1;
    public static int cachedScaleFactor = 2;
    public static int cachedScaledWidth = 0;
    public static int cachedScaledHeight = 0;

    /**
     * Headless injection-window scale override source (0 = disabled).
     *
     * <p>
     * When {@code > 0}, {@link #scaleFactor()} returns this value directly,
     * short-circuiting before any {@link ScaledResolution} computation. This
     * bypasses the MC 1.7.10 ScaledResolution geometric cap
     * ({@code sf = min(guiScale, floor(dw/320), floor(dh/240))}), which would
     * otherwise clamp the effective scaleFactor to 2 on the 854×480 headless
     * window even with guiScale=4 injected.
     *
     * <p>
     * Scope is strictly equal to {@code RenderPageService}'s injection
     * window: the value is set right after the guiscale injection and MUST be
     * cleared (set to 0) when that window ends.
     */
    private static int injectedScaleFactor = 0;

    /**
     * Set the headless injection-window scale override (0 = disabled).
     * See {@link #injectedScaleFactor} for the scope/cleanup contract.
     */
    public static void setInjectedScaleFactor(int sf) {
        injectedScaleFactor = sf;
    }

    private DisplayScale() {}

    public static void refreshIfNeeded() {
        Minecraft mc;
        try {
            mc = Minecraft.getMinecraft();
        } catch (Throwable t) {
            return; // headless (unit tests): Minecraft class unavailable, keep cached defaults
        }
        if (mc == null) return; // headless (unit tests): keep cached defaults
        int dw = mc.displayWidth;
        int dh = mc.displayHeight;
        int gs = mc.gameSettings != null ? mc.gameSettings.guiScale : 0;
        if (dw == cachedDW && dh == cachedDH && gs == cachedGuiScale) return;
        ScaledResolution sr = new ScaledResolution(mc, dw, dh);
        cachedDW = dw;
        cachedDH = dh;
        cachedGuiScale = gs;
        cachedScaleFactor = sr.getScaleFactor();
        cachedScaledWidth = sr.getScaledWidth();
        cachedScaledHeight = sr.getScaledHeight();
    }

    public static int scaleFactor() {
        if (injectedScaleFactor > 0) return injectedScaleFactor;
        refreshIfNeeded();
        return cachedScaleFactor;
    }

    public static int scaledWidth() {
        refreshIfNeeded();
        return cachedScaledWidth;
    }

    public static int scaledHeight() {
        refreshIfNeeded();
        return cachedScaledHeight;
    }
}
