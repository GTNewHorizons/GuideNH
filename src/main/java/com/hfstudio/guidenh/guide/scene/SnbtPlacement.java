package com.hfstudio.guidenh.guide.scene;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import lombok.Getter;

/**
 * Records the placement configuration for a single {@code <ImportStructure>} element.
 * Symmetric to {@link StructureLibSceneBinding} — the compiler registers placement configs,
 * and {@link LytGuidebookScene#build()} uses them to place blocks.
 *
 * <p>
 * The compiler has already parsed the structure's NBT by the time it registers a placement, so the parsed
 * form is carried here. A rebuild then places the structure from this, instead of looking it up in a cache
 * that may have evicted it - which used to leave the structure missing from the scene after a slider or
 * layer change.
 */
@Getter
public class SnbtPlacement {

    private final ResourceLocation src;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final boolean formed;
    /** The parsed structure, or null when the compiler could not read it and reported that instead. */
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
