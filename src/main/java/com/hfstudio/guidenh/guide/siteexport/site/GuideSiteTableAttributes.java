package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxFlowElement;

public class GuideSiteTableAttributes {

    private GuideSiteTableAttributes() {}

    public static void appendColumnWidths(List<String> widths, MdxJsxFlowElement column) {
        String width = normalizeTableLength(column.getAttributeString("width", ""));
        int span = parseTableSpanValue(column.getAttributeString("span", null));
        for (int index = 0; index < span && widths.size() < 1024; index++) {
            widths.add(width == null ? "" : width);
        }
    }

    public static void appendTableAttribute(StringBuilder html, String name, @Nullable String value) {
        if (value == null || value.isBlank()
            || value.length() > 4096
            || value.chars()
                .anyMatch(character -> character < 0x20 && character != '\t')) {
            return;
        }
        html.append(' ')
            .append(name)
            .append("=\"")
            .append(escapeTableAttribute(value))
            .append('"');
    }

    @Nullable
    public static String sanitizeTableStyle(@Nullable String value) {
        if (value == null || value.isBlank() || value.length() > 4096) {
            return null;
        }
        String normalized = value.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        String compact = lower.replaceAll("[\\u0000-\\u0020]+", "");
        if (compact.contains("javascript:") || compact.contains("vbscript:")
            || compact.contains("data:")
            || compact.contains("expression(")
            || compact.contains("url(")
            || normalized.indexOf('\\') >= 0
            || normalized.indexOf('<') >= 0
            || normalized.indexOf('>') >= 0
            || normalized.chars()
                .anyMatch(character -> character < 0x20 && character != '\t')) {
            return null;
        }
        return normalized;
    }

    @Nullable
    public static String sanitizeTableStyleValue(@Nullable String value) {
        if (value == null || value.isBlank() || value.length() > 512) {
            return null;
        }
        String normalized = value.trim();
        String compact = normalized.toLowerCase(Locale.ROOT)
            .replaceAll("[\\u0000-\\u0020]+", "");
        if (normalized.contains("<") || normalized.contains(">")
            || normalized.indexOf('\\') >= 0
            || normalized.contains(";")
            || compact.contains("url(")
            || compact.contains("expression(")
            || compact.contains("javascript:")
            || compact.contains("vbscript:")
            || compact.contains("data:")
            || normalized.chars()
                .anyMatch(character -> character < 0x20 && character != '\t')) {
            return null;
        }
        return normalized;
    }

    public static void appendTableNumericAttribute(StringBuilder html, String name, @Nullable String value) {
        if (value == null || !value.trim()
            .matches("\\d+(?:px)?")) {
            return;
        }
        html.append(' ')
            .append(name)
            .append("=\"")
            .append(escapeTableAttribute(value.trim()))
            .append('"');
    }

    public static void appendTableLengthAttribute(StringBuilder html, String name, @Nullable String value) {
        String normalized = normalizeTableLength(value);
        if (normalized != null) {
            html.append(' ')
                .append(name)
                .append("=\"")
                .append(escapeTableAttribute(normalized))
                .append('"');
        }
    }

    @Nullable
    public static String normalizeTableLength(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (!normalized.matches("\\d+(?:\\.\\d+)?(?:px|%|em|rem|vw|vh|pt)?")) {
            return null;
        }
        return normalized;
    }

    public static void appendTableAlignment(StringBuilder style, String align) {
        switch (align) {
            case "center" -> style.append("width:100%;max-width:100%;margin-left:auto;margin-right:auto;");
            case "right" -> style.append("width:100%;max-width:100%;margin-left:auto;margin-right:0;");
            case "left" -> style.append("width:100%;max-width:100%;margin-left:0;margin-right:auto;");
            default -> {}
        }
    }

    @Nullable
    public static String findTableChildAttribute(MdxJsxElementFields table, String childName, String attribute) {
        for (Object child : table.children()) {
            if (child instanceof MdxJsxFlowElement element && childName.equalsIgnoreCase(element.name())) {
                return element.getAttributeString(attribute, null);
            }
        }
        return null;
    }

    public static String escapeTableAttribute(String value) {
        return value.replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    public static void appendTableSpan(StringBuilder html, String attribute, int span) {
        if (span > 1) {
            html.append(' ')
                .append(attribute)
                .append("=\"")
                .append(span)
                .append('"');
        }
    }

    public static void appendTableSpan(StringBuilder html, String attribute, @Nullable String rawValue) {
        appendTableSpan(html, attribute, parseTableSpanValue(rawValue));
    }

    public static int parseTableSpanValue(@Nullable String rawValue) {
        if (rawValue == null) {
            return 1;
        }
        try {
            int span = Integer.parseInt(rawValue);
            return Math.max(1, Math.min(1024, span));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
