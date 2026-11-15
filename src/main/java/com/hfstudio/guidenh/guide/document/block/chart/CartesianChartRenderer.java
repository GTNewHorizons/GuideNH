package com.hfstudio.guidenh.guide.document.block.chart;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Shared axis / grid / data-value-label rendering for Cartesian charts (column / bar / line / scatter).
 */
public class CartesianChartRenderer {

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
            for (double t = yRange.min; t <= yRange.max + 1e-9; t += yRange.step) {
                String s = yAxis.formatTick(t);
                int w = context.getStringWidth(s, style);
                if (w > maxLabel) {
                    maxLabel = w;
                }
            }
            left = Math.max(left, maxLabel + 6);
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

    public static void drawGridLine(RenderContext context, float x1, float y1, float x2, float y2, int color) {
        int alpha = Math.round((color >>> 24) * 0.55f);
        int faintColor = (color & 0x00FFFFFF) | (alpha << 24);
        float length = (float) Math.hypot(x2 - x1, y2 - y1);
        for (float offset = 0; offset < length; offset += 10) {
            float start = offset / length;
            float end = Math.min(offset + 1, length) / length;
            context.drawLine(
                x1 + (x2 - x1) * start,
                y1 + (y2 - y1) * start,
                x1 + (x2 - x1) * end,
                y1 + (y2 - y1) * end,
                1f,
                faintColor);
        }
    }

    public static void drawReferenceLines(RenderContext context, LytRect plotRect, ChartAxisOptions axis,
        AxisRange range) {
        for (ChartReferenceLine line : axis.getReferenceLines()) {
            if (line.y() < range.min || line.y() > range.max) continue;
            float y = mapY(line.y(), range, plotRect);
            for (int x = plotRect.x(); x < plotRect.right(); x += 7) {
                context.drawLine(x, y, Math.min(x + 4, plotRect.right()), y, 1f, line.color());
            }
            String label = line.label();
            if (label.isEmpty()) continue;
            ResolvedTextStyle style = LytChartBase.textStyle(line.color());
            int labelY = (int) y - context.getLineHeight(style) / 2;
            context.drawText(label, plotRect.x() - context.getStringWidth(label, style) - 4, labelY, style);
        }
    }

    public static void drawVerticalReferenceLines(RenderContext context, LytRect plot, ChartAxisOptions axis,
        AxisRange range) {
        for (ChartReferenceLine line : axis.getReferenceLines()) {
            if (line.y() < range.min || line.y() > range.max) continue;
            float x = mapX(line.y(), range, plot);
            for (int y = plot.y(); y < plot.bottom(); y += 7) {
                context.drawLine(x, y, x, Math.min(y + 4, plot.bottom()), 1f, line.color());
            }
            if (line.label()
                .isEmpty()) continue;
            ResolvedTextStyle style = LytChartBase.textStyle(line.color());
            context.drawText(
                line.label(),
                Math.round(x) - context.getStringWidth(line.label(), style) / 2,
                plot.bottom() + 3,
                style);
        }
    }

    /**
     * Render axes + grid lines + tick text inside plotRect.
     */
    public static void drawAxes(RenderContext context, LytRect plotRect, ChartAxisOptions xAxis, ChartAxisOptions yAxis,
        AxisRange xRange, AxisRange yRange, String[] xCategories, boolean numericX) {
        ResolvedTextStyle xLabelStyle = LytChartBase.textStyle(xAxis.getLabelColor());
        ResolvedTextStyle yLabelStyle = LytChartBase.textStyle(yAxis.getLabelColor());

        // Y-axis grid + ticks.
        if (yRange != null) {
            for (double t = yRange.min; t <= yRange.max + 1e-9; t += yRange.step) {
                float y = mapY(t, yRange, plotRect);
                if (yAxis.isGridVisible()) {
                    drawGridLine(context, plotRect.x(), y, plotRect.right(), y, yAxis.getGridColor());
                }
                String s = yAxis.formatTick(t);
                int sw = context.getStringWidth(s, yLabelStyle);
                int lh = context.getLineHeight(yLabelStyle);
                context.drawText(s, plotRect.x() - sw - 4, (int) y - lh / 2, yLabelStyle);
            }
        }

        // X axis: numeric or categorical. Tick labels are clamped horizontally so they do not
        // overflow the chart bounds at the edges.
        if (numericX && xRange != null) {
            for (double t = xRange.min; t <= xRange.max + 1e-9; t += xRange.step) {
                float x = mapX(t, xRange, plotRect);
                if (xAxis.isGridVisible()) {
                    drawGridLine(context, x, plotRect.y(), x, plotRect.bottom(), xAxis.getGridColor());
                }
                String s = xAxis.formatTick(t);
                int sw = context.getStringWidth(s, xLabelStyle);
                int tx = (int) x - sw / 2;
                // Allow at most half the label to extend past the plot edge so neighbouring text
                // does not collide with the chart border.
                tx = Math.max(plotRect.x() - sw / 2, Math.min(plotRect.right() - sw / 2, tx));
                context.drawText(s, tx, plotRect.bottom() + 3, xLabelStyle);
            }
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
