package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EntityList;

import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/**
 * Suggests entity registry names for entity id attributes.
 *
 * <p>
 * The entity registry is filled while mods load, so it is answered from a snapshot instead of a fresh
 * lowercase copy of every name per keystroke.
 */
public class EntityNameValueSource implements SyntaxValueSource {

    private final NameSnapshot snapshot = new NameSnapshot(EntityNameValueSource::registeredNames);

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.ENTITY_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        return snapshot.suggestions(request.partialText(), limit);
    }

    private static List<String> registeredNames() {
        List<String> names = new ArrayList<>();
        for (Object key : EntityList.stringToClassMapping.keySet()) {
            if (key instanceof String name) {
                names.add(name);
            }
        }
        return names;
    }
}
