package com.hfstudio.guidenh.capture.server;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.inventory.Slot;

public class CaptureContainerInventories {

    private CaptureContainerInventories() {}

    public static boolean contains(Container container, IInventory inventory) {
        if (container instanceof ContainerChest chest) {
            IInventory lower = chest.getLowerChestInventory();
            if (lower == inventory || lower instanceof InventoryLargeChest large && large.isPartOfLargeChest(inventory))
                return true;
        }
        for (Object value : container.inventorySlots) {
            if (((Slot) value).inventory == inventory) return true;
        }
        return false;
    }
}
