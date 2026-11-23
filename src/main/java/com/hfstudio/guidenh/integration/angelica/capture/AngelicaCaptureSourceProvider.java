package com.hfstudio.guidenh.integration.angelica.capture;

import com.gtnewhorizons.angelica.glsm.backend.BackendManager;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureRenderContextKey;
import com.hfstudio.guidenh.client.capture.api.CaptureSource;
import com.hfstudio.guidenh.client.capture.api.CaptureSourceProvider;

public class AngelicaCaptureSourceProvider implements CaptureSourceProvider {

    private final CaptureSourceProvider delegate;

    public AngelicaCaptureSourceProvider(CaptureSourceProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public String id() {
        return delegate.id();
    }

    @Override
    public CaptureProviderAvailability availability(CaptureContext context, CaptureContentOptions content) {
        if (!BackendManager.RENDER_BACKEND.hasContextOnThread()) return CaptureProviderAvailability
            .unavailable("The Angelica backend has no rendering context on this thread");
        return delegate.availability(context, content);
    }

    @Override
    public CaptureSource open(CaptureContext context) {
        return delegate.open(context);
    }

    @Override
    public boolean automaticSelection(CaptureContext context, CaptureContentOptions content) {
        return delegate.automaticSelection(context, content);
    }

    @Override
    public Object contextKey(CaptureContext context) {
        return new CaptureRenderContextKey(delegate.contextKey(context), AngelicaCaptureBackendContext.key());
    }
}
