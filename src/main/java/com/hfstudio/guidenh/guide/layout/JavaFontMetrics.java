package com.hfstudio.guidenh.guide.layout;

import net.minecraft.client.Minecraft;

import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/** Font metrics backed by the Java/Minecraft renderer with a headless estimate. */
public class JavaFontMetrics implements FontMetrics {

    private volatile MinecraftFontMetrics fallback;

    private MinecraftFontMetrics fallback() {
        MinecraftFontMetrics value = fallback;
        if (value == null) {
            try {
                if (Minecraft.getMinecraft() == null) return null;
                value = new MinecraftFontMetrics();
                fallback = value;
            } catch (Throwable ignored) {
                return null;
            }
        }
        return value;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        MinecraftFontMetrics value = fallback();
        if (value != null) return value.getAdvance(codePoint, style);
        return codePoint < 128 ? 6f : 12f;
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        MinecraftFontMetrics value = fallback();
        return value != null ? value.getLineHeight(style) : GuideText.BASE_LINE_HEIGHT;
    }

    @Override
    public int getStringWidth(String text, ResolvedTextStyle style) {
        MinecraftFontMetrics value = fallback();
        return value != null ? value.getStringWidth(text, style) : FontMetrics.super.getStringWidth(text, style);
    }
}
