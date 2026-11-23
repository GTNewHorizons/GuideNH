package com.hfstudio.guidenh.integration.angelica.capture;

import java.nio.IntBuffer;

import net.coderbot.iris.shadows.ShadowRenderingState;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderContextKey;
import com.hfstudio.guidenh.client.capture.render.GlCaptureRenderProbe;

public class AngelicaCaptureRenderProbe extends GlCaptureRenderProbe {

    @Override
    protected String backendId() {
        return BackendManager.RENDER_BACKEND.getName();
    }

    @Override
    protected boolean openGl() {
        return backendId().startsWith("OpenGL");
    }

    @Override
    protected Object contextKey() {
        return new CaptureRenderContextKey(BackendManager.RENDER_BACKEND, openGl() ? super.contextKey() : null);
    }

    @Override
    protected int integer(int parameter) {
        return BackendManager.RENDER_BACKEND.getInteger(parameter);
    }

    @Override
    protected void integers(int parameter, IntBuffer target) {
        BackendManager.RENDER_BACKEND.getInteger(parameter, target);
    }

    @Override
    public boolean shadowPass() {
        return ShadowRenderingState.areShadowsCurrentlyBeingRendered();
    }
}
