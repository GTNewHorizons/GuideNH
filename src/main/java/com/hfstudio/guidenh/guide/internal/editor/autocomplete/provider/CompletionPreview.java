package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

public class CompletionPreview {

    private static final int MAX_LENGTH = 80;

    private CompletionPreview() {}

    public static String summarize(String text) {
        return summarize(text, MAX_LENGTH);
    }

    public static String summarize(String text, int limit) {
        if (text == null || text.isEmpty() || limit <= 0) return "";
        StringBuilder result = new StringBuilder(Math.min(text.length(), limit));
        for (int i = 0; i < text.length(); i++) {
            int character = text.codePointAt(i);
            String marker = character == '\r' || character == '\n' ? "\\n" : character == '\t' ? "\\t" : null;
            if (result.length() + (marker != null ? marker.length() : Character.charCount(character)) > limit)
                return result.append("...")
                    .toString();
            if (marker != null) {
                result.append(marker);
                if (character == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
            } else if (!Character.isISOControl(character)) {
                result.appendCodePoint(character);
            }
            if (Character.charCount(character) == 2) i++;
        }
        return result.toString();
    }
}
