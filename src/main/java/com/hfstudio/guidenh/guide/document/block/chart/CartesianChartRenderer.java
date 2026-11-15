package com.hfstudio.guidenh.guide.document.block.chart;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Shared axis / grid / data-value-label rendering for Cartesian charts (column / bar / line / scatter).
 */
public class CartesianChartRenderer {

    private static final int CURSOR_COLOR = 0xFFF2C14E;
    private static final ResolvedTextStyle CURSOR_STYLE = LytChartBase.textStyle(0xFF161A22);

    protected CartesianChartRenderer() {}

    /**
     * Compute insets reserved for axis labels; returns [left, top, right, bottom] (pixels).
     */
    public static int[] computeAxisInsets(RenderContext context, ChartAxisOptions xAxis, ChartAxisOptions yAxis,
        AxisRange xRange, AxisRange yRange, String[] xCategories, boolean showXTicks, boolean showYTicks) {
        ResolvedTextStyle style = LytChartBase.textStyle(ColorUtils.ARGB_FFCCCCCC.getColor());
        int lineH = context.getLineHeight(style);
        int left = 4;
        int top = 4;
        int right = 4;
        int bottom = 4;
        if (showYTicks && yRange != null) {
            int maxLabel = 0;
            ChartTicks ticks = ChartTicks.create(yRange.min, yRange.max, yRange.step);
            for (int i = 0; i < ticks.count(); i++) {
                double t = ticks.value(i);
                String s = yAxis.formatTick(t);
                int w = context.getStringWidth(s, style);
                if (w > maxLabel) {
                    maxLabel = w;
                }
            }
            double nearMin = yRange.min * 0.9999 + yRange.max * 0.0001;
            double nearMax = yRange.max * 0.9999 + yRange.min * 0.0001;
            maxLabel = Math.max(maxLabel, context.getStringWidth(yAxis.formatTick(nearMin), style));
            maxLabel = Math.max(maxLabel, context.getStringWidth(yAxis.formatTick(nearMax), style));
            left = Math.max(left, maxLabel + 10);
            // Y-axis title is drawn horizontally above the plot's top-left corner; reserve a row.
            if (yAxis.getLabel() != null && !yAxis.getLabel()
                .isEmpty()) {
                top = Math.max(top, lineH + 4);
            }
        }
        if (showXTicks) {
            bottom = Math.max(bottom, lineH + 4);
            if (xAxis.getLabel() != null && !xAxis.getLabel()
                .isEmpty()) {
                bottom += lineH + 2;
            }
            // Reserve a small right margin so the last X tick label does not overflow the plot.
            if (xRange != null) {
                String last = xAxis.formatTick(xRange.max);
                right = Math.max(right, context.getStringWidth(last, style) / 2 + 2);
            } else if (xCategories != null && xCategories.length > 0) {
                String last = xCategories[xCategories.length - 1];
                if (last != null) {
                    right = Math.max(right, context.getStringWidth(last, style) / 2 + 2);
                }
                String first = xCategories[0];
                if (first != null) {
                    left = Math.max(left, context.getStringWidth(first, style) / 2 + 2);
                }
            }
        }
        return new int[] { left, top, right, bottom };
    }

    /**
     * Data value -> screen X coordinate.
     */
    public static float mapX(double value, AxisRange range, LytRect plotRect) {
        double t = range.normalize(value);
        return (float) (plotRect.x() + t * plotRect.width());
    }

    /**
     * Data value -> screen Y coordinate (Y axis points up, so it is inverted).
     */
    public static float mapY(double value, AxisRange range, LytRect plotRect) {
        double t = range.normalize(value);
        return (float) (plotRect.bottom() - t * plotRect.height());
    }

    public static void drawHoverCursor(RenderContext context, LytRect plot, ChartAxisOptions axis, AxisRange range,
        float mouseX, float mouseY) {
        if (!Float.isFinite(mouseX) || !Float.isFinite(mouseY)
            || plot.isEmpty()
            || mouseX < plot.x()
            || mouseX > plot.right()
            || mouseY < plot.y()
            || mouseY > plot.bottom()) return;
        int accent = CURSOR_COLOR;
        context.drawDashedLine(plot.x(), mouseY, plot.right(), mouseY, 1, 4, 3, accent);
        double fraction = (mouseY - plot.y()) / (double) plot.height();
        double value = (1 - fraction) * range.max + fraction * range.min;
        String label = axis.formatTick(value);
        ResolvedTextStyle style = CURSOR_STYLE;
        int textWidth = context.getStringWidth(label, style);
        int lineHeight = context.getLineHeight(style);
        int right = plot.x() - 1;
        int top = Math.round(mouseY) - lineHeight / 2 - 2;
        context.fillRect(right - textWidth - 8, top, textWidth + 8, lineHeight + 4, accent);
        context.drawText(label, right - textWidth - 4, top + 2, style);
    }

