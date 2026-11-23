package com.hfstudio.guidenh.client.capture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import com.hfstudio.guidenh.client.capture.api.CaptureBatchCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureContentOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureGuiRenderer;
import com.hfstudio.guidenh.client.capture.api.CaptureLifecycleListener;
import com.hfstudio.guidenh.client.capture.api.CapturePixelReaderProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureProviderAvailability;
import com.hfstudio.guidenh.client.capture.api.CaptureSourceProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;

public class CaptureRegistry {

    private final List<CaptureGeometryProvider> geometryProviders = new ArrayList<>();
    private final List<CaptureLifecycleListener> lifecycleListeners = new ArrayList<>();
    private final List<CaptureGuiRenderer> guiRenderers = new ArrayList<>();
    private final Map<String, CaptureBatchProvider> batchProviders = new LinkedHashMap<>();
    private final Map<String, CapturePixelReaderProvider> readerProviders = new LinkedHashMap<>();
    private final Map<String, CaptureSourceProvider> sourceProviders = new LinkedHashMap<>();
    private long backendRevision;

    public void registerReader(CapturePixelReaderProvider provider) {
        Objects.requireNonNull(provider);
        registerBackend(readerProviders, provider.id(), provider);
        backendRevision++;
    }

    public void registerSource(CaptureSourceProvider provider) {
        Objects.requireNonNull(provider);
        registerBackend(sourceProviders, provider.id(), provider);
        backendRevision++;
    }

    private static <P> void registerBackend(Map<String, P> providers, String id, P provider) {
        if (id == null || id.isBlank() || id.equals("automatic"))
            throw new IllegalArgumentException("A capture provider needs a non-reserved identifier");
        providers.remove(id);
        providers.put(id, provider);
    }

    public long backendRevision() {
        return backendRevision;
    }

    public List<String> sourceIds() {
        return List.copyOf(sourceProviders.keySet());
    }

    public List<String> readerIds() {
        return List.copyOf(readerProviders.keySet());
    }

    public CapturePixelReaderProvider readerProvider(CaptureContext context, String id) {
        return resolveBackend(readerProviders, id, provider -> provider.availability(context), "pixel reader");
    }

    public CaptureSourceProvider sourceProvider(CaptureContext context, CaptureContentOptions content, String id) {
        return resolveBackend(sourceProviders, id, provider -> provider.availability(context, content), "source");
    }

    private static <P> P resolveBackend(Map<String, P> providers, String id,
        Function<P, CaptureProviderAvailability> availability, String kind) {
        if (!id.equals("automatic")) {
            P provider = providers.get(id);
            if (provider == null) throw new IllegalArgumentException("Unknown capture " + kind + ": " + id);
            CaptureProviderAvailability status = availability.apply(provider);
            if (!status.available())
                throw new IllegalStateException("Capture " + kind + " " + id + ": " + status.reason());
            return provider;
        }
        List<Map.Entry<String, P>> ordered = new ArrayList<>(providers.entrySet());
        List<String> reasons = new ArrayList<>();
        for (int index = ordered.size() - 1; index >= 0; index--) {
            Map.Entry<String, P> entry = ordered.get(index);
            CaptureProviderAvailability status = availability.apply(entry.getValue());
            if (status.available()) return entry.getValue();
            reasons.add(entry.getKey() + ": " + status.reason());
        }
        throw new IllegalStateException("No capture " + kind + " is available. " + String.join("; ", reasons));
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
