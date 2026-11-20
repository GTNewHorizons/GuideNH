package com.hfstudio.guidenh.integration.api.binding;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public record BindingIcon(@Nullable String itemId, int meta, @Nullable String nbt, @Nullable ResourceLocation texture) {

    public BindingIcon {
        if (itemId != null && texture != null)
            throw new IllegalArgumentException("A binding icon accepts an item or a texture");
    }
}
