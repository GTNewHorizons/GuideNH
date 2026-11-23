package com.hfstudio.guidenh.client.capture.render;

import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;

public class GlCapturePixelReaderProvider implements CapturePixelReaderProvider {

    @Override
    public String id() {
        return "opengl";
    }

    @Override
    public CaptureProviderAvailability availability(CaptureContext context) {
        return GLContext.getCapabilities().OpenGL12 ? CaptureProviderAvailability.ready()
            : CaptureProviderAvailability.unavailable("The OpenGL reader requires BGRA pixel readback support");
    }

    @Override
    public CapturePixelReader open(CaptureContext context) {
        return new GlCapturePixelReader();
    }
}
