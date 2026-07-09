package com.hfstudio.guidenh.bridge.semantic;

import java.util.Map;

import lombok.Getter;

@Getter
public class SemanticQuery {

    private final String cursor;
    private final int limit;
    private final String prefix;
    private final Map<String, String> filters;

    public SemanticQuery(String cursor, int limit, String prefix, Map<String, String> filters) {
        this.cursor = cursor == null ? "" : cursor;
        this.limit = limit;
        this.prefix = prefix == null ? "" : prefix;
        this.filters = filters == null || filters.isEmpty() ? Map.of() : Map.copyOf(filters);
    }

}
