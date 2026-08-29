package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.scilab.forge.jlatexmath.TeXConstants;

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
    private int sourceRefHeightPx;
    /**
     * Exact jlatexmath math-baseline ratio from
     * {@code GuideLatexRenderer.measureBaselineRatio} (see
     * {@link org.scilab.forge.jlatexmath.TeXIcon#getBaseLine()}: baseline distance from the icon top as a
     * fraction of the icon's total height, insets included, in [0,1]). Used
     * instead of a source-pixel depth so the display depth is rounded exactly
     * once, at display resolution.
     */
    private float sourceBaseLineRatio;

    /**
     * Total icon insets (2px per side) applied by
     * {@code GuideLatexRenderer.setInsets(new Insets(2,2,2,2), true)} to every measured icon
     * and the calibration "x". The two-arg (trueValues) form is essential here: the single-arg
     * {@code setInsets(Insets)} delegates to {@code setInsets(insets, false)} and silently adds
     * {@code (int)(0.18f*size)} to every side — 18px extra per side at the default size 100, i.e.
     * the "2px" padding was actually 20px/side (40px total per dimension). That phantom padding
     * inflated {@code sourceRefHeightPx} so badly that this 4px subtraction could not recover the
     * true x-content height, and inline formulas rendered ≈0.67× the body x-height. With the
     * real 2px/side insets, {@code sourceRefHeightPx - LATEX_INSET_PX} is again the calibration
     * "x" glyph content height; the fixed padding is removed from the calibration height before
     * the scaling ratio so it does not distort the scale once the target is the (smaller) body
     * x-height; the display box then scales the whole icon (content + insets) uniformly, so the
     * padding is present but scaled, never inflating the glyph itself.
     */
    static final int LATEX_INSET_PX = 4;

    /**
     * Perceptual size compensation applied on top of the exact x-height
     * calibration target. TeX math is designed with tight lower-case metrics
     * (Computer Modern's x-height is small relative to its cap height, and
     * formulas mostly consist of lower-case letters), so an inline formula
     * whose body letters exactly equal the surrounding CJK text's x-height
     * still reads as noticeably smaller than the body text. This factor
     * restores visual weight parity: the calibration "x" targets
     * {@code x-height × 1.2} instead of the raw x-height (user report:
     * "公式字体过小").
     */
    static final float INLINE_PERCEPTUAL_FACTOR = 1.2f;

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
        int lineHeight = GuideText.lineHeight(null);
        float scaleFactor = inlineScaleFactor();
        formulaDisplayH = scaleSourceMetricCeil(sourceHeightPx, scaleFactor);
        formulaDisplayW = scaleSourceMetricCeil(sourceWidthPx, scaleFactor);
        // baselineAscent must also be computed here — the Java layout pre-pass
        // has been removed (Rust is sole geometry authority), so computeLayout()
        // is never called. The Rust inline post-pass uses this value as param
        // (align=1) to anchor the formula's math baseline at the text baseline.
        int depthDisplay = scaleSourceDepthFromBaseline(sourceBaseLineRatio, formulaDisplayH);
        int alignOffset = switch (valign) {
            case CENTER -> (lineHeight - formulaDisplayH) / 2;
            case BOTTOM -> lineHeight - formulaDisplayH;
            case BASELINE -> lineHeight - formulaDisplayH + depthDisplay;
            default -> 0; // TOP
        };
        baselineAscent = lineHeight - (alignOffset + offsetY);
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
        float scaleFactor = inlineScaleFactor();
        formulaDisplayH = scaleSourceMetricCeil(sourceHeightPx, scaleFactor);
        formulaDisplayW = scaleSourceMetricCeil(sourceWidthPx, scaleFactor);

        int alignOffset = switch (valign) {
            case CENTER -> (lineHeight - formulaDisplayH) / 2;
            case BOTTOM -> lineHeight - formulaDisplayH;
            case BASELINE -> {
                // Align the formula's math baseline with the text baseline.
                //
                // The anchor depth is the icon bottom minus the math baseline:
                // the true content depth PLUS the 2px bottom inset, both scaled
                // to display pixels by the actual texture scale (displayH /
                // sourceHeightPx). The texture is the full icon (insets
                // included) drawn uniformly into the displayH-tall box, so the
                // math baseline sits exactly that far above the box bottom:
                //
                // alignOffset = lineHeight - displayH + depthDisplay
                //
                // The depth is derived from the exact TeXIcon#getBaseLine()
                // ratio (baseline fraction of the icon height), so it is
                // rounded exactly once at display resolution — the old path
                // ceil'd the source depth then rounded again after scaling,
                // introducing ≤1-2px anchor drift.
                //
                // For depth-zero formulas this is identical to BOTTOM.
                int depthDisplay = scaleSourceDepthFromBaseline(sourceBaseLineRatio, formulaDisplayH);
                yield lineHeight - formulaDisplayH + depthDisplay;
            }
            default -> 0; // TOP
        };
        int desiredRenderYOffset = alignOffset + offsetY;
        int topInset = Math.max(0, -desiredRenderYOffset);
        int bottomInset = Math.max(0, desiredRenderYOffset);
        renderYOffset = desiredRenderYOffset + topInset;
        // Ascent above the text baseline, in the same units the Rust inline
        // post-pass anchors with: for BASELINE this is the box-top-to-math-
        // baseline distance (displayH - depthDisplay, depthDisplay including
        // the scaled bottom inset), so the texture's math baseline lands on the
        // text baseline; the algebra also covers TOP/CENTER/BOTTOM via their
        // align offsets.
        baselineAscent = lineHeight - desiredRenderYOffset;

        return new LytRect(x, y - topInset, formulaDisplayW, topInset + formulaDisplayH + bottomInset);
    }

    private boolean resolveSourceMetrics() {
        if (sourceMetricsResolved) {
            return sourceWidthPx > 0 && sourceHeightPx > 0;
        }
        sourceMetricsResolved = true;
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale, style);
        if (size == null) {
            return false;
        }
        sourceWidthPx = size[0];
        sourceHeightPx = size[1];
        sourceRefHeightPx = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);
        sourceBaseLineRatio = GuideLatexRenderer.INSTANCE
            .measureBaselineRatio(formula, fillColorArgb, sourceScale, style);
        return sourceWidthPx > 0 && sourceHeightPx > 0;
    }

    /**
     * Body size the inline formula calibrates against: a source "x" must
     * display at the surrounding text's x-height, not at the full line height
     * (the previous lineHeight target made inline formulas ≈ lineHeight /
     * x-height ≈ 1.43× larger than body text) and not at the font ascent
     * ({@link GuideText#ascent()} is the ascent ≈0.75-0.85em, which still
     * leaves formula letters ≈1.4-1.6× larger than the ≈0.5em x-height).
     *
     * <p>
     * The inline flow carries no style context (the line height is queried
     * with a {@code null} style, i.e. font scale 1), so the target is
     * {@link GuideText#xHeight()} at the base font scale.
     *
     * <p>
     * The exact x-height target is then multiplied by
     * {@link #INLINE_PERCEPTUAL_FACTOR} (×1.2): matching the raw x-height was
     * still perceptually too small next to CJK body text, because TeX math
     * glyphs follow their own design conventions (a lower x-height relative to
     * the em than most body fonts), so formula body letters need a small
     * upward nudge to read as the same visual weight as the surrounding text
     * (user report: "公式字体过小").
     */
    private float inlineCalibrationTarget() {
        return GuideText.xHeight() * INLINE_PERCEPTUAL_FACTOR;
    }

    /**
     * Display pixels per source-content pixel for this block: the calibration
     * "x" content height (sourceRefHeightPx minus the true 2px/side icon
     * insets applied by {@code GuideLatexRenderer#setInsets(new Insets(2,2,2,2), true)})
     * maps to the body x-height × {@link #INLINE_PERCEPTUAL_FACTOR} × userScale.
     * Keeping the insets out of the ratio avoids the fixed +4px padding
     * distorting the scale once the target is the smaller x-height — with the
     * old full-height ratio a simple inline formula's box matched the line
     * height instead of the x-height (≈ lineHeight / x-height ≈ 1.43× too
     * large). Note the two-arg inset call is mandatory: the single-arg
     * {@code setInsets(Insets)} adds a phantom {@code (int)(0.18f*size)} per
     * side, which made this subtraction undershoot the real content by 36px
     * and shrank inline formulas to ≈0.67× the body x-height.
     */
    public float inlineScaleFactor() {
        float contentRefHeight = Math.max(1f, sourceRefHeightPx - LATEX_INSET_PX);
        return inlineCalibrationTarget() * userScale / contentRefHeight;
    }

    /**
     * Scales an icon metric (width/height, insets included) to display pixels.
     * The whole source icon (glyph content + the symmetric 2px/side insets of
     * {@code setInsets(new Insets(2,2,2,2), true)}) is scaled in a single
     * ceil: the texture is blitted with full UV coverage into a
     * {@code displayW}×{@code displayH} box, so the display box is exactly
     * the uniform scale of the source icon. One ceil over the whole icon
     * replaces the old double rounding (content ceil + separately rounded
     * scaled inset) and can never clip the glyph.
     */
    public int scaleSourceMetricCeil(int sourceMetric, float scaleFactor) {
        return Math.max(1, (int) Math.ceil(sourceMetric * scaleFactor));
    }

    /**
     * Display distance from the formula's math baseline to the icon bottom,
     * derived directly from the exact jlatexmath baseline ratio instead of a
     * source-pixel depth. {@link org.scilab.forge.jlatexmath.TeXIcon#getBaseLine()} returns the distance
     * from the icon's top edge to its math baseline as a fraction of the
     * icon's total height (the true 2px/side insets of the two-arg
     * {@code setInsets(insets, true)} included), so the math baseline sits
     * {@code formulaDisplayH × (1 - ratio)} above the bottom of the uniformly
     * scaled display box. This is rounded exactly once, at display resolution
     * — the old path computed
     * {@code round(displayH × (ceil(getTrueIconDepth()) + 2) / sourceHeightPx)},
     * which ceil'd the source depth first and then rounded the scaled result,
     * a double rounding that drifted the anchor by ≤1-2px. It also replaces
     * the old depth-plus-bottom-inset bookkeeping: the insets are already part
     * of the icon height the ratio is measured against.
     */
    private int scaleSourceDepthFromBaseline(float baseLineRatio, int formulaDisplayH) {
        return Math.max(0, Math.round(formulaDisplayH * (1f - baseLineRatio)));
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

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale, style);
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

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale, style);
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
