package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.integration.api.RecipeHandlerCatalog;

public class RecipeHandlerSemanticProvider extends AbstractCollectionSemanticProvider {

    public RecipeHandlerSemanticProvider() {
        super(SemanticCapability.NEI_HANDLERS);
    }

    @Override
    protected List<Map<String, String>> loadEntries() {
        return RecipeHandlerCatalog.entries()
            .stream()
            .map(entry -> createEntry(entry.id(), entry.name()))
            .toList();
    }
}
