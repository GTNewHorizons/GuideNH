package com.hfstudio.guidenh.guide.syntax;

import java.util.List;
import java.util.Set;

import com.hfstudio.guidenh.guide.extensions.Extension;

/** Supplies completion values for one or more {@link SyntaxValueKind value kinds}. */
public interface SyntaxValueSource extends Extension {

    Set<SyntaxValueKind> kinds();

    List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit);
}
