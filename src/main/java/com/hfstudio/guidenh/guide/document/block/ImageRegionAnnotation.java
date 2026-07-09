package com.hfstudio.guidenh.guide.document.block;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.sound.GuideSoundSpec;
import com.hfstudio.guidenh.guide.sound.GuideSoundTrigger;

import lombok.Getter;

/**
 * An annotation attached to a rectangular region of an image (in image-pixel coordinates).
 * When the mouse hovers over the region, a rich-text tooltip is shown.
 * Optionally renders a colored border around the region.
 */
public class ImageRegionAnnotation {

    @Getter
    private final int imgX;
    @Getter
    private final int imgY;
    @Getter
    private final int imgW;
    @Getter
    private final int imgH;
    @Getter
    private final boolean wholeImage;
    @Getter
    private final boolean showBorder;
    @Getter
    private final ColorValue borderColor;
    @Getter
    private final int borderThickness;

    @Nullable
    private GuideTooltip tooltip;
    @Nullable
    private GuideSoundSpec sound;
    @Getter
    private GuideSoundTrigger soundTrigger = GuideSoundTrigger.CLICK;

    /**
     * Annotation covering a specific rectangular region of the image.
     *
     * @param imgX            left edge in image pixels
     * @param imgY            top edge in image pixels
     * @param imgW            width in image pixels (clamped to at least 1)
     * @param imgH            height in image pixels (clamped to at least 1)
     * @param showBorder      whether to draw a colored border around the region
     * @param borderColor     border color value
     * @param borderThickness border line thickness in display pixels (clamped to at least 1)
     */
    public ImageRegionAnnotation(int imgX, int imgY, int imgW, int imgH, boolean showBorder, ColorValue borderColor,
        int borderThickness) {
        this.imgX = imgX;
        this.imgY = imgY;
        this.imgW = Math.max(1, imgW);
        this.imgH = Math.max(1, imgH);
        this.wholeImage = false;
        this.showBorder = showBorder;
        this.borderColor = borderColor;
        this.borderThickness = Math.max(1, borderThickness);
    }

    /**
     * Annotation covering the entire image.
     *
     * @param showBorder      whether to draw a border around the whole image
     * @param borderColor     border color value
     * @param borderThickness border line thickness in display pixels (clamped to at least 1)
     */
    public ImageRegionAnnotation(boolean showBorder, ColorValue borderColor, int borderThickness) {
        this.imgX = 0;
        this.imgY = 0;
        this.imgW = 0;
        this.imgH = 0;
        this.wholeImage = true;
        this.showBorder = showBorder;
        this.borderColor = borderColor;
        this.borderThickness = Math.max(1, borderThickness);
    }

    @Nullable
    public GuideTooltip getTooltip() {
        return tooltip;
    }

    public void setTooltip(@Nullable GuideTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Nullable
    public GuideSoundSpec getSound() {
        return sound;
    }

    public void setSound(@Nullable GuideSoundSpec sound) {
        this.sound = sound;
    }

    public void setSoundTrigger(GuideSoundTrigger soundTrigger) {
        this.soundTrigger = soundTrigger != null ? soundTrigger : GuideSoundTrigger.CLICK;
    }

    /**
     * Returns true if the given image-space point falls within this annotation's region.
     * Always returns true for whole-image annotations.
     *
     * @param imgPx x coordinate in image pixels (0 = left edge)
     * @param imgPy y coordinate in image pixels (0 = top edge)
     */
    public boolean containsImagePoint(float imgPx, float imgPy) {
        if (wholeImage) {
            return true;
        }
        return imgPx >= imgX && imgPx < imgX + imgW && imgPy >= imgY && imgPy < imgY + imgH;
    }
}
