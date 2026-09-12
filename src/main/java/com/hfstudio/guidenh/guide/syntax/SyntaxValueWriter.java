package com.hfstudio.guidenh.guide.syntax;

/**
 * Writes one accepted value of a {@link SyntaxSlot} into the page.
 */
public interface SyntaxValueWriter {

    SyntaxReplacement write(SyntaxSuggestion suggestion);
}
