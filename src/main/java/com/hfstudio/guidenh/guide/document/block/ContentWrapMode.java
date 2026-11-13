package com.hfstudio.guidenh.guide.document.block;

import java.util.Locale;

/**
 * Word-style text-wrapping modes for embedded block elements.
 *
 * <p>
 * These modes mirror the "Text Wrapping" options found in word processors such as Microsoft
 * Word. When combined with a {@link ContentAlign}, they control both the horizontal position
 * and how surrounding text interacts with the embedded block.
 */
public enum ContentWrapMode {

    /**
     * block sits in normal document flow; no text wraps around its sides.
     */
    INLINE,

    /**
     * surrounding text wraps in a rectangle around the block.
     */
    SQUARE,

    /**
     * tighter wrap; functionally equivalent to {@link #SQUARE} in this layout system.
     */
    TIGHT,

    /**
     * through-wrap; functionally equivalent to {@link #SQUARE} in this layout system.
     */
    THROUGH,

    /**
     * text appears only above and below the block, never beside it.
     */
    TOP_BOTTOM,

    /**
     * block is rendered behind (below) surrounding text.
     */
    BEHIND,

    /**
     * block is rendered in front of (above) surrounding text.
     */
    FRONT;

    /**
     * Parses a wrap mode from the attribute string found in MDX markup. Unrecognised values fall
     * back to {@link #INLINE}.
     */
    public static ContentWrapMode fromString(String value) {
        if (value == null) {
            return INLINE;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "square" -> SQUARE;
            case "tight" -> TIGHT;
            case "through" -> THROUGH;
            case "topbottom", "top-bottom", "top_bottom" -> TOP_BOTTOM;
            case "behind" -> BEHIND;
            case "front" -> FRONT;
            default -> INLINE;
        };
    }

    /**
     * Returns {@code true} when this mode implies that the block should float to one side and
     * allow text to fill the space beside it ({@link #SQUARE}, {@link #TIGHT}, {@link #THROUGH}).
     */
    public boolean isDocumentFloat() {
        return this == SQUARE || this == TIGHT || this == THROUGH;
    }
}
