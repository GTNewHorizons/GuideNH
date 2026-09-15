package com.hfstudio.guidenh.guide.style;

import com.hfstudio.guidenh.guide.color.ColorValue;

/**
 * Represents the styling of text for rendering.
 */
public record ResolvedTextStyle(float fontScale, boolean bold, boolean italic, boolean underlined,
    boolean wavyUnderline, boolean dottedUnderline, boolean strikethrough, boolean obfuscated, String font,
    ColorValue color, WhiteSpaceMode whiteSpace, TextAlignment alignment, boolean dropShadow,
    ColorValue backgroundColor, boolean inlineCode) {}
