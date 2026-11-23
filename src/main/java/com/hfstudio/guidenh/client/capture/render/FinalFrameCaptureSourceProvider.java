package com.hfstudio.guidenh.client.capture.render;

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
        return content.includeWorld() ? CaptureProviderAvailability.ready()
            : CaptureProviderAvailability.unavailable("The final frame source requires world inclusion to be enabled");
    }

    @Override
    public CaptureSource open(CaptureContext context) {
        return new FinalFrameCaptureSource();
    }

    @Override
    public Object contextKey(CaptureContext context) {
        return new CaptureRenderContextKey(context.screen(), GLContext.getCapabilities());
    }
}
