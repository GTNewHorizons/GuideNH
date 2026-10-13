package com.hfstudio.guidenh.mixins.late.compat.gregtech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

import gregtech.common.blocks.ItemMachines;

@Mixin(value = ItemMachines.class, remap = false)
public abstract class MixinItemMachinesPreview {

    @Inject(method = "placeBlockAt", at = @At("RETURN"))
    private void guidenh$ensurePreviewMachineBinding(ItemStack stack, EntityPlayer player, World world, int x, int y,
        int z, int side, float hitX, float hitY, float hitZ, int metadata, CallbackInfoReturnable<Boolean> callback) {
        if (Boolean.TRUE.equals(callback.getReturnValue())) {
            GregTechHelpers.ensurePreviewMachineTile(world, x, y, z, stack);
        }
    }
}
