package com.hfstudio.guidenh.guide.document.block.chart;

import lombok.Getter;
import lombok.Setter;

/**
 * A single slice of a pie chart.
 */
@Getter
public class PieSlice {

    private final String label;
    private final double value;
    private final int color;
    @Setter
    private ChartIcon icon;
    @Setter
    private String tooltipExtra;

    public PieSlice(String label, double value, int color) {
        this.label = label != null ? label : "";
        this.value = value;
        this.color = color;
    }

}
