package com.hfstudio.guidenh.integration.thaumcraft;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.Nullable;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

public class ThaumcraftResearchCatalog {

    private Map<String, ResearchItem> research = Map.of();
    private int registrationSize = -1;
    private long revision;

    public Map<String, ResearchItem> entries() {
        int size = 0;
        for (var category : ResearchCategories.researchCategories.values()) size += category.research.size();
        if (size != registrationSize) {
            var updated = new TreeMap<String, ResearchItem>();
            for (var category : ResearchCategories.researchCategories.values()) {
                for (var item : category.research.values()) updated.putIfAbsent(item.key, item);
            }
            research = Collections.unmodifiableMap(new LinkedHashMap<>(updated));
            registrationSize = size;
            revision++;
        }
        return research;
    }

    @Nullable
    public ResearchItem get(String key) {
        if (registrationSize == -1) entries();
        return research.get(key);
    }

    public long revision() {
        entries();
        return revision;
    }

    public void invalidate() {
        registrationSize = -1;
    }
}
