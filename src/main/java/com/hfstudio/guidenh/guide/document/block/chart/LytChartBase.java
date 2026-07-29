package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.ResponsiveVisualSizing;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.tooltip.AppendedItemTooltip;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

import lombok.Getter;
import lombok.Setter;

/**
 * Common base class for all 5 chart types: handles common attributes (title, size, background, legend
 * position, etc.), layout, and hover state management.
 */
public abstract class LytChartBase extends LytBlock implements InteractiveElement {

    public static final int DEFAULT_WIDTH = 320;
    public static final int DEFAULT_HEIGHT = 200;
    protected static final int PADDING = 8;
    protected static final int TITLE_GAP = 4;
    protected static final int LEGEND_GAP = 6;
    protected static final int LEGEND_SWATCH_SIZE = 8;
    protected static final int LEGEND_ENTRY_GAP = 12;
    protected static final int MIN_PLOT_HEIGHT = 72;

    @Getter
    @Setter
    private String title;
    private int explicitWidth = -1;
    private int explicitHeight = -1;
    @Getter
    @Setter
    private int backgroundColor = 0xFF1B1F23;
    @Getter
    @Setter
    private int borderColor = 0xFF3A4047;
    @Getter
    @Setter
    private int titleColor = 0xFFE6E6E6;
    @Getter
    private ChartLegendPosition legendPosition = ChartLegendPosition.TOP;
    @Getter
    private ChartLabelPosition labelPosition = ChartLabelPosition.NONE;
    @Getter
    @Setter
    private int labelColor = 0xFFEEEEEE;
    @Getter
    private CornerLegendPosition cornerLegendPosition = CornerLegendPosition.NONE;
    @Getter
    private int cornerLegendWidth = CornerLegendRenderer.DEFAULT_WIDTH;
    @Getter
    private int cornerLegendHeight = CornerLegendRenderer.DEFAULT_HEIGHT;
    @Getter
    @Setter
    private int cornerLegendBackgroundColor = CornerLegendRenderer.DEFAULT_BACKGROUND;

    /** Currently hovered hit key; {@code -1} means none. The exact semantics is decided by each subclass. */
    protected int hoveredKey = -1;

    /**
     * Chrome height (title + legend + padding), cached during {@link #computeLayout}
     * for serialization into PieChartData.
     * Computed lazily when accessed and still zero (no pre-pass), using static
     * font metrics via {@link GuideText} instead of {@link LayoutContext}.
     */
    private int chromeHeight;

    /**
     * Returns the chrome height, computing it lazily if no layout pass has been run.
     * Uses {@link GuideText} static font metrics so it works without a {@link LayoutContext}.
     * <p>
     * NOTE: This is only used by the Java render path (computeLayout for scaling).
     * The Rust measure path now computes chrome internally from the final width,
     * using legend wrapping transplanted from {@link ChartLegendRenderer}.
     * See the T6a fix in measure.rs for the Rust-side computation.
     */
    public int getChromeHeight() {
        if (chromeHeight <= 0) {
            chromeHeight = computeChromeHeightForWidth(preferredWidth());
        }
        return chromeHeight;
    }

    /**
     * Computes chrome height purely from block fields and static font metrics.
     * Does NOT require a {@link LayoutContext}, so it works even without the
     * Java layout pre-pass having been run.
     */
    private int computeChromeHeightForWidth(int width) {
        int chrome = PADDING * 2;
        if (title != null && !title.isEmpty()) {
            chrome += GuideText.lineHeight(textStyle(titleColor)) + TITLE_GAP;
        }
        int contentWidth = Math.max(1, width - PADDING * 2);
        chrome += ChartLegendRenderer
            .measureHeightStatic(collectLegendEntries(), legendPosition, contentWidth);
        if (legendPosition == ChartLegendPosition.TOP || legendPosition == ChartLegendPosition.BOTTOM) {
            chrome += legendPosition == ChartLegendPosition.NONE ? 0 : LEGEND_GAP;
        }
        return chrome;
    }

    /**
     * Returns the title chrome (lineHeight + TITLE_GAP), width-independent,
     * for Rust-side chrome computation. 0 if no title is set.
     */
    public float getTitleChromeForRust() {
        if (title != null && !title.isEmpty()) {
            return GuideText.lineHeight(textStyle(titleColor)) + TITLE_GAP;
        }
        return 0f;
    }

    /**
     * Returns the legend position as a byte matching the schema:
     * 0=NONE, 1=TOP, 2=BOTTOM, 3=LEFT, 4=RIGHT.
     */
    public byte getLegendPositionForRust() {
        return switch (legendPosition) {
            case TOP -> (byte) 1;
            case BOTTOM -> (byte) 2;
            case LEFT -> (byte) 3;
            case RIGHT -> (byte) 4;
            default -> (byte) 0;
        };
    }

