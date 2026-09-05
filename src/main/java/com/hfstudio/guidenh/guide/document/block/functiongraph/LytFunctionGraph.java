package com.hfstudio.guidenh.guide.document.block.functiongraph;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.ResponsiveVisualSizing;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendEntry;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendPosition;
import com.hfstudio.guidenh.guide.document.block.chart.CornerLegendRenderer;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.markdown.MarkdownLatexShorthand;
import com.hfstudio.guidenh.guide.latex.GuideLatexRenderer;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

import lombok.Getter;
import lombok.Setter;

/**
 * Function graph block. Plots one or more {@link FunctionPlot} curves on a Cartesian panel with
 * interactive Desmos-style hovering: while the cursor is over a curve the segment is thickened and an
 * accent point is drawn at the cursor's x value. Pressing the mouse button latches the highlight
 * onto that curve so the user can drag along it freely until the button is released, even when the
 * cursor strays vertically.
 *
 * <p>
 * Layout, sampling and hover-state tracking are all handled inside this single block so the rest of
 * the document does not need to coordinate with it.
 */
public class LytFunctionGraph extends LytBlock implements InteractiveElement, DocumentDragTarget {

    private static final int DEFAULT_WIDTH = 320;
    private static final int DEFAULT_HEIGHT = 220;
    private static final int PADDING = 8;
    private static final int TITLE_GAP = 4;
    private static final int AXIS_LABEL_GAP = 4;
    private static final int AXIS_PAD_LEFT = 28;
    private static final int AXIS_PAD_BOTTOM = 14;
    private static final int MIN_SAMPLES = 64;
    private static final int MAX_SAMPLES = 1024;
    private static final float HIT_THRESHOLD_PX = 4f;
    private static final float PRESET_HIT_RADIUS = 6f;
    private static final float HIGHLIGHT_LINE_BONUS = 1.0f;
    private static final int POINT_RADIUS = 3;
    private static final float POINT_OUTER_RING = 1f;
    private static final int LEGEND_GAP_ABOVE = 4;
    private static final int LEGEND_ROW_GAP = 2;
    private static final int LEGEND_ITEM_GAP = 10;
    private static final int LEGEND_SWATCH_SIZE = 7;
    private static final int LEGEND_SWATCH_TEXT_GAP = 4;
    private static final int AUTO_POINT_MAX_PER_PLOT = 96;
    private static final int AUTO_POINT_MAX_TARGETS_PER_PLOT = 256;
    private static final int AUTO_POINT_Y_SCAN_STEPS = 128;
    private static final int AUTO_POINT_SOLVE_STEPS = 24;
    private static final int AUTO_POINT_LABEL_GAP = 3;
    private static final int MIN_PLOT_HEIGHT = 88;
    private static final float LABEL_LATEX_SOURCE_SCALE = 100f;

    private static final ResolvedTextStyle TITLE_STYLE = makeStyle(ColorUtils.ARGB_FFE6E6E6.getColor(), true);
    private static final ResolvedTextStyle AXIS_LABEL_STYLE = makeStyle(ColorUtils.CHART_LABEL.getColor(), false);
    private static final ResolvedTextStyle TOOLTIP_BODY_STYLE = makeStyle(ColorUtils.ARGB_FFD7DEE7.getColor(), false);
    private static final ResolvedTextStyle LEGEND_LABEL_STYLE = makeStyle(ColorUtils.ARGB_FFD7DEE7.getColor(), false);

    @Getter
    private final List<FunctionPlot> plots = new ArrayList<>();
    @Getter
    private final List<MarkedPoint> points = new ArrayList<>();

    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String xLabel;
    @Getter
    @Setter
    private String yLabel;
    @Getter
    private int explicitWidth = -1;
    @Getter
    private int explicitHeight = -1;
    @Getter
    @Setter
    private int backgroundColor = ColorUtils.CHART_BACKGROUND.getColor();
    @Getter
    @Setter
    private int borderColor = ColorUtils.CHART_BORDER.getColor();
    @Getter
    @Setter
    private int axisColor = ColorUtils.CHART_LABEL.getColor();
    @Getter
    @Setter
    private int gridColor = ColorUtils.CHART_GRID.getColor();
    @Getter
    @Setter
    private boolean showGrid = true;
    @Getter
    @Setter
    private boolean showAxes = true;
    @Getter
    private CornerLegendPosition cornerLegendPosition = CornerLegendPosition.NONE;
    @Getter
    private int cornerLegendWidth = CornerLegendRenderer.DEFAULT_WIDTH;
    @Getter
    private int cornerLegendHeight = CornerLegendRenderer.DEFAULT_HEIGHT;
    @Getter
    @Setter
    private int cornerLegendBackgroundColor = CornerLegendRenderer.DEFAULT_BACKGROUND;

    @Getter
    private double explicitXMin = Double.NaN;
    @Getter
    private double explicitXMax = Double.NaN;
    @Getter
    private double explicitYMin = Double.NaN;
    @Getter
    private double explicitYMax = Double.NaN;
    @Getter
    @Setter
    private double explicitXStep = Double.NaN;
    @Getter
    @Setter
    private double explicitYStep = Double.NaN;

    /** Bitmask of visible quadrants (bit i = quadrant i+1). {@code 0} means auto. */
    private int explicitQuadrantMask = 0;

    private double effectiveXMin;
    private double effectiveXMax;
    private double effectiveYMin;
    private double effectiveYMax;
    private double effectiveXStep;
    private double effectiveYStep;

    private LytRect plotRectCache = LytRect.empty();
    private float[][] sampleXs;
    private float[][] sampleYs;
    private long sampleCacheKey = -1L;

    private int activePlotIndex = -1;
    private double activeDataX;
    private float activeScreenX;
    private int activeMarkedIndex = -1;
    private double activeMarkedDataX;
    private double activeMarkedDataY;
    private int activeMarkedColor;
    private int activeAutoPlotIndex = -1;
    private double activeAutoDataX;
    private double activeAutoDataY;
    private int activeAutoColor;
    private final List<double[]> autoPointHitCache = new ArrayList<>();
    private boolean isDragging;
    private int dragButton;

    public void addPlot(FunctionPlot plot) {
        if (plot != null) {
            plots.add(plot);
        }
    }

    public void addPoint(MarkedPoint point) {
        if (point != null) {
            points.add(point);
        }
    }

    public void setExplicitSize(int width, int height) {
        this.explicitWidth = width > 0 ? width : -1;
        this.explicitHeight = height > 0 ? height : -1;
    }

    public void setCornerLegendPosition(CornerLegendPosition cornerLegendPosition) {
        this.cornerLegendPosition = cornerLegendPosition != null ? cornerLegendPosition : CornerLegendPosition.NONE;
    }

    public void setCornerLegendSize(int width, int height) {
        this.cornerLegendWidth = width > 0 ? width : CornerLegendRenderer.DEFAULT_WIDTH;
        this.cornerLegendHeight = height > 0 ? height : CornerLegendRenderer.DEFAULT_HEIGHT;
    }

    public void setExplicitXRange(double min, double max) {
        this.explicitXMin = min;
        this.explicitXMax = max;
    }

