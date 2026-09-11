package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.List;
import java.util.Set;

import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironment;
import com.hfstudio.guidenh.guide.syntax.SyntaxEnvironmentAware;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/**
 * Suggests OreDictionary names for ore attributes.
 *
 * <p>
 * The ore dictionary holds thousands of names and only changes while mods load, so it is answered from
 * a snapshot that a query scans without allocating.
 */
public class OreDictValueSource implements SyntaxValueSource, SyntaxEnvironmentAware {

    private final NameSnapshot snapshot = new NameSnapshot(() -> List.of(OreDictionary.getOreNames()));

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.ORE_DICT);
    }

    @Override
    public void prepare(SyntaxEnvironment environment) {
        snapshot.refresh();
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return snapshot.suggestions(request.partialText(), limit);
    }
}
