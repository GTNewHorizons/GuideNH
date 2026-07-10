package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.AutocompleteContext;

/** Suggests guide page paths. Call {@link #setPages} before use. */
public class PageReferenceProvider implements AutocompleteProvider {

    private static final Set<AutocompleteKey> KEYS = buildKeys();
    @Nullable
    private static volatile List<String> pagePaths;

    private static Set<AutocompleteKey> buildKeys() {
        return Set.of(
            AutocompleteKey.forValue("a", "href"),
            AutocompleteKey.forValue("SubPages", "id"),
            AutocompleteKey.forValue("ItemLink", "linksTo"),
            AutocompleteKey.forValue("*", "parent"),
            AutocompleteKey.forValue("link", "url"));
    }

    /** Set the available page paths from the guide's page collection. */
    public static void setPages(@Nullable Collection<String> paths) {
        pagePaths = paths != null ? List.copyOf(paths) : null;
    }

    @Override
    public Set<AutocompleteKey> getSupportedKeys() {
        return KEYS;
    }

    @Override
    public List<AutocompleteCandidate> provide(AutocompleteContext ctx, int limit) {
        if (pagePaths == null) return Collections.emptyList();
        String partial = ctx.getPartialText()
            .toLowerCase();
        List<AutocompleteCandidate> results = new ArrayList<>();
        for (String path : pagePaths) {
            if (results.size() >= limit) break;
            if (partial.isEmpty() || path.toLowerCase()
                .contains(partial)) {
                results.add(new TextCandidate(path));
            }
        }
        return results;
    }
}
