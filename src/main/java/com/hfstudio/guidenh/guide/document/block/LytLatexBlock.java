package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.scilab.forge.jlatexmath.TeXConstants;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.latex.GuideLatexRenderer;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuideText;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

/**
 * Inline-flow LaTeX block. When placed inside a
 * {@link LytFlowInlineBlock}, it renders a LaTeX formula at a
 * size proportional to the surrounding text, automatically expanding the line height when the formula is
 * taller than a single character (e.g. fractions).
 *
 * <p>
 * <b>Typeset-at-target-size.</b> The formula is typeset by jlatexmath directly at
 * {@code fontSize = GuideText.BASE_FONT_SIZE × userScale} pixels ({@code userScale} = the tag's
 * {@code scale} attribute). {@link #getFormulaDisplayW()}/{@link #getFormulaDisplayH()} ARE the
 * TeXIcon pixel dimensions returned by {@link GuideLatexRenderer#measureSize}: no scale factor,
 * no reference-string calibration, no texture scaling.
 *
 * <p>
 * Vertical alignment is controlled by {@link LatexVerticalAlign}:
 * <ul>
 * <li>{@link LatexVerticalAlign#BASELINE}: formula math baseline aligns with the text baseline (default).
 * This is the best choice for most inline formulas: letters and superscripts sit flush with
 * surrounding text, while fractions and integrals extend above/below the baseline naturally.</li>
 * <li>{@link LatexVerticalAlign#TOP}: formula top aligns with the text line top.</li>
 * <li>{@link LatexVerticalAlign#CENTER}: formula is centered on the text line.</li>
 * <li>{@link LatexVerticalAlign#BOTTOM}: formula bottom aligns with the text line bottom.</li>
 * </ul>
 * {@code offsetX} and {@code offsetY} are pixel offsets applied on top of the alignment.
 */
public class LytLatexBlock extends LytBlock implements InteractiveElement {

    @Getter
    private final String formula;
    @Getter
    private final int fillColorArgb;
    /**
     * DEPRECATED: legacy jlatexmath render size, parsed for backward compatibility and ignored.
     * Retained so old documents that set {@code sourceScale} still parse. No longer serialized
     * (the LatexDisplayData.source_scale slot is DEPRECATED and Rust never reads it), so the
     * active render size is {@link #getFontSize()}.
     */
    @Getter
    private final float sourceScale;
    @Getter
    private final float userScale;
    /** Target typeset font size in pixels: {@link GuideText#BASE_FONT_SIZE} × {@code userScale}. */
    @Getter
    private final float fontSize;
    private final int style;
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
    /**
     * Exact jlatexmath math-baseline ratio from
     * {@code GuideLatexRenderer.measureBaselineRatio} (see
     * {@link TeXIcon#getBaseLine()}: baseline distance from the icon top as a
     * fraction of the icon's total height, insets included, in [0,1]). Used
     * instead of a source-pixel depth so the display depth is rounded exactly
     * once, at display resolution.
     */
    private float sourceBaseLineRatio;

    public LytLatexBlock(String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, LatexVerticalAlign valign, int offsetX, int offsetY) {
        this(
            formula,
            new LatexRenderOptions(
                TeXConstants.STYLE_DISPLAY,
                fillColorArgb,
                sourceScale,
                userScale,
                tooltip,
                valign,
                offsetX,
                offsetY));
    }

    public LytLatexBlock(String formula, LatexRenderOptions options) {
        this.formula = formula;
        this.fillColorArgb = options.fillColorArgb();
        this.sourceScale = options.sourceScale();
        this.userScale = options.userScale();
        this.fontSize = options.fontSize();
        this.style = options.style();
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
        // Typeset-at-target-size: the icon pixel dimensions ARE the display dimensions
        // (no scale factor, no calibration, no texture scaling).
        formulaDisplayH = sourceHeightPx;
        formulaDisplayW = sourceWidthPx;
        // baselineAscent must also be computed here, because the Java layout pre-pass
        // has been removed (Rust is sole geometry authority), so computeLayout()
        // is never called. The Rust inline post-pass uses this value as param
        // (align=1) to anchor the formula's math baseline at the text baseline.
        int lineHeight = GuideText.lineHeight(null);
        int depthDisplay = scaleSourceDepthFromBaseline(sourceBaseLineRatio, formulaDisplayH);
        int alignOffset = switch (valign) {
            case CENTER -> (lineHeight - formulaDisplayH) / 2;
            case BOTTOM -> lineHeight - formulaDisplayH;
            case BASELINE -> lineHeight - formulaDisplayH + depthDisplay;
            default -> 0; // TOP
        };
        baselineAscent = lineHeight - (alignOffset + offsetY);
    }

