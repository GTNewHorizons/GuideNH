package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.entity.IMerchant;
import net.minecraft.inventory.ContainerMerchant;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerMerchant.class)
public interface AccessorCaptureMerchantContainer {

    @Accessor("theMerchant")
    IMerchant guidenh$captureMerchant();
}
