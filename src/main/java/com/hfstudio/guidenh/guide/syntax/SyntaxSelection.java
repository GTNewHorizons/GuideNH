package com.hfstudio.guidenh.guide.syntax;

/**
 * A range of the editor's text, such as the range a double click inside a {@link SyntaxSlot} selects.
 *
 * @param start first character of the range
 * @param end   first character after the range
 */
public record SyntaxSelection(int start, int end) {

    public SyntaxSelection {
        start = Math.max(0, start);
        end = Math.max(start, end);
    }

    public boolean isEmpty() {
        return start == end;
    }
}
