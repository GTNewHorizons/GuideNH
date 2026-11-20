package com.hfstudio.guidenh.bridge.preview;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RuntimePreviewFacade implements AutoCloseable {

    private final Map<String, RuntimePreviewProvider> providers;

    public RuntimePreviewFacade(ItemPreviewSearchService itemPreviewSearchService,
        ItemPreviewService itemPreviewService) {
        this(List.of(new ItemPreviewProvider(itemPreviewSearchService, itemPreviewService)));
    }

    public RuntimePreviewFacade(List<RuntimePreviewProvider> providers) {
        var registered = new LinkedHashMap<String, RuntimePreviewProvider>();
        for (var provider : providers) {
            if (registered.putIfAbsent(provider.capability(), provider) != null) {
                throw new IllegalArgumentException("Duplicate preview capability: " + provider.capability());
            }
        }
        this.providers = Map.copyOf(registered);
    }

    public PreviewSearchResult search(PreviewSearchQuery query) {
        return provider(query.getCapability()).search(query);
    }

    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        return provider(query.getCapability()).resolve(query);
    }

    private RuntimePreviewProvider provider(String capability) {
        RuntimePreviewProvider provider = providers.get(capability);
        if (provider == null) throw new IllegalArgumentException("Unknown preview capability: " + capability);
        return provider;
    }

    @Override
    public void close() {
        providers.values()
            .forEach(RuntimePreviewProvider::close);
    }
}
