package com.hfstudio.guidenh.mixins.early.minecraft;

import java.util.Map;

import net.minecraft.client.renderer.texture.TextureManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureManager.class)
public interface AccessorTextureManager {

    @Accessor("mapTextureObjects")
    Map guidenh$getMapTextureObjects();
}
