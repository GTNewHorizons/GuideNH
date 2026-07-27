package com.hfstudio.guidenh.guide.document.block;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.latex.GuideLatexRenderer;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
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
    @Nullable
    private final GuideTooltip tooltip;
    @Getter
    private final int offsetX;
    @Getter
    private final int offsetY;

    /** Cached formula display width (pixels in GUI units), set during layout. */
    @Getter
    private int formulaDisplayW;
    /** Cached formula display height (pixels in GUI units), set during layout. */
    @Getter
    private int formulaDisplayH;

    public LytLatexDisplayBlock(String formula, int fillColorArgb, float sourceScale, float userScale,
        @Nullable GuideTooltip tooltip, int offsetX, int offsetY) {
        this(
            formula,
            new LatexRenderOptions(
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
        this.tooltip = options.tooltip();
        this.offsetX = options.offsetX();
        this.offsetY = options.offsetY();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int[] size = GuideLatexRenderer.INSTANCE.measureSize(formula, fillColorArgb, sourceScale);
        if (size == null) {
            formulaDisplayW = 0;
            formulaDisplayH = 0;
            return new LytRect(x, y, availableWidth, 0);
        }

        int lineHeight = context.getLineHeight(null);
        int refH = GuideLatexRenderer.INSTANCE.calibrateRefHeight(sourceScale);

        formulaDisplayH = (int) Math.max(1, Math.ceil((double) size[1] * lineHeight * userScale / refH));
        formulaDisplayW = (int) Math.max(1, Math.ceil((double) size[0] * lineHeight * userScale / refH));

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

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale);
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

        int[] tex = GuideLatexRenderer.INSTANCE.getOrCreateTexture(formula, fillColorArgb, sourceScale);
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
