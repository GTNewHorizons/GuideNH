package com.hfstudio.guidenh.client.capture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureLifecycleListener;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;

public class CaptureRegistry {

    private final List<CaptureGeometryProvider> geometryProviders = new ArrayList<>();
    private final List<CaptureLifecycleListener> lifecycleListeners = new ArrayList<>();
    private final List<CaptureGuiRenderer> guiRenderers = new ArrayList<>();
    private final Map<String, CaptureBatchProvider> batchProviders = new LinkedHashMap<>();
    private final Map<String, CapturePixelReaderProvider> readerProviders = new LinkedHashMap<>();

    public void registerReader(CapturePixelReaderProvider provider) {
        Objects.requireNonNull(provider);
        if (provider.id() == null || provider.id()
            .isBlank()
            || provider.id()
                .equals("automatic"))
            throw new IllegalArgumentException("A pixel reader needs a non-reserved identifier");
        readerProviders.remove(provider.id());
        readerProviders.put(provider.id(), provider);
    }

    public List<String> readerIds() {
        return List.copyOf(readerProviders.keySet());
    }

    public CapturePixelReaderProvider readerProvider(CaptureContext context, String id) {
        if (!id.equals("automatic")) {
            CapturePixelReaderProvider provider = readerProviders.get(id);
            if (provider == null) throw new IllegalArgumentException("Unknown capture pixel reader: " + id);
            CaptureProviderAvailability availability = provider.availability(context);
            if (!availability.available())
                throw new IllegalStateException("Capture pixel reader " + id + ": " + availability.reason());
            return provider;
        }
        List<CapturePixelReaderProvider> providers = new ArrayList<>(readerProviders.values());
        List<String> reasons = new ArrayList<>();
        for (int index = providers.size() - 1; index >= 0; index--) {
            CapturePixelReaderProvider provider = providers.get(index);
            CaptureProviderAvailability availability = provider.availability(context);
            if (availability.available()) return provider;
            reasons.add(provider.id() + ": " + availability.reason());
        }
        throw new IllegalStateException("No capture pixel reader is available. " + String.join("; ", reasons));
    }

    public void registerBatch(CaptureBatchProvider provider) {
        batchProviders.put(provider.id(), provider);
    }

    public CaptureBatchProvider batchProvider(String id) {
        return batchProviders.get(id);
    }

    public CaptureBatchCatalog catalog(CaptureBatchContext context, CaptureBatchOptions options) {
        Map<String, CaptureTargetDescriptor> targets = new LinkedHashMap<>();
        for (CaptureBatchProvider provider : batchProviders.values()) {
            if (!options.providerIds()
                .isEmpty()
                && !options.providerIds()
                    .contains(provider.id()))
                continue;
            try {
                if (!provider.supports(context)) continue;
                for (CaptureTargetDescriptor target : provider.enumerate(context, options))
                    targets.put(target.id(), target);
            } catch (RuntimeException error) {
                CaptureTargetDescriptor failed = new CaptureTargetDescriptor(
                    provider.id() + ":catalog_error",
                    provider.id(),
                    "",
                    provider.id(),
                    "failed",
                    error.toString(),
                    Map.of());
                targets.put(failed.id(), failed);
            }
        }
        Set<String> covered = new HashSet<>();
        for (CaptureTargetDescriptor target : targets.values()) covered.add(target.scope());
        for (String scope : options.scopes()) {
            if (!covered.contains(scope)) {
                CaptureTargetDescriptor missing = new CaptureTargetDescriptor(
                    "unavailable:" + scope,
                    "",
                    scope,
                    scope,
                    "unsupported",
                    "No registered provider can enumerate this scope in the current GUI",
                    Map.of());
                targets.put(missing.id(), missing);
            }
        }
        return new CaptureBatchCatalog(context, options, List.copyOf(targets.values()));
    }

    public void registerRenderer(CaptureGuiRenderer renderer) {
        guiRenderers.removeIf(
            existing -> existing.id()
                .equals(renderer.id()));
        guiRenderers.add(renderer);
    }

    public CaptureGuiRenderer renderer(CaptureContext context) {
        for (int index = guiRenderers.size() - 1; index >= 0; index--) {
            CaptureGuiRenderer renderer = guiRenderers.get(index);
            if (renderer.supports(context)) return renderer;
        }
        return null;
    }

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
