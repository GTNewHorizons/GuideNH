package com.hfstudio.guidenh.guide.syntax;

/**
 * The text an accepted value is written as, plus where the caret and the selection end land inside it.
 *
 * @param text               text that replaces the slot's range
 * @param caretOffset        caret position counted from the start of {@code text}
 * @param selectionEndOffset selection end counted from the start of {@code text}; a negative value
 *                           leaves the caret alone
 */
public record SyntaxReplacement(String text, int caretOffset, int selectionEndOffset) {

    public SyntaxReplacement {
        text = text != null ? text : "";
        caretOffset = Math.clamp(caretOffset, 0, text.length());
        selectionEndOffset = selectionEndOffset >= 0 ? Math.clamp(selectionEndOffset, 0, text.length()) : caretOffset;
    }

    /** Writes {@code text} with the caret at its end. */
    public static SyntaxReplacement cursorAtEnd(String text) {
        String safe = text != null ? text : "";
        return new SyntaxReplacement(safe, safe.length(), safe.length());
    }

    /** Writes {@code text} and selects everything from {@code selectionStart} to its end. */
    public static SyntaxReplacement selectingTail(String text, int selectionStart) {
        String safe = text != null ? text : "";
        return new SyntaxReplacement(safe, safe.length(), Math.max(0, selectionStart));
    }
}
