package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttributeNode;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxTextElement;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstFlowContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstHTML;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstPhrasingContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public class MarkdownHtmlRuntimeNormalizer {

    private static final Pattern SIMPLE_TAG_PATTERN = Pattern
        .compile("^<\\s*(/)?\\s*([A-Za-z][A-Za-z0-9:-]*)([^>]*)>$", Pattern.DOTALL);
    private static final Pattern ATTRIBUTE_PATTERN = Pattern
        .compile("([A-Za-z_:][A-Za-z0-9_:\\-]*)" + "(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s\"'=<>`]+)))?");
    private static final Pattern DETAILS_OPEN_PATTERN = Pattern.compile(
        "^<\\s*details\\b([^>]*)>" + "(?:\\s*<\\s*summary\\b[^>]*>(.*?)</\\s*summary\\s*>)?\\s*$",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Set<String> TABLE_FLOW_TAGS = Set
        .of("table", "caption", "colgroup", "col", "thead", "tbody", "tfoot", "tr", "th", "td");
    private static final Set<String> HTML_VOID_ELEMENTS = Set
        .of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "source", "track", "wbr");

    private MarkdownHtmlRuntimeNormalizer() {}

    public static void normalize(MdAstRoot root) {
        normalizeParent(root);
    }

    private static void normalizeParent(MdAstParent<?> parent) {
        List<?> children = parent.children();
        for (Object child : new ArrayList<Object>(children)) {
            if (child instanceof MdAstParent<?>childParent) {
                normalizeParent(childParent);
            }
        }

        if (isPhrasingParent(parent)) {
            normalizePhrasingChildren(castPhrasingChildren(parent.children()));
        } else {
            if (parent instanceof MdxJsxFlowElement flowElement && "table".equals(flowElement.name())) {
                normalizeEmbeddedHtmlTable(flowElement);
            }
            normalizeFlowChildren(castAnyChildren(parent.children()));
        }
    }

    private static boolean isPhrasingParent(MdAstParent<?> parent) {
        return parent instanceof MdAstParagraph || parent instanceof MdxJsxTextElement
            || "link".equals(parent.type())
            || "strong".equals(parent.type())
            || "emphasis".equals(parent.type())
            || "delete".equals(parent.type())
            || "heading".equals(parent.type());
    }

    @SuppressWarnings("unchecked")
    private static List<MdAstPhrasingContent> castPhrasingChildren(List<?> children) {
        return (List<MdAstPhrasingContent>) children;
    }

    @SuppressWarnings("unchecked")
    private static List<MdAstAnyContent> castAnyChildren(List<?> children) {
        return (List<MdAstAnyContent>) children;
    }

    private static void normalizePhrasingChildren(List<MdAstPhrasingContent> children) {
        for (int i = 0; i < children.size(); i++) {
            if (!(children.get(i) instanceof MdAstHTML html)) {
                continue;
            }

            TagInfo info = parseSimpleTag(html.value);
            if (info == null) {
                continue;
            }

            String name = info.name();
            if (("br".equals(name) || "wbr".equals(name)) && !info.closing()) {
                children.set(i, createTextElement(info, new ArrayList<>()));
                continue;
            }

            if ("a".equals(name) && !info.closing() && info.selfClosing()) {
                children.set(i, createTextElement(info, new ArrayList<>()));
                continue;
            }

            if (!isSupportedInlinePair(name) || info.closing() || info.selfClosing()) {
                continue;
            }

            int closingIndex = findClosingInlineTag(children, i + 1, name);
            if (closingIndex < 0) {
                continue;
            }

            List<MdAstPhrasingContent> nestedChildren = new ArrayList<>(children.subList(i + 1, closingIndex));
            MdxJsxTextElement element = createTextElement(info, nestedChildren);
            normalizeParent(element);
            replaceRange(children, i, closingIndex, element);
        }
    }

    /**
     * CommonMark treats a complete HTML table as one raw HTML block. Convert that
     * block to the same MDX tree produced by an explicit JSX table so every
     * renderer can use the shared table compiler.
     */
    private static MdxJsxFlowElement parseHtmlTable(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        Deque<MdxJsxElementFields> stack = new ArrayDeque<>();
        MdxJsxFlowElement root = null;
        int cursor = 0;
        while (cursor < source.length()) {
            int tagStart = source.indexOf('<', cursor);
            if (tagStart < 0) {
                appendHtmlText(stack.peek(), source.substring(cursor));
                break;
            }
            appendHtmlText(stack.peek(), source.substring(cursor, tagStart));

            if (source.startsWith("<!--", tagStart)) {
                int commentEnd = source.indexOf("-->", tagStart + 4);
                if (commentEnd < 0) {
                    return null;
                }
                cursor = commentEnd + 3;
                continue;
            }

            int tagEnd = findHtmlTagEnd(source, tagStart + 1);
            if (tagEnd < 0) {
                return null;
            }
            TagInfo info = parseSimpleTag(source.substring(tagStart, tagEnd + 1));
            if (info == null) {
                return null;
            }

            if (info.closing()) {
                if (stack.isEmpty() || !info.name()
                    .equals(
                        stack.peek()
                            .name())) {
                    return null;
                }
                stack.pop();
            } else {
                MdxJsxElementFields element = createHtmlTableElement(info);
                if (stack.isEmpty()) {
                    if (root != null || !"table".equals(info.name())) {
                        return null;
                    }
                    root = (MdxJsxFlowElement) element;
                } else {
                    appendHtmlChild(stack.peek(), (MdAstNode) element);
                }
                if (!info.selfClosing() && !isHtmlVoidElement(info.name())) {
                    stack.push(element);
                }
            }
            cursor = tagEnd + 1;
        }

        return root != null && stack.isEmpty() ? root : null;
    }

    private static void normalizeEmbeddedHtmlTable(MdxJsxFlowElement table) {
        List<MdAstAnyContent> children = castAnyChildren(table.children());
        if (children.size() != 1 || !(children.getFirst() instanceof MdAstHTML html)) {
            return;
        }
        String body = html.value.replaceFirst("(?is)</\\s*table\\s*>\\s*$", "");
        MdxJsxFlowElement parsed = parseHtmlTable("<table>" + body + "</table>");
        if (parsed == null) {
            return;
        }
        children.clear();
        children.addAll(castAnyChildren(parsed.children()));
    }

    private static MdxJsxElementFields createHtmlTableElement(TagInfo info) {
        List<MdxJsxAttributeNode> attributes = new ArrayList<>();
        for (Map.Entry<String, String> entry : info.attributes()
            .entrySet()) {
            attributes.add(
                new MdxJsxAttribute(
                    entry.getKey()
                        .toLowerCase(Locale.ROOT),
                    entry.getValue()));
        }
        if (TABLE_FLOW_TAGS.contains(info.name())) {
            return new MdxJsxFlowElement(info.name(), attributes);
        }
        return new MdxJsxTextElement(info.name(), attributes);
    }

    private static void appendHtmlText(MdxJsxElementFields parent, String value) {
        if (parent == null || value == null || value.isEmpty()) {
            return;
        }
        String normalized = value.replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return;
        }
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            normalized = normalized.trim();
        }
        MdAstText text = new MdAstText();
        text.value = normalized;
        appendHtmlChild(parent, text);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void appendHtmlChild(MdxJsxElementFields parent, MdAstNode child) {
        ((List) parent.children()).add(child);
    }

    private static boolean isHtmlVoidElement(String name) {
        return HTML_VOID_ELEMENTS.contains(name);
    }

    private static int findHtmlTagEnd(String source, int start) {
        char quote = 0;
        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
            } else if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '>') {
                return index;
            }
        }
        return -1;
    }

    private static void normalizeFlowChildren(List<MdAstAnyContent> children) {
        for (int i = 0; i < children.size(); i++) {
            if (!(children.get(i) instanceof MdAstHTML html)) {
                continue;
            }

            MdxJsxFlowElement table = parseHtmlTable(html.value);
            if (table != null) {
                children.set(i, table);
                continue;
            }

            TagInfo info = parseSimpleTag(html.value);
            if (info != null && isSupportedStandaloneFlowTag(info)) {
                children.set(i, createFlowElement(info, new ArrayList<>()));
                continue;
            }

            DetailsOpenTag detailsOpen = parseDetailsOpen(html.value);
            if (detailsOpen != null) {
                int closingIndex = findClosingFlowTag(children, i + 1, "details");
                if (closingIndex < 0) {
                    continue;
                }

                List<MdAstFlowContent> nestedChildren = collectFlowChildren(children, i + 1, closingIndex);
                if (nestedChildren == null) {
                    continue;
                }

                MdxJsxFlowElement element = new MdxJsxFlowElement();
                element.setName("details");
                copyAttributes(element.attributes(), detailsOpen.attributes());
                if (detailsOpen.open()) {
                    element.addAttribute("open", "");
                }
                if (!detailsOpen.summaryText()
                    .isEmpty()) {
                    element.addChild(createSummaryElement(detailsOpen.summaryText()));
                }
                for (MdAstFlowContent child : nestedChildren) {
                    element.addChild((MdAstNode) child);
                }
                normalizeParent(element);
                replaceRange(children, i, closingIndex, element);
                continue;
            }

            if (info == null || info.closing() || info.selfClosing() || !"div".equals(info.name())) {
                continue;
            }

            int closingIndex = findClosingFlowTag(children, i + 1, "div");
            if (closingIndex < 0) {
                continue;
            }

            List<MdAstFlowContent> nestedChildren = collectFlowChildren(children, i + 1, closingIndex);
            if (nestedChildren == null) {
                continue;
            }

            MdxJsxFlowElement element = createFlowElement(info, nestedChildren);
            normalizeParent(element);
            replaceRange(children, i, closingIndex, element);
        }
    }

    private static boolean isSupportedInlinePair(String name) {
        return "a".equals(name) || "kbd".equals(name) || "sub".equals(name) || "sup".equals(name);
    }

    private static boolean isSupportedStandaloneFlowTag(TagInfo info) {
        return !info.closing()
            && ("a".equals(info.name()) && info.selfClosing() || "br".equals(info.name()) || "wbr".equals(info.name()));
    }

    private static int findClosingInlineTag(List<MdAstPhrasingContent> children, int start, String name) {
        for (int i = start; i < children.size(); i++) {
            if (children.get(i) instanceof MdAstHTML html) {
                TagInfo info = parseSimpleTag(html.value);
                if (info != null && info.closing() && name.equals(info.name())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int findClosingFlowTag(List<MdAstAnyContent> children, int start, String name) {
        for (int i = start; i < children.size(); i++) {
            if (children.get(i) instanceof MdAstHTML html) {
                TagInfo info = parseSimpleTag(html.value);
                if (info != null && info.closing() && name.equals(info.name())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<MdAstFlowContent> collectFlowChildren(List<MdAstAnyContent> children, int start, int end) {
        List<MdAstFlowContent> result = new ArrayList<>(Math.max(0, end - start));
        for (int i = start; i < end; i++) {
            MdAstAnyContent child = children.get(i);
            if (!(child instanceof MdAstFlowContent flowContent)) {
                return null;
            }
            result.add(flowContent);
        }
        return result;
    }

    private static MdxJsxFlowElement createSummaryElement(String summaryText) {
        MdxJsxFlowElement summary = new MdxJsxFlowElement();
        summary.setName("summary");

        MdAstParagraph paragraph = new MdAstParagraph();
        MdAstText text = new MdAstText();
        text.setValue(summaryText);
        paragraph.addChild(text);
        summary.addChild(paragraph);
        return summary;
    }

    private static MdxJsxTextElement createTextElement(TagInfo info, List<MdAstPhrasingContent> children) {
        MdxJsxTextElement element = new MdxJsxTextElement();
        element.setName(info.name());
        copyAttributes(element.attributes(), info.attributes());
        for (MdAstPhrasingContent child : children) {
            element.addChild((MdAstNode) child);
        }
        return element;
    }

    private static MdxJsxFlowElement createFlowElement(TagInfo info, List<MdAstFlowContent> children) {
        MdxJsxFlowElement element = new MdxJsxFlowElement();
        element.setName(info.name());
        copyAttributes(element.attributes(), info.attributes());
        for (MdAstFlowContent child : children) {
            element.addChild((MdAstNode) child);
        }
        return element;
    }

    private static void copyAttributes(List<MdxJsxAttributeNode> target, Map<String, String> attributes) {
        for (var entry : attributes.entrySet()) {
            target.add(new MdxJsxAttribute(entry.getKey(), entry.getValue()));
        }
    }

    private static <T> void replaceRange(List<T> children, int startInclusive, int endInclusive, T replacement) {
        children.subList(startInclusive, endInclusive + 1)
            .clear();
        children.add(startInclusive, replacement);
    }

    private static DetailsOpenTag parseDetailsOpen(String html) {
        Matcher matcher = DETAILS_OPEN_PATTERN.matcher(html == null ? "" : html);
        if (!matcher.matches()) {
            return null;
        }

        Map<String, String> attributes = parseAttributes(matcher.group(1));
        boolean open = attributes.containsKey("open");
        String summaryText = stripHtmlTags(matcher.group(2));
        attributes.remove("open");
        return new DetailsOpenTag(attributes, summaryText, open);
    }

    private static TagInfo parseSimpleTag(String html) {
        Matcher matcher = SIMPLE_TAG_PATTERN.matcher(html == null ? "" : html.trim());
        if (!matcher.matches()) {
            return null;
        }

        boolean closing = matcher.group(1) != null;
        String rawName = matcher.group(2);
        String rest = matcher.group(3) != null ? matcher.group(3) : "";
        boolean selfClosing = !closing && rest.trim()
            .endsWith("/");
        String normalizedName = rawName.toLowerCase(Locale.ROOT);
        return new TagInfo(normalizedName, closing, selfClosing, parseAttributes(rest));
    }

    private static Map<String, String> parseAttributes(String raw) {
        Map<String, String> attributes = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) {
            return attributes;
        }

        String normalized = raw.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(2);
            if (value == null) {
                value = matcher.group(3);
            }
            if (value == null) {
                value = matcher.group(4);
            }
            attributes.put(name, value != null ? value : "");
        }
        return attributes;
    }

    private static String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }

        StringBuilder stripped = new StringBuilder(html.length());
        boolean inTag = false;
        for (int i = 0; i < html.length(); i++) {
            char current = html.charAt(i);
            if (current == '<') {
                inTag = true;
                continue;
            }
            if (current == '>') {
                inTag = false;
                continue;
            }
            if (!inTag) {
                stripped.append(current);
            }
        }
        return stripped.toString()
            .trim();
    }

    private record TagInfo(String name, boolean closing, boolean selfClosing, Map<String, String> attributes) {}

    private record DetailsOpenTag(Map<String, String> attributes, String summaryText, boolean open) {}
}
