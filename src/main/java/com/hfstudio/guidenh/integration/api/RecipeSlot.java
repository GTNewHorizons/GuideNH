package com.hfstudio.guidenh.integration.api;

import java.util.List;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public class RecipeSlot {

    private final int x;
    private final int y;
    private final List<ItemStack> stacks;
    private final @Nullable ItemStack currentStack;

    public RecipeSlot(int x, int y, List<ItemStack> stacks) {
        this(x, y, stacks, null);
    }

    public RecipeSlot(int x, int y, List<ItemStack> stacks, @Nullable ItemStack currentStack) {
        this.x = x;
        this.y = y;
        this.stacks = stacks == null ? List.of() : List.copyOf(stacks);
        this.currentStack = currentStack;
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
}
