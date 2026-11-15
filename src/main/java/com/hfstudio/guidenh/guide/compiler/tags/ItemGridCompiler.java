package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemGridCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemGrid");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        List<GridEntry> entries = new ArrayList<>();
        for (var childNode : el.children()) {
            if (childNode instanceof MdxJsxElementFields jsxChild && "ItemIcon".equals(jsxChild.name())) {
                var itemId = MdxAttrs.getString(compiler, parent, jsxChild, "id", null);
                var ore = MdxAttrs.getString(compiler, parent, jsxChild, "ore", null);
                if (itemId != null || ore != null) {
                    entries.add(new ItemEntry(itemId, ore));
                } else {
                    parent.appendError(compiler, "ItemIcon requires id or ore", childNode);
                }
                continue;
            }
            if (childNode instanceof MdxJsxElementFields jsxChild && "FluidIcon".equals(jsxChild.name())) {
                try {
                    entries.add(
                        new FluidEntry(
                            FluidTagAttributes.readFluid(jsxChild),
                            FluidTagAttributes.showTooltip(jsxChild)));
                } catch (IllegalArgumentException exception) {
                    parent.appendError(compiler, exception.getMessage(), childNode);
                }
                continue;
            }
            parent.appendError(compiler, "Unsupported child-element in ItemGrid", childNode);
        }

        ItemGridPlaceholder placeholder = new ItemGridPlaceholder(
            entries.stream()
                .filter(ItemEntry.class::isInstance)
                .map(ItemEntry.class::cast)
                .map(ItemEntry::id)
                .filter(Objects::nonNull)
                .toList(),
            entries);
        parent.append(placeholder);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {}

    public interface GridEntry {
    }

    public record ItemEntry(String id, String ore) implements GridEntry {

        public ItemEntry(String id) {
            this(id, null);
        }
    }

    public record FluidEntry(FluidStackSpec fluid, boolean showTooltip) implements GridEntry {}

    public static class ItemGridPlaceholder extends LytParagraph {

        public final List<String> itemIds;
        private final List<GridEntry> entries;

        public ItemGridPlaceholder(List<String> itemIds) {
            this(
                itemIds,
                itemIds.stream()
                    .map(ItemEntry::new)
                    .map(GridEntry.class::cast)
                    .toList());
        }

        public ItemGridPlaceholder(List<String> itemIds, List<GridEntry> entries) {
            this.itemIds = List.copyOf(itemIds);
            this.entries = List.copyOf(entries);
            setStyleClass("ItemGrid");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
            appendText("[ItemGrid]");
        }

        public List<GridEntry> getEntries() {
            return entries;
        }
    }
}
