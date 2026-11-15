package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.List;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Draws a PieInsetSpec in the requested corner of a host chart's plot rectangle.
 */
public class PieInsetRenderer {

    private static final int CIRCLE_SEGMENTS = 24;
    private static final int MARGIN = 4;

    protected PieInsetRenderer() {}

    public record Layout(float cx, float cy, float radius, double total, double startAngle, boolean clockwise,
        List<PieSlice> slices) {

        public int hitTest(float x, float y) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || Math.hypot(x - cx, y - cy) > radius) return -1;
            double pointAngle = Math.atan2(y - cy, x - cx);
            double angle = startAngle;
            for (int i = 0; i < slices.size(); i++) {
                double sweep = Math.max(
                    0,
                    slices.get(i)
                        .getValue())
                    / total
                    * Math.PI
                    * 2
                    * (clockwise ? 1 : -1);
                if (LytPieChart.angleIsBetween(pointAngle, angle, angle + sweep)) return i;
                angle += sweep;
            }
            return -1;
        }
    }

    public static Layout draw(RenderContext context, LytRect plotRect, PieInsetSpec spec, float mouseX, float mouseY) {
        if (spec == null || spec.getSlices()
            .isEmpty()) {
            return null;
        }
        int size = Math.min(spec.getSize(), Math.min(plotRect.width(), plotRect.height()) - MARGIN * 2);
        if (size < 16) return null;
        int areaX;
        int areaY;
        switch (spec.getPosition()) {
            case TOP_LEFT:
                areaX = plotRect.x() + MARGIN;
                areaY = plotRect.y() + MARGIN;
                break;
            case BOTTOM_RIGHT:
                areaX = plotRect.right() - size - MARGIN;
                areaY = plotRect.bottom() - size - MARGIN;
                break;
            case BOTTOM_LEFT:
                areaX = plotRect.x() + MARGIN;
                areaY = plotRect.bottom() - size - MARGIN;
                break;
            case RIGHT_OUTSIDE:
                // Should be drawn via drawAt(...) into a dedicated area; ignore here.
                return null;
            case TOP_RIGHT:
            default:
                areaX = plotRect.right() - size - MARGIN;
                areaY = plotRect.y() + MARGIN;
                break;
        }
        return drawInternal(context, spec, areaX, areaY, size, mouseX, mouseY);
    }

    /**
     * Draw the pie inset filling the provided rectangle entirely; used when the host chart reserves a
     * dedicated outside area (e.g. {@link PieInsetSpec.Position#RIGHT_OUTSIDE}).
     */
    public static Layout drawAt(RenderContext context, LytRect area, PieInsetSpec spec, float mouseX, float mouseY) {
        if (spec == null || area == null || area.width() <= 16 || area.height() <= 16) {
            return null;
        }
        if (spec.getSlices()
            .isEmpty()) {
            return null;
        }
        int size = Math.min(area.width(), area.height());
        int areaX = area.x() + (area.width() - size) / 2;
        int areaY = area.y() + (area.height() - size) / 2;
        return drawInternal(context, spec, areaX, areaY, size, mouseX, mouseY);
    }

    private static Layout drawInternal(RenderContext context, PieInsetSpec spec, int areaX, int areaY, int size,
        float mouseX, float mouseY) {
        double total = 0d;
        for (PieSlice slice : spec.getSlices()) {
            total += Math.max(0d, slice.getValue());
        }
        if (total <= 0d) return null;

        // Optional title above the pie.
        ResolvedTextStyle titleStyle = textStyle(spec.getTitleColor());
        int titleHeight = 0;
        if (!spec.getTitle()
            .isEmpty()) {
            titleHeight = context.getLineHeight(titleStyle);
            int tw = context.getStringWidth(spec.getTitle(), titleStyle);
            // Center within the inset; if the title is wider than the inset, anchor to the inset's
            // left edge so it does not bleed into the host plot.
            int tx = tw <= size ? areaX + (size - tw) / 2 : areaX;
            int ty = areaY;
            context.drawText(spec.getTitle(), tx, ty, titleStyle);
        }

        int pieSize = size - titleHeight;
        if (pieSize < 12) return null;
        float cx = areaX + size / 2f;
        float cy = areaY + titleHeight + pieSize / 2f;
        float radius = pieSize / 2f - 1f;

        double angle = Math.toRadians(spec.getStartAngleDeg());
        Layout layout = new Layout(cx, cy, radius, total, angle, spec.isClockwise(), List.copyOf(spec.getSlices()));
        int hovered = layout.hitTest(mouseX, mouseY);
        double dir = spec.isClockwise() ? 1d : -1d;
        for (int i = 0; i < spec.getSlices()
            .size(); i++) {
            PieSlice slice = spec.getSlices()
                .get(i);
            double sweep = (Math.max(0, slice.getValue()) / total) * Math.PI * 2d * dir;
            drawSlice(context, cx, cy, i == hovered ? radius + 2 : radius, angle, sweep, slice.getColor());
            angle += sweep;
        }
        // Thin outline around the pie to separate it from the host plot.
        context.drawCircleOutline(cx, cy, radius, 1f, ColorUtils.ARGB_FF202020.getColor());
        return layout;
    }

    private static void drawSlice(RenderContext context, float cx, float cy, float radius, double startAngle,
        double sweepAngle, int color) {
        if (Math.abs(sweepAngle) < 1e-6) return;
        int segments = Math.max(2, (int) Math.ceil(CIRCLE_SEGMENTS * Math.abs(sweepAngle) / (Math.PI * 2d)));
        float[] xs = new float[segments + 2];
        float[] ys = new float[segments + 2];
        xs[0] = cx;
        ys[0] = cy;
        for (int i = 0; i <= segments; i++) {
            double a = startAngle + sweepAngle * (i / (double) segments);
            xs[i + 1] = cx + (float) Math.cos(a) * radius;
            ys[i + 1] = cy + (float) Math.sin(a) * radius;
        }
        context.fillPolygon(xs, ys, color);
    }

    private static ResolvedTextStyle textStyle(int color) {
        return LytChartBase.textStyle(color);
    }
}
