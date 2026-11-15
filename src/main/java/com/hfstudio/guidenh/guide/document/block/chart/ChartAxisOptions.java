package com.hfstudio.guidenh.guide.document.block.chart;

import com.hfstudio.guidenh.guide.color.ColorUtils;

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
    private int gridColor = ColorUtils.ARGB_33FFFFFF.getColor();
    private int axisColor = ColorUtils.ARGB_FF7A7A7A.getColor();
    private int labelColor = ColorUtils.ARGB_FFCCCCCC.getColor();

    /**
     * Format a numeric value using the configured tickFormat and unit; when tickFormat is unspecified,
     * automatically choose between integer / one-decimal formatting.
     */
    public String formatTick(double value) {
        return ChartNumberFormat.format(value, tickFormat, unit);
    }

}
