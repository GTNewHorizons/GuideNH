package com.hfstudio.guidenh.guide.scene;

import net.minecraft.util.ResourceLocation;

/**
 * Records the placement configuration for a single {@code <ImportStructure>} element.
 * Symmetric to {@link StructureLibSceneBinding} — the compiler registers placement configs,
 * and {@link LytGuidebookScene#build()} uses them to place blocks.
 */
public class SnbtPlacement {

    private final ResourceLocation src;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;
    private final boolean formed;

    public SnbtPlacement(ResourceLocation src, int offsetX, int offsetY, int offsetZ, boolean formed) {
        this.src = src;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.formed = formed;
    }

    public ResourceLocation getSrc() {
        return src;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getOffsetZ() {
        return offsetZ;
    }

    public boolean isFormed() {
        return formed;
    }
}
