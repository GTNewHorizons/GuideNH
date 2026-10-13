package com.hfstudio.guidenh.guide.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.render.GuideFontCompat;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class MinecraftFontMetrics implements FontMetrics {

    @Nullable
    private final FontRenderer fixedFont;

    public MinecraftFontMetrics() {
        this.fixedFont = null;
    }

    public MinecraftFontMetrics(FontRenderer font) {
        this.fixedFont = font;
    }

    /**
     * The font to measure with. A caller that supplied one keeps it; otherwise the game's current font is
     * read each time, because the game builds a new one when the language or the resources change and a
     * captured reference would then measure against the font that was replaced.
     */
    private FontRenderer font() {
        FontRenderer fixed = fixedFont;
        return fixed != null ? fixed : Minecraft.getMinecraft().fontRenderer;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        boolean bold = style != null && style.bold();
        float raw = GuideFontCompat.getRenderedAdvance(font(), codePoint, bold, false);
        if (raw <= 0f) {
            return 0f;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return scale == 1f ? raw : raw * scale;
    }

    @Override
    public int getStringWidth(String text, ResolvedTextStyle style) {
        return GuideFontCompat.getStringWidth(font(), text, style);
    }

    @Override
    public float getRenderedAdvance(int codePoint, ResolvedTextStyle style, boolean hasVisibleGlyphBefore) {
        boolean bold = style != null && style.bold();
        float raw = GuideFontCompat.getRenderedAdvance(font(), codePoint, bold, hasVisibleGlyphBefore);
        if (raw <= 0f) {
            return 0f;
        }
        float scale = style != null ? style.fontScale() : 1f;
        return scale == 1f ? raw : raw * scale;
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        int base = GuideFontCompat.getLineHeight(font()) + 1;
        if (style == null) {
            return base;
        }
        float scale = style.fontScale();
        return scale == 1f ? base : (int) Math.ceil(base * scale);
    }

    @Override
    public int getAscent(ResolvedTextStyle style) {
        FontRenderer renderer = font();
        // Minecraft's eight-pixel glyph cell has its baseline seven pixels below the top.
        float ascent = Math.max(0, renderer.FONT_HEIGHT - 2) * GuideFontCompat.getGlyphScaleY(renderer)
            * GuideFontCompat.getYScaleMultiplier();
        float scale = style != null ? style.fontScale() : 1f;
        return Math.clamp(Math.round(ascent * scale), 0, getLineHeight(style));
    }
}
