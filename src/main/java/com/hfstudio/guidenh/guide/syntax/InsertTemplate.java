package com.hfstudio.guidenh.guide.syntax;

/**
 * The text a tag completes as, for tags whose useful form is more than the tag name.
 *
 * <p>
 * Completing a tag name normally inserts {@code <Name />} or a paired tag with the caret inside. A tag
 * that needs attributes to be useful can declare the form the editor should write instead, which is what
 * makes a contributor's tag insertable in one step.
 *
 * @param tagName     tag the template belongs to
 * @param text        text that replaces the typed tag name
 * @param caretOffset caret position counted from the start of {@code text}
 */
public record InsertTemplate(String tagName, String text, int caretOffset) {

    public InsertTemplate {
        tagName = tagName != null ? tagName : "";
        text = text != null ? text : "";
        caretOffset = Math.clamp(caretOffset, 0, text.length());
    }

    /** A template that leaves the caret at the end of its text. */
    public static InsertTemplate of(String tagName, String text) {
        String safe = text != null ? text : "";
        return new InsertTemplate(tagName, safe, safe.length());
    }

    /**
     * A template that puts the caret right after the first occurrence of {@code marker}, so a template can
     * say where typing continues without counting characters. A marker the text does not contain leaves
     * the caret at the end.
     */
    public static InsertTemplate caretAfter(String tagName, String text, String marker) {
        String safe = text != null ? text : "";
        int index = marker != null && !marker.isEmpty() ? safe.indexOf(marker) : -1;
        return new InsertTemplate(tagName, safe, index >= 0 ? index + marker.length() : safe.length());
    }
}
