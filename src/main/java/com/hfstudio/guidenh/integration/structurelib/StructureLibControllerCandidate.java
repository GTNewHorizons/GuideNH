package com.hfstudio.guidenh.integration.structurelib;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class StructureLibControllerCandidate {

    @Getter
    private final String blockId;
    @Getter
    private final Block block;
    @Getter
    private final int meta;
    @Nullable
    private final ItemStack displayStack;

    public StructureLibControllerCandidate(String blockId, Block block, int meta, @Nullable ItemStack displayStack) {
        this.blockId = blockId;
        this.block = block;
        this.meta = meta;
        this.displayStack = displayStack != null ? displayStack.copy() : null;
    }

    @Nullable
    public ItemStack getDisplayStack() {
        return displayStack != null ? displayStack.copy() : null;
    }

    public String getControllerArgument() {
        return blockId + ":" + meta;
    }
}
