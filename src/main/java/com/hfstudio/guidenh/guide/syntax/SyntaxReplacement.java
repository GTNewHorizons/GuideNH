package com.hfstudio.guidenh.guide.syntax;

public record SyntaxReplacement(String text, int caretOffset, int selectionEndOffset) {

    public SyntaxReplacement {
        text = text != null ? text : "";
        caretOffset = Math.clamp(caretOffset, 0, text.length());
        selectionEndOffset = selectionEndOffset >= 0 ? Math.clamp(selectionEndOffset, 0, text.length()) : caretOffset;
    }

    public static SyntaxReplacement cursorAtEnd(String text) {
        String safe = text != null ? text : "";
        return new SyntaxReplacement(safe, safe.length(), safe.length());
    }

    public static SyntaxReplacement selectingTail(String text, int selectionStart) {
        String safe = text != null ? text : "";
        return new SyntaxReplacement(safe, safe.length(), Math.max(0, selectionStart));
    }
}
