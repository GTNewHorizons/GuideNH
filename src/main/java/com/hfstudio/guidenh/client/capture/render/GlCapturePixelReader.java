package com.hfstudio.guidenh.client.capture.render;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;

public class GlCapturePixelReader extends CaptureBufferPixelReader {

    private CaptureGlReadbackQueue readbacks;

    @Override
    public CaptureReadback enqueue(int x, int y, int width, int height, long timestamp, CaptureAlphaMode alphaMode,
        long memoryBudget) {
        if (!CaptureGlReadbackQueue.supported())
            return super.enqueue(x, y, width, height, timestamp, alphaMode, memoryBudget);
        if (readbacks == null) readbacks = new CaptureGlReadbackQueue();
        return readbacks.submit(x, y, width, height, timestamp, alphaMode, memoryBudget);
    }

    @Override
    public void close() {
        if (readbacks != null) readbacks.close();
        readbacks = null;
        super.close();
    }

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
