package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironmentAware;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/**
 * Suggests guide page ids for link and page reference attributes.
 *
 * <p>
 * One instance belongs to one guide model, so the page list it answers with is the list of the guide
 * the editor currently has open.
 */
public class PagePathValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    private List<String> pagePaths = List.of();

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
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String path : pagePaths) {
            if (results.size() >= limit) {
                break;
            }
            if (lower.isEmpty() || path.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(SyntaxSuggestion.of(path));
            }
        }
        return results;
    }
}