    /**
     * Returns the legend row height (max of swatch size and line height)
     * for Rust-side chrome computation.
     */
    public float getLegendRowHeightForRust() {
        ResolvedTextStyle legendStyle = textStyle(0xFFCCCCCC);
        int lineHeight = GuideText.lineHeight(legendStyle);
        return Math.max(LEGEND_SWATCH_SIZE, lineHeight);
    }

    /**
     * Returns per-legend-entry label widths for Rust-side chrome computation.
     * Each entry's width = LEGEND_SWATCH_SIZE + SWATCH_TEXT_GAP + measureWidth(label, legendStyle).
     */
    public float[] getLegendLabelWidthsForRust() {
        List<ChartLegendRenderer.LegendEntry> entries = collectLegendEntries();
        ResolvedTextStyle legendStyle = textStyle(0xFFCCCCCC);
        float[] widths = new float[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            ChartLegendRenderer.LegendEntry entry = entries.get(i);
            int labelW = GuideText.measureWidth(entry.name, legendStyle);
            widths[i] = LEGEND_SWATCH_SIZE + ChartLegendRenderer.getSwatchTextGap() + labelW;
        }
        return widths;
    }

    public void setExplicitSize(int width, int height) {
        this.explicitWidth = width > 0 ? width : -1;
        this.explicitHeight = height > 0 ? height : -1;
    }

    @Override
    public int getExplicitWidth() {
        return explicitWidth;
    }

    @Override
    public int getExplicitHeight() {
        return explicitHeight;
    }

    public void setLegendPosition(ChartLegendPosition legendPosition) {
        this.legendPosition = legendPosition != null ? legendPosition : ChartLegendPosition.NONE;
    }

    public void setLabelPosition(ChartLabelPosition labelPosition) {
        this.labelPosition = labelPosition != null ? labelPosition : ChartLabelPosition.NONE;
    }

    public void setCornerLegendPosition(CornerLegendPosition cornerLegendPosition) {
        this.cornerLegendPosition = cornerLegendPosition != null ? cornerLegendPosition : CornerLegendPosition.NONE;
    }

    public void setCornerLegendSize(int width, int height) {
        this.cornerLegendWidth = width > 0 ? width : CornerLegendRenderer.DEFAULT_WIDTH;
        this.cornerLegendHeight = height > 0 ? height : CornerLegendRenderer.DEFAULT_HEIGHT;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int width = preferredWidth();
        width = ResponsiveVisualSizing.scaleWidth(width, context.getVisualScale(), 64);
        int height = explicitHeight > 0 ? explicitHeight : DEFAULT_HEIGHT;
        width = Math.max(1, Math.min(width, availableWidth));
        int estimatedChrome = estimateFixedChromeHeight(context, width);
        this.chromeHeight = estimatedChrome;
        height = ResponsiveVisualSizing.scaleBodyHeightForWidth(
            preferredWidth(),
            height,
            width,
            estimatedChrome,
            MIN_PLOT_HEIGHT);
        return new LytRect(x, y, width, height);
    }

    private int preferredWidth() {
        return (explicitWidth > 0 ? explicitWidth : DEFAULT_WIDTH) + getExtraPlotWidth();
    }

    private int estimateFixedChromeHeight(LayoutContext context, int width) {
        int chromeHeight = PADDING * 2;
        if (title != null && !title.isEmpty()) {
            chromeHeight += context.getLineHeight(textStyle(titleColor)) + TITLE_GAP;
        }
        int contentWidth = Math.max(1, width - PADDING * 2);
        chromeHeight += ChartLegendRenderer
            .measureHeight(context, collectLegendEntries(), legendPosition, contentWidth);
        if (legendPosition == ChartLegendPosition.TOP || legendPosition == ChartLegendPosition.BOTTOM) {
            chromeHeight += legendPosition == ChartLegendPosition.NONE ? 0 : LEGEND_GAP;
        }
        return chromeHeight;
    }

