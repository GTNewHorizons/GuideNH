package com.hfstudio.guidenh.client.capture.geometry;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureDrawProjection {

    private final FloatBuffer model = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer pixels = BufferUtils.createIntBuffer(16);
    private final FloatBuffer point = BufferUtils.createFloatBuffer(3);
    private final ByteBuffer colorMask = BufferUtils.createByteBuffer(4);

    public CaptureRect project(double left, double top, double near, double right, double bottom, double far,
        CaptureViewport viewport) {
        colorMask.clear();
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, colorMask);
        if (colorMask.get(0) == 0 && colorMask.get(1) == 0 && colorMask.get(2) == 0) return null;
        model.clear();
        projection.clear();
        pixels.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, pixels);
        if (pixels.get(2) <= 0 || pixels.get(3) <= 0) return null;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            point.clear();
            if (!GLU.gluProject(
                (float) ((corner & 1) == 0 ? left : right),
                (float) ((corner & 2) == 0 ? top : bottom),
                (float) ((corner & 4) == 0 ? near : far),
                model,
                projection,
                pixels,
                point)) return null;
            minX = Math.min(minX, point.get(0));
            maxX = Math.max(maxX, point.get(0));
            minY = Math.min(minY, point.get(1));
            maxY = Math.max(maxY, point.get(1));
        }
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(maxX) || !Double.isFinite(maxY))
            return null;
        CaptureRect bounds = CaptureRect
            .between(
                minX / viewport.scaleX(),
                (viewport.pixelHeight() - maxY) / viewport.scaleY(),
                maxX / viewport.scaleX(),
                (viewport.pixelHeight() - minY) / viewport.scaleY())
            .intersect(viewport.bounds());
        if (GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)) {
            pixels.clear();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, pixels);
            bounds = bounds.intersect(
                new CaptureRect(
                    pixels.get(0) / viewport.scaleX(),
                    (viewport.pixelHeight() - pixels.get(1) - pixels.get(3)) / viewport.scaleY(),
                    pixels.get(2) / viewport.scaleX(),
                    pixels.get(3) / viewport.scaleY()));
        }
        return bounds.isEmpty() ? null : bounds;
    }
}
