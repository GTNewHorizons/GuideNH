package com.hfstudio.guidenh.guide.document.block;

import java.util.Locale;

/**
 * Horizontal alignment of an embedded block element within its available width.
 *
 * <p>
 * Used together with {@link ContentWrapMode} to fully describe how a block is positioned and
 * how surrounding text interacts with it, matching the alignment options available in word
 * processors.
 */
public enum ContentAlign {

    /**
     * Flush to the left edge of the available area (default).
     */
    LEFT,

    /**
     * Horizontally centred within the available area.
     */
    CENTER,

    /**
     * Flush to the right edge of the available area.
     */
    RIGHT;

    /**
     * Parses an alignment from the attribute string found in MDX markup. Unrecognised values fall
     * back to {@link #LEFT}.
     */
    public static ContentAlign fromString(String value) {
        if (value == null) {
            return LEFT;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "center", "centre" -> CENTER;
            case "right" -> RIGHT;
            default -> LEFT;
        };
    }
}
