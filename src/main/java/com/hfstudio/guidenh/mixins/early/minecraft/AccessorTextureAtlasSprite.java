package com.hfstudio.guidenh.mixins.early.minecraft;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TextureAtlasSprite.class)
public interface AccessorTextureAtlasSprite {

    @Accessor("animationMetadata")
    AnimationMetadataSection guidenh$getAnimationMetadata();

    @Accessor("frameCounter")
    int guidenh$getFrameCounter();

    @Accessor("frameCounter")
    void guidenh$setFrameCounter(int frameCounter);

    @Accessor("tickCounter")
    int guidenh$getTickCounter();

    @Accessor("tickCounter")
    void guidenh$setTickCounter(int tickCounter);
}
