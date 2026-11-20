package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;
import com.hfstudio.guidenh.guide.internal.binding.BindingQueries;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;

public class BindingSemanticProvider implements SemanticProvider {

    @Override
    public String getCapability() {
        return SemanticCapability.BINDINGS;
    }

    @Override
    public SemanticQueryResult query(SemanticQuery query) {
        int offset = 0;
        try {
            offset = Math.max(0, Integer.parseInt(query.getCursor()));
        } catch (NumberFormatException ignored) {}
        var result = BindingQueries.search(
            query.getFilters()
                .get("type"),
            query.getPrefix(),
            offset,
            query.getLimit());
        var entries = result.entries()
            .stream()
            .map(
                display -> Map.of(
                    "type",
                    display.key()
                        .type(),
                    "id",
                    display.key()
                        .id(),
                    "label",
                    BindingRuntime.title(display),
                    "state",
                    display.state()
                        .id()))
            .toList();
        int end = offset + entries.size();
        return new SemanticQueryResult(
            getCapability(),
            (int) Math.max(1, result.revision() & Integer.MAX_VALUE),
            entries,
            end < result.total() ? Integer.toString(end) : null);
    }
}
