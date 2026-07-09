package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.IllegalFormatException;

import lombok.Getter;
import lombok.Setter;

/**
 * Cartesian chart axis configuration. All numeric fields use boxed types; {@code null} means auto.
 */
@Getter
@Setter
public class ChartAxisOptions {

    private String label;
    private Double min;
    private Double max;
    private Double step;
    private String tickFormat;
    private String unit;
    private boolean gridVisible;
    private int gridColor = 0x33FFFFFF;
    private int axisColor = 0xFF7A7A7A;
    private int labelColor = 0xFFCCCCCC;

    /**
     * Format a numeric value using the configured tickFormat and unit; when tickFormat is unspecified,
     * automatically choose between integer / one-decimal formatting.
     */
    public String formatTick(double value) {
        String text;
        if (tickFormat != null && !tickFormat.isEmpty()) {
            try {
                text = String.format(tickFormat, value);
            } catch (IllegalFormatException ex) {
                text = defaultFormat(value);
            }
        } else {
            text = defaultFormat(value);
        }
        if (unit != null && !unit.isEmpty()) {
            text = text + unit;
        }
        return text;
    }

    private static String defaultFormat(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }
}
