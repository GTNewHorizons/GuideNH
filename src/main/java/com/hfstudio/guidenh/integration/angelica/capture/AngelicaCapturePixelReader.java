package com.hfstudio.guidenh.integration.angelica.capture;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL12;

import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import com.hfstudio.guidenh.client.capture.api.CaptureAlphaMode;
import com.hfstudio.guidenh.client.capture.api.CaptureReadback;
import com.hfstudio.guidenh.client.capture.render.CaptureBufferPixelReader;
import com.hfstudio.guidenh.client.capture.render.CaptureGlReadbackQueue;

public class AngelicaCapturePixelReader extends CaptureBufferPixelReader {

    private final RenderBackend backend;
    private CaptureGlReadbackQueue readbacks;

    public AngelicaCapturePixelReader(RenderBackend backend) {
        this.backend = backend;
    }

    @Override
    public CaptureReadback enqueue(int x, int y, int width, int height, long timestamp, CaptureAlphaMode alphaMode,
        long memoryBudget) {
        if (!backend.hasContextOnThread()) throw new IllegalStateException("The rendering backend has no context");
        if (backend.isSDLGPU() || !CaptureGlReadbackQueue.supported())
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
        if (!backend.hasContextOnThread())
            throw new IllegalStateException("The rendering backend has no context on this thread");
        try (AngelicaCaptureReadbackScope scope = new AngelicaCaptureReadbackScope(backend)) {
            backend.readPixels(x, y, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
        }
    }
}
