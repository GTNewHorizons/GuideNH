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
import com.hfstudio.guidenh.guide.style.token.DimensionValue;
import com.hfstudio.guidenh.guide.style.token.GuideThemeManager;
import com.hfstudio.guidenh.guide.style.token.TokenKey;
import com.hfstudio.guidenh.guide.style.token.TokenType;

import lombok.Getter;

/**
 * Block-level (display) LaTeX element. Occupies the full available width and centers the formula
 * horizontally, with a small vertical margin above and below.
 *
 * <p>
 * {@code offsetX} and {@code offsetY} are pixel offsets applied on top of the default centered position.
 */
public class LytLatexDisplayBlock extends LytBlock implements InteractiveElement {

    /** Theme token: vertical margin above and below a display formula. */
    private static final TokenKey<DimensionValue> VERTICAL_MARGIN = TokenKey
        .define("--lyt-latex-display-vertical-margin", TokenType.DIMENSION, DimensionValue.px(4));

    private static int verticalMargin() {
        return GuideThemeManager.instance()
            .active()
            .dim(VERTICAL_MARGIN)
            .pxInt();
    }

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
    private final int offsetX;
    @Getter
    private final int offsetY;

    /** Cached formula display width (pixels in GUI units), set during layout. */
    private int formulaDisplayW;
    /** Cached formula display height (pixels in GUI units), set during layout. */
    private int formulaDisplayH;
    /** True when lazy computation has been attempted (even if result is 0). */
    private boolean formulaDisplayComputed;

    public LytLatexDisplayBlock(String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, int offsetX, int offsetY) {
        this(
            formula,
            new LatexRenderOptions(
                TeXConstants.STYLE_DISPLAY,
                fillColorArgb,
                sourceScale,
                userScale,
                tooltip,
                LatexVerticalAlign.BASELINE,
                offsetX,
                offsetY));
    }

    public LytLatexDisplayBlock(String formula, LatexRenderOptions options) {
        this.formula = formula;
        this.fillColorArgb = options.fillColorArgb();
        this.sourceScale = options.sourceScale();
        this.userScale = options.userScale();
        this.style = options.style();
        this.tooltip = options.tooltip();
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

    /**
     * Display pixels per source-content pixel for this block, unified with the
     * inline calibration standard (see {@link LytLatexBlock#inlineScaleFactor()}):
     * the calibration "x" content height (refH minus the true 2px/side icon
     * insets applied by {@code GuideLatexRenderer#setInsets(new Insets(2,2,2,2), true)})
     * maps to the body x-height × {@code INLINE_PERCEPTUAL_FACTOR} × userScale.
     * Display and inline formulas therefore share one calibration target (body
     * x-height × 1.2), matching the mature convention (MathJax/KaTeX/LaTeX) where
     * display and inline math render at the same size and only the internal
     * layout differs (handled by the jlatexmath style, not here).
     */
    private float displayScaleFactor() {
        int refH = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);
        float contentRefHeight = Math.max(1f, refH - LytLatexBlock.LATEX_INSET_PX);
        return GuideText.xHeight() * LytLatexBlock.INLINE_PERCEPTUAL_FACTOR * userScale / contentRefHeight;
    }

    /** Lazy-compute formula display dimensions using static font metrics. */
    private void computeFormulaDisplay() {
        formulaDisplayComputed = true;
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale, style);
        if (size == null) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            return;
        }
        float scaleFactor = displayScaleFactor();
        formulaDisplayH = Math.max(1, (int) Math.ceil(size[1] * scaleFactor));
        formulaDisplayW = Math.max(1, (int) Math.ceil(size[0] * scaleFactor));
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        formulaDisplayComputed = true;
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale, style);
        if (size == null) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            return new LytRect(x, y, availableWidth, 0);
        }

        float scaleFactor = displayScaleFactor();
        formulaDisplayH = Math.max(1, (int) Math.ceil(size[1] * scaleFactor));
        formulaDisplayW = Math.max(1, (int) Math.ceil(size[0] * scaleFactor));

        return new LytRect(x, y, availableWidth, formulaDisplayH + 2 * verticalMargin());
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

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale, style);
        if (tex == null) {
            return;
        }

        int centeredX = bounds.x() + (bounds.width() - formulaDisplayW) / 2;
        int formulaY = bounds.y() + verticalMargin();
        c.emit(
            new GuideRenderPrimitive.BlitTexture(
                tex[0],
                centeredX + offsetX,
                formulaY + offsetY,
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

        int centeredX = bounds.x() + (bounds.width() - formulaDisplayW) / 2;
        int formulaY = bounds.y() + verticalMargin();
        GuideLatexRenderer.INSTANCE
            .renderLatex(centeredX + offsetX, formulaY + offsetY, formulaDisplayW, formulaDisplayH, tex[0]);
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
        if (bounds == null || bounds.isEmpty() || formulaDisplayW <= 0 || formulaDisplayH <= 0) {
            return bounds != null ? bounds : LytRect.empty();
        }
        int centeredX = bounds.x() + (bounds.width() - formulaDisplayW) / 2;
        int formulaY = bounds.y() + verticalMargin();
        return new LytRect(centeredX + offsetX, formulaY + offsetY, formulaDisplayW, formulaDisplayH);
    }

    @Nullable
    @Override
    public LytRect getBounds() {
        return bounds;
    }
}
