package com.hfstudio.guidenh.guide.document.block.chart;

import lombok.Getter;
import lombok.Setter;

/**
 * A data series in a chart, used by column / bar / line / scatter charts.
 * Index-based column/bar/line use only {@link #ys}; scatter uses both {@link #xs} and {@link #ys}.
 */
@Getter
public class ChartSeries {

    private final String name;
    private final int color;
    private final double[] xs;
    private final double[] ys;
    @Setter
    private ChartIcon icon;
    @Setter
    private String tooltipExtra;

    public ChartSeries(String name, int color, double[] xs, double[] ys) {
        this.name = name != null ? name : "";
        this.color = color;
        this.xs = xs != null ? xs : new double[0];
        this.ys = ys != null ? ys : new double[0];
    }

    public static ChartSeries fromValues(String name, int color, double[] values) {
        double[] indices;
        if (values == null) {
            indices = new double[0];
        } else {
            indices = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                indices[i] = i;
            }
        }
        return new ChartSeries(name, color, indices, values);
    }

    public int size() {
        return ys.length;
    }

}