    public static void drawVerticalHoverCursor(RenderContext context, LytRect plot, ChartAxisOptions axis,
        AxisRange range, float mouseX, float mouseY) {
        if (!Float.isFinite(mouseX) || !Float.isFinite(mouseY)
            || plot.isEmpty()
            || mouseX < plot.x()
            || mouseX > plot.right()
            || mouseY < plot.y()
            || mouseY > plot.bottom()) return;
        int accent = CURSOR_COLOR;
        context.drawDashedLine(mouseX, plot.y(), mouseX, plot.bottom(), 1, 4, 3, accent);
        double fraction = (mouseX - plot.x()) / (double) plot.width();
        double value = (1 - fraction) * range.min + fraction * range.max;
        String label = axis.formatTick(value);
        ResolvedTextStyle style = CURSOR_STYLE;
        int textWidth = context.getStringWidth(label, style);
        int lineHeight = context.getLineHeight(style);
        int left = Math.round(mouseX) - textWidth / 2 - 4;
        left = Math.max(plot.x() - 4, Math.min(plot.right() - textWidth - 4, left));
        int top = plot.bottom() + 1;
        context.fillRect(left, top, textWidth + 8, lineHeight + 4, accent);
        context.drawText(label, left + 4, top + 2, style);
    }

    public static void drawGridLine(RenderContext context, float x1, float y1, float x2, float y2, int color) {
        int alpha = Math.round((color >>> 24) * 0.55f);
        int faintColor = (color & 0x00FFFFFF) | (alpha << 24);
        context.drawDashedLine(x1, y1, x2, y2, 1, 1, 9, faintColor);
    }

    public static void drawReferenceLines(RenderContext context, LytRect plotRect, ChartAxisOptions axis,
        AxisRange range) {
        for (ChartReferenceLine line : axis.getReferenceLines()) {
            if (line.value() < range.min || line.value() > range.max || line.color() >>> 24 == 0) continue;
            float y = mapY(line.value(), range, plotRect);
            context.drawDashedLine(plotRect.x(), y, plotRect.right(), y, 1, 4, 3, line.color());
            String label = line.label();
            if (label.isEmpty()) continue;
            ResolvedTextStyle style = LytChartBase.textStyle(line.color());
            int labelY = Math.max(plotRect.y() + 2, (int) y - context.getLineHeight(style) - 2);
            int labelX = plotRect.x() + 4;
            context.pushLocalScissor(plotRect);
            try {
                context.drawText(label, labelX, labelY, style);
            } finally {
                context.popScissor();
            }
        }
    }

    public static void drawVerticalReferenceLines(RenderContext context, LytRect plot, ChartAxisOptions axis,
        AxisRange range) {
        for (ChartReferenceLine line : axis.getReferenceLines()) {
            if (line.value() < range.min || line.value() > range.max || line.color() >>> 24 == 0) continue;
            float x = mapX(line.value(), range, plot);
            context.drawDashedLine(x, plot.y(), x, plot.bottom(), 1, 4, 3, line.color());
            if (line.label()
                .isEmpty()) continue;
            ResolvedTextStyle style = LytChartBase.textStyle(line.color());
            int labelX = Math.max(
                plot.x() + 4,
                Math.min(Math.round(x) + 4, plot.right() - context.getStringWidth(line.label(), style) - 4));
            context.pushLocalScissor(plot);
            try {
                context.drawText(line.label(), labelX, plot.y() + 3, style);
            } finally {
                context.popScissor();
            }
        }
    }

