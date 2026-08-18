package com.hfstudio.guidenh.guide.indices;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.util.data.ItemId;
import com.hfstudio.guidenh.guide.GuidePageChange;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * An index of Minecraft items to the main guidebook page describing it.
 * <p/>
 * Supports per-meta entries: an {@code item_ids} frontmatter list may contain either
 * {@code modid:name} (matches any metadata) or {@code modid:name:meta} (exact match only).
 */
public class ItemIndex extends UniqueIndex<ItemId, PageAnchor> {

    private List<ItemIdExpressionBinding> itemIdExpressions = List.of();

    public ItemIndex() {
        super(
            "Item Index",
            ItemIndex::getItemAnchors,
            (writer, value) -> writer.value(formatKey(value)),
            (writer, value) -> writer.value(value.toString()));
    }

    /**
     * Looks up a page for the given stack: exact-meta first, then wildcard-meta fallback.
     */
    @Nullable
    public PageAnchor findByStack(@Nullable ItemStack stack) {
        if (stack == null) return null;
        Item item = stack.getItem();
        if (item == null) return null;
        PageAnchor anchor = findDirect(item, stack.getItemDamage());
        return anchor != null ? anchor : findExpression(stack);
    }

    /**
     * Looks up a page by (item, meta): exact-meta first, then wildcard-meta fallback.
     */
    @Nullable
    public PageAnchor findByItem(Item item, int meta) {
        if (item == null) return null;
        return findByStack(new ItemStack(item, 1, meta));
    }

    @Override
    public void rebuild(List<ParsedGuidePage> pages) {
        super.rebuild(pages);
        itemIdExpressions = getItemIdExpressionBindings(pages);
    }

    @Override
    public void update(List<ParsedGuidePage> allPages, List<GuidePageChange> changes) {
        super.update(allPages, changes);
        itemIdExpressions = getItemIdExpressionBindings(allPages);
    }

    @Nullable
    private PageAnchor findDirect(Item item, int meta) {
        PageAnchor exact = get(ItemId.createNoCopy(item, meta, null));
        if (exact != null) return exact;
        if (meta != OreDictionary.WILDCARD_VALUE) {
            return get(ItemId.createNoCopy(item, OreDictionary.WILDCARD_VALUE, null));
        }
        return null;
    }

    @Nullable
    private PageAnchor findExpression(ItemStack stack) {
        for (ItemIdExpressionBinding binding : itemIdExpressions) {
            if (binding.expression()
                .matches(stack)) {
                return binding.anchor();
            }
        }
        return null;
    }

    public static String formatKey(ItemId key) {
        Object name = Item.itemRegistry.getNameForObject(key.getItem());
        String base = name != null ? name.toString() : "unknown";
        return key.getItemMeta() == OreDictionary.WILDCARD_VALUE ? base : base + ":" + key.getItemMeta();
    }

    public static List<Pair<ItemId, PageAnchor>> getItemAnchors(ParsedGuidePage page) {
        var properties = page.getFrontmatter()
            .additionalProperties();
        var itemAnchors = new ArrayList<Pair<ItemId, PageAnchor>>();
        appendDirectItemAnchors(itemAnchors, page, properties.get("item_id"), "item_id");
        appendDirectItemAnchors(itemAnchors, page, properties.get("item_ids"), "item_ids");
        return itemAnchors;
    }

    public static List<ItemIdExpressionBinding> getItemIdExpressionBindings(List<ParsedGuidePage> pages) {
        var bindings = new ArrayList<ItemIdExpressionBinding>();
        for (ParsedGuidePage page : pages) {
            var properties = page.getFrontmatter()
                .additionalProperties();
            appendExpressionBindings(bindings, page, properties.get("item_id"), "item_id");
            appendExpressionBindings(bindings, page, properties.get("item_ids"), "item_ids");
        }
        return List.copyOf(bindings);
    }

    private static void appendExpressionBindings(List<ItemIdExpressionBinding> bindings, ParsedGuidePage page,
        Object value, String key) {
        if (value == null) {
            return;
        }
        if (value instanceof List<?>values) {
            for (Object entry : values) {
                appendExpressionBindings(bindings, page, entry, key);
            }
            return;
        }
        if (!(value instanceof String expressionSource)) {
            GuideDebugLog.warn("[GuideNH] [ItemIndex] Page {} contains malformed {} frontmatter", page.getId(), key);
            return;
        }
        ItemExpressionSource expressionSourceWithAnchor = splitAnchor(expressionSource);
        ItemIdExpression expression = ItemIdExpression.parse(expressionSourceWithAnchor.expression());
        if (expression == null) {
            GuideDebugLog.warn(
                "[GuideNH] [ItemIndex] Page {} contains an invalid {} expression: {}",
                page.getId(),
                key,
                expressionSourceWithAnchor.expression());
            return;
        }
        bindings.add(
            new ItemIdExpressionBinding(expression, new PageAnchor(page.getId(), expressionSourceWithAnchor.anchor())));
    }

    private static void appendDirectItemAnchors(List<Pair<ItemId, PageAnchor>> itemAnchors, ParsedGuidePage page,
        Object value, String key) {
        if (value == null) {
            return;
        }
        if (value instanceof List<?>values) {
            for (Object entry : values) {
                appendDirectItemAnchors(itemAnchors, page, entry, key);
            }
            return;
        }
        if (!(value instanceof String itemIdSource)) {
            GuideDebugLog.warn("[GuideNH] [ItemIndex] Page {} contains malformed {} frontmatter", page.getId(), key);
            return;
        }

        ItemExpressionSource itemIdWithAnchor = splitAnchor(itemIdSource);
        if (!isDirectItemReference(itemIdWithAnchor.expression())) {
            return;
        }
        try {
            IdUtils.ParsedItemRef reference = IdUtils.parseItemRef(
                itemIdWithAnchor.expression(),
                page.getId()
                    .getResourceDomain());
            Item item = reference == null ? null : (Item) Item.itemRegistry.getObject(reference.rawKey());
            if (item != null) {
                ItemId itemId = ItemId.createNoCopy(item, reference.meta(), reference.nbt());
                itemAnchors.add(Pair.of(itemId, new PageAnchor(page.getId(), itemIdWithAnchor.anchor())));
            }
        } catch (IllegalArgumentException e) {
            GuideDebugLog.warn(
                "[GuideNH] [ItemIndex] Page {} contains a malformed {} frontmatter entry: {}",
                page.getId(),
                key,
                itemIdSource);
        }
    }

    private static ItemExpressionSource splitAnchor(String source) {
        int hashIndex = source.indexOf('#');
        return hashIndex < 0 ? new ItemExpressionSource(source, null)
            : new ItemExpressionSource(source.substring(0, hashIndex), source.substring(hashIndex + 1));
    }

    private static boolean isDirectItemReference(String value) {
        return value.indexOf(' ') < 0 && value.indexOf('\t') < 0
            && value.indexOf('|') < 0
            && value.indexOf(',') < 0
            && value.indexOf('!') < 0
            && value.indexOf('<') < 0
            && value.indexOf('>') < 0
            && !value.startsWith("r/")
            && !IdUtils.isNonNegativeInt(value);
    }

    @Desugar
    public record ItemIdExpressionBinding(ItemIdExpression expression, PageAnchor anchor) {}

    @Desugar
    private record ItemExpressionSource(String expression, @Nullable String anchor) {}
}
