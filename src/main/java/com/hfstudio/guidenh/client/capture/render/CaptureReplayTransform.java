package com.hfstudio.guidenh.client.capture.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureReplayTransform {

    private final CaptureViewport viewport;
    private final float[] base = new float[16];
    private final float[] mapping = new float[16];
    private final float[] product = new float[16];
    private final float[] output = new float[16];
    private final FloatBuffer scratch = BufferUtils.createFloatBuffer(16);
    private final IntBuffer pixels = BufferUtils.createIntBuffer(16);

    public CaptureReplayTransform(CaptureViewport viewport) {
        this.viewport = viewport;
        float[] projection = read(GL11.GL_PROJECTION_MATRIX);
        float[] model = read(GL11.GL_MODELVIEW_MATRIX);
        multiply(projection, model, base);
        GL11.glGetInteger(GL11.GL_VIEWPORT, pixels);
    }

    private float[] read(int parameter) {
        scratch.clear();
        GL11.glGetFloat(parameter, scratch);
        float[] values = new float[16];
        scratch.get(values);
        return values;
    }

    public void load(int mode, float[] values) {
        GL11.glMatrixMode(mode);
        GL11.glLoadMatrix(values(values));
    }

    public FloatBuffer values(float[] data) {
        scratch.clear();
        scratch.put(data);
        while (scratch.position() < 4) scratch.put(0);
        scratch.flip();
        return scratch;
    }

    public void projection(float[] original, int[] originalViewport) {
        for (int index = 0; index < 16; index++) mapping[index] = 0;
        mapping[0] = (float) (originalViewport[2] / (2 * viewport.scaleX()));
        mapping[5] = (float) (-originalViewport[3] / (2 * viewport.scaleY()));
        mapping[10] = -1000;
        mapping[12] = (float) ((originalViewport[0] + originalViewport[2] * 0.5) / viewport.scaleX());
        mapping[13] = (float) ((viewport.pixelHeight() - originalViewport[1] - originalViewport[3] * 0.5)
            / viewport.scaleY());
        mapping[15] = 1;
        multiply(base, mapping, product);
        multiply(product, original, output);
        load(GL11.GL_PROJECTION, output);
    }

    public void scissor(int[] rectangle) {
        double left = rectangle[0] / viewport.scaleX();
        double right = (rectangle[0] + rectangle[2]) / viewport.scaleX();
        double top = (viewport.pixelHeight() - rectangle[1] - rectangle[3]) / viewport.scaleY();
        double bottom = (viewport.pixelHeight() - rectangle[1]) / viewport.scaleY();
        double x0 = base[0] * left + base[12];
        double x1 = base[0] * right + base[12];
        double y0 = base[5] * bottom + base[13];
        double y1 = base[5] * top + base[13];
        int x = Math.max(pixels.get(0), CapturePixelEdges.floor(pixels.get(0) + (x0 + 1) * pixels.get(2) * 0.5));
        int y = Math.max(pixels.get(1), CapturePixelEdges.floor(pixels.get(1) + (y0 + 1) * pixels.get(3) * 0.5));
        int farX = Math
            .min(pixels.get(0) + pixels.get(2), CapturePixelEdges.ceil(pixels.get(0) + (x1 + 1) * pixels.get(2) * 0.5));
        int farY = Math
            .min(pixels.get(1) + pixels.get(3), CapturePixelEdges.ceil(pixels.get(1) + (y1 + 1) * pixels.get(3) * 0.5));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, Math.max(0, farX - x), Math.max(0, farY - y));
    }

    private static void multiply(float[] left, float[] right, float[] output) {
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int index = 0; index < 4; index++) sum += left[index * 4 + row] * right[column * 4 + index];
                output[column * 4 + row] = sum;
            }
        }
    }
}
