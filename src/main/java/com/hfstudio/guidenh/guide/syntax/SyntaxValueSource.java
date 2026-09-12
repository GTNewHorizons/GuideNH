package com.hfstudio.guidenh.guide.syntax;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.extensions.Extension;

/**
 * Supplies completion values for one or more {@link SyntaxValueKind value kinds}.
 */
public interface SyntaxValueSource extends Extension {

    /** The kinds this source answers. */
    Set<SyntaxValueKind> kinds();

    /**
     * @param request what is being completed, including the text typed so far
     * @param limit   maximum number of suggestions to return
     */
    List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit);
}
