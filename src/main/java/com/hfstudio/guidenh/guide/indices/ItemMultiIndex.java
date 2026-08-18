package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;

/**
 * An index that maps each Minecraft item to ALL guide pages that reference it via
 * {@code item_ids} frontmatter. Unlike {@link ItemIndex}, duplicate entries are kept rather
 * than silently discarded, so a single item can be bound to multiple pages.
 */
public class ItemMultiIndex extends MultiValuedIndex<ItemId, PageAnchor> {

    private List<ItemIndex.ItemIdExpressionBinding> itemIdExpressions = List.of();

    public ItemMultiIndex() {
        super(
            "Item Multi-Index",
            ItemIndex::getItemAnchors,
            (writer, value) -> writer.value(ItemIndex.formatKey(value)),
            (writer, value) -> writer.value(value.toString()));
    }

    /**
     * Returns all pages matching the given stack. Exact-meta entries appear before
     * wildcard-meta entries. Returns an empty list if the stack is null or has no item.
     */
    public List<PageAnchor> findAllByStack(ItemStack stack) {
        if (stack == null) return List.of();
        Item item = stack.getItem();
        if (item == null) return List.of();
        return appendExpressionMatches(findAllDirect(item, stack.getItemDamage()), stack);
    }

    /**
     * Returns all pages matching the given item and meta. Exact-meta entries appear first,
     * followed by wildcard-meta entries.
     */
    public List<PageAnchor> findAllByItem(Item item, int meta) {
        if (item == null) return List.of();
        return findAllByStack(new ItemStack(item, 1, meta));
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        super.rebuild(pages);
        itemIdExpressions = ItemIndex.getItemIdExpressionBindings(pages);
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        super.update(allPages, changes);
        itemIdExpressions = ItemIndex.getItemIdExpressionBindings(allPages);
    }

    private List<PageAnchor> findAllDirect(Item item, int meta) {
        if (meta == OreDictionary.WILDCARD_VALUE) {
            return get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        }
        var exact = get(ItemId.createNoCopy(item, meta, null));
        var wildcard = get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        if (exact.isEmpty()) return wildcard;
        if (wildcard.isEmpty()) return exact;
        var combined = new ArrayList<PageAnchor>(exact.size() + wildcard.size());
        combined.addAll(exact);
        combined.addAll(wildcard);
        return combined;
    }

    private List<PageAnchor> appendExpressionMatches(List<PageAnchor> directMatches, ItemStack stack) {
        List<PageAnchor> matches = directMatches;
        for (ItemIndex.ItemIdExpressionBinding binding : itemIdExpressions) {
            if (!binding.expression()
                .matches(stack) || matches.contains(binding.anchor())) {
                continue;
            }
            if (matches == directMatches) {
                matches = new ArrayList<>(directMatches);
            }
            matches.add(binding.anchor());
        }
        return matches;
    }
}
