package com.hfstudio.guidenh.integration.angelica.capture;

import java.nio.IntBuffer;

import org.lwjgl.opengl.GL12;

import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import com.hfstudio.guidenh.client.capture.render.CaptureBufferPixelReader;

public class AngelicaCapturePixelReader extends CaptureBufferPixelReader {

    private final RenderBackend backend;

    public AngelicaCapturePixelReader(RenderBackend backend) {
        this.backend = backend;
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
