package com.hfstudio.guidenh.guide.scene.render;

import java.nio.IntBuffer;

import org.joml.Vector3fc;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.hfstudio.guidenh.guide.scene.annotation.AnnotationColorStyle;

/**
 * Owns the reusable shader for solid and animated box colors.
 */
public class AnnotationColorShader implements AutoCloseable {

    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private SceneShaderProgram program;

    public int begin(AnnotationColorStyle style, float highlightOffset, float occlusionOffset) {
        return begin(style, highlightOffset, occlusionOffset, 0f, 0f, 0f);
    }

    public int begin(AnnotationColorStyle style, float highlightOffset, float occlusionOffset, float centerX,
        float centerY, float centerZ) {
        return begin(style, highlightOffset, occlusionOffset, centerX, centerY, centerZ, 0xFFFFFFFF);
    }

    private int begin(AnnotationColorStyle style, float highlightOffset, float occlusionOffset, float centerX,
        float centerY, float centerZ, int argb) {
        int previous = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        if (program == null) {
            program = SceneShaderProgram.load("annotation/box.vsh", "annotation/box.fsh");
        }
        program.use();
        if (style.isRainbow()) {
            viewport.clear();
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            program.setVec4("uViewport", viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            program.setFloat("uPhase", style.phase(System.currentTimeMillis()));
        }
        program.setVec2("uColorOffsets", highlightOffset, occlusionOffset);
        program.setVec4("uColorCenter", centerX, centerY, centerZ, 1f);
        program.setInt("uRainbow", style.isRainbow() ? 1 : 0);
        program.setInt("uFilterBoxSurface", 0);
        program.setVec4(
            "uBoxColor",
            ((argb >>> 16) & 255) / 255f,
            ((argb >>> 8) & 255) / 255f,
            (argb & 255) / 255f,
            (argb >>> 24) / 255f);
        return previous;
    }

    public int beginBox(AnnotationColorStyle style, Vector3fc min, Vector3fc max, float halfThickness,
        float highlightOffset, float occlusionOffset, boolean singleSurface) {
        return beginBox(style, min, max, halfThickness, highlightOffset, occlusionOffset, singleSurface, 0xFFFFFFFF);
    }

    public int beginBox(AnnotationColorStyle style, Vector3fc min, Vector3fc max, float halfThickness,
        float highlightOffset, float occlusionOffset, boolean singleSurface, int argb) {
        int previous = begin(
            style,
            highlightOffset,
            occlusionOffset,
            (min.x() + max.x()) * 0.5f,
            (min.y() + max.y()) * 0.5f,
            (min.z() + max.z()) * 0.5f,
            argb);
        program.setVec4(
            "uBoxShape",
            (max.x() - min.x()) * 0.5f,
            (max.y() - min.y()) * 0.5f,
            (max.z() - min.z()) * 0.5f,
            halfThickness);
        program.setInt("uFilterBoxSurface", singleSurface ? 1 : 0);
        return previous;
    }

    @Override
    public void close() {
        if (program != null) {
            program.delete();
            program = null;
        }
    }
}