    public void setExplicitYRange(double min, double max) {
        this.explicitYMin = min;
        this.explicitYMax = max;
    }

    public void setQuadrantMask(int mask) {
        this.explicitQuadrantMask = mask & 0xF;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int width = ResponsiveVisualSizing
            .scaleWidth(explicitWidth > 0 ? explicitWidth : DEFAULT_WIDTH, context.getVisualScale(), 72);
        width = Math.clamp(width, 1, availableWidth);
        int height = explicitHeight > 0 ? explicitHeight : DEFAULT_HEIGHT;
        int plotWidth = Math.max(0, width - PADDING * 2 - AXIS_PAD_LEFT);
        int fixedChromeHeight = PADDING * 2 + AXIS_PAD_BOTTOM;
        if (title != null && !title.isEmpty()) {
            fixedChromeHeight += measureRichTextHeight(context, title, TITLE_STYLE) + TITLE_GAP;
        }
        if (yLabel != null && !yLabel.isEmpty()) {
            fixedChromeHeight += measureRichTextHeight(context, yLabel, AXIS_LABEL_STYLE) + AXIS_LABEL_GAP;
        }
        if (xLabel != null && !xLabel.isEmpty()) {
            fixedChromeHeight += measureRichTextHeight(context, xLabel, AXIS_LABEL_STYLE) + AXIS_LABEL_GAP;
        }
        int legendHeight = measureLegendHeight(context, plotWidth);
        if (legendHeight > 0) {
            fixedChromeHeight += legendHeight + LEGEND_GAP_ABOVE;
        }
        height = ResponsiveVisualSizing.scaleBodyHeightForWidth(
            explicitWidth > 0 ? explicitWidth : DEFAULT_WIDTH,
            height,
            width,
            fixedChromeHeight,
            MIN_PLOT_HEIGHT);
        invalidateSamples();
        return new LytRect(x, y, width, height);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        invalidateSamples();
    }

    @Override
    public void render(RenderContext context) {
        context.fillRect(bounds, backgroundColor);
        context.drawBorder(bounds, borderColor, 1);

        int contentTop = bounds.y() + PADDING;
        int contentBottom = bounds.bottom() - PADDING;
        int contentLeft = bounds.x() + PADDING;
        int contentRight = bounds.right() - PADDING;

        if (title != null && !title.isEmpty()) {
            int titleHeight = measureRichTextHeight(context, title, TITLE_STYLE);
            int tw = measureRichTextWidth(context, title, TITLE_STYLE);
            int tx = bounds.x() + (bounds.width() - tw) / 2;
            drawRichText(context, title, tx, contentTop, TITLE_STYLE);
            contentTop += titleHeight + TITLE_GAP;
        }

        if (yLabel != null && !yLabel.isEmpty()) {
            drawRichText(context, yLabel, contentLeft + AXIS_PAD_LEFT, contentTop, AXIS_LABEL_STYLE);
            contentTop += measureRichTextHeight(context, yLabel, AXIS_LABEL_STYLE) + AXIS_LABEL_GAP;
        }

        int plotLeft = contentLeft + AXIS_PAD_LEFT;
        int plotRight = contentRight;
        int plotTop = contentTop;
        int legendWidth = Math.max(0, plotRight - plotLeft);
        int legendHeight = measureLegendHeight(context, legendWidth);
        int xLabelHeight = xLabel != null && !xLabel.isEmpty()
            ? measureRichTextHeight(context, xLabel, AXIS_LABEL_STYLE) + AXIS_LABEL_GAP
            : 0;
        int plotBottom = contentBottom - AXIS_PAD_BOTTOM
            - xLabelHeight
            - (legendHeight > 0 ? legendHeight + LEGEND_GAP_ABOVE : 0);
        if (plotRight - plotLeft <= 16 || plotBottom - plotTop <= 16) {
            return;
        }
        LytRect plotRect = new LytRect(plotLeft, plotTop, plotRight - plotLeft, plotBottom - plotTop);

        resolveRanges();
        plotRectCache = plotRect;
        ensureSamples(plotRect);

        // Re-resolve once with sampled extents now that all plots have been visited; this lets the
        // y-axis auto-expand when samples produced negative values while the user did not pin yMin.
        if (autoExpandFromSamples(plotRect)) {
            ensureSamples(plotRect);
        }

        if (showGrid) {
            drawGrid(context, plotRect);
        }
        if (showAxes) {
            drawAxes(context, plotRect);
        }

        for (int i = 0; i < plots.size(); i++) {
            renderPlot(context, plotRect, i);
        }

        renderMarkedPoints(context, plotRect);
        renderAutoPoints(context, plotRect);

        if ((activePlotIndex >= 0 && activePlotIndex < plots.size()) || activeMarkedIndex >= 0
            || activeAutoPlotIndex >= 0) {
            renderActiveOverlay(context, plotRect);
        }
        renderCornerLegend(context, plotRect);

        if (legendHeight > 0) {
            int legendTop = plotRect.bottom() + AXIS_PAD_BOTTOM + LEGEND_GAP_ABOVE;
            renderLegend(context, plotRect.x(), legendTop, legendWidth);
        }
        if (xLabel != null && !xLabel.isEmpty()) {
            int labelWidth = measureRichTextWidth(context, xLabel, AXIS_LABEL_STYLE);
            int labelX = plotRect.x() + Math.max(0, (plotRect.width() - labelWidth) / 2);
            int labelY = plotRect.bottom() + AXIS_PAD_BOTTOM + (legendHeight > 0 ? legendHeight + LEGEND_GAP_ABOVE : 0);
            drawRichText(context, xLabel, labelX, labelY, AXIS_LABEL_STYLE);
        }
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (!isDragging) {
            updateHover(x, y);
        }
        return createActiveTooltip();
    }

    @Override
    public void onMouseLeave() {
        if (!isDragging) {
            clearActive();
        }
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (plotRectCache.isEmpty()) {
            return false;
        }
        if (!plotRectCache.contains(documentX, documentY)) {
            return false;
        }
        // Preset points do not support drag.
        if (activeMarkedIndex >= 0 || activeAutoPlotIndex >= 0) {
            return false;
        }
        int hit = hitTest(documentX, documentY);
        if (hit < 0) {
            return false;
        }
        activePlotIndex = hit;
        activeDataX = unmapXToData(
            activeScreenX,
            plots.get(hit)
                .isInverse());
        isDragging = true;
        dragButton = button;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (!isDragging || activePlotIndex < 0 || activePlotIndex >= plots.size()) {
            return;
        }
        FunctionPlot plot = plots.get(activePlotIndex);
        // Clamp the cursor x onto the plot rect so dragging out of the panel still tracks the
        // closest valid sample; vertical movement is intentionally ignored.
        int clampedX = Math.clamp(documentX, plotRectCache.x(), plotRectCache.right());
        activeDataX = unmapXToData(clampedX, plot.isInverse());
    }

    @Override
    public void endDrag() {
        isDragging = false;
        // Keep the active highlight visible until the next hover update clears it.
    }

