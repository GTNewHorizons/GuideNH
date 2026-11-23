package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderContextKey;
import com.hfstudio.guidenh.client.capture.api.CaptureSource;
import com.hfstudio.guidenh.client.capture.api.CaptureSourceProvider;

public class GuiLayerCaptureSourceProvider implements CaptureSourceProvider {

    @Override
    public String id() {
        return "gui_layer";
    }

    @Override
    public CaptureProviderAvailability availability(CaptureContext context, CaptureContentOptions content) {
        return OpenGlHelper.isFramebufferEnabled() ? CaptureProviderAvailability.ready()
            : CaptureProviderAvailability.unavailable("The GUI layer source requires framebuffer support");
    }

    @Override
    public CaptureSource open(CaptureContext context) {
        return new GuiLayerCaptureSource();
    }

    @Override
    public boolean automaticSelection(CaptureContext context, CaptureContentOptions content) {
        return !content.includeWorld();
    }

    @Override
    public Object contextKey(CaptureContext context) {
        return new CaptureRenderContextKey(context.screen(), GLContext.getCapabilities());
    }
}
