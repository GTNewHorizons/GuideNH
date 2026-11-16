package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

public class RecipeSlot {

    private final int x;
    private final int y;
    private final List<ItemStack> stacks;
    private final @Nullable ItemStack currentStack;
    private final List<FluidStackSpec> fluids;
    private final @Nullable FluidStackSpec currentFluid;

    public RecipeSlot(int x, int y, List<ItemStack> stacks) {
        this(x, y, stacks, null);
    }

    public RecipeSlot(int x, int y, List<ItemStack> stacks, @Nullable ItemStack currentStack) {
        this(x, y, stacks, currentStack, List.of(), null);
    }

    public RecipeSlot(int x, int y, List<ItemStack> stacks, @Nullable ItemStack currentStack,
        List<FluidStackSpec> fluids, @Nullable FluidStackSpec currentFluid) {
        this.x = x;
        this.y = y;
        this.stacks = stacks == null ? List.of() : List.copyOf(stacks);
        this.currentStack = currentStack;
        this.fluids = fluids == null ? List.of() : List.copyOf(fluids);
        this.currentFluid = currentFluid;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public List<ItemStack> stacks() {
        return stacks;
    }

    @Nullable
    public ItemStack currentStack() {
        return currentStack;
    }

    public List<FluidStackSpec> fluids() {
        return fluids;
    }

    public @Nullable FluidStackSpec currentFluid() {
        if (currentStack != null) return currentFluid;
        return currentFluid != null ? currentFluid : fluids.isEmpty() ? null : fluids.getFirst();
    }
}
