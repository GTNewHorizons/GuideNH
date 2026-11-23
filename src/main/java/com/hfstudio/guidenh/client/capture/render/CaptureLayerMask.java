package com.hfstudio.guidenh.client.capture.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public class CaptureLayerMask {

    public static void clear(List<CaptureRect> rectangles) {
        if (rectangles.isEmpty()) return;
        FloatBuffer model = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer point = BufferUtils.createFloatBuffer(3);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(true, true, true, true);
            GL11.glClearColor(0, 0, 0, 0);
            for (CaptureRect rectangle : rectangles) {
                double left = Double.POSITIVE_INFINITY;
                double bottom = Double.POSITIVE_INFINITY;
                double right = Double.NEGATIVE_INFINITY;
                double top = Double.NEGATIVE_INFINITY;
                for (int corner = 0; corner < 4; corner++) {
                    float x = (float) ((corner & 1) == 0 ? rectangle.x() : rectangle.right());
                    float y = (float) ((corner & 2) == 0 ? rectangle.y() : rectangle.bottom());
                    if (!GLU.gluProject(x, y, 0, model, projection, viewport, point)) continue;
                    left = Math.min(left, point.get(0));
                    right = Math.max(right, point.get(0));
                    bottom = Math.min(bottom, point.get(1));
                    top = Math.max(top, point.get(1));
                }
                if (!Double.isFinite(left)) continue;
                int x = Math.max(viewport.get(0), (int) Math.floor(left));
                int y = Math.max(viewport.get(1), (int) Math.floor(bottom));
                int w = Math.max(0, Math.min(viewport.get(0) + viewport.get(2), (int) Math.ceil(right)) - x);
                int h = Math.max(0, Math.min(viewport.get(1) + viewport.get(3), (int) Math.ceil(top)) - y);
                GL11.glScissor(x, y, w, h);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            }
        }
    }
}
