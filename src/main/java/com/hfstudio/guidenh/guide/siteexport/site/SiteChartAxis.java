package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.IllegalFormatException;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

/** One axis of an exported chart: the label, the range, the tick step and the formatting of its ticks. */
public class SiteChartAxis {

    @Nullable
    private final String label;
    @Nullable
    private final Double min;
    @Nullable
    private final Double max;
    @Nullable
    private final Double step;
    @Nullable
    private final String tickFormat;
    @Nullable
    private final String unit;
    private final boolean gridVisible;
    private final int gridColor;

    private SiteChartAxis(@Nullable String label, @Nullable Double min, @Nullable Double max, @Nullable Double step,
        @Nullable String tickFormat, @Nullable String unit, boolean gridVisible, int gridColor) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.tickFormat = tickFormat;
        this.unit = unit;
        this.gridVisible = gridVisible;
        this.gridColor = gridColor;
    }

    /** Reads one axis from a chart element. */
    public static SiteChartAxis read(MdxJsxElementFields el, String prefix, String gridFlagAttr, String gridColorAttr,
        int defaultGridColor) {
        int gridColor = defaultGridColor;
        String gridColorText = MdxAttrs.getString(el, gridColorAttr, null);
        if (gridColorText != null) {
            gridColor = ChartAttrParser.parseColor(gridColorText, defaultGridColor);
        }
        return new SiteChartAxis(
            MdxAttrs.getString(el, prefix + "Label", null),
            ChartAttrParser.parseBoxedDouble(el, prefix + "Min"),
            ChartAttrParser.parseBoxedDouble(el, prefix + "Max"),
            ChartAttrParser.parseBoxedDouble(el, prefix + "Step"),
            MdxAttrs.getString(el, prefix + "TickFormat", null),
            MdxAttrs.getString(el, prefix + "Unit", null),
            MdxAttrs.getBoolean(el, gridFlagAttr, true),
            gridColor);
    }

    public static SiteChartAxis automatic(int defaultGridColor) {
        return new SiteChartAxis(null, null, null, null, null, null, true, defaultGridColor);
    }

    @Nullable
    public String label() {
        return label;
    }

    @Nullable
    public Double min() {
        return min;
    }

    @Nullable
    public Double max() {
        return max;
    }

    @Nullable
    public Double step() {
        return step;
    }

    public boolean gridVisible() {
        return gridVisible;
    }

    public int gridColor() {
        return gridColor;
    }

    public double stepFor(double range) {
        if (step != null && step > 0 && Double.isFinite(step)) {
            return step;
        }
        return GuideSiteGraphRenderer.niceStepForAxis(range);
    }

    /** The range an axis shows: the declared bounds where they exist, the data otherwise. */
    public double[] rangeFor(double dataMin, double dataMax, double dataStep) {
        double low = min != null ? min : Math.floor(dataMin / dataStep) * dataStep;
        double high = max != null ? max : Math.ceil(dataMax / dataStep) * dataStep;
        if (low == high) {
            high = low + dataStep;
        }
        return new double[] { low, high };
    }

    public String formatTick(double value) {
        String text;
        if (tickFormat != null && !tickFormat.isEmpty()) {
            try {
                text = String.format(Locale.ROOT, tickFormat, value);
            } catch (IllegalFormatException e) {
                text = GuideSiteGraphRenderer.formatChartValue(value);
            }
        } else {
            text = GuideSiteGraphRenderer.formatChartValue(value);
        }
        return unit != null && !unit.isEmpty() ? text + unit : text;
    }
}
