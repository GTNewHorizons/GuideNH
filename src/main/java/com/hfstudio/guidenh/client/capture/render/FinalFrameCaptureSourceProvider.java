package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.OpenGlHelper;

import org.lwjgl.opengl.GLContext;

import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderContextKey;
import com.hfstudio.guidenh.client.capture.api.CaptureSource;
import com.hfstudio.guidenh.client.capture.api.CaptureSourceProvider;

public class FinalFrameCaptureSourceProvider implements CaptureSourceProvider {

    @Override
    public String id() {
        return "final_frame";
    }

    @Override
    public CaptureProviderAvailability availability(CaptureContext context, CaptureContentOptions content) {
        return OpenGlHelper.isFramebufferEnabled() ? CaptureProviderAvailability.ready()
            : CaptureProviderAvailability.unavailable("Content filtering requires framebuffer support");
    }

    @Override
    public CaptureSource open(CaptureContext context) {
        return new FinalFrameCaptureSource();
    }

    @Override
    public boolean automaticSelection(CaptureContext context, CaptureContentOptions content) {
        return content.includeWorld();
    }

    @Override
    public Object contextKey(CaptureContext context) {
        return new CaptureRenderContextKey(context.screen(), GLContext.getCapabilities());
    }
}
