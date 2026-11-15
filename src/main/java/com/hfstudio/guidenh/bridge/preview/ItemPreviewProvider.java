package com.hfstudio.guidenh.bridge.preview;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;

public record ItemPreviewProvider(ItemPreviewSearchService searchService, ItemPreviewService resolveService)
    implements RuntimePreviewProvider {

    @Override
    public String capability() {
        return SemanticCapability.ITEMS;
    }

    @Override
    public PreviewSearchResult search(PreviewSearchQuery query) {
        return searchService.search(query);
    }

    @Override
    public void close() {
        resolveService.close();
    }

    @Override
    public PreviewResolveResult resolve(PreviewResolveQuery query) {
        return resolveService.resolve(query);
    }
}
