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
     * Legacy padding difference that the pre-fix insets injected into every icon.
     * Before the inset fix, every TeXIcon was built with the single-arg
     * {@code setInsets(Insets)}, which delegates to {@code setInsets(insets, false)}
     * and silently adds {@code (int)(0.18f*size)} to every side — at the default
     * sourceScale=100 that made the intended 2px/side insets actually 20px/side
     * (40px total per dimension). Every display formula icon AND the calibration
     * "x" carried that padding, and the display page's block sizes were accepted
     * and baselined against it.
     *
     * <p>Now {@code GuideLatexRenderer} builds every icon with the two-arg
     * {@code setInsets(insets, true)} (true 2px/side), so every icon shrank by this
     * padding difference in both dimensions. To keep the display page's visual size
     * identical to the accepted baseline, the difference is re-added to BOTH the
     * measured formula size and the calibrated reference:
     *
     * <pre>
     *   displayH = (size + pad) * lineHeight * userScale / (refH + pad)
     *           = (Fc + 40)     * lineHeight * userScale / (Xc + 40)     // pre-fix ratio, exact
     * </pre>
     *
     * where {@code Fc}/{@code Xc} are the true glyph content heights. This is an
     * exact identity for any formula size (the pad cancels out of the ratio only
     * when applied to both terms), so the display blocks keep their baselined
     * dimensions while the textures inside — now with only 2px padding — carry the
     * real glyph pixels.
     *
     * @return per-dimension padding (px) the inset fix removed, e.g. 36 at sourceScale=100
     */
    private int legacyPaddingDiff() {
        int oldInsetSide = 2 + (int) (0.18f * sourceScale); // single-arg setInsets(Insets) behavior
        int newInsetSide = 2; // setInsets(insets, true)
        return 2 * (oldInsetSide - newInsetSide);
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
        int lineHeight = GuideText.lineHeight(null);
        int refH = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);
        // See legacyPaddingDiff(): the pre-fix ratio that the display baseline was
        // accepted against had +40px padding on both size and refH (single-arg
        // setInsets); re-add the removed padding to both so the boxes stay identical.
        int pad = legacyPaddingDiff();
        formulaDisplayH = (int) Math.max(1, Math.ceil((double) (size[1] + pad) * lineHeight * userScale / (refH + pad)));
        formulaDisplayW = (int) Math.max(1, Math.ceil((double) (size[0] + pad) * lineHeight * userScale / (refH + pad)));
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

        int lineHeight = context.getLineHeight(null);
        int refH = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);
        // See legacyPaddingDiff(): re-add the padding the inset fix removed so the
        // display boxes stay at the accepted baseline sizes.
        int pad = legacyPaddingDiff();
        formulaDisplayH = (int) Math.max(1, Math.ceil((double) (size[1] + pad) * lineHeight * userScale / (refH + pad)));
        formulaDisplayW = (int) Math.max(1, Math.ceil((double) (size[0] + pad) * lineHeight * userScale / (refH + pad)));

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
