package com.hfstudio.guidenh.mixins.early.minecraft;

import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureMap.class)
public interface AccessorTextureMap {

    @Accessor("listAnimatedSprites")
    List<TextureAtlasSprite> guidenh$getAnimatedSprites();
}
