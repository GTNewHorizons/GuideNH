package com.hfstudio.guidenh.bridge.preview;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class PreviewResolveQuery {

    private final String capability;
    private final String id;
    private final int count;
    private final String nbt;
    private final String renderVariant;
    private final Map<String, String> filters;

    public PreviewResolveQuery(String capability, String id, int count, String nbt, String renderVariant,
        Map<String, String> filters) {
        this.capability = capability == null ? "" : capability;
        this.id = id == null ? "" : id;
        this.count = count;
        this.nbt = nbt == null ? "" : nbt;
        this.renderVariant = renderVariant == null ? "default" : renderVariant;
        this.filters = filters == null || filters.isEmpty() ? Map.of() : Map.copyOf(new LinkedHashMap<>(filters));
    }

}
