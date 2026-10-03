package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;
import org.scilab.forge.jlatexmath.TeXConstants;

import com.hfstudio.guidenh.guide.color.ColorUtils;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.render.GuideText;

/**
 * Render options for a LaTeX block (inline or display).
 *
 * <p>
 * <b>Typeset-at-target-size contract.</b> Formulas are typeset directly at
 * {@link #fontSize()} = {@link GuideText#BASE_FONT_SIZE} × {@code userScale}
 * pixels (jlatexmath {@code setSize} is a pixel font size). The {@code scale}
 * attribute of the {@code <Latex>} tag maps to {@code userScale}; the
 * {@code sourceScale} attribute is DEPRECATED; it is parsed for backward
 * compatibility with old documents but ignored by every computation.
 */
public record LatexRenderOptions(int style, int fillColorArgb, float sourceScale, float userScale,
    @Nullable GuideTooltip tooltip, LatexVerticalAlign valign, int offsetX, int offsetY) {

    public static final int DEFAULT_FILL_COLOR_ARGB = ColorUtils.WHITE.getColor();

    /**
     * DEPRECATED: legacy jlatexmath render size (e.g. 100.0). No longer part of
     * the render pipeline; formulas are typeset at {@link #fontSize()} directly.
     * Retained so old documents that set {@code sourceScale} still parse; the
     * value is ignored by all computation. The getter also feeds a passive
     * flatbuffer passthrough field consumed by nothing (see LayoutNodeSerializer /
     * Rust measure_latex).
     */
    public static final float DEFAULT_SOURCE_SCALE = 100.0f;
    public static final float DEFAULT_USER_SCALE = 1.0f;

    public static Builder builder() {
        return new Builder();
    }

    public LatexRenderOptions {
        sourceScale = Math.max(16f, sourceScale);
        userScale = Math.max(0.1f, userScale);
        if (valign == null) {
            valign = LatexVerticalAlign.BASELINE;
        }
        if (style != TeXConstants.STYLE_DISPLAY && style != TeXConstants.STYLE_TEXT) {
            style = TeXConstants.STYLE_DISPLAY;
        }
    }

    /**
     * Target typeset font size in pixels: the body base font size ({@link GuideText#BASE_FONT_SIZE})
     * × {@link #userScale()}. Passed straight to jlatexmath {@code setSize} so the formula is
     * typeset at its final pixel size, with no scaling and no calibration.
     *
     * @return font size in pixels, e.g. {@code 11f × 1.5f = 16.5f}
     */
    public float fontSize() {
        return GuideText.BASE_FONT_SIZE * userScale;
    }

    public static final class Builder {

        private int style = TeXConstants.STYLE_DISPLAY;
        private int fillColorArgb = DEFAULT_FILL_COLOR_ARGB;
        private float sourceScale = DEFAULT_SOURCE_SCALE;
        private float userScale = DEFAULT_USER_SCALE;
        @Nullable
        private GuideTooltip tooltip;
        private LatexVerticalAlign valign = LatexVerticalAlign.BASELINE;
        private int offsetX;
        private int offsetY;

        private Builder() {}

        public Builder style(int style) {
            this.style = style;
            return this;
        }

        public Builder fillColorArgb(int fillColorArgb) {
            this.fillColorArgb = fillColorArgb;
            return this;
        }

        /**
         * DEPRECATED: legacy attribute, parsed for backward compatibility and ignored.
         * Use {@link #userScale(float)} instead; the render size is now derived from
         * {@link GuideText#BASE_FONT_SIZE} × user scale.
         */
        public Builder sourceScale(float sourceScale) {
            this.sourceScale = sourceScale;
            return this;
        }

        public Builder userScale(float userScale) {
            this.userScale = userScale;
            return this;
        }

        public Builder tooltip(@Nullable GuideTooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder valign(LatexVerticalAlign valign) {
            this.valign = valign;
            return this;
        }

        public Builder offset(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            return this;
        }

        public LatexRenderOptions build() {
            return new LatexRenderOptions(
                style,
                fillColorArgb,
                sourceScale,
                userScale,
                tooltip,
                valign,
                offsetX,
                offsetY);
        }
    }
}
