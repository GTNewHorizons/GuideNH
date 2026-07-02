package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Legend placement position.
 */
public enum ChartLegendPosition {

    NONE,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT;

    public static ChartLegendPosition fromString(String s, ChartLegendPosition def) {
        if (s == null) {
            return def;
        }
        return switch (s.trim()
            .toLowerCase()) {
            case "none", "off", "false" -> NONE;
            case "top" -> TOP;
            case "bottom" -> BOTTOM;
            case "left" -> LEFT;
            case "right" -> RIGHT;
            default -> def;
        };
    }
}
