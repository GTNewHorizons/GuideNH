package com.hfstudio.guidenh.mixins.late.compat.nei;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import codechicken.nei.LayoutManager;
import codechicken.nei.Widget;

@Mixin(value = LayoutManager.class, remap = false)
public interface AccessorCaptureNeiLayout {

    @Accessor("drawWidgets")
    static Set<Widget> guidenh$getCaptureWidgets() {
        throw new UnsupportedOperationException();
    }
}
