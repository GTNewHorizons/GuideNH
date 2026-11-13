package com.hfstudio.guidenh.guide.scene;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import lombok.Getter;

/**
 * ImportStructure placement configuration registered by the compiler and applied during scene builds. Owns
 * the parsed NBT so rebuilds do not depend on the asset cache retaining the structure.
 */
@Getter
public class SnbtPlacement {

    private final ResourceLocation src;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final boolean formed;
    private final NBTTagCompound root;

    public SnbtPlacement(ResourceLocation src, int offsetX, int offsetY, int offsetZ, boolean formed,
        NBTTagCompound root) {
        this.src = src;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.formed = formed;
        this.root = root;
    }

}
