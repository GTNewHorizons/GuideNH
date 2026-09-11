package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironmentAware;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/**
 * Suggests OreDictionary names for ore attributes.
 *
 * <p>
 * The ore dictionary holds thousands of names and only changes while mods load, so the names and their
 * lowercase forms are copied into a snapshot that a query scans without allocating. The snapshot is
 * refreshed once per {@link #REFRESH_INTERVAL_MILLIS}, which keeps a reload visible without reading the
 * live dictionary on every keystroke.
 */
public class OreDictValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    private static final long REFRESH_INTERVAL_MILLIS = 10_000L;

    private List<Entry> entries = List.of();
    private long nextRefreshAtMillis;

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.ORE_DICT);
    }

    @Override
    public void prepare(SyntaxEnvironment environment) {
        refreshIfStale();
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        refreshIfStale();
        int safeLimit = Math.max(0, limit);
        List<SyntaxSuggestion> results = new ArrayList<>();
        if (safeLimit == 0) {
            return results;
        }
        String lower = request.partialText() != null ? request.partialText()
            .toLowerCase(Locale.ROOT) : "";
        for (Entry entry : entries) {
            if (results.size() >= safeLimit) {
                break;
            }
            if (lower.isEmpty() || entry.lower()
                .contains(lower)) {
                results.add(SyntaxSuggestion.of(entry.name()));
            }
        }
        return results;
    }

    private void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (!entries.isEmpty() && now < nextRefreshAtMillis) {
            return;
        }
        List<Entry> snapshot = new ArrayList<>();
        for (String name : OreDictionary.getOreNames()) {
            if (name != null && !name.isEmpty()) {
                snapshot.add(new Entry(name, name.toLowerCase(Locale.ROOT)));
            }
        }
        entries = List.copyOf(snapshot);
        nextRefreshAtMillis = now + REFRESH_INTERVAL_MILLIS;
    }

    /** One ore name with its lowercase form, so matching a query never allocates. */
    private record Entry(String name, String lower) {}
}
