package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

import lombok.Getter;

public class MarkdownActionLink {

    private static final String PLACEHOLDER_PREFIX = "\uE000GUIDENH_ACTION_";
    private static final String PLACEHOLDER_SUFFIX = "_\uE001";

    private MarkdownActionLink() {}

    public static boolean mayContain(String text) {
        return text != null && text.contains("&[");
    }

    /**
     * Replaces every {@code &[label](uri)} with an opaque placeholder before the page is parsed.
     *
     * <p>
     * The syntax is a Markdown link with a leading {@code &}, so without this the parser consumes
     * {@code [label](uri)} as an ordinary link and leaves a stray {@code &} behind. The URI is then gone by
     * the time the compiler runs, which is why the form rendered as plain text instead of an action.
     */
    public static MaskResult mask(String source) {
        if (source == null) {
            return new MaskResult("", Map.of());
        }
        if (!mayContain(source)) {
            return new MaskResult(source, Map.of());
        }
        StringBuilder masked = new StringBuilder(source.length());
        Map<String, String> actions = new LinkedHashMap<>();
        int cursor = 0;
        int index = 0;
        while (cursor < source.length()) {
            int start = source.indexOf("&[", cursor);
            if (start < 0) {
                masked.append(source, cursor, source.length());
                break;
            }
            ParsedLink parsed = parseAt(source, start);
            if (parsed == null) {
                masked.append(source, cursor, start + 2);
                cursor = start + 2;
                continue;
            }
            masked.append(source, cursor, start);
            String placeholder = PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
            actions.put(placeholder, source.substring(start, parsed.endIndex));
            masked.append(placeholder);
            cursor = parsed.endIndex;
            index++;
        }
        return new MaskResult(masked.toString(), actions);
    }

    public static void restore(MdAstNode root, MaskResult maskResult) {
        if (maskResult == null || maskResult.isEmpty() || root == null) {
            return;
        }
        restoreNode(root, maskResult);
    }

    private static void restoreNode(MdAstNode node, MaskResult maskResult) {
        if (node instanceof MdAstText text && text.value != null && text.value.contains(PLACEHOLDER_PREFIX)) {
            String restored = text.value;
            for (Map.Entry<String, String> entry : maskResult.actions()
                .entrySet()) {
                restored = restored.replace(entry.getKey(), entry.getValue());
            }
            text.value = restored;
        }
        if (node instanceof MdAstParent<?>parent) {
            for (var child : parent.children()) {
                if (child instanceof MdAstNode childNode) {
                    restoreNode(childNode, maskResult);
                }
            }
        }
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

    public record MaskResult(String source, Map<String, String> actions) {

        public MaskResult {
            actions = actions == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(actions));
        }

        public boolean isEmpty() {
            return actions.isEmpty();
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
