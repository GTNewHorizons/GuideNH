package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;

public class GlCapturePixelReader extends CaptureBufferPixelReader {

    @Override
    protected void readPixels(int x, int y, int width, int height, IntBuffer pixels) {
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            if (GLContext.getCapabilities().OpenGL21) GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, GL11.GL_FALSE);
            GL11.glReadPixels(x, y, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
        }
    }
}
