package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;

public class GlCapturePixelReader implements CapturePixelReader {

    private IntBuffer scratch;

    @Override
    public CaptureFrame read(int x, int y, int width, int height, long timestampNanos) {
        return read(x, y, width, height, timestampNanos, false, false);
    }

    public CaptureFrame readPremultiplied(int x, int y, int width, int height, long timestampNanos) {
        return read(x, y, width, height, timestampNanos, true, false);
    }

    public CaptureFrame readOpaque(int x, int y, int width, int height, long timestampNanos) {
        return read(x, y, width, height, timestampNanos, false, true);
    }

    private CaptureFrame read(int x, int y, int width, int height, long timestampNanos, boolean premultiplied,
        boolean opaque) {
        int count = Math.multiplyExact(width, height);
        if (scratch == null || scratch.capacity() < count) scratch = BufferUtils.createIntBuffer(count);
        scratch.clear();
        scratch.limit(count);
        int[] pixels = new int[count];
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            if (GLContext.getCapabilities().OpenGL21) GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_FALSE);
            GL11.glReadPixels(x, y, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, scratch);
            for (int row = 0; row < height; row++) {
                scratch.position((height - 1 - row) * width);
                scratch.get(pixels, row * width, width);
            }
        }
        if (opaque) {
            for (int index = 0; index < pixels.length; index++) pixels[index] |= 0xFF000000;
        } else if (premultiplied) {
            for (int index = 0; index < pixels.length; index++) {
                int pixel = pixels[index];
                int alpha = pixel >>> 24;
                if (alpha == 0) pixels[index] = 0;
                else if (alpha < 255) {
                    int red = Math.min(255, ((pixel >>> 16) & 255) * 255 / alpha);
                    int green = Math.min(255, ((pixel >>> 8) & 255) * 255 / alpha);
                    int blue = Math.min(255, (pixel & 255) * 255 / alpha);
                    pixels[index] = alpha << 24 | red << 16 | green << 8 | blue;
                }
            }
        }
        return new CaptureFrame(width, height, timestampNanos, pixels);
    }

    public void release() {
        scratch = null;
    }
}
