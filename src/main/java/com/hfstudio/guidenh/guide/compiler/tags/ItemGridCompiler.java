package com.hfstudio.guidenh.guide.compiler.tags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.IndexingContext;
import com.hfstudio.guidenh.guide.compiler.IndexingSink;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.document.block.LytBlockContainer;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;

public class ItemGridCompiler extends BlockTagCompiler {

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ItemGrid");
    }

    @Override
    protected void compile(PageCompiler compiler, LytBlockContainer parent, MdxJsxElementFields el) {
        List<ItemGridEntry> entries = new ArrayList<>();

        // We expect children to only contain ItemIcon elements
        for (var childNode : el.children()) {
            if (childNode instanceof MdxJsxElementFields jsxChild && "ItemIcon".equals(jsxChild.name())) {
                // Extract raw attributes (no registry lookups); keep both id and
                // ore so ore-dictionary entries can be resolved at runtime.
                String itemId = MdxAttrs.getString(compiler, parent, jsxChild, "id", null);
                String ore = MdxAttrs.getString(compiler, parent, jsxChild, "ore", null);
                if (itemId == null && ore == null) {
                    parent.appendError(compiler, "Missing id or ore attribute.", jsxChild);
                    continue;
                }
                entries.add(new ItemGridEntry(itemId != null ? itemId.trim() : null, ore != null ? ore.trim() : null));
                continue;
            }
            parent.appendError(compiler, "Unsupported child-element in ItemGrid", childNode);
        }

        ItemGridPlaceholder placeholder = new ItemGridPlaceholder(entries);
        parent.append(placeholder);
    }

    @Override
    public void index(IndexingContext indexer, MdxJsxElementFields el, IndexingSink sink) {}

    public static class ItemGridPlaceholder extends LytParagraph {

        public final List<ItemGridEntry> entries;

        public ItemGridPlaceholder(List<ItemGridEntry> entries) {
            this.entries = entries;
            setStyleClass("ItemGrid");
            setStyle(LytParagraph.PLACEHOLDER_STYLE);
            appendText("[ItemGrid]");
        }
    }

    /** A single {@code <ItemIcon>} child: raw item id and/or ore dictionary name. */
    public record ItemGridEntry(@Nullable String id, @Nullable String ore) {}
}
