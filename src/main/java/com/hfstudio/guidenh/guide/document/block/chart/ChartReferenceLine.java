package com.hfstudio.guidenh.guide.document.block.chart;

/** A reference on the numeric value axis; horizontal bar charts interpret y as an X value. */
public record ChartReferenceLine(double y, String label, int color) {

    public static final int DEFAULT_COLOR = 0x8C9CA3AF;

    public ChartReferenceLine {
        if (!Double.isFinite(y)) {
            throw new IllegalArgumentException("ReferenceLine requires a finite y value");
        }
        label = label != null ? label : "";
    }
}
