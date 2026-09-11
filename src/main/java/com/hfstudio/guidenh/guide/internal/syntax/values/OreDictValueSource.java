package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraftforge.oredict.OreDictionary;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests OreDictionary names for ore attributes. */
public class OreDictValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.ORE_DICT);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String name : OreDictionary.getOreNames()) {
            if (results.size() >= limit) {
                break;
            }
            if (lower.isEmpty() || name.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(SyntaxSuggestion.of(name));
            }
        }
        return results;
    }
}
