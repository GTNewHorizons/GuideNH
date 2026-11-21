package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class LytItemGrid extends LytIconGrid {

    public void addItem(Item item) {
        addItem(new ItemStack(item));
    }

    public void addItem(ItemStack stack) {
        var slot = new LytSlot(stack);
        addSlot(slot);
    }

    public void addItems(List<ItemStack> stacks) {
        var slot = new LytSlot(stacks);
        addSlot(slot);
    }
}
