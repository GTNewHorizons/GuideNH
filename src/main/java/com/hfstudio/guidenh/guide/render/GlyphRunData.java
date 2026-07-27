package com.hfstudio.guidenh.guide.render;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

/**
 * A paragraph's complete Rust text output: glyph runs grouped by span, plus
 * the span decoration rects. {@code backgrounds} (highlight / inline-code)
 * render before the glyph runs, {@code lines} (underline / strikethrough)
 * after them. {@code separators} are kind=3 rects for heading separator
 * lines, consumed by {@code LytHeading.separatorExtent()}.
 */
@Desugar
public record GlyphRunData(List<GlyphRunGroup> runs, List<GuideRenderPrimitive.FillRect> backgrounds,
    List<GuideRenderPrimitive.FillRect> lines, List<GuideRenderPrimitive.FillRect> separators) {}
