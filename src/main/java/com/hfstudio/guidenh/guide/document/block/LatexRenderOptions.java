package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;
import org.scilab.forge.jlatexmath.TeXConstants;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;

@Desugar
public record LatexRenderOptions(int style, int fillColorArgb, float sourceScale, float userScale,
    @Nullable GuideTooltip tooltip, LatexVerticalAlign valign, int offsetX, int offsetY) {

    public static final int DEFAULT_FILL_COLOR_ARGB = 0xFFFFFFFF;
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
