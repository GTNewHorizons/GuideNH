package com.hfstudio.guidenh.guide.syntax;

/**
 * Writes one accepted value of a {@link SyntaxSlot} into the page.
 *
 * <p>
 * A slot's values usually need their own surroundings, so the writer decides the text that replaces the
 * slot's range and where the caret lands in it. When a slot declares no writer, the value replaces the
 * range as it is and the caret follows it.
 */
public interface SyntaxValueWriter {

    SyntaxReplacement write(SyntaxSuggestion suggestion);
}
