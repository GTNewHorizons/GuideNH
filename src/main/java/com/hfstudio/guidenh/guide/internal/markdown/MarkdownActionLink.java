package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.ActionLinkResolver;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

import lombok.Getter;

public class MarkdownActionLink {

    private static final String PLACEHOLDER_PREFIX = "\uE000GUIDENH_ACTION_";
    private static final String PLACEHOLDER_SUFFIX = "_\uE001";
    private static final Object PLAIN_LABEL = new Object();

    private MarkdownActionLink() {}

    public static MdAstText plainLabel(String label) {
        MdAstText text = new MdAstText();
        text.value = label;
        text.data = PLAIN_LABEL;
        return text;
    }

    public static boolean isPlainLabel(MdAstText text) {
        return text.data == PLAIN_LABEL;
    }

    public static Map<String, ActionLinkResolver> indexResolvers(ExtensionCollection extensions) {
        return indexResolvers(extensions.get(ActionLinkResolver.EXTENSION_POINT));
    }

    public static Map<String, ActionLinkResolver> indexResolvers(Iterable<ActionLinkResolver> resolvers) {
        Map<String, ActionLinkResolver> indexed = new LinkedHashMap<>();
        for (ActionLinkResolver resolver : resolvers) {
            for (String scheme : resolver.getSchemes()) {
                if (scheme == null || !scheme.matches("[A-Za-z][A-Za-z0-9+.-]*")) {
                    throw new IllegalArgumentException("Invalid action link scheme: " + scheme);
                }
                String key = scheme.toLowerCase(Locale.ROOT);
                if (indexed.putIfAbsent(key, resolver) != null) {
                    throw new IllegalArgumentException("Duplicate action link scheme: " + scheme);
                }
            }
        }
        return Map.copyOf(indexed);
    }

    public static @Nullable MdxJsxTextElement expand(String label, String destination, String defaultNamespace,
        Map<String, ActionLinkResolver> resolvers) {
        int separator = destination.indexOf(':');
        if (separator <= 0) return null;
        ActionLinkResolver resolver = resolvers.get(
            destination.substring(0, separator)
                .toLowerCase(Locale.ROOT));
        return resolver == null ? null : resolver.resolve(label, destination, defaultNamespace);
    }

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
        if (node instanceof MdAstLiteral text && text.value != null && text.value.contains(PLACEHOLDER_PREFIX)) {
            text.value = restoreText(text.value, maskResult);
        }
        if (node instanceof MdxJsxElementFields element) {
            for (var attribute : element.attributes()) {
                if (attribute instanceof MdxJsxAttribute named) {
                    if (named.hasStringValue()) named.setValue(restoreText(named.getStringValue(), maskResult));
                    else if (named.hasExpressionValue())
                        named.setExpression(restoreText(named.getExpressionValue(), maskResult));
                }
            }
        }
        if (node instanceof MdAstParent<?>parent) {
            for (var child : parent.children()) {
                if (child instanceof MdAstNode childNode) {
                    restoreNode(childNode, maskResult);
                }
            }
        }
    }

    private static String restoreText(String text, MaskResult maskResult) {
        if (text == null || !text.contains(PLACEHOLDER_PREFIX)) return text;
        StringBuilder restored = new StringBuilder(text.length());
        int cursor = 0;
        int start = text.indexOf(PLACEHOLDER_PREFIX);
        while (start >= 0) {
            int end = text.indexOf(PLACEHOLDER_SUFFIX, start + PLACEHOLDER_PREFIX.length());
            if (end < 0) break;
            end += PLACEHOLDER_SUFFIX.length();
            String placeholder = text.substring(start, end);
            restored.append(text, cursor, start)
                .append(
                    maskResult.actions()
                        .getOrDefault(placeholder, placeholder));
            cursor = end;
            start = text.indexOf(PLACEHOLDER_PREFIX, cursor);
        }
        return restored.append(text, cursor, text.length())
            .toString();
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
                result.add(new Segment(parsed.label, parsed.href, true, text.substring(start, parsed.endIndex)));
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
        private final String source;
        @Getter
        private final boolean link;

        public Segment(String text, String href, boolean link) {
            this(text, href, link, link ? "&[" + text + "](" + href + ")" : text);
        }

        public Segment(String text, String href, boolean link, String source) {
            this.text = text;
            this.href = href;
            this.link = link;
            this.source = source;
        }

        public String text() {
            return text;
        }

        public String href() {
            return href;
        }

        public String source() {
            return source;
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
