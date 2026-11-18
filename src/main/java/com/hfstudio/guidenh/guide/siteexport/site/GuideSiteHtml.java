package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.color.ColorValue;

public class GuideSiteHtml {

    private GuideSiteHtml() {}

    public static void appendEscapedHtml(StringBuilder target, CharSequence text) {
        if (target == null || text == null || text.isEmpty()) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '&' -> target.append("&amp;");
                case '<' -> target.append("&lt;");
                case '>' -> target.append("&gt;");
                case '"' -> target.append("&quot;");
                default -> target.append(text.charAt(i));
            }
        }
    }

    public static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text != null ? text : "";
        }
        boolean requiresEscaping = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' || ch == '<' || ch == '>' || ch == '"') {
                requiresEscaping = true;
                break;
            }
        }
        if (!requiresEscaping) {
            return text;
        }
        StringBuilder escaped = new StringBuilder(text.length() + 16);
        appendEscapedHtml(escaped, text);
        return escaped.toString();
    }

    public static String escapeAttribute(String text) {
        return escapeHtml(text);
    }

    public static String toCssColor(@Nullable ColorValue color) {
        int argb = color != null ? color.resolve() : ColorUtils.WHITE.getColor();
        int alpha = argb >>> 24 & 0xFF;
        int red = argb >>> 16 & 0xFF;
        int green = argb >>> 8 & 0xFF;
        int blue = argb & 0xFF;
        return "rgba(" + red + "," + green + "," + blue + "," + alpha / 255.0f + ")";
    }

    public static String argbToRgba(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0xFF) {
            return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
        }
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)", r, g, b, a / 255.0);
    }
}
