package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironmentAware;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/**
 * Suggests guide page ids for link and page reference attributes.
 */
public class PagePathValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    private List<String> pagePaths = List.of();
    private NameSnapshot snapshot = new NameSnapshot(() -> List.of());

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.PAGE_PATH);
    }

    @Override
    public void prepare(SyntaxEnvironment environment) {
        List<String> paths = environment.pagePaths();
        if (paths == null || paths.equals(pagePaths)) {
            return;
        }
        pagePaths = List.copyOf(paths);
        snapshot = new NameSnapshot(() -> pagePaths);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return snapshot.suggestions(request.partialText(), limit);
    }
}
