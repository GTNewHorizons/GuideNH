package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests the two boolean literals. */
public class BooleanValueSource implements SyntaxValueSource {

    private static final String[] VALUES = { "true", "false" };

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.BOOLEAN);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String value : VALUES) {
            if (results.size() >= limit) {
                break;
            }
            if (lower.isEmpty() || value.startsWith(lower)) {
                results.add(SyntaxSuggestion.of(value));
            }
        }
        return results;
    }
}
