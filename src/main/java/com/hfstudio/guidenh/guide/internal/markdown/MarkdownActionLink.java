package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class MarkdownActionLink {

    private MarkdownActionLink() {}

    public static boolean mayContain(String text) {
        return text != null && text.contains("&[");
    }

    public static List<Segment> split(String text) {
        List<Segment> result = new ArrayList<>();
        int cursor = 0;
        while (cursor < text.length()) {
            int start = text.indexOf("&[", cursor);
            if (start < 0) {
                addText(result, text.substring(cursor));
                break;
            }
            if (start > cursor) {
                addText(result, text.substring(cursor, start));
            }
            ParsedLink parsed = parseAt(text, start);
            if (parsed == null) {
                addText(result, text.substring(start, start + 2));
                cursor = start + 2;
            } else {
                result.add(new Segment(parsed.label, parsed.href, true));
                cursor = parsed.endIndex;
            }
        }
        return result;
    }

    private static void addText(List<Segment> result, String text) {
        if (!text.isEmpty()) {
            result.add(new Segment(text, null, false));
        }
    }

    private static ParsedLink parseAt(String text, int start) {
        int labelStart = start + 2;
        int labelEnd = findClosing(text, labelStart, ']');
        if (labelEnd < 0 || labelEnd + 1 >= text.length() || text.charAt(labelEnd + 1) != '(') {
            return null;
        }
        int hrefStart = labelEnd + 2;
        int hrefEnd = findClosing(text, hrefStart, ')');
        if (hrefEnd < 0) {
            return null;
        }
        String label = unescape(text.substring(labelStart, labelEnd));
        String href = unescape(text.substring(hrefStart, hrefEnd));
        if (label.isEmpty() || href.isEmpty()) {
            return null;
        }
        return new ParsedLink(label, href, hrefEnd + 1);
    }

    private static int findClosing(String text, int start, char closing) {
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == closing) {
                return i;
            }
        }
        return -1;
    }

    private static String unescape(String value) {
        if (value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                if (isEscapableDelimiter(c)) {
                    result.append(c);
                } else {
                    result.append('\\')
                        .append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }

    private static boolean isEscapableDelimiter(char c) {
        return c == '[' || c == ']' || c == '(' || c == ')';
    }

    public static class Segment {

        private final String text;
        private final String href;
        @Getter
        private final boolean link;

        public Segment(String text, String href, boolean link) {
            this.text = text;
            this.href = href;
            this.link = link;
        }

        public String text() {
            return text;
        }

        public String href() {
            return href;
        }

    }

    private static class ParsedLink {

        private final String label;
        private final String href;
        private final int endIndex;

        private ParsedLink(String label, String href, int endIndex) {
            this.label = label;
            this.href = href;
            this.endIndex = endIndex;
        }
    }
}
