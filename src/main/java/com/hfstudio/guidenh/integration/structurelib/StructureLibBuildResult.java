package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;

public record StructureLibBuildResult(List<PlacedBlock> blocks, boolean success, @Nullable String error,
    @Nullable StructureLibSceneMetadata metadata) {

    public static final StructureLibBuildResult EMPTY_FAILURE = new StructureLibBuildResult(
        List.of(),
        false,
        null,
        null);

    public StructureLibBuildResult(List<PlacedBlock> blocks, boolean success, @Nullable String error) {
        this(blocks, success, error, null);
    }

    public record PlacedBlock(int x, int y, int z, Block block, int meta, @Nullable NBTTagCompound tileTag,
        String blockId) {}
}
