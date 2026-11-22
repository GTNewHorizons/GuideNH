package com.hfstudio.guidenh.client.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureLifecycleListener;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public class CaptureRegistry {

    private final List<CaptureGeometryProvider> geometryProviders = new ArrayList<>();
    private final List<CaptureLifecycleListener> lifecycleListeners = new ArrayList<>();

    public void registerLifecycle(CaptureLifecycleListener listener) {
        lifecycleListeners.add(listener);
    }

    public void opened(CaptureContext context) {
        for (CaptureLifecycleListener listener : lifecycleListeners) listener.opened(context);
    }

    public void closed(CaptureContext context) {
        for (int index = lifecycleListeners.size() - 1; index >= 0; index--) lifecycleListeners.get(index)
            .closed(context);
    }

    public void registerGeometry(CaptureGeometryProvider provider) {
        geometryProviders.removeIf(
            existing -> existing.id()
                .equals(provider.id()));
        geometryProviders.add(provider);
    }

    public CaptureGeometry collect(CaptureContext context) {
        Map<String, CaptureTarget> targets = new LinkedHashMap<>();
        for (CaptureGeometryProvider provider : geometryProviders) {
            if (!provider.supports(context)) continue;
            for (CaptureTarget target : provider.collect(context)
                .targets()) targets.put(target.id(), target);
        }
        List<CaptureTarget> collected = List.copyOf(targets.values());
        return new CaptureGeometry(collected.hashCode(), context.viewport(), collected);
    }
}
