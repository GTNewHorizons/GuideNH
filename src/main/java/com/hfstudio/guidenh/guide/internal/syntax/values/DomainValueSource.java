package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class DomainValueSource implements SyntaxValueSource {

    private static final List<SyntaxSuggestion> DOMAINS = List.of(
        SyntaxSuggestion.of("-inf..inf"),
        SyntaxSuggestion.of("-10..10"),
        SyntaxSuggestion.of("-pi..pi"),
        SyntaxSuggestion.of("0..2*pi"),
        SyntaxSuggestion.of("..0"),
        SyntaxSuggestion.of("0.."),
        SyntaxSuggestion.of("-5..5"),
        SyntaxSuggestion.of("-1..1"));

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.DOMAIN);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return SyntaxSuggestion.containing(DOMAINS, request.partialText(), limit);
    }
}
