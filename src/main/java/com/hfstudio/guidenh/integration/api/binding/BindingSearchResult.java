package com.hfstudio.guidenh.integration.api.binding;

import java.util.List;

public record BindingSearchResult(List<BindingDisplay> entries, int total, long revision) {

    public BindingSearchResult {
        entries = List.copyOf(entries);
    }
}
