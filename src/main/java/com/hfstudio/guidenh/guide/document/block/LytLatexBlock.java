package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.latex.GuideLatexRenderer;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * Inline-flow LaTeX block. When placed inside a
 * {@link com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock}, it renders a LaTeX formula at a
 * size proportional to the surrounding text, automatically expanding the line height when the formula is
 * taller than a single character (e.g. fractions).
 *
 * <p>
 * Vertical alignment is controlled by {@link LatexVerticalAlign}:
 * <ul>
 * <li>{@link LatexVerticalAlign#BASELINE} — formula math baseline aligns with the text baseline (default).
 * This is the best choice for most inline formulas: letters and superscripts sit flush with
 * surrounding text, while fractions and integrals extend above/below the baseline naturally.</li>
 * <li>{@link LatexVerticalAlign#TOP} — formula top aligns with the text line top.</li>
 * <li>{@link LatexVerticalAlign#CENTER} — formula is centered on the text line.</li>
 * <li>{@link LatexVerticalAlign#BOTTOM} — formula bottom aligns with the text line bottom.</li>
 * </ul>
 * {@code offsetX} and {@code offsetY} are pixel offsets applied on top of the alignment.
 */
public class LytLatexBlock extends LytBlock implements InteractiveElement {

    @Getter
    private final String formula;
    @Getter
    private final int fillColorArgb;
    @Getter
    private final float sourceScale;
    @Getter
    private final float userScale;
    @Nullable
    private final GuideTooltip tooltip;
    @Getter
    private final LatexVerticalAlign valign;
    @Getter
    private final int offsetX;
    @Getter
    private final int offsetY;

    /** Formula display width in GUI pixels, recomputed each layout pass. */
    private int formulaDisplayW;
    /** Formula display height in GUI pixels, recomputed each layout pass. */
    private int formulaDisplayH;
    /** True when lazy computation has been attempted (even if result is 0). */
    private boolean formulaDisplayComputed;
    /** Vertical pixel offset inside the layout bounds, recomputed each layout pass. */
    private int renderYOffset;
    /**
     * Distance from the formula's top to the text baseline it aligns with, in GUI
     * pixels. Recomputed each layout pass; consumed by the Rust inline post-pass,
     * which anchors the block's top this far above the placeholder's baseline.
     */
    @Getter
    private float baselineAscent;
    private boolean sourceMetricsResolved;
    private int sourceWidthPx;
    private int sourceHeightPx;
    private int sourceDepthPx;
    private int sourceRefHeightPx;

    public LytLatexBlock(String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, LatexVerticalAlign valign, int offsetX, int offsetY) {
        this(formula, new LatexRenderOptions(fillColorArgb, sourceScale, userScale, tooltip, valign, offsetX, offsetY));
    }

    public LytLatexBlock(String formula, LatexRenderOptions options) {
        this.formula = formula;
        this.fillColorArgb = options.fillColorArgb();
        this.sourceScale = options.sourceScale();
        this.userScale = options.userScale();
        this.tooltip = options.tooltip();
        this.valign = options.valign();
        this.offsetX = options.offsetX();
        this.offsetY = options.offsetY();
    }

    /**
     * Returns the formula display width, computing it lazily if no layout pass has been run.
     * Uses static font metrics via {@link GuideText} so it works without a {@link LayoutContext}.
     */
    public int getFormulaDisplayW() {
        if (!formulaDisplayComputed) {
            computeFormulaDisplay();
        }
        return formulaDisplayW;
    }

    /**
     * Returns the formula display height, computing it lazily if no layout pass has been run.
     * Uses static font metrics via {@link GuideText} so it works without a {@link LayoutContext}.
     */
    public int getFormulaDisplayH() {
        if (!formulaDisplayComputed) {
            computeFormulaDisplay();
        }
        return formulaDisplayH;
    }

    /** Lazy-compute formula display dimensions using static font metrics. */
    private void computeFormulaDisplay() {
        formulaDisplayComputed = true;
        if (!resolveSourceMetrics()) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            return;
        }
        int lineHeight = GuideText.lineHeight(null);
        formulaDisplayH = scaleSourceMetricCeil(sourceHeightPx, lineHeight);
        formulaDisplayW = scaleSourceMetricCeil(sourceWidthPx, lineHeight);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        formulaDisplayComputed = true;
        if (!resolveSourceMetrics()) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            renderYOffset = 0;
            return new LytRect(x, y, 0, 0);
        }

        int lineHeight = context.getLineHeight(null);
        formulaDisplayH = scaleSourceMetricCeil(sourceHeightPx, lineHeight);
        formulaDisplayW = scaleSourceMetricCeil(sourceWidthPx, lineHeight);

        int alignOffset = switch (valign) {
            case CENTER -> (lineHeight - formulaDisplayH) / 2;
            case BOTTOM -> lineHeight - formulaDisplayH;
            case BASELINE -> {
                // Align the formula's math baseline with the text baseline.
                //
                // Both calibrateRefHeight() and measureSize() apply the same Insets value,
                // so the bottom-inset term B cancels out in the algebra:
                //
                // text_baseline = (refH - B) * lineHeight / refH
                // formula_ascent = (size[1] - B - size[2]) * lineHeight * userScale / refH
                // alignOffset = text_baseline - formula_ascent
                // = (lineHeight - displayH) + size[2] * lineHeight * userScale / refH
                // = (lineHeight - displayH) + depthDisplay
                //
                // For depth-zero formulas (size[2]==0) this is identical to BOTTOM.
                int depthDisplay = scaleSourceMetricRound(sourceDepthPx, lineHeight);
                yield lineHeight - formulaDisplayH + depthDisplay;
            }
            default -> 0; // TOP
        };
        int desiredRenderYOffset = alignOffset + offsetY;
        int topInset = Math.max(0, -desiredRenderYOffset);
        int bottomInset = Math.max(0, desiredRenderYOffset);
        renderYOffset = desiredRenderYOffset + topInset;
        // Ascent above the text baseline, in the same units the Rust inline
        // post-pass anchors with: for BASELINE this is exactly the formula's
        // math ascent (displayH - depth); the algebra also covers TOP/CENTER/
        // BOTTOM via their align offsets.
        baselineAscent = lineHeight - desiredRenderYOffset;

        return new LytRect(x, y - topInset, formulaDisplayW, topInset + formulaDisplayH + bottomInset);
    }

    private boolean resolveSourceMetrics() {
        if (sourceMetricsResolved) {
            return sourceWidthPx > 0 && sourceHeightPx > 0;
        }
        sourceMetricsResolved = true;
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale);
        if (size == null) {
            return false;
        }
        sourceWidthPx = size[0];
        sourceHeightPx = size[1];
        sourceDepthPx = size[2];
        sourceRefHeightPx = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);
        return sourceWidthPx > 0 && sourceHeightPx > 0;
    }

    private int scaleSourceMetricCeil(int sourceMetric, int lineHeight) {
        return (int) Math.max(1, Math.ceil((double) sourceMetric * lineHeight * userScale / sourceRefHeightPx));
    }

    private int scaleSourceMetricRound(int sourceMetric, int lineHeight) {
        return (int) Math.round((double) sourceMetric * lineHeight * userScale / sourceRefHeightPx);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    /**
     * External (Rust) layout anchors the block's bounds at the formula's visual
     * box — the legacy line-expansion insets and render offset no longer apply.
     * Zero the offset so the primitive blit and {@link #getVisualBounds()} use
     * the bounds origin from now on (the legacy layout path recomputes it on
     * the next Java pass before it is needed again).
     */
    @Override
    protected void afterExternalLayout() {
        renderYOffset = 0;
    }

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        if (formulaDisplayW <= 0 || formulaDisplayH <= 0) {
            return;
        }

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale);
        if (tex == null) {
            return;
        }

        c.emit(
            new GuideRenderPrimitive.BlitTexture(
                tex[0],
                bounds.x() + offsetX,
                bounds.y() + renderYOffset,
                formulaDisplayW,
                formulaDisplayH,
                0f,
                0f,
                1f,
                1f));
    }

    @Override
    public void render(RenderContext context) {
        if (formulaDisplayW <= 0 || formulaDisplayH <= 0) {
            return;
        }

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale);
        if (tex == null) {
            return;
        }

        GuideLatexRenderer.INSTANCE
            .renderLatex(bounds.x() + offsetX, bounds.y() + renderYOffset, formulaDisplayW, formulaDisplayH, tex[0]);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.ofNullable(tooltip);
    }

    @Override
    protected LytVisitor.Result visitChildren(LytVisitor visitor, boolean includeOutOfTreeContent) {
        return LytVisitor.Result.CONTINUE;
    }

    public boolean isShowTooltip() {
        return tooltip != null;
    }

    @Nullable
    public GuideTooltip getLatexTooltip() {
        return tooltip;
    }

    public LytRect getVisualBounds() {
        if (bounds == null || bounds.isEmpty()) {
            return LytRect.empty();
        }
        return new LytRect(bounds.x() + offsetX, bounds.y() + renderYOffset, formulaDisplayW, formulaDisplayH);
    }

    @Nullable
    @Override
    public LytRect getBounds() {
        return bounds;
    }
}
