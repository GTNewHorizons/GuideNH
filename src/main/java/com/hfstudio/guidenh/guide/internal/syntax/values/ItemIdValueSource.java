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
        int wanted = Math.max(1, limit);
        List<SyntaxSuggestion> results = new ArrayList<>();
        // The index trims its answer to a limit, and an entry that is not an item is dropped here, so asking
        // for exactly the limit can return fewer than that - or none at all when the first matches happen to
        // be entries without an item. A larger window is asked for and the list is trimmed once it is full of
        // entries that really have an icon.
        int window = wanted * 4;
        for (String candidate : RegistryIdIndex.items()
            .match(request.partialText(), window)) {
            if (results.size() >= wanted) {
                break;
            }
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