    /**
     * Legacy Java layout path. The Java pre-pass has been removed (Rust is the sole geometry
     * authority), so this only keeps the abstract contract of {@link LytBlock} satisfied while
     * {@link LytParagraph} still calls {@code layout(...)} for every inline child. Geometry
     * comes from {@link #computeFormulaDisplay()}.
     */
    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        computeFormulaDisplay();
        return new LytRect(x, y, formulaDisplayW, formulaDisplayH);
    }

    private boolean resolveSourceMetrics() {
        if (sourceMetricsResolved) {
            return sourceWidthPx > 0 && sourceHeightPx > 0;
        }
        sourceMetricsResolved = true;
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, fontSize, style);
        if (size == null) {
            return false;
        }
        sourceWidthPx = size[0];
        sourceHeightPx = size[1];
        sourceBaseLineRatio = GuideLatexRenderer.INSTANCE.measureBaselineRatio(formula, fillColorArgb, fontSize, style);
        return sourceWidthPx > 0 && sourceHeightPx > 0;
    }

    /**
     * Display distance from the formula's math baseline to the icon bottom,
     * derived directly from the exact jlatexmath baseline ratio instead of a
     * source-pixel depth. {@link TeXIcon#getBaseLine()} returns the distance
     * from the icon's top edge to its math baseline as a fraction of the
     * icon's total height (the true 2px/side insets of the two-arg
     * {@code setInsets(insets, true)} included), so the math baseline sits
     * {@code formulaDisplayH × (1 - ratio)} above the bottom of the display
     * box (which is exactly the icon at typeset-at-target-size, insets
     * included). This is rounded exactly once, at display resolution.
     */
    private int scaleSourceDepthFromBaseline(float baseLineRatio, int formulaDisplayH) {
        return Math.max(0, (int) Math.round(formulaDisplayH * (1f - baseLineRatio)));
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {}

    @Override
    public boolean usePrimitives() {
        return true;
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        if (formulaDisplayW <= 0 || formulaDisplayH <= 0) {
            return;
        }

        // Deferred rasterization: emit a stable latex blit token; the render
        // engine resolves the real texture at draw time at the exact device
        // output scale (texture rasterized at fontSize × rasterScale, blit
        // quad stays at the document formulaDisplayW/H, 1:1 device pixels).
        int token = GuideLatexRenderer.INSTANCE.registerLatexBlit(formula, fillColorArgb, fontSize, style);

        c.emit(
            new GuideRenderPrimitive.BlitTexture(
                token,
                bounds.x() + offsetX,
                bounds.y(),
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

        int token = GuideLatexRenderer.INSTANCE.registerLatexBlit(formula, fillColorArgb, fontSize, style);
        // Legacy (non-primitive) render path: no transform stack is available,
        // so fall back to the in-game display scale.
        int[] tex = GuideLatexRenderer.INSTANCE.resolveLatexTexture(token, DisplayScale.scaleFactor());
        if (tex == null) {
            return;
        }

        GuideLatexRenderer.INSTANCE
            .renderLatex(bounds.x() + offsetX, bounds.y(), formulaDisplayW, formulaDisplayH, tex[0]);
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
        return new LytRect(bounds.x() + offsetX, bounds.y(), formulaDisplayW, formulaDisplayH);
    }

    @Nullable
    @Override
    public LytRect getBounds() {
        return bounds;
    }
}
