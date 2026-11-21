package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.internal.binding.BindingQueries;
import com.hfstudio.guidenh.guide.internal.binding.BindingRuntime;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class BindingIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.BINDING_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String type = request.tagName() != null && request.tagName()
            .startsWith("Research") || request.frontmatterKey() != null
                && request.frontmatterKey()
                    .startsWith("research_id") ? "thaumcraft"
                        : request.contextValues()
                            .get("type");
        if (type == null || type.isBlank()) return List.of();
        return BindingQueries.search(type, request.partialText(), 0, limit)
            .entries()
            .stream()
            .map(
                display -> SyntaxSuggestion.withValue(
                    encode(
                        display.key()
                            .id(),
                        request.contextValues()
                            .get("quote")),
                    BindingRuntime.title(display),
                    display.key()
                        .type()))
            .toList();
    }

    private static String encode(String id, String quote) {
        if ("\"".equals(quote)) return id.replace("\\", "\\\\")
            .replace("\"", "\\\"");
        if ("'".equals(quote)) return id.replace("'", "''");
        return id;
    }
}
