package com.hfstudio.guidenh.integration.angelica.capture;

import org.lwjgl.opengl.GLContext;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.gtnewhorizons.angelica.glsm.backend.RenderBackend;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderContextKey;

public class AngelicaCaptureBackendContext {

    private AngelicaCaptureBackendContext() {}

    public static Object key() {
        RenderBackend backend = BackendManager.RENDER_BACKEND;
        return new CaptureRenderContextKey(backend, backend.isSDLGPU() ? null : GLContext.getCapabilities());
    }
}
