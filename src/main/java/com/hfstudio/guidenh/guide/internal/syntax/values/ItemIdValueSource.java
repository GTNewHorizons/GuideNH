package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.RegistryIdIndex;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

/** Suggests item registry names, including the namespace shortcuts that start a mod id. */
public class ItemIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.ITEM_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String candidate : RegistryIdIndex.items()
            .match(request.partialText(), limit)) {
            if (candidate.endsWith(":")) {
                results.add(SyntaxSuggestion.of(candidate));
                continue;
            }
            Item item = (Item) Item.itemRegistry.getObject(candidate);
            if (item != null) {
                results.add(SyntaxSuggestion.withIcon(candidate, new ItemStack(item)));
            }
        }
        return results;
    }
}
