package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemGridEntry;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemGridPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.item.GuideDisplayItemStacks;

public class ItemGridScript implements LytScript {

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "ItemGrid";
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() == EventType.MOUNT && node instanceof ItemGridPlaceholder ph) {
            LytItemGrid grid = new LytItemGrid();
            int resolved = 0;
            for (ItemGridEntry entry : ph.entries) {
                ItemStack stack = resolveEntry(entry);
                if (stack != null) {
                    grid.addItem(stack);
                    resolved++;
                }
            }
            if (resolved == 0) {
                ctx.replace(LytParagraph.error("[ItemGrid] No items to display"));
            } else {
                ctx.replace(grid);
            }
        }
    }

    @Nullable
    private static ItemStack resolveEntry(ItemGridEntry entry) {
        // Prefer the direct item id; fall back to the ore dictionary name.
        if (entry.id() != null && !entry.id()
            .isEmpty()) {
            ItemStack stack = GuideDisplayItemStacks.resolveItemStack(entry.id(), "minecraft");
            if (stack != null) {
                return stack;
            }
        }
        if (entry.ore() != null && !entry.ore()
            .isEmpty()) {
            return GuideDisplayItemStacks.resolveOreStack(entry.ore());
        }
        return null;
    }
}
