package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class CapturePreviewInventories {

    private CapturePreviewInventories() {}

    public static InventoryPlayer player() {
        InventoryPlayer source = Minecraft.getMinecraft().thePlayer.inventory;
        InventoryPlayer snapshot = new InventoryPlayer(Minecraft.getMinecraft().thePlayer);
        for (int slot = 0; slot < source.getSizeInventory(); slot++) {
            ItemStack stack = source.getStackInSlot(slot);
            snapshot.setInventorySlotContents(slot, stack == null ? null : stack.copy());
        }
        snapshot.currentItem = source.currentItem;
        return snapshot;
    }
}
