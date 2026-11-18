package com.hfstudio.guidenh.guide.siteexport.site;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartAttrParser;
import com.hfstudio.guidenh.guide.compiler.tags.chart.ChartChildParser;
import com.hfstudio.guidenh.guide.document.block.chart.AxisRange;
import com.hfstudio.guidenh.guide.document.block.chart.ChartAxisOptions;
import com.hfstudio.guidenh.guide.document.block.chart.ChartNumberFormat;
import com.hfstudio.guidenh.guide.document.block.chart.ChartReferenceLine;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

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
    private final List<ChartReferenceLine> referenceLines;

    private SiteChartAxis(@Nullable String label, @Nullable Double min, @Nullable Double max, @Nullable Double step,
        @Nullable String tickFormat, @Nullable String unit, boolean gridVisible, int gridColor,
        List<ChartReferenceLine> referenceLines) {
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.tickFormat = tickFormat;
        this.unit = unit;
        this.gridVisible = gridVisible;
        this.gridColor = gridColor;
        this.referenceLines = List.copyOf(referenceLines);
    }

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
            gridColor,
            readReferenceLines(el, prefix));
    }

    private static List<ChartReferenceLine> readReferenceLines(MdxJsxElementFields el, String prefix) {
        boolean bar = "BarChart".equals(el.name());
        if ((bar ? "xAxis" : "yAxis").equals(prefix)) {
            List<ChartReferenceLine> lines = new ArrayList<>();
            for (var childEl : ChartChildParser.childElements(el)) {
                if (!"ReferenceLine".equals(childEl.name())) continue;
                var line = ChartChildParser.readReferenceLine(childEl, bar ? "x" : "y");
                if (line != null) lines.add(line);
            }
            return lines;
        }
        return List.of();
    }

    public static SiteChartAxis automatic(int defaultGridColor) {
        return new SiteChartAxis(null, null, null, null, null, null, true, defaultGridColor, List.of());
    }

    public static SiteChartAxis from(ChartAxisOptions axis, int defaultGridColor) {
        if (axis == null) {
            return automatic(defaultGridColor);
        }
        return new SiteChartAxis(
            axis.getLabel(),
            axis.getMin(),
            axis.getMax(),
            axis.getStep(),
            axis.getTickFormat(),
            axis.getUnit(),
            axis.isGridVisible(),
            axis.getGridColor(),
            axis.getReferenceLines());
    }

    public List<ChartReferenceLine> referenceLines() {
        return referenceLines;
    }

    @Nullable
    public String label() {
        return label;
    }

    @Nullable
    public String unit() {
        return unit;
    }

    @Nullable
    public String tickFormat() {
        return tickFormat;
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

    public double[] rangeFor(double dataMin, double dataMax, double dataStep) {
        AxisRange range = AxisRange.compute(min, max, dataStep, dataMin, dataMax);
        return new double[] { range.min, range.max };
    }

    public String formatTick(double value) {
        return ChartNumberFormat.format(value, tickFormat, unit);
    }

}
