package com.hfstudio.guidenh.mixins.late.compat.gregtech;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

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
