package com.hfstudio.guidenh.bridge.semantic.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.bridge.semantic.SemanticCapability;
import com.hfstudio.guidenh.bridge.semantic.SemanticProvider;
import com.hfstudio.guidenh.bridge.semantic.SemanticQuery;
import com.hfstudio.guidenh.bridge.semantic.SemanticQueryResult;
import com.hfstudio.guidenh.guide.internal.fluid.FluidCatalog;

public class FluidSemanticProvider implements SemanticProvider {

    private volatile Snapshot cached;

    private record Snapshot(List<FluidCatalog.Entry> source, List<Map<String, String>> entries, int version) {}

    @Override
    public String getCapability() {
        return SemanticCapability.FLUIDS;
    }

    @Override
    public SemanticQueryResult query(SemanticQuery query) {
        Snapshot snapshot = snapshot();
        String prefix = query.getPrefix()
            .trim();
        int cursor = 0;
        try {
            cursor = Math.max(0, Integer.parseInt(query.getCursor()));
        } catch (NumberFormatException ignored) {}
        int limit = query.getLimit() > 0 ? query.getLimit() : Integer.MAX_VALUE;
        var result = new ArrayList<Map<String, String>>();
        int matched = 0;
        boolean hasMore = false;
        for (int index = 0; index < snapshot.source()
            .size(); index++) {
            var fluid = snapshot.source()
                .get(index);
            if (!startsWith(fluid.id(), prefix) && !startsWith(fluid.name(), prefix)) continue;
            if (matched++ < cursor) continue;
            if (result.size() == limit) {
                hasMore = true;
                break;
            }
            result.add(
                snapshot.entries()
                    .get(index));
        }
        return new SemanticQueryResult(
            getCapability(),
            snapshot.version(),
            result,
            hasMore ? Long.toString((long) cursor + result.size()) : null);
    }

    private Snapshot snapshot() {
        var source = FluidCatalog.INSTANCE.entries();
        Snapshot current = cached;
        if (current != null && current.source() == source) return current;
        synchronized (this) {
            current = cached;
            if (current != null && current.source() == source) return current;
            var entries = source.stream()
                .map(fluid -> Map.of("id", fluid.id(), "label", fluid.name(), "detail", fluid.id()))
                .toList();
            current = new Snapshot(source, entries, 1 + (entries.hashCode() & 0x7ffffffe));
            cached = current;
            return current;
        }
    }

    private static boolean startsWith(String text, String prefix) {
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
