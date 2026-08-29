package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

import lombok.Getter;
import lombok.Setter;

/**
 * Pie chart. On hover the hovered slice is offset outward along its angle bisector.
 */
public class LytPieChart extends LytChartBase implements DebugComponent {

    private static final int CIRCLE_SEGMENTS = 32;
    private static final float HOVER_OFFSET = 4f;

    @Getter
    private List<PieSlice> slices = new ArrayList<>();
    @Setter
    private float startAngleDeg = -90f;
    @Setter
    private boolean clockwise = true;

    private float cxCache;
    private float cyCache;
    private float radiusCache;
    private double totalCache;

    public void setSlices(List<PieSlice> slices) {
        this.slices = slices != null ? slices : new ArrayList<>();
    }

    @Override
    protected List<ChartLegendRenderer.LegendEntry> collectLegendEntries() {
        List<ChartLegendRenderer.LegendEntry> entries = new ArrayList<>();
        for (PieSlice s : slices) {
            entries.add(new ChartLegendRenderer.LegendEntry(s.getLabel(), s.getColor(), s.getIcon()));
        }
        return entries;
    }

    @Override
    protected LytRect renderChart(PrimitiveCollector c, LytRect plotRect) {
        if (slices.isEmpty()) return plotRect;
        double total = 0d;
        for (PieSlice s : slices) {
            total += Math.max(0d, s.getValue());
        }
        if (total <= 0d) return plotRect;
        totalCache = total;

        float cx = plotRect.x() + plotRect.width() / 2f;
        float cy = plotRect.y() + plotRect.height() / 2f;
        float radius = Math.max(8f, Math.min(plotRect.width(), plotRect.height()) / 2f - HOVER_OFFSET - 2f);
        cxCache = cx;
        cyCache = cy;
        radiusCache = radius;

        ResolvedTextStyle labelStyle = textStyle(getLabelColor());
        int lh = GuideText.lineHeight(labelStyle);
        double angle = Math.toRadians(startAngleDeg);
        double dir = clockwise ? 1d : -1d;
        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            double sweep = (slice.getValue() / total) * Math.PI * 2d * dir;
            double mid = angle + sweep / 2d;
            boolean hovered = i == hoveredKey;
            // Hovered slice keeps its apex at (cx, cy); only the outer arc bulges outward by
            // HOVER_OFFSET so the wedge is emphasized without dislocating its centre.
            float drawRadius = hovered ? radius + HOVER_OFFSET : radius;
            drawSlice(c, cx, cy, drawRadius, angle, sweep, slice.getColor());
            // Label.
            ChartLabelPosition pos = getLabelPosition();
            if (pos != ChartLabelPosition.NONE) {
                String text = switch (pos) {
                    case OUTSIDE, ABOVE, BELOW -> slice.getLabel() + " " + formatPercent(slice.getValue() / total);
                    default -> formatPercent(slice.getValue() / total);
                };
                int tw = GuideText.measureWidth(text, labelStyle);
                // R4-16: OUTSIDE labels use labelR = drawRadius + lh + 4f so text is pushed clear of
                // the pie boundary instead of sitting only 4px out. The clamp is relaxed by tw/2 on
                // left/right and lh/2 on top/bottom so large-slice labels are not pulled back inside.
                float labelR = pos == ChartLabelPosition.OUTSIDE || pos == ChartLabelPosition.ABOVE
                    || pos == ChartLabelPosition.BELOW ? drawRadius + Math.max(lh + 2f, 8f) : drawRadius * 0.6f;
                float tx = cx + (float) Math.cos(mid) * labelR - tw / 2f;
                float ty = cy + (float) Math.sin(mid) * labelR - lh / 2f;
                // Clamp label with relaxed bounds so outside labels project beyond the pie without
                // overflowing the chart frame entirely.
                int relaxedLeft = plotRect.x() - (pos == ChartLabelPosition.OUTSIDE ? tw / 2 : 0);
                int relaxedRight = plotRect.right() - tw + (pos == ChartLabelPosition.OUTSIDE ? tw / 2 : 0);
                int clampedTx = Math.max(relaxedLeft, Math.min(relaxedRight, (int) tx));
                int clampedTy = Math.max(
                    plotRect.y() - (pos == ChartLabelPosition.OUTSIDE ? lh / 2 : 0),
                    Math.min(plotRect.bottom() - lh + (pos == ChartLabelPosition.OUTSIDE ? lh / 2 : 0), (int) ty));
                GuideText.emitText(c, text, clampedTx, clampedTy, labelStyle);
            }
            angle += sweep;
        }
        return plotRect;
    }

    private static void drawSlice(PrimitiveCollector c, float cx, float cy, float radius, double startAngle,
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
        c.emit(new GuideRenderPrimitive.DrawPolygon(xs, ys, color));
    }

    @Override
    protected int hitTest(float x, float y) {
        if (radiusCache <= 0f || totalCache <= 0d) return -1;
        float dx = x - cxCache;
        float dy = y - cyCache;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        // Allow slightly more than the radius to cover the hover offset.
        if (dist > radiusCache + HOVER_OFFSET) return -1;
        double pointAngle = Math.atan2(dy, dx);
        double startAngle = Math.toRadians(startAngleDeg);
        double dir = clockwise ? 1d : -1d;
        double angle = startAngle;
        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            double sweep = (slice.getValue() / totalCache) * Math.PI * 2d * dir;
            if (angleIsBetween(pointAngle, angle, angle + sweep)) {
                return i;
            }
            angle += sweep;
        }
        return -1;
    }

    /** Tests whether {@code angle} lies on the arc (from, to) (in the direction from -> to). */
    public static boolean angleIsBetween(double angle, double from, double to) {
        // Normalize all angles to the same cycle as `from`: bring `angle` into [from, from + 2π).
        double twoPi = Math.PI * 2d;
        if (Math.abs(to - from) < 1e-9) return false;
        double sweep = to - from;
        // Move `angle` to the right side of `from` (along the sweep direction).
        double rel = angle - from;
        if (sweep > 0) {
            while (rel < 0) rel += twoPi;
            while (rel >= twoPi) rel -= twoPi;
            return rel >= 0 && rel < sweep;
        } else {
            // Reverse: along the decreasing direction.
            while (rel > 0) rel -= twoPi;
            while (rel <= -twoPi) rel += twoPi;
            return rel <= 0 && rel > sweep;
        }
    }

    @Override
    protected String describeHit(int key) {
        if (key < 0 || key >= slices.size()) return null;
        PieSlice slice = slices.get(key);
        StringBuilder sb = new StringBuilder();
        if (!slice.getLabel()
            .isEmpty()) {
            sb.append(slice.getLabel())
                .append('\n');
        }
        sb.append(formatValue(slice.getValue()));
        if (totalCache > 0d) {
            sb.append('\n')
                .append(formatPercent(slice.getValue() / totalCache));
        }
        return sb.toString();
    }

    @Override
    protected ItemStack getHitItemStack(int key) {
        if (key < 0 || key >= slices.size()) return null;
        ChartIcon icon = slices.get(key)
            .getIcon();
        return icon != null && icon.hasItemStack() ? icon.getStack() : null;
    }

    @Override
    protected String getHitExtraTooltip(int key) {
        if (key < 0 || key >= slices.size()) return null;
        return slices.get(key)
            .getTooltipExtra();
    }

    // Debug implementation

    @Override
    public List<ComponentEntry> getDebugComponents() {
        List<ComponentEntry> components = new ArrayList<>();

        if (slices.isEmpty() || totalCache <= 0 || bounds == null) {
            return components;
        }

        // Use precise sector hit-testing for each slice
        double angle = Math.toRadians(startAngleDeg);
        double dir = clockwise ? 1d : -1d;

        for (int i = 0; i < slices.size(); i++) {
            PieSlice slice = slices.get(i);
            double sweep = (slice.getValue() / totalCache) * Math.PI * 2d * dir;

            // Create a custom ComponentEntry that uses sector hit-testing
            int sliceIndex = i;
            ComponentEntry sliceEntry = new ComponentEntry() {

                @Override
                public String getName() {
                    String label = slice.getLabel();
                    if (label == null || label.isEmpty()) {
                        label = "Slice" + (sliceIndex + 1);
                    }
                    return "Slice:" + label;
                }

                @Override
                public LytRect getBounds() {
                    // Return bounding box of the sector
                    double startAngle = Math.toRadians(startAngleDeg);
                    double currentAngle = startAngle;
                    for (int j = 0; j < sliceIndex; j++) {
                        currentAngle += (slices.get(j)
                            .getValue() / totalCache) * Math.PI * 2d * dir;
                    }
                    double sweepLocal = (slice.getValue() / totalCache) * Math.PI * 2d * dir;

                    // Calculate bounding box of sector
                    float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                    float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

                    // Include center point
                    minX = Math.min(minX, cxCache);
                    minY = Math.min(minY, cyCache);
                    maxX = Math.max(maxX, cxCache);
                    maxY = Math.max(maxY, cyCache);

                    // Sample points along the arc
                    int samples = 16;
                    for (int k = 0; k <= samples; k++) {
                        double a = currentAngle + sweepLocal * (k / (double) samples);
                        float x = cxCache + (float) Math.cos(a) * (radiusCache + HOVER_OFFSET);
                        float y = cyCache + (float) Math.sin(a) * (radiusCache + HOVER_OFFSET);
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                    }

                    return new LytRect(
                        (int) Math.floor(minX),
                        (int) Math.floor(minY),
                        (int) Math.ceil(maxX - minX),
                        (int) Math.ceil(maxY - minY));
                }

                @Override
                public String getExtraInfo() {
                    double percent = (slice.getValue() / totalCache) * 100;
                    return String.format("Value: %.1f (%.1f%%)", slice.getValue(), percent);
                }

                @Override
                public int getPriority() {
                    return 15;
                }

                /**
                 * Override contains check to use precise sector hit-testing
                 */
                public boolean containsPoint(int x, int y) {
                    // Use the chart's built-in hitTest which has precise sector detection
                    return hitTest(x, y) == sliceIndex;
                }
            };

            components.add(sliceEntry);
            angle += sweep;
        }

        return components;
    }
}
