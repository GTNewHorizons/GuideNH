package com.hfstudio.guidenh.mixins.late.offscreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.sbancuz.offscreen.window.Window;

@Mixin(value = Window.class, remap = false)
public interface AccessorOffscreenWindow {

    @Accessor("focusState")
    boolean guidenh$isFocused();
}
