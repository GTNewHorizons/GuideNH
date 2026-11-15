package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.regex.Pattern;

public class ChartNumberFormat {

    private static final Pattern FORMAT_TOKEN = Pattern
        .compile("%(?:\\d+\\$)?[-#+ 0,(<]*(\\d+)?(?:\\.(\\d+))?[a-zA-Z%]");

    protected ChartNumberFormat() {}

    public static String format(double value, String template, String unit) {
        String text;
        if (template != null && !template.isEmpty() && isBounded(template)) {
            try {
                text = String.format(Locale.ROOT, template, value);
            } catch (IllegalFormatException ex) {
                text = formatValue(value);
            }
        } else {
            text = formatValue(value);
        }
        return unit != null && !unit.isEmpty() ? text + unit : text;
    }

    public static String formatValue(double value) {
        if (!Double.isFinite(value) || value < Long.MIN_VALUE || value >= 0x1p63) return Double.toString(value);
        if (Math.abs(value - Math.rint(value)) < 1e-6) return Long.toString((long) Math.rint(value));
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static boolean isBounded(String template) {
        if (template.length() > 256) return false;
        var matcher = FORMAT_TOKEN.matcher(template);
        while (matcher.find()) {
            if (!withinLimit(matcher.group(1), 128) || !withinLimit(matcher.group(2), 20)) return false;
        }
        return true;
    }

    private static boolean withinLimit(String digits, int limit) {
        if (digits == null) return true;
        int value = 0;
        for (int i = 0; i < digits.length(); i++) {
            value = value * 10 + digits.charAt(i) - '0';
            if (value > limit) return false;
        }
        return true;
    }
}
