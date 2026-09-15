package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class ExpressionValueSource implements SyntaxValueSource {

    private static final Set<SyntaxValueKind> KINDS = Collections.singleton(SyntaxValueKind.EXPRESSION);

    private static final String[] EXPRESSIONS = { "sin(x)", "cos(x)", "tan(x)", "x^2", "x^3", "sqrt(x)", "abs(x)",
        "log(x)", "ln(x)", "exp(x)", "1/x", "sin(x)*cos(x)", "x*sin(x)", "floor(x)", "ceil(x)" };

    @Override
    public Set<SyntaxValueKind> kinds() {
        return KINDS;
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String lower = request.partialText()
            .toLowerCase(Locale.ROOT);
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String expression : EXPRESSIONS) {
            if (results.size() >= limit) {
                break;
            }
            if (lower.isEmpty() || expression.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(SyntaxSuggestion.of(expression));
            }
        }
        return results;
    }
}
