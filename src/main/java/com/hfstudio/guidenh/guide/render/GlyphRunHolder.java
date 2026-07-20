package com.hfstudio.guidenh.guide.render;

import org.jetbrains.annotations.Nullable;

/**
 * Interface for text nodes that hold glyph data from Rust cosmic-text shaping.
 */
public interface GlyphRunHolder {

    /** Set the glyph data from the measureLayout result (null to clear). */
    void setGlyphData(@Nullable GlyphRunData data);

    /** Get the glyph data, or null if not yet measured / opaque. */
    @Nullable
    GlyphRunData getGlyphData();
}
