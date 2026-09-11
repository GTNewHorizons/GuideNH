package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.entity.EntityList;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests entity registry names for entity id attributes. */
public class EntityNameValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.ENTITY_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        String partial = request.partialText();
        String lower = partial != null ? partial.toLowerCase(Locale.ROOT) : "";
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (Object key : EntityList.stringToClassMapping.keySet()) {
            if (results.size() >= limit) {
                break;
            }
            if (!(key instanceof String name)) {
                continue;
            }
            if (lower.isEmpty() || name.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                results.add(SyntaxSuggestion.of(name));
            }
        }
        return results;
    }
}
