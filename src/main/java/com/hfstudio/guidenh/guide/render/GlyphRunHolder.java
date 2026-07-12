package com.hfstudio.guidenh.guide.render;

import java.util.List;

/**
 * Interface for text nodes that hold glyph data from Rust cosmic-text shaping.
 */
public interface GlyphRunHolder {
    /** Set the glyph run from measureLayout result. */
    void setGlyphRun(List<GuideRenderPrimitive.PlacedGlyph> glyphs);

    /** Get the glyph run, or null if not yet measured. */
    List<GuideRenderPrimitive.PlacedGlyph> getGlyphRun();
}
