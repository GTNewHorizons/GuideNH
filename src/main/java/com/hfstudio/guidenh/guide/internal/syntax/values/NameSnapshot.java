package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;

/**
 * A cached view of the names a registry or a document exposes, with the lowercase form of each name
 * precomputed, so answering a completion query scans strings instead of allocating a lowercase copy of
 * every candidate.
 */
public class NameSnapshot {

    /** One name with its lowercase form, so matching never allocates. */
    public record Entry(String name, String lower) {}

    private static final long REFRESH_INTERVAL_MILLIS = 10_000L;

    private final Supplier<List<String>> source;
    private List<Entry> entries = List.of();
    private long nextRefreshAtMillis;

    public NameSnapshot(Supplier<List<String>> source) {
        this.source = source;
    }

    /** Copies the names when the snapshot is stale, so the next query answers from memory. */
    public void refresh() {
        long now = System.currentTimeMillis();
        if (!entries.isEmpty() && now < nextRefreshAtMillis) {
            return;
        }
        List<Entry> snapshot = new ArrayList<>();
        for (String name : source.get()) {
            if (name != null && !name.isEmpty()) {
                snapshot.add(new Entry(name, name.toLowerCase(Locale.ROOT)));
            }
        }
        entries = List.copyOf(snapshot);
        nextRefreshAtMillis = now + REFRESH_INTERVAL_MILLIS;
    }

    /** Names containing {@code partial}, in source order, at most {@code limit} of them. */
    public List<String> match(@Nullable String partial, int limit) {
        refresh();
        int safeLimit = Math.max(0, limit);
        List<String> results = new ArrayList<>();
        if (safeLimit == 0) {
            return results;
        }
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        for (Entry entry : entries) {
            if (results.size() >= safeLimit) {
                break;
            }
            if (lower.isEmpty() || entry.lower()
                .contains(lower)) {
                results.add(entry.name());
            }
        }
        return results;
    }

    /** The matching names as suggestions, ready to answer a value request. */
    public List<SyntaxSuggestion> suggestions(@Nullable String partial, int limit) {
        List<String> names = match(partial, limit);
        List<SyntaxSuggestion> results = new ArrayList<>(names.size());
        for (String name : names) {
            results.add(SyntaxSuggestion.of(name));
        }
        return results;
    }
}