    private void resolveRanges() {
        double xMin = !Double.isNaN(explicitXMin) ? explicitXMin : 0d;
        double xMax = !Double.isNaN(explicitXMax) ? explicitXMax : 10d;
        if (xMax <= xMin) {
            xMax = xMin + 1d;
        }
        double yMin = !Double.isNaN(explicitYMin) ? explicitYMin : 0d;
        double yMax = !Double.isNaN(explicitYMax) ? explicitYMax : 10d;
        if (yMax <= yMin) {
            yMax = yMin + 1d;
        }

        // Quadrant attribute can force the visible window to span negative axes. Each quadrant bit
        // expands the corresponding half-axis when not pinned by the user.
        if (explicitQuadrantMask != 0) {
            boolean q1 = (explicitQuadrantMask & 1) != 0;
            boolean q2 = (explicitQuadrantMask & 2) != 0;
            boolean q3 = (explicitQuadrantMask & 4) != 0;
            boolean q4 = (explicitQuadrantMask & 8) != 0;
            if ((q2 || q3) && Double.isNaN(explicitXMin)) {
                xMin = -Math.max(Math.abs(xMax), 10d);
            }
            if ((q1 || q4) && Double.isNaN(explicitXMax)) {
                xMax = Math.max(Math.abs(xMin), 10d);
            }
            if ((q1 || q2) && Double.isNaN(explicitYMax)) {
                yMax = Math.max(Math.abs(yMin), 10d);
            }
            if ((q3 || q4) && Double.isNaN(explicitYMin)) {
                yMin = -Math.max(Math.abs(yMax), 10d);
            }
        }

        effectiveXMin = xMin;
        effectiveXMax = xMax;
        effectiveYMin = yMin;
        effectiveYMax = yMax;
        effectiveXStep = !Double.isNaN(explicitXStep) && explicitXStep > 0 ? explicitXStep : niceStep(xMax - xMin);
        effectiveYStep = !Double.isNaN(explicitYStep) && explicitYStep > 0 ? explicitYStep : niceStep(yMax - yMin);
    }