    /**
     * Subclasses override to request additional horizontal space (for example, a side-mounted pie inset).
     * Default 0.
     */
    public int getExtraPlotWidth() {
        return 0;
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public final void computePrimitives(PrimitiveCollector c) {
        c.emit(
            new GuideRenderPrimitive.FillRect(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                backgroundColor));
        c.emit(
            new GuideRenderPrimitive.DrawBorder(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                1,
                1,
                1,
                1,
                borderColor));

        ResolvedTextStyle textStyle = textStyle(0xFFFFFFFF);
        int contentTop = bounds.y() + PADDING;
        int contentBottom = bounds.bottom() - PADDING;
        int contentLeft = bounds.x() + PADDING;
        int contentRight = bounds.right() - PADDING;

        if (title != null && !title.isEmpty()) {
            ResolvedTextStyle titleStyle = textStyle(titleColor);
            int titleWidth = GuideText.measureWidth(title, titleStyle);
            int titleX = bounds.x() + (bounds.width() - titleWidth) / 2;
            GuideText.emitText(c, title, titleX, contentTop, titleStyle);
            contentTop += GuideText.lineHeight(titleStyle) + TITLE_GAP;
        }

        // Compute legend area.
        List<ChartLegendRenderer.LegendEntry> legend = collectLegendEntries();
        ChartLegendRenderer.Layout legendLayout = ChartLegendRenderer
            .computeLayout(legend, legendPosition, contentLeft, contentTop, contentRight, contentBottom);

        int plotLeft = legendLayout.plotLeft;
        int plotTop = legendLayout.plotTop;
        int plotRight = legendLayout.plotRight;
        int plotBottom = legendLayout.plotBottom;
        if (plotRight - plotLeft <= 8 || plotBottom - plotTop <= 8) {
            return;
        }
        LytRect plotRect = new LytRect(plotLeft, plotTop, plotRight - plotLeft, plotBottom - plotTop);

        LytRect innerPlotRect = renderChart(c, plotRect);
        if (innerPlotRect == null || innerPlotRect.isEmpty()) {
            innerPlotRect = plotRect;
        }
        CornerLegendRenderer.emit(
            c,
            innerPlotRect,
            collectCornerLegendEntries(),
            cornerLegendPosition,
            cornerLegendWidth,
            cornerLegendHeight,
            cornerLegendBackgroundColor);
        ChartLegendRenderer.emit(c, legendLayout, textStyle);
    }

    /**
     * Migrated to {@link #computePrimitives}; the legacy path is unreachable
     * (the collector only invokes it when {@link #usePrimitives()} is false).
     */
    @Override
    public final void render(RenderContext context) {}

    /**
     * Subclasses implement the chart-specific drawing; {@code plotRect} has already excluded the space
     * occupied by the title and legend.
     */
    /** @return the inner rectangle actually used for data plotting (may be {@code plotRect} itself) */
    protected abstract LytRect renderChart(PrimitiveCollector c, LytRect plotRect);

    /**
     * Collect legend entries; empty by default. Subclasses override as needed.
     */
    protected abstract List<ChartLegendRenderer.LegendEntry> collectLegendEntries();

    protected List<CornerLegendEntry> collectCornerLegendEntries() {
        return List.of();
    }

    @Override
    public final Optional<GuideTooltip> getTooltip(float x, float y) {
        int hit = hitTest(x, y);
        hoveredKey = hit;
        if (hit < 0) {
            return Optional.empty();
        }
        String text = describeHit(hit);
        String extra = getHitExtraTooltip(hit);
        ItemStack stack = getHitItemStack(hit);
        if (stack != null) {
            List<String> extraLines = new ArrayList<>();
            appendNonEmptyLines(extraLines, text);
            appendNonEmptyLines(extraLines, extra);
            return Optional.of(new AppendedItemTooltip(stack, extraLines));
        }
        StringBuilder sb = new StringBuilder();
        if (text != null && !text.isEmpty()) {
            sb.append(text);
        }
        if (extra != null && !extra.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(extra);
        }
        if (sb.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new TextTooltip(sb.toString()));
    }

    private static void appendNonEmptyLines(List<String> sink, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        for (String line : text.split("\n")) {
            if (!line.isEmpty()) {
                sink.add(line);
            }
        }
    }

    /**
     * The {@link ItemStack} corresponding to the hit item; when non-{@code null} is returned, the vanilla
     * item tooltip is used with extra text appended.
     */
    protected ItemStack getHitItemStack(int key) {
        return null;
    }

    /**
     * Optional extra tooltip text for the hit item; appended after {@link #describeHit(int)}.
     */
    protected String getHitExtraTooltip(int key) {
        return null;
    }

    /**
     * Hit testing: returns a non-negative key indicating that an item is hovered (semantics decided by
     * subclass), or {@code -1} for none.
     */
    protected abstract int hitTest(float x, float y);

    /**
     * Convert a hit key to tooltip text (use \n to separate multiple lines).
     */
    protected abstract String describeHit(int key);

    public static ResolvedTextStyle textStyle(int argb) {
        return new ResolvedTextStyle(
            1f,
            false,
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
            false,
            0.0f);
    }

    public static String formatPercent(double ratio) {
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return "0%";
        }
        return String.format("%.1f%%", ratio * 100d);
    }

    public static String formatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-6) {
            return Long.toString((long) Math.rint(value));
        }
        return String.format("%.2f", value);
    }
}
