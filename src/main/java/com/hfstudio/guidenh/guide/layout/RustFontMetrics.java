package com.hfstudio.guidenh.guide.layout;

import javax.annotation.Nullable;

import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * {@link FontMetrics} backed by the unified text pipeline ({@link GuideText},
 * cosmic-text over the system TTF) — layout measurement is thereby identical
 * to what the glyph-run renderer draws.
 * <p>
 * Falls back when the Rust font system is not initialized: to
 * {@link MinecraftFontMetrics} in-game, or to constant estimates in pure-JVM
 * tests where the Minecraft class itself is unavailable (C-6).
 */
public class RustFontMetrics implements FontMetrics {

    private volatile MinecraftFontMetrics fallback;

    @Nullable
    private MinecraftFontMetrics fallback() {
        MinecraftFontMetrics f = fallback;
        if (f == null) {
            try {
                if (net.minecraft.client.Minecraft.getMinecraft() == null) {
                    return null;
                }
                f = new MinecraftFontMetrics();
            } catch (Throwable t) {
                return null; // headless: Minecraft class unavailable
            }
            fallback = f;
        }
        return f;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        if (!GuideText.isAvailable()) {
            var f = fallback();
            if (f != null) {
                return f.getAdvance(codePoint, style);
            }
            // Headless constant estimate (matches the harness mock convention).
            return codePoint < 128 ? 6f : 12f;
        }
        return GuideText.advanceOf(codePoint, style);
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        if (!GuideText.isAvailable()) {
            var f = fallback();
            return f != null ? f.getLineHeight(style) : GuideText.BASE_LINE_HEIGHT;
        }
        return GuideText.lineHeight(style);
    }

    @Override
    public int getStringWidth(String text, ResolvedTextStyle style) {
        if (!GuideText.isAvailable()) {
            var f = fallback();
            if (f != null) {
                return f.getStringWidth(text, style);
            }
            return FontMetrics.super.getStringWidth(text, style);
        }
        return GuideText.measureWidth(text, style);
    }
}
