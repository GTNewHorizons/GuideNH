package com.hfstudio.guidenh.guide.scene.annotation;

import org.joml.Vector3f;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.scene.render.BoxFrameGeometry;

public class InWorldBoxAnnotation extends InWorldAnnotation {

    public static float DEFAULT_THICKNESS = 1f;

    private final Vector3f min;
    private final Vector3f max;
    private final ColorValue color;
    private final float thickness;
    private final AnnotationColorStyle colorStyle;
    private final BoxFrameGeometry frameGeometry = new BoxFrameGeometry();

    public InWorldBoxAnnotation(Vector3f min, Vector3f max, ColorValue color, float thickness) {
        this(min, max, color, thickness, AnnotationColorStyle.SOLID);
    }

    public InWorldBoxAnnotation(Vector3f min, Vector3f max, ColorValue color, float thickness,
        AnnotationColorStyle colorStyle) {
        this.min = min;
        this.max = max;
        this.color = color;
        this.thickness = thickness;
        this.colorStyle = colorStyle;
    }

    public InWorldBoxAnnotation(Vector3f min, Vector3f max, ColorValue color) {
        this(min, max, color, DEFAULT_THICKNESS);
    }

    public static InWorldBoxAnnotation forBlock(int x, int y, int z, ColorValue color) {
        return new InWorldBoxAnnotation(new Vector3f(x, y, z), new Vector3f(x + 1, y + 1, z + 1), color);
    }

    public Vector3f min() {
        return min;
    }

    public Vector3f max() {
        return max;
    }

    public ColorValue color() {
        return color;
    }

    public float thickness() {
        return thickness;
    }

    public AnnotationColorStyle colorStyle() {
        return colorStyle;
    }

    public BoxFrameGeometry frameGeometry() {
        frameGeometry.update(min, max, Math.max(thickness / 32f, 1f / 256f) * 0.5f);
        return frameGeometry;
    }
}
