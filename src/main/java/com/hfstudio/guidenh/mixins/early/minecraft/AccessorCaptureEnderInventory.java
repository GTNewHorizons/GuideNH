package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.inventory.InventoryEnderChest;
import net.minecraft.tileentity.TileEntityEnderChest;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InventoryEnderChest.class)
public interface AccessorCaptureEnderInventory {

    @Accessor("associatedChest")
    TileEntityEnderChest guidenh$captureChest();
}
