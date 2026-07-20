package com.hfstudio.guidenh.guide.render;

import java.util.List;

import com.github.bsideup.jabel.Desugar;

/**
 * One span's worth of a paragraph's glyph run: the glyphs plus how to draw
 * them. {@code argb} tints the (white) atlas bitmaps; {@code shear} asks the
 * engine for a synthetic-italic slant (fake-oblique, mirroring MC §o).
 */
@Desugar
public record GlyphRunGroup(List<GuideRenderPrimitive.PlacedGlyph> glyphs, int argb, boolean shear) {}
