package com.hfstudio.guidenh.guide.document.block.chart;

public record ChartReferenceLine(double value, String label, int color) {

    public static final int DEFAULT_COLOR = 0x8C9CA3AF;

    public ChartReferenceLine {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("ReferenceLine requires a finite value");
        }
        label = label != null ? label : "";
    }
}
