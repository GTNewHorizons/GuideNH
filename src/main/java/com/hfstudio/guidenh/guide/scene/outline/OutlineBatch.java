package com.hfstudio.guidenh.guide.scene.outline;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.hfstudio.guidenh.guide.scene.annotation.AnnotationColorStyle;

import lombok.Getter;

@Getter
public class OutlineBatch {

    private int visibleArgb;
    private int occludedArgb;
    private float thickness;
    private AnnotationColorStyle colorStyle = AnnotationColorStyle.SOLID;
    private float visibleColorOffset;
    private float occludedColorOffset;
    private final Vector3f colorCenter = new Vector3f();
    private final OutlineTargetSet solid = new OutlineTargetSet();
    private final OutlineTargetSet cutout = new OutlineTargetSet();

    public void reset(int visibleArgb, int occludedArgb, float thickness) {
        reset(visibleArgb, occludedArgb, thickness, AnnotationColorStyle.SOLID, 0f, 0f);
    }

    public void reset(int visibleArgb, int occludedArgb, float thickness, AnnotationColorStyle colorStyle,
        float visibleColorOffset, float occludedColorOffset) {
        reset(visibleArgb, occludedArgb, thickness, colorStyle, visibleColorOffset, occludedColorOffset, null);
    }

    public void reset(int visibleArgb, int occludedArgb, float thickness, AnnotationColorStyle colorStyle,
        float visibleColorOffset, float occludedColorOffset, @Nullable Vector3fc colorCenter) {
        this.visibleArgb = visibleArgb;
        this.occludedArgb = occludedArgb;
        this.thickness = thickness;
        this.colorStyle = colorStyle;
        this.visibleColorOffset = visibleColorOffset;
        this.occludedColorOffset = occludedColorOffset;
        if (colorCenter == null) {
            this.colorCenter.zero();
        } else {
            this.colorCenter.set(colorCenter);
        }
        solid.clear();
        cutout.clear();
    }

    public boolean hasStyle(int visibleArgb, int occludedArgb, float thickness) {
        return hasStyle(visibleArgb, occludedArgb, thickness, AnnotationColorStyle.SOLID, 0f, 0f);
    }

    public boolean hasStyle(int visibleArgb, int occludedArgb, float thickness, AnnotationColorStyle colorStyle,
        float visibleColorOffset, float occludedColorOffset) {
        return hasStyle(
            visibleArgb,
            occludedArgb,
            thickness,
            colorStyle,
            visibleColorOffset,
            occludedColorOffset,
            null);
    }

    public boolean hasStyle(int visibleArgb, int occludedArgb, float thickness, AnnotationColorStyle colorStyle,
        float visibleColorOffset, float occludedColorOffset, @Nullable Vector3fc colorCenter) {
        return this.visibleArgb == visibleArgb && this.occludedArgb == occludedArgb
            && this.thickness == thickness
            && this.colorStyle.equals(colorStyle)
            && this.visibleColorOffset == visibleColorOffset
            && this.occludedColorOffset == occludedColorOffset
            && (!colorStyle.isRainbow()
                || (colorCenter == null ? this.colorCenter.equals(0f, 0f, 0f) : this.colorCenter.equals(colorCenter)));
    }

    public OutlineTargetSet targets(OutlineSilhouette silhouette) {
        return silhouette.usesTextureAlpha() ? cutout : solid;
    }

    public boolean isEmpty() {
        return solid.isEmpty() && cutout.isEmpty();
    }
}