    /**
     * After the first sampling pass, optionally expand the y axis (and x for inverse plots) to fit
     * actual sample values; only applied when the user did not pin the corresponding bound.
     */
    private boolean autoExpandFromSamples(LytRect plotRect) {
        if (sampleXs == null || plots.isEmpty()) {
            return false;
        }
        boolean changed = false;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < plots.size(); i++) {
            FunctionPlot plot = plots.get(i);
            if (plot.isInverse()) {
                continue;
            }
            int n = sampleXs[i] != null ? sampleXs[i].length : 0;
            for (int j = 0; j < n; j++) {
                double dy = unmapY(sampleYs[i][j]);
                if (Double.isFinite(dy)) {
                    if (dy < minY) {
                        minY = dy;
                    }
                    if (dy > maxY) {
                        maxY = dy;
                    }
                }
            }
        }
        if (Double.isFinite(minY) && minY < effectiveYMin && Double.isNaN(explicitYMin)) {
            effectiveYMin = Math.floor(minY);
            changed = true;
        }
        if (Double.isFinite(maxY) && maxY > effectiveYMax && Double.isNaN(explicitYMax)) {
            effectiveYMax = Math.ceil(maxY);
            changed = true;
        }
        if (changed) {
            effectiveYStep = !Double.isNaN(explicitYStep) && explicitYStep > 0 ? explicitYStep
                : niceStep(effectiveYMax - effectiveYMin);
            invalidateSamples();
        }
        return changed;
    }

    private void invalidateSamples() {
        sampleCacheKey = -1L;
        sampleXs = null;
        sampleYs = null;
    }

    private void ensureSamples(LytRect plotRect) {
        long key = ((long) plotRect.x()) * 31L + plotRect.y();
        key = key * 31L + plotRect.width();
        key = key * 31L + plotRect.height();
        key = key * 31L + Double.hashCode(effectiveXMin);
        key = key * 31L + Double.hashCode(effectiveXMax);
        key = key * 31L + Double.hashCode(effectiveYMin);
        key = key * 31L + Double.hashCode(effectiveYMax);
        key = key * 31L + plots.size();
        if (key == sampleCacheKey && sampleXs != null) {
            return;
        }
        int sampleCount = Math.clamp(plotRect.width() * 2L, MIN_SAMPLES, MAX_SAMPLES);
        sampleXs = new float[plots.size()][];
        sampleYs = new float[plots.size()][];
        for (int i = 0; i < plots.size(); i++) {
            FunctionPlot plot = plots.get(i);
            float[] xs = new float[sampleCount];
            float[] ys = new float[sampleCount];
            for (int s = 0; s < sampleCount; s++) {
                double t = (double) s / (double) (sampleCount - 1);
                if (plot.isInverse()) {
                    double yVal = effectiveYMin + (effectiveYMax - effectiveYMin) * t;
                    double xVal = plot.evaluate(yVal);
                    xs[s] = (float) mapX(xVal);
                    ys[s] = (float) mapY(yVal);
                    if (!Double.isFinite(xVal)) {
                        xs[s] = Float.NaN;
                    }
                } else {
                    double xVal = effectiveXMin + (effectiveXMax - effectiveXMin) * t;
                    double yVal = plot.evaluate(xVal);
                    xs[s] = (float) mapX(xVal);
                    ys[s] = (float) mapY(yVal);
                    if (!Double.isFinite(yVal)) {
                        ys[s] = Float.NaN;
                    }
                }
            }
            sampleXs[i] = xs;
            sampleYs[i] = ys;
        }
        sampleCacheKey = key;
    }

    private double mapX(double value) {
        double t = (value - effectiveXMin) / (effectiveXMax - effectiveXMin);
        return plotRectCache.x() + t * plotRectCache.width();
    }

    private double mapY(double value) {
        double t = (value - effectiveYMin) / (effectiveYMax - effectiveYMin);
        return plotRectCache.bottom() - t * plotRectCache.height();
    }

    private double unmapX(double screenX) {
        double t = (screenX - plotRectCache.x()) / plotRectCache.width();
        return effectiveXMin + t * (effectiveXMax - effectiveXMin);
    }

    private double unmapY(double screenY) {
        double t = (plotRectCache.bottom() - screenY) / plotRectCache.height();
        return effectiveYMin + t * (effectiveYMax - effectiveYMin);
    }

    /** Convert a screen x to data coordinates. For inverse plots returns the data y instead. */
    private double unmapXToData(double screenX, boolean inverse) {
        return inverse ? unmapY(screenX) : unmapX(screenX);
    }

    private void drawGrid(RenderContext context, LytRect plotRect) {
        if (effectiveXStep > 0) {
            double start = Math.ceil(effectiveXMin / effectiveXStep) * effectiveXStep;
            for (double v = start; v <= effectiveXMax + 1e-9; v += effectiveXStep) {
                float x = (float) mapX(v);
                context.drawLine(x, plotRect.y(), x, plotRect.bottom(), 1f, gridColor);
            }
        }
        if (effectiveYStep > 0) {
            double start = Math.ceil(effectiveYMin / effectiveYStep) * effectiveYStep;
            for (double v = start; v <= effectiveYMax + 1e-9; v += effectiveYStep) {
                float y = (float) mapY(v);
                context.drawLine(plotRect.x(), y, plotRect.right(), y, 1f, gridColor);
            }
        }
    }

    private void drawAxes(RenderContext context, LytRect plotRect) {
        // Vertical (y) axis pinned to x = 0 when visible, otherwise to plotRect.x.
        float axisX = (float) mapX(0d);
        if (axisX < plotRect.x() || axisX > plotRect.right()) {
            axisX = plotRect.x();
        }
        context.drawLine(axisX, plotRect.y(), axisX, plotRect.bottom(), 1f, axisColor);

        float axisY = (float) mapY(0d);
        if (axisY < plotRect.y() || axisY > plotRect.bottom()) {
            axisY = plotRect.bottom();
        }
        context.drawLine(plotRect.x(), axisY, plotRect.right(), axisY, 1f, axisColor);

        // Y tick labels along left edge of plot rect.
        if (effectiveYStep > 0) {
            double start = Math.ceil(effectiveYMin / effectiveYStep) * effectiveYStep;
            int lh = context.getLineHeight(AXIS_LABEL_STYLE);
            for (double v = start; v <= effectiveYMax + 1e-9; v += effectiveYStep) {
                String label = formatTick(v);
                int sw = context.getStringWidth(label, AXIS_LABEL_STYLE);
                int ly = (int) mapY(v) - lh / 2;
                context.drawText(label, plotRect.x() - sw - AXIS_LABEL_GAP, ly, AXIS_LABEL_STYLE);
            }
        }
        if (effectiveXStep > 0) {
            double start = Math.ceil(effectiveXMin / effectiveXStep) * effectiveXStep;
            for (double v = start; v <= effectiveXMax + 1e-9; v += effectiveXStep) {
                String label = formatTick(v);
                int sw = context.getStringWidth(label, AXIS_LABEL_STYLE);
                int lx = (int) mapX(v) - sw / 2;
                lx = Math.clamp(lx, plotRect.x() - sw / 2, plotRect.right() - sw / 2);
                context.drawText(label, lx, plotRect.bottom() + AXIS_LABEL_GAP, AXIS_LABEL_STYLE);
            }
        }
    }

    private void renderPlot(RenderContext context, LytRect plotRect, int index) {
        FunctionPlot plot = plots.get(index);
        float[] xs = sampleXs[index];
        float[] ys = sampleYs[index];
        if (xs == null || ys == null) {
            return;
        }
        boolean highlighted = activePlotIndex == index;
        float thickness = 1f + (highlighted ? HIGHLIGHT_LINE_BONUS : 0f);
        int color = plot.getColor();
        float plotHalfHeight = plotRect.height() * 0.5f;

        for (int i = 0; i + 1 < xs.length; i++) {
            float x1 = xs[i];
            float y1 = ys[i];
            float x2 = xs[i + 1];
            float y2 = ys[i + 1];
            if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                continue;
            }
            // Skip catastrophic vertical jumps which usually indicate an asymptote.
            if (Math.abs(y2 - y1) > plotHalfHeight) {
                continue;
            }
            // Quick scissor: if both endpoints are clearly outside the rect on the same side, skip.
            if ((y1 < plotRect.y() && y2 < plotRect.y()) || (y1 > plotRect.bottom() && y2 > plotRect.bottom())) {
                continue;
            }
            if ((x1 < plotRect.x() && x2 < plotRect.x()) || (x1 > plotRect.right() && x2 > plotRect.right())) {
                continue;
            }
            context.drawLine(x1, y1, x2, y2, thickness, color);
        }
    }

    private void renderMarkedPoints(RenderContext context, LytRect plotRect) {
        for (MarkedPoint point : points) {
            double[] res = resolveMarkedPoint(point);
            if (res == null) {
                continue;
            }
            double dataX = res[0];
            double dataY = res[1];
            int color = (int) res[2];
            float sx = (float) mapX(dataX);
            float sy = (float) mapY(dataY);
            if (sx < plotRect.x() - POINT_RADIUS || sx > plotRect.right() + POINT_RADIUS) {
                continue;
            }
            if (sy < plotRect.y() - POINT_RADIUS || sy > plotRect.bottom() + POINT_RADIUS) {
                continue;
            }
            context.fillCircle(sx, sy, POINT_RADIUS + POINT_OUTER_RING, ColorUtils.WHITE.getColor());
            context.fillCircle(sx, sy, POINT_RADIUS, color);
        }
    }

    /** Resolves a MarkedPoint to {dataX, dataY, color}, or null if unresolvable. */
    private double[] resolveMarkedPoint(MarkedPoint point) {
        double dataX;
        double dataY;
        int color;
        switch (point.getMode()) {
            case MarkedPoint.MODE_EXPLICIT:
                dataX = point.getValueA();
                dataY = point.getValueB();
                color = point.getColor();
                break;
            case MarkedPoint.MODE_PLOT_AT_X: {
                int pi = point.getPlotIndex();
                if (pi < 0 || pi >= plots.size()) {
                    return null;
                }
                FunctionPlot plot = plots.get(pi);
                dataX = point.getValueA();
                dataY = plot.evaluate(dataX);
                color = point.isColorInherit() ? plot.getColor() : point.getColor();
                break;
            }
            case MarkedPoint.MODE_PLOT_AT_Y: {
                int pi = point.getPlotIndex();
                if (pi < 0 || pi >= plots.size()) {
                    return null;
                }
                FunctionPlot plot = plots.get(pi);
                dataY = point.getValueA();
                dataX = solveForX(plot, dataY);
                if (Double.isNaN(dataX)) {
                    return null;
                }
                color = point.isColorInherit() ? plot.getColor() : point.getColor();
                break;
            }
            default:
                return null;
        }
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return null;
        }
        return new double[] { dataX, dataY, (double) color };
    }

    private void renderAutoPoints(RenderContext context, LytRect plotRect) {
        autoPointHitCache.clear();
        for (int pi = 0; pi < plots.size(); pi++) {
            FunctionPlot plot = plots.get(pi);
            AutoPointSpec spec = plot.getAutoPointSpec();
            if (spec == null || !spec.isEnabled()) {
                continue;
            }
            int color = spec.colorInherit() ? plot.getColor() : spec.color();
            int drawn = 0;
            if (!Double.isNaN(spec.everyX())) {
                drawn = renderAutoPointsEveryX(context, plotRect, plot, spec, color, drawn, pi);
            }
            if (!Double.isNaN(spec.everyY()) && drawn < AUTO_POINT_MAX_PER_PLOT) {
                renderAutoPointsEveryY(context, plotRect, plot, spec, color, drawn, pi);
            }
        }
    }

    private int renderAutoPointsEveryX(RenderContext context, LytRect plotRect, FunctionPlot plot, AutoPointSpec spec,
        int color, int drawn, int plotIndex) {
        if (plot.isInverse()) {
            return renderAutoPointIntersectionsForAxis(
                context,
                plotRect,
                plot,
                spec,
                color,
                spec.everyX(),
                effectiveXMin,
                effectiveXMax,
                true,
                drawn,
                plotIndex);
        }
        double min = effectiveXMin;
        double max = effectiveXMax;
        double step = spec.everyX();
        double value = Math.ceil(min / step) * step;
        int targets = 0;
        while (value <= max + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
            double dataX = value;
            double dataY = plot.evaluate(value);
            if (drawAutoPoint(context, plotRect, dataX, dataY, color, spec.labelMode(), plotIndex)) {
                drawn++;
            }
            value += step;
            targets++;
        }
        return drawn;
    }

    private int renderAutoPointsEveryY(RenderContext context, LytRect plotRect, FunctionPlot plot, AutoPointSpec spec,
        int color, int drawn, int plotIndex) {
        if (plot.isInverse()) {
            double step = spec.everyY();
            double value = Math.ceil(effectiveYMin / step) * step;
            int targets = 0;
            while (value <= effectiveYMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT
                && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
                double dataY = value;
                double dataX = plot.evaluate(value);
                if (drawAutoPoint(context, plotRect, dataX, dataY, color, spec.labelMode(), plotIndex)) {
                    drawn++;
                }
                value += step;
                targets++;
            }
            return drawn;
        }
        double step = spec.everyY();
        double value = Math.ceil(effectiveYMin / step) * step;
        int targets = 0;
        while (value <= effectiveYMax + 1e-9 && drawn < AUTO_POINT_MAX_PER_PLOT
            && targets < AUTO_POINT_MAX_TARGETS_PER_PLOT) {
            drawn = renderAutoPointIntersectionsForAxis(
                context,
                plotRect,
                plot,
                spec,
                color,
                value,
                effectiveYMin,
                effectiveYMax,
                false,
                drawn,
                plotIndex);
            value += step;
            targets++;
        }
        return drawn;
    }

    private int renderAutoPointIntersectionsForAxis(RenderContext context, LytRect plotRect, FunctionPlot plot,
        AutoPointSpec spec, int color, double target, double targetMin, double targetMax, boolean targetX, int drawn,
        int plotIndex) {
        double independentMin = plot.isInverse() ? effectiveYMin : effectiveXMin;
        double independentMax = plot.isInverse() ? effectiveYMax : effectiveXMax;
        double prevIndependent = independentMin;
        double prevValue = autoPointTargetDifference(plot, prevIndependent, target, targetX);
        for (int i = 1; i <= AUTO_POINT_Y_SCAN_STEPS && drawn < AUTO_POINT_MAX_PER_PLOT; i++) {
            double t = (double) i / (double) AUTO_POINT_Y_SCAN_STEPS;
            double independent = independentMin + (independentMax - independentMin) * t;
            double value = autoPointTargetDifference(plot, independent, target, targetX);
            if (Double.isFinite(prevValue) && Double.isFinite(value) && prevValue * value <= 0d) {
                double solved = solveIndependentForAxis(plot, target, targetX, prevIndependent, independent, prevValue);
                double dataX;
                double dataY;
                if (plot.isInverse()) {
                    dataY = solved;
                    dataX = plot.evaluate(solved);
                } else {
                    dataX = solved;
                    dataY = plot.evaluate(solved);
                }
                double targetValue = targetX ? dataX : dataY;
                if (targetValue < targetMin - 1e-9 || targetValue > targetMax + 1e-9) {
                    prevIndependent = independent;
                    prevValue = value;
                    continue;
                }
                if (drawAutoPoint(context, plotRect, dataX, dataY, color, spec.labelMode(), plotIndex)) {
                    drawn++;
                }
            }
            prevIndependent = independent;
            prevValue = value;
        }
        return drawn;
    }

    private double autoPointTargetDifference(FunctionPlot plot, double independent, double target, boolean targetX) {
        double dataX = plot.isInverse() ? plot.evaluate(independent) : independent;
        double dataY = plot.isInverse() ? independent : plot.evaluate(independent);
        return (targetX ? dataX : dataY) - target;
    }

    private double solveIndependentForAxis(FunctionPlot plot, double target, boolean targetX, double lo, double hi,
        double fLo) {
        for (int i = 0; i < AUTO_POINT_SOLVE_STEPS; i++) {
            double mid = (lo + hi) * 0.5d;
            double fMid = autoPointTargetDifference(plot, mid, target, targetX);
            if (!Double.isFinite(fMid) || Math.abs(fMid) < 1e-9) {
                return mid;
            }
            if (fLo * fMid <= 0d) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }
        return (lo + hi) * 0.5d;
    }

    private boolean drawAutoPoint(RenderContext context, LytRect plotRect, double dataX, double dataY, int color,
        AutoPointLabelMode labelMode, int plotIndex) {
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return false;
        }
        float sx = (float) mapX(dataX);
        float sy = (float) mapY(dataY);
        if (sx < plotRect.x() - POINT_RADIUS || sx > plotRect.right() + POINT_RADIUS) {
            return false;
        }
        if (sy < plotRect.y() - POINT_RADIUS || sy > plotRect.bottom() + POINT_RADIUS) {
            return false;
        }
        autoPointHitCache.add(new double[] { sx, sy, dataX, dataY, (double) color, (double) plotIndex });
        context.fillCircle(sx, sy, POINT_RADIUS + POINT_OUTER_RING, ColorUtils.WHITE.getColor());
        context.fillCircle(sx, sy, POINT_RADIUS, color);
        if (labelMode != null && labelMode != AutoPointLabelMode.NONE) {
            String label = autoPointLabel(labelMode, dataX, dataY);
            int width = context.getStringWidth(label, TOOLTIP_BODY_STYLE);
            int lineHeight = context.getLineHeight(TOOLTIP_BODY_STYLE);
            int x = (int) sx + AUTO_POINT_LABEL_GAP;
            if (x + width > plotRect.right()) {
                x = (int) sx - width - AUTO_POINT_LABEL_GAP;
            }
            int y = (int) sy - lineHeight - AUTO_POINT_LABEL_GAP;
            if (y < plotRect.y()) {
                y = (int) sy + AUTO_POINT_LABEL_GAP;
            }
            context.drawText(label, x, y, TOOLTIP_BODY_STYLE);
        }
        return true;
    }

    private String autoPointLabel(AutoPointLabelMode labelMode, double dataX, double dataY) {
        return switch (labelMode) {
            case X -> formatValue(dataX);
            case Y -> formatValue(dataY);
            case XY -> "(" + formatValue(dataX) + ", " + formatValue(dataY) + ")";
            case NONE -> "";
        };
    }

    private void renderCornerLegend(RenderContext context, LytRect plotRect) {
        if (cornerLegendPosition == CornerLegendPosition.NONE) {
            return;
        }
        List<CornerLegendEntry> entries = new ArrayList<>();
        for (FunctionPlot plot : plots) {
            if (plot.getLabel() != null && !plot.getLabel()
                .isEmpty()) {
                entries.add(new CornerLegendEntry(plot.getLabel(), plot.getColor(), true));
            }
        }
        CornerLegendRenderer.render(
            context,
            plotRect,
            entries,
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor);
    }

    private void renderActiveOverlay(RenderContext context, LytRect plotRect) {
        if (activeMarkedIndex >= 0) {
            renderMarkedPointOverlay(context, plotRect);
            return;
        }
        if (activeAutoPlotIndex >= 0) {
            renderAutoPointOverlay(context, plotRect);
            return;
        }
        if (activePlotIndex < 0 || activePlotIndex >= plots.size()) {
            return;
        }
        FunctionPlot plot = plots.get(activePlotIndex);
        double dataX = activeDataX;
        double dataY;
        if (plot.isInverse()) {
            dataY = dataX;
            dataX = plot.evaluate(dataY);
        } else {
            dataY = plot.evaluate(dataX);
        }
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return;
        }
        float sx = (float) mapX(dataX);
        float sy = (float) mapY(dataY);
        if (sx < plotRect.x() || sx > plotRect.right() || sy < plotRect.y() || sy > plotRect.bottom()) {
            return;
        }
        context.fillCircle(sx, sy, POINT_RADIUS + POINT_OUTER_RING, ColorUtils.WHITE.getColor());
        context.fillCircle(sx, sy, POINT_RADIUS, plot.getColor());

    }

    private void renderMarkedPointOverlay(RenderContext context, LytRect plotRect) {
        double dataX = activeMarkedDataX;
        double dataY = activeMarkedDataY;
        int color = activeMarkedColor;
        float sx = (float) mapX(dataX);
        float sy = (float) mapY(dataY);
        if (sx < plotRect.x() || sx > plotRect.right() || sy < plotRect.y() || sy > plotRect.bottom()) {
            return;
        }
        // Larger highlight for marked points.
        context.fillCircle(sx, sy, POINT_RADIUS + 2f, ColorUtils.WHITE.getColor());
        context.drawCircleOutline(sx, sy, POINT_RADIUS + 2f, 1f, ColorUtils.BLACK.getColor());
        context.fillCircle(sx, sy, POINT_RADIUS, color);

    }

    private void renderAutoPointOverlay(RenderContext context, LytRect plotRect) {
        double dataX = activeAutoDataX;
        double dataY = activeAutoDataY;
        int color = activeAutoColor;
        float sx = (float) mapX(dataX);
        float sy = (float) mapY(dataY);
        if (sx < plotRect.x() || sx > plotRect.right() || sy < plotRect.y() || sy > plotRect.bottom()) {
            return;
        }
        context.fillCircle(sx, sy, POINT_RADIUS + 2f, ColorUtils.WHITE.getColor());
        context.drawCircleOutline(sx, sy, POINT_RADIUS + 2f, 1f, ColorUtils.BLACK.getColor());
        context.fillCircle(sx, sy, POINT_RADIUS, color);

    }

    private Optional<GuideTooltip> createActiveTooltip() {
        if (activeMarkedIndex >= 0 && activeMarkedIndex < points.size()) {
            MarkedPoint point = points.get(activeMarkedIndex);
            return coordinateTooltip(
                !isEmpty(point.getLabel()) ? point.getLabel() : "Point",
                activeMarkedDataX,
                activeMarkedDataY);
        }
        if (activeAutoPlotIndex >= 0 && activeAutoPlotIndex < plots.size()) {
            FunctionPlot plot = plots.get(activeAutoPlotIndex);
            return createPlotTooltip(plot, activeAutoDataX, activeAutoDataY);
        }
        if (activePlotIndex < 0 || activePlotIndex >= plots.size()) {
            return Optional.empty();
        }
        FunctionPlot plot = plots.get(activePlotIndex);
        double dataX;
        double dataY;
        if (plot.isInverse()) {
            dataY = activeDataX;
            dataX = plot.evaluate(dataY);
        } else {
            dataX = activeDataX;
            dataY = plot.evaluate(dataX);
        }
        if (!Double.isFinite(dataX) || !Double.isFinite(dataY)) {
            return Optional.empty();
        }
        return createPlotTooltip(plot, dataX, dataY);
    }

    private Optional<GuideTooltip> createPlotTooltip(FunctionPlot plot, double dataX, double dataY) {
        String summary = buildPlotTooltipSummary(plot, dataX, dataY);
        var richTooltip = plot.updateRichTooltip(summary);
        if (richTooltip != null) {
            return Optional.of(richTooltip);
        }
        String text = summary;
        if (!isEmpty(plot.getTooltipText())) {
            text = text.isEmpty() ? plot.getTooltipText() : text + "\n" + plot.getTooltipText();
        }
        return text.isEmpty() ? Optional.empty() : Optional.of(new TextTooltip(text));
    }

    private String buildPlotTooltipSummary(FunctionPlot plot, double dataX, double dataY) {
        StringBuilder text = new StringBuilder();
        if (!isEmpty(plot.getLabel())) {
            text.append(plot.getLabel());
        }
        if (plot.isShowFunction()) {
            appendTooltipLine(text, functionDisplay(plot));
        }
        if (plot.isShowValues()) {
            appendTooltipLine(text, "x = " + formatValue(dataX) + ", y = " + formatValue(dataY));
        }
        return text.toString();
    }

    private static void appendTooltipLine(StringBuilder text, String line) {
        if (isEmpty(line)) {
            return;
        }
        if (!text.isEmpty()) {
            text.append('\n');
        }
        text.append(line);
    }

    private static String functionDisplay(FunctionPlot plot) {
        String expression = plot.getExpressionText();
        String normalized = expression != null ? expression.trim() : "";
        if (normalized.startsWith("x =") || normalized.startsWith("y =")) {
            return normalized;
        }
        return (plot.isInverse() ? "x = " : "y = ") + normalized;
    }

    private Optional<GuideTooltip> coordinateTooltip(String label, double dataX, double dataY) {
        return Optional.of(new TextTooltip(label + "\n(" + formatValue(dataX) + ", " + formatValue(dataY) + ")"));
    }

    private int measureRichTextWidth(RenderContext context, String text, ResolvedTextStyle style) {
        int width = 0;
        int lineHeight = context.getLineHeight(style);
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            if (segment.isFormula()) {
                LatexMetrics metrics = measureLatex(segment.getValue(), lineHeight);
                width += metrics != null ? metrics.width
                    : context.getStringWidth(delimitedFormula(segment.getValue()), style);
            } else {
                width += context.getStringWidth(segment.getValue(), style);
            }
        }
        return width;
    }

    private int measureRichTextHeight(RenderContext context, String text, ResolvedTextStyle style) {
        int height = context.getLineHeight(style);
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            if (segment.isFormula()) {
                LatexMetrics metrics = measureLatex(segment.getValue(), height);
                if (metrics != null) {
                    height = Math.max(height, metrics.height);
                }
            }
        }
        return height;
    }

    private int measureRichTextWidth(LayoutContext context, String text, ResolvedTextStyle style) {
        int width = 0;
        int lineHeight = context.getLineHeight(style);
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            if (segment.isFormula()) {
                LatexMetrics metrics = measureLatex(segment.getValue(), lineHeight);
                width += metrics != null ? metrics.width
                    : measureTextWidth(context, style, delimitedFormula(segment.getValue()));
            } else {
                width += measureTextWidth(context, style, segment.getValue());
            }
        }
        return width;
    }

    private int measureRichTextHeight(LayoutContext context, String text, ResolvedTextStyle style) {
        int height = context.getLineHeight(style);
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            if (segment.isFormula()) {
                LatexMetrics metrics = measureLatex(segment.getValue(), height);
                if (metrics != null) {
                    height = Math.max(height, metrics.height);
                }
            }
        }
        return height;
    }

    private void drawRichText(RenderContext context, String text, int x, int y, ResolvedTextStyle style) {
        int lineHeight = context.getLineHeight(style);
        int totalHeight = measureRichTextHeight(context, text, style);
        int cursorX = x;
        int fillColor = style == TITLE_STYLE ? ColorUtils.ARGB_FFE6E6E6.getColor() : ColorUtils.CHART_LABEL.getColor();
        for (MarkdownLatexShorthand.Segment segment : MarkdownLatexShorthand.split(text)) {
            if (!segment.isFormula()) {
                context.drawText(segment.getValue(), cursorX, y + (totalHeight - lineHeight) / 2, style);
                cursorX += context.getStringWidth(segment.getValue(), style);
                continue;
            }

            LatexMetrics metrics = measureLatex(segment.getValue(), lineHeight);
            int[] texture = metrics != null
                ? GuideLatexRenderer.INSTANCE
                    .getOrCreateTexture(segment.getValue(), fillColor, LABEL_LATEX_SOURCE_SCALE)
                : null;
            if (metrics == null || texture == null) {
                String fallback = delimitedFormula(segment.getValue());
                context.drawText(fallback, cursorX, y + (totalHeight - lineHeight) / 2, style);
                cursorX += context.getStringWidth(fallback, style);
                continue;
            }
            GuideLatexRenderer.INSTANCE.renderLatex(
                cursorX,
                y + (totalHeight - metrics.height) / 2,
                metrics.width,
                metrics.height,
                texture[0]);
            cursorX += metrics.width;
        }
    }

    private static LatexMetrics measureLatex(String formula, int lineHeight) {
        int[] source = GuideLatexRenderer.INSTANCE.measureSize(formula, ColorUtils.WHITE.getColor(), LABEL_LATEX_SOURCE_SCALE);
        if (source == null) {
            return null;
        }
        int referenceHeight = GuideLatexRenderer.INSTANCE.calibrateRefHeight(LABEL_LATEX_SOURCE_SCALE);
        if (referenceHeight <= 0) {
            return null;
        }
        int width = Math.max(1, (int) Math.ceil((double) source[0] * lineHeight / referenceHeight));
        int height = Math.max(1, (int) Math.ceil((double) source[1] * lineHeight / referenceHeight));
        return new LatexMetrics(width, height);
    }

    private static String delimitedFormula(String formula) {
        return "$$" + formula + "$$";
    }

    private record LatexMetrics(int width, int height) {}

    /**
     * Measure the total height needed to lay out the legend below the plot, given the available
     * width. Returns {@code 0} when no plot has a label, suppressing the legend area entirely.
     */
    private int measureLegendHeight(RenderContext context, int availableWidth) {
        if (availableWidth <= 0) {
            return 0;
        }
        boolean any = false;
        for (FunctionPlot plot : plots) {
            if (plot.getLabel() != null && !plot.getLabel()
                .isEmpty()) {
                any = true;
                break;
            }
        }
        if (!any) {
            return 0;
        }
        int rowHeight = Math.max(LEGEND_SWATCH_SIZE, context.getLineHeight(LEGEND_LABEL_STYLE));
        int rows = 1;
        int rowWidth = 0;
        for (FunctionPlot plot : plots) {
            String label = plot.getLabel();
            if (label == null || label.isEmpty()) {
                continue;
            }
            int itemWidth = LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP
                + context.getStringWidth(label, LEGEND_LABEL_STYLE);
            int needed = rowWidth == 0 ? itemWidth : rowWidth + LEGEND_ITEM_GAP + itemWidth;
            if (rowWidth > 0 && needed > availableWidth) {
                rows++;
                rowWidth = itemWidth;
            } else {
                rowWidth = needed;
            }
        }
        return rows * rowHeight + (rows - 1) * LEGEND_ROW_GAP;
    }

    private int measureLegendHeight(LayoutContext context, int availableWidth) {
        if (availableWidth <= 0) {
            return 0;
        }
        boolean any = false;
        for (FunctionPlot plot : plots) {
            if (plot.getLabel() != null && !plot.getLabel()
                .isEmpty()) {
                any = true;
                break;
            }
        }
        if (!any) {
            return 0;
        }
        int rowHeight = Math.max(LEGEND_SWATCH_SIZE, context.getLineHeight(LEGEND_LABEL_STYLE));
        int rows = 1;
        int rowWidth = 0;
        for (FunctionPlot plot : plots) {
            String label = plot.getLabel();
            if (label == null || label.isEmpty()) {
                continue;
            }
            int itemWidth = LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP
                + measureTextWidth(context, LEGEND_LABEL_STYLE, label);
            int needed = rowWidth == 0 ? itemWidth : rowWidth + LEGEND_ITEM_GAP + itemWidth;
            if (rowWidth > 0 && needed > availableWidth) {
                rows++;
                rowWidth = itemWidth;
            } else {
                rowWidth = needed;
            }
        }
        return rows * rowHeight + (rows - 1) * LEGEND_ROW_GAP;
    }

    private int measureTextWidth(LayoutContext context, ResolvedTextStyle style, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += context.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
    }

    /**
     * Render the legend at {@code (left, top)}. Items flow left-to-right and wrap onto a new row
     * once the next item would exceed {@code availableWidth}.
     */
    private void renderLegend(RenderContext context, int left, int top, int availableWidth) {
        if (availableWidth <= 0) {
            return;
        }
        int rowHeight = Math.max(LEGEND_SWATCH_SIZE, context.getLineHeight(LEGEND_LABEL_STYLE));
        int x = left;
        int y = top;
        boolean firstInRow = true;
        for (FunctionPlot plot : plots) {
            String label = plot.getLabel();
            if (label == null || label.isEmpty()) {
                continue;
            }
            int itemWidth = LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP
                + context.getStringWidth(label, LEGEND_LABEL_STYLE);
            int needed = firstInRow ? itemWidth : (x - left) + LEGEND_ITEM_GAP + itemWidth;
            if (!firstInRow && needed > availableWidth) {
                y += rowHeight + LEGEND_ROW_GAP;
                x = left;
                firstInRow = true;
            }
            if (!firstInRow) {
                x += LEGEND_ITEM_GAP;
            }
            int swatchY = y + (rowHeight - LEGEND_SWATCH_SIZE) / 2;
            LytRect swatch = new LytRect(x, swatchY, LEGEND_SWATCH_SIZE, LEGEND_SWATCH_SIZE);
            context.fillRect(swatch, plot.getColor());
            context.drawBorder(swatch, ColorUtils.BLACK.getColor(), 1);
            int textY = y + (rowHeight - context.getLineHeight(LEGEND_LABEL_STYLE)) / 2;
            context.drawText(label, x + LEGEND_SWATCH_SIZE + LEGEND_SWATCH_TEXT_GAP, textY, LEGEND_LABEL_STYLE);
            x += itemWidth;
            firstInRow = false;
        }
    }

    private void updateHover(float x, float y) {
        if (plotRectCache.isEmpty()) {
            clearActive();
            return;
        }
        if (!plotRectCache.contains((int) x, (int) y)) {
            clearActive();
            return;
        }
        activeMarkedIndex = -1;
        activeAutoPlotIndex = -1;
        activePlotIndex = -1;
        // Preset points take priority over curve segments.
        if (hitTestMarkedPoints(x, y)) {
            return;
        }
        if (hitTestAutoPoints(x, y)) {
            return;
        }
        int hit = hitTest(x, y);
        if (hit < 0) {
            return;
        }
        activePlotIndex = hit;
        FunctionPlot plot = plots.get(hit);
        activeDataX = unmapXToData(activeScreenX, plot.isInverse());
    }

    private void clearActive() {
        activePlotIndex = -1;
        activeMarkedIndex = -1;
        activeAutoPlotIndex = -1;
    }

    private int hitTest(float x, float y) {
        if (sampleXs == null) {
            return -1;
        }
        float bestDistSq = HIT_THRESHOLD_PX * HIT_THRESHOLD_PX;
        int bestIndex = -1;
        float bestNearX = x;
        for (int i = 0; i < plots.size(); i++) {
            float[] xs = sampleXs[i];
            float[] ys = sampleYs[i];
            if (xs == null) {
                continue;
            }
            for (int s = 0; s + 1 < xs.length; s++) {
                float x1 = xs[s];
                float y1 = ys[s];
                float x2 = xs[s + 1];
                float y2 = ys[s + 1];
                if (Float.isNaN(x1) || Float.isNaN(y1) || Float.isNaN(x2) || Float.isNaN(y2)) {
                    continue;
                }
                float[] nr = nearestOnSegment(x, y, x1, y1, x2, y2);
                if (nr[0] < bestDistSq) {
                    bestDistSq = nr[0];
                    bestIndex = i;
                    bestNearX = nr[1];
                }
            }
        }
        if (bestIndex >= 0) {
            activeScreenX = bestNearX;
        }
        return bestIndex;
    }

    /** Scans MarkedPoints within {@link #PRESET_HIT_RADIUS} and sets the active marked state. */
    private boolean hitTestMarkedPoints(float x, float y) {
        if (points.isEmpty()) {
            return false;
        }
        float bestDistSq = PRESET_HIT_RADIUS * PRESET_HIT_RADIUS;
        int bestIndex = -1;
        double bestDataX = 0d;
        double bestDataY = 0d;
        int bestColor = 0;
        for (int i = 0; i < points.size(); i++) {
            double[] res = resolveMarkedPoint(points.get(i));
            if (res == null) {
                continue;
            }
            float sx = (float) mapX(res[0]);
            float sy = (float) mapY(res[1]);
            float dx = x - sx;
            float dy = y - sy;
            float distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestIndex = i;
                bestDataX = res[0];
                bestDataY = res[1];
                bestColor = (int) res[2];
            }
        }
        if (bestIndex >= 0) {
            activeMarkedIndex = bestIndex;
            activeMarkedDataX = bestDataX;
            activeMarkedDataY = bestDataY;
            activeMarkedColor = bestColor;
            return true;
        }
        return false;
    }

    /** Scans auto points (cached during render) within {@link #PRESET_HIT_RADIUS}. */
    private boolean hitTestAutoPoints(float x, float y) {
        if (autoPointHitCache.isEmpty()) {
            return false;
        }
        float bestDistSq = PRESET_HIT_RADIUS * PRESET_HIT_RADIUS;
        int bestIndex = -1;
        for (int i = 0; i < autoPointHitCache.size(); i++) {
            double[] entry = autoPointHitCache.get(i);
            float dx = x - (float) entry[0];
            float dy = y - (float) entry[1];
            float distSq = dx * dx + dy * dy;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestIndex = i;
            }
        }
        if (bestIndex >= 0) {
            double[] entry = autoPointHitCache.get(bestIndex);
            activeAutoPlotIndex = (int) entry[5];
            activeAutoDataX = entry[2];
            activeAutoDataY = entry[3];
            activeAutoColor = (int) entry[4];
            return true;
        }
        return false;
    }

    /** Returns [distSq, nearestX, nearestY] for the closest point on segment (x1,y1)-(x2,y2). */
    private static float[] nearestOnSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSq = dx * dx + dy * dy;
        if (lenSq < 1e-6f) {
            float ex = px - x1;
            float ey = py - y1;
            return new float[] { ex * ex + ey * ey, x1, y1 };
        }
        float t = ((px - x1) * dx + (py - y1) * dy) / lenSq;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        float qx = x1 + t * dx;
        float qy = y1 + t * dy;
        float ex = px - qx;
        float ey = py - qy;
        return new float[] { ex * ex + ey * ey, qx, qy };
    }

    /** Bisection solver used when a marked point only knows the y value. */
    private double solveForX(FunctionPlot plot, double targetY) {
        double xMin = effectiveXMin;
        double xMax = effectiveXMax;
        int steps = 64;
        double prevX = xMin;
        double prevF = plot.evaluate(prevX) - targetY;
        for (int i = 1; i < steps; i++) {
            double t = (double) i / (double) (steps - 1);
            double xVal = xMin + (xMax - xMin) * t;
            double f = plot.evaluate(xVal) - targetY;
            if (Double.isFinite(prevF) && Double.isFinite(f) && prevF * f <= 0d) {
                // Bisect within [prevX, xVal].
                double lo = prevX;
                double hi = xVal;
                double fLo = prevF;
                for (int k = 0; k < 32; k++) {
                    double mid = (lo + hi) * 0.5d;
                    double fm = plot.evaluate(mid) - targetY;
                    if (Math.abs(fm) < 1e-9) {
                        return mid;
                    }
                    if (fLo * fm <= 0d) {
                        hi = mid;
                    } else {
                        lo = mid;
                        fLo = fm;
                    }
                }
                return (lo + hi) * 0.5d;
            }
            prevX = xVal;
            prevF = f;
        }
        return Double.NaN;
    }

    private static double niceStep(double range) {
        if (!(range > 0d) || !Double.isFinite(range)) {
            return 1d;
        }
        double exponent = Math.floor(Math.log10(range));
        double pow10 = Math.pow(10d, exponent);
        double mantissa = range / pow10;
        double base;
        if (mantissa < 1.5d) {
            base = 0.2d;
        } else if (mantissa < 3d) {
            base = 0.5d;
        } else if (mantissa < 7d) {
            base = 1d;
        } else {
            base = 2d;
        }
        return base * pow10;
    }

    private static String formatTick(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }

    private static String formatValue(double value) {
        if (!Double.isFinite(value)) {
            return Double.toString(value);
        }
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.3f", value);
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private static ResolvedTextStyle makeStyle(int argb, boolean bold) {
        return new ResolvedTextStyle(
            1f,
            bold,
            false,
            false,
            false,
            false,
            false,
            false,
            null,
            new ConstantColor(argb),
            WhiteSpaceMode.NORMAL,
            TextAlignment.LEFT,
            false,
            null,
            false);
    }
}
