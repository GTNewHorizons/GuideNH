package com.hfstudio.guidenh.guide.internal.host.scripts;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.FluidEntry;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemEntry;
import com.hfstudio.guidenh.guide.compiler.tags.ItemGridCompiler.ItemGridPlaceholder;
import com.hfstudio.guidenh.guide.document.block.LytFluidSlot;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
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
        if (event.type() == EventType.MOUNT) {
            ItemGridPlaceholder ph = LytFlowInlineBlock.unwrapPlaceholder(node, ItemGridPlaceholder.class);
            if (ph == null) return;
            LytItemGrid grid = new LytItemGrid();
            int resolved = 0;
            for (var entry : ph.getEntries()) {
                if (entry instanceof ItemEntry item) {
                    ItemStack stack = item.id() == null ? null
                        : resolveItemId(
                            item.id()
                                .trim());
                    if (stack == null && item.ore() != null) stack = GuideDisplayItemStacks.resolveOreStack(item.ore());
                    if (stack != null) {
                        grid.addItem(stack);
                        resolved++;
                    }
                } else if (entry instanceof FluidEntry fluid) {
                    try {
                        LytFluidSlot slot = new LytFluidSlot(fluid.fluid());
                        slot.setShowTooltip(fluid.showTooltip());
                        grid.addSlot(slot);
                        resolved++;
                    } catch (IllegalArgumentException exception) {
                        ctx.replace(LytParagraph.error("[FluidIcon] " + exception.getMessage()));
                        return;
                    }
                }
            }
            if (resolved == 0) {
                ctx.replace(LytParagraph.error("[ItemGrid] No icons to display"));
            } else {
                ctx.replace(grid);
            }
        }
    }

    private static ItemStack resolveItemId(String itemId) {
        return GuideDisplayItemStacks.resolveItemStack(itemId, "minecraft");
    }
}
