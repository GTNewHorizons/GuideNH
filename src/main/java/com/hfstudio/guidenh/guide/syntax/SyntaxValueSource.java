package com.hfstudio.guidenh.guide.syntax;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.extensions.Extension;

/**
 * Supplies completion values for one or more {@link SyntaxValueKind value kinds}.
 *
 * <p>
 * A mod that owns a registry the guide syntax can reference — a machine id, a fluid id, a quest id —
 * implements this interface, registers it through {@link SyntaxSink#valueSource} and then marks the
 * relevant attributes with the matching kind. Reusing a built-in kind means the mod does not have to
 * register anything at all.
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
