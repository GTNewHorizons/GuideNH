package com.hfstudio.guidenh.guide.document.block.chart;

/**
 * Placement position for data value labels.
 */
public enum ChartLabelPosition {

    NONE,
    INSIDE,
    OUTSIDE,
    ABOVE,
    BELOW,
    CENTER;

    public static ChartLabelPosition fromString(String s, ChartLabelPosition def) {
        if (s == null) {
            return def;
        }
        return switch (s.trim()
            .toLowerCase()) {
            case "none" -> NONE;
            case "inside", "in" -> INSIDE;
            case "outside", "out" -> OUTSIDE;
            case "above", "top" -> ABOVE;
            case "below", "bottom" -> BELOW;
            case "center", "middle" -> CENTER;
            default -> def;
        };
    }
}
