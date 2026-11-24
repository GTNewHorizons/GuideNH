package com.hfstudio.guidenh.integration.angelica.capture;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.render.GlCapturePixelReaderProvider;

public class AngelicaOpenGlPixelReaderProvider extends GlCapturePixelReaderProvider {

    @Override
    public CaptureProviderAvailability availability(CaptureContext context) {
        if (BackendManager.RENDER_BACKEND.isSDLGPU()) return CaptureProviderAvailability
            .unavailable("The OpenGL reader requires the OpenGL backend; select the Angelica reader");
        return super.availability(context);
    }
}
