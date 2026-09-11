package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.RegistryIdIndex;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/**
 * Suggests mod ids for the frontmatter keys that gate a page on installed mods. The ids come from the
 * item registry namespaces, so they cover every loaded mod without a separate mod list.
 */
public class ModIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.MOD_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (RegistryIdIndex.Entry namespace : RegistryIdIndex.items()
            .namespaces()) {
            if (results.size() >= limit) {
                break;
            }
            if (lower.isEmpty() || namespace.lower()
                .startsWith(lower)) {
                results.add(SyntaxSuggestion.of(namespace.id()));
            }
        }
        return results;
    }
}
