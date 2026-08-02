package com.hfstudio.guidenh.guide.render;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

/**
 * A paragraph's complete Rust text output: glyph runs grouped by span, plus
 * the span decoration rects. {@code backgrounds} (highlight / inline-code)
 * render before the glyph runs, {@code lines} (underline / strikethrough)
 * after them. {@code separators} are kind=3 rects for heading separator
 * lines, consumed by {@code LytHeading.separatorExtent()}. {@code decorations}
 * are kind=4 (wavy) / kind=5 (dotted) underline decorations, rendered as
 * batched sine / dot brushes by {@code GuideRenderEngine.drawDecorationLine}.
 */
@Desugar
public record GlyphRunData(List<GlyphRunGroup> runs, List<GuideRenderPrimitive.FillRect> backgrounds,
    List<GuideRenderPrimitive.FillRect> lines, List<GuideRenderPrimitive.FillRect> separators,
    List<GuideRenderPrimitive.DrawDecorationLine> decorations) {

    /**
     * Convenience constructor for callers without wavy/dotted (kind 4/5)
     * decorations — keeps existing call sites source-compatible.
     */
    public GlyphRunData(List<GlyphRunGroup> runs, List<GuideRenderPrimitive.FillRect> backgrounds,
        List<GuideRenderPrimitive.FillRect> lines, List<GuideRenderPrimitive.FillRect> separators) {
        this(runs, backgrounds, lines, separators, List.of());
    }
}
