package com.hfstudio.guidenh.mixins.late.compat.mui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizons.modularui.api.screen.ModularWindow;

@Mixin(value = ModularWindow.class, remap = false)
public interface AccessorCaptureModularWindow {

    @Accessor("translateX")
    float guidenh$getCaptureTranslateX();

    @Accessor("translateY")
    float guidenh$getCaptureTranslateY();

    @Accessor("rotation")
    float guidenh$getCaptureRotation();
}
