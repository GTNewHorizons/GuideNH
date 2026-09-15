package com.hfstudio.guidenh.guide.internal.syntax.values;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.RegistryIdIndex;
import com.hfstudio.guidenh.guide.syntax.SyntaxSuggestion;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueKind;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueRequest;
import com.hfstudio.guidenh.guide.syntax.SyntaxValueSource;

public class BlockIdValueSource implements SyntaxValueSource {

    @Override
    public Set<SyntaxValueKind> kinds() {
        return Set.of(SyntaxValueKind.BLOCK_ID);
    }

    @Override
    public List<SyntaxSuggestion> suggest(SyntaxValueRequest request, int limit) {
        List<SyntaxSuggestion> results = new ArrayList<>();
        for (String candidate : RegistryIdIndex.blocks()
            .match(request.partialText(), limit)) {
            if (candidate.endsWith(":")) {
                results.add(SyntaxSuggestion.of(candidate));
                continue;
            }
            results.add(createSuggestion(candidate));
        }
        return results;
    }

    private static SyntaxSuggestion createSuggestion(String blockId) {
        Block block = (Block) Block.blockRegistry.getObject(blockId);
        Item item = block != null ? Item.getItemFromBlock(block) : null;
        if (item == null) {
            return SyntaxSuggestion.of(blockId);
        }
        return SyntaxSuggestion.withIcon(blockId, new ItemStack(item));
    }
}
