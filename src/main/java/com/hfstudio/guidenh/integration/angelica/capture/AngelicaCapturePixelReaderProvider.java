package com.hfstudio.guidenh.integration.angelica.capture;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReader;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;

public class AngelicaCapturePixelReaderProvider implements CapturePixelReaderProvider {

    @Override
    public String id() {
        return "angelica";
    }

    @Override
    public CaptureProviderAvailability availability(CaptureContext context) {
        return BackendManager.RENDER_BACKEND.hasContextOnThread() ? CaptureProviderAvailability.ready()
            : CaptureProviderAvailability.unavailable("The Angelica backend has no rendering context on this thread");
    }

    @Override
    public CapturePixelReader open(CaptureContext context) {
        return new AngelicaCapturePixelReader(BackendManager.RENDER_BACKEND);
    }

    @Override
    public Object contextKey(CaptureContext context) {
        return AngelicaCaptureBackendContext.key();
    }
}
