package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class FormatPatternValueSource implements SyntaxValueSource {

    private static final List<SyntaxSuggestion> PATTERNS = List.of(
        SyntaxSuggestion.of("%s"),
        SyntaxSuggestion.of("%s items"),
        SyntaxSuggestion.of("**%s**"),
        SyntaxSuggestion.of("*%s*"),
        SyntaxSuggestion.of("~~%s~~"));

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.FORMAT_PATTERN);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return SyntaxSuggestion.containing(PATTERNS, request.partialText(), limit);
    }
}
