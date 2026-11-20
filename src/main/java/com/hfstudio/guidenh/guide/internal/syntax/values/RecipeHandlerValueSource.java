package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;
import com.hfstudio.guidenh.integration.api.RecipeHandlerCatalog;

public class RecipeHandlerValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.RECIPE_HANDLER_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String prefix = request.partialText()
            .toLowerCase(Locale.ROOT);
        return RecipeHandlerCatalog.entries()
            .stream()
            .filter(
                entry -> entry.id()
                    .toLowerCase(Locale.ROOT)
                    .startsWith(prefix))
            .limit(limit)
            .map(entry -> SyntaxSuggestion.of(entry.id()))
            .toList();
    }
}
