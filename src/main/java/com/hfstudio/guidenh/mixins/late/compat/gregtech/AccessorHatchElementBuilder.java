package com.hfstudio.guidenh.mixins.late.compat.gregtech;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Predicate;

import gregtech.api.util.HatchElementBuilder;

@Mixin(value = HatchElementBuilder.class, remap = false)
public interface AccessorHatchElementBuilder {

    @Accessor("mHint")
    int guidenh$getHint();

    @Accessor("mReject")
    Predicate<?> guidenh$getReject();

    @Accessor("mCasingIndex")
    int guidenh$getCasingIndex();
}
