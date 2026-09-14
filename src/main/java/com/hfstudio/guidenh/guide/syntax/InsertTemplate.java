package com.hfstudio.guidenh.guide.syntax;

public record InsertTemplate(String tagName, String text, int caretOffset) {

    public InsertTemplate {
        tagName = tagName != null ? tagName : "";
        text = text != null ? text : "";
        caretOffset = Math.clamp(caretOffset, 0, text.length());
    }

    public static InsertTemplate of(String tagName, String text) {
        String safe = text != null ? text : "";
        return new InsertTemplate(tagName, safe, safe.length());
    }

    /**
     * A template that puts the caret right after the first occurrence of {@code marker}, so a template can say where.
     */
    public static InsertTemplate caretAfter(String tagName, String text, String marker) {
        String safe = text != null ? text : "";
        int index = marker != null && !marker.isEmpty() ? safe.indexOf(marker) : -1;
        return new InsertTemplate(tagName, safe, index >= 0 ? index + marker.length() : safe.length());
    }
}
