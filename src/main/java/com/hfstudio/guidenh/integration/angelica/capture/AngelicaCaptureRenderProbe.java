package com.hfstudio.guidenh.integration.angelica.capture;

import java.nio.IntBuffer;
import java.util.List;
import java.util.Set;

import net.coderbot.iris.shadows.ShadowRenderingState;
import net.minecraft.client.renderer.OpenGlHelper;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.hfstudio.guidenh.client.capture.render.GlCaptureRenderProbe;

public class AngelicaCaptureRenderProbe extends GlCaptureRenderProbe {

    @Override
    protected String backendId() {
        return BackendManager.RENDER_BACKEND.getName();
    }

    @Override
    protected boolean openGl() {
        return !BackendManager.RENDER_BACKEND.isSDLGPU();
    }

    @Override
    protected Object contextKey() {
        return AngelicaCaptureBackendContext.key();
    }

    @Override
    protected void describeCapabilities(Set<String> features, List<String> limitations) {
        if (openGl()) super.describeCapabilities(features, limitations);
        else if (OpenGlHelper.isFramebufferEnabled()) features.add("gui_framebuffer");
        features.add("backend_readback");
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
