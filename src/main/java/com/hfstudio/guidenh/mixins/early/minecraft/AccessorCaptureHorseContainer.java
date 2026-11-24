package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.inventory.ContainerHorseInventory;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerHorseInventory.class)
public interface AccessorCaptureHorseContainer {

    @Accessor("theHorse")
    EntityHorse guidenh$captureHorse();
}