    public static void drawNumericXTicks(RenderContext context, LytRect plotRect, ChartAxisOptions axis,
        AxisRange range) {
        ResolvedTextStyle style = LytChartBase.textStyle(axis.getLabelColor());
        ChartTicks ticks = ChartTicks.create(range.min, range.max, range.step);
        int nextLabelX = Integer.MIN_VALUE;
        for (int i = 0; i < ticks.count(); i++) {
            double value = ticks.value(i);
            float x = mapX(value, range, plotRect);
            if (axis.isGridVisible()) {
                drawGridLine(context, x, plotRect.y(), x, plotRect.bottom(), axis.getGridColor());
            }
            String label = axis.formatTick(value);
            int width = context.getStringWidth(label, style);
            int left = (int) x - width / 2;
            if (left < nextLabelX) continue;
            context.drawText(label, left, plotRect.bottom() + 3, style);
            nextLabelX = left + width + 4;
        }
    }

    public static void drawAxes(RenderContext context, LytRect plotRect, ChartAxisOptions xAxis, ChartAxisOptions yAxis,
        AxisRange xRange, AxisRange yRange, String[] xCategories, boolean numericX) {
        ResolvedTextStyle xLabelStyle = LytChartBase.textStyle(xAxis.getLabelColor());
        ResolvedTextStyle yLabelStyle = LytChartBase.textStyle(yAxis.getLabelColor());

        // Y-axis grid + ticks.
        if (yRange != null) {
            ChartTicks ticks = ChartTicks.create(yRange.min, yRange.max, yRange.step);
            int lineHeight = context.getLineHeight(yLabelStyle);
            float previousLabelY = Float.POSITIVE_INFINITY;
            for (int i = 0; i < ticks.count(); i++) {
                double t = ticks.value(i);
                float y = mapY(t, yRange, plotRect);
                if (yAxis.isGridVisible()) {
                    drawGridLine(context, plotRect.x(), y, plotRect.right(), y, yAxis.getGridColor());
                }
                if (previousLabelY - y < lineHeight + 4) continue;
                String s = yAxis.formatTick(t);
                int sw = context.getStringWidth(s, yLabelStyle);
                context.drawText(s, plotRect.x() - sw - 4, (int) y - lineHeight / 2, yLabelStyle);
                previousLabelY = y;
            }
        }

        // X axis: numeric or categorical. Tick labels are clamped horizontally so they do not
        // overflow the chart bounds at the edges.
        if (numericX && xRange != null) {
            drawNumericXTicks(context, plotRect, xAxis, xRange);
        } else if (xCategories != null && xCategories.length > 0) {
            float step = (float) plotRect.width() / xCategories.length;
            for (int i = 0; i < xCategories.length; i++) {
                String label = xCategories[i] != null ? xCategories[i] : "";
                int sw = context.getStringWidth(label, xLabelStyle);
                float cx = plotRect.x() + step * (i + 0.5f);
                if (xAxis.isGridVisible()) {
                    drawGridLine(context, cx, plotRect.y(), cx, plotRect.bottom(), xAxis.getGridColor());
                }
                int tx = (int) cx - sw / 2;
                context.drawText(label, tx, plotRect.bottom() + 3, xLabelStyle);
            }
        }

        // Axis border.
        context.drawLine(plotRect.x(), plotRect.y(), plotRect.x(), plotRect.bottom(), 1f, xAxis.getAxisColor());
        context
            .drawLine(plotRect.x(), plotRect.bottom(), plotRect.right(), plotRect.bottom(), 1f, xAxis.getAxisColor());

        // Axis title.
        if (xAxis.getLabel() != null && !xAxis.getLabel()
            .isEmpty()) {
            int sw = context.getStringWidth(xAxis.getLabel(), xLabelStyle);
            int lh = context.getLineHeight(xLabelStyle);
            // Center the X-axis label below the tick row, but clamp to the plot's horizontal range
            // so a long label does not bleed past the right edge.
            int tx = plotRect.x() + Math.max(0, (plotRect.width() - sw) / 2);
            context.drawText(xAxis.getLabel(), tx, plotRect.bottom() + 3 + lh + 2, xLabelStyle);
        }
        if (yAxis.getLabel() != null && !yAxis.getLabel()
            .isEmpty()) {
            int lh = context.getLineHeight(yLabelStyle);
            // Place the Y-axis label horizontally above the plot's top-left corner instead of to
            // its left, so long labels (e.g. "Strength (dB)") never overflow the chart frame.
            context.drawText(yAxis.getLabel(), plotRect.x(), plotRect.y() - lh - 2, yLabelStyle);
        }
    }
}
