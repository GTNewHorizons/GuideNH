package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.appendEscapedHtml;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeAttribute;
import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.escapeHtml;

import org.jetbrains.annotations.Nullable;

/**
 * Interprets Minecraft formatting codes in exported tooltip text.
 */
public class GuideSiteLegacyTextRenderer {

    private GuideSiteLegacyTextRenderer() {}

    public static String renderPlainTextFragment(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.indexOf('\\') >= 0 ? text.replace("\\n", "\n") : text;
        if (normalized.indexOf('\n') < 0) {
            return renderLegacyFormattedText(normalized);
        }
        String[] lines = normalized.split("\\n", -1);
        StringBuilder html = new StringBuilder();
        for (String line : lines) {
            if (!html.isEmpty()) {
                html.append("<br>");
            }
            html.append(renderLegacyFormattedText(line));
        }
        return html.toString();
    }

    public static String renderPlainTextTooltip(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.indexOf('\\') >= 0 ? text.replace("\\n", "\n") : text;
        if (normalized.indexOf('\n') < 0) {
            return "<p>" + renderLegacyFormattedText(normalized) + "</p>";
        }
        String[] lines = normalized.split("\\n", -1);
        StringBuilder html = new StringBuilder();
        for (String line : lines) {
            html.append("<p>")
                .append(renderLegacyFormattedText(line))
                .append("</p>");
        }
        return html.toString();
    }

    public static String renderLegacyFormattedText(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (text.indexOf('§') < 0) {
            return escapeHtml(text);
        }

        LegacyStyle style = new LegacyStyle();
        StringBuilder html = new StringBuilder();
        StringBuilder segment = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '§' && i + 1 < text.length()) {
                appendLegacySegment(html, segment, style);
                style.apply(text.charAt(++i));
                continue;
            }
            segment.append(ch);
        }
        appendLegacySegment(html, segment, style);
        return html.toString();
    }

    private static void appendLegacySegment(StringBuilder html, StringBuilder segment, LegacyStyle style) {
        if (segment.isEmpty()) {
            return;
        }
        StringBuilder css = new StringBuilder();
        if (style.color != null) {
            css.append("color:")
                .append(style.color)
                .append(";");
        }
        if (style.bold) {
            css.append("font-weight:700;");
        }
        if (style.italic) {
            css.append("font-style:italic;");
        }
        if (style.underlined || style.strikethrough) {
            css.append("text-decoration:");
            if (style.underlined) {
                css.append(" underline");
            }
            if (style.strikethrough) {
                css.append(" line-through");
            }
            css.append(";");
        }
        if (style.obfuscated) {
            css.append("filter:blur(0.6px);");
        }

        if (css.isEmpty()) {
            appendEscapedHtml(html, segment);
            segment.setLength(0);
            return;
        }

        html.append("<span style=\"")
            .append(escapeAttribute(css.toString()))
            .append("\">");
        appendEscapedHtml(html, segment);
        html.append("</span>");
        segment.setLength(0);
    }

    private static class LegacyStyle {

        @Nullable
        private String color;
        private boolean bold;
        private boolean italic;
        private boolean underlined;
        private boolean strikethrough;
        private boolean obfuscated;

        private void apply(char rawCode) {
            char code = Character.toLowerCase(rawCode);
            String nextColor = switch (code) {
                case '0' -> "#000000";
                case '1' -> "#0000AA";
                case '2' -> "#00AA00";
                case '3' -> "#00AAAA";
                case '4' -> "#AA0000";
                case '5' -> "#AA00AA";
                case '6' -> "#FFAA00";
                case '7' -> "#AAAAAA";
                case '8' -> "#555555";
                case '9' -> "#5555FF";
                case 'a' -> "#55FF55";
                case 'b' -> "#55FFFF";
                case 'c' -> "#FF5555";
                case 'd' -> "#FF55FF";
                case 'e' -> "#FFFF55";
                case 'f' -> "#FFFFFF";
                default -> null;
            };

            if (nextColor != null) {
                reset();
                color = nextColor;
                return;
            }

            switch (code) {
                case 'k':
                    obfuscated = true;
                    break;
                case 'l':
                    bold = true;
                    break;
                case 'm':
                    strikethrough = true;
                    break;
                case 'n':
                    underlined = true;
                    break;
                case 'o':
                    italic = true;
                    break;
                case 'r':
                    reset();
                    break;
                default:
                    break;
            }
        }

        private void reset() {
            color = null;
            bold = false;
            italic = false;
            underlined = false;
            strikethrough = false;
            obfuscated = false;
        }
    }
}
