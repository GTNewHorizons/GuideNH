package com.hfstudio.guidenh.guide.syntax;

public record SyntaxSelection(int start, int end) {

    public SyntaxSelection {
        start = Math.max(0, start);
        end = Math.max(start, end);
    }

    public boolean isEmpty() {
        return start == end;
    }
}
