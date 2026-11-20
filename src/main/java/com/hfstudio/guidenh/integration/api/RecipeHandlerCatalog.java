package com.hfstudio.guidenh.integration.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecipeHandlerCatalog {

    private RecipeHandlerCatalog() {}

    public static List<RecipeHandlerDescriptor> entries() {
        Map<String, RecipeHandlerDescriptor> entries = new LinkedHashMap<>();
        for (RecipeHandlerMetadataProvider provider : GuideNhIntegrationRegistry.global()
            .recipeHandlerMetadataProviders()) {
            for (RecipeHandlerDescriptor entry : provider.handlerDescriptors()) {
                entries.putIfAbsent(
                    entry.id()
                        .toLowerCase(Locale.ROOT),
                    entry);
            }
        }
        return entries.values()
            .stream()
            .sorted((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.id(), right.id()))
            .toList();
    }
}
