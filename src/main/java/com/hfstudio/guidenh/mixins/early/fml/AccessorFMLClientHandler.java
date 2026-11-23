package com.hfstudio.guidenh.mixins.early.fml;

import java.util.List;

import net.minecraft.client.resources.IResourcePack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import cpw.mods.fml.client.FMLClientHandler;

@Mixin(value = FMLClientHandler.class, remap = false)
public interface AccessorFMLClientHandler {

    @Accessor("resourcePackList")
    List<IResourcePack> guidenh$getResourcePackList();
}
