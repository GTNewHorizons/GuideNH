package com.hfstudio.guidenh.guide.internal.fluid;

import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;

public record FluidStackSpec(String id, @Nullable Integer amount, @Nullable String nbt) {

    public FluidStackSpec {
        if (id == null || id.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Fluid id must not be empty");
        }
        id = id.trim();
        if (amount != null && amount < 0) {
            throw new IllegalArgumentException("Fluid amount must be an integer");
        }
        nbt = nbt == null || nbt.trim()
            .isEmpty() ? null : nbt.trim();
    }

    public FluidStackSpec(String id) {
        this(id, null, null);
    }

    public static FluidStackSpec fromStack(FluidStack stack, boolean includeAmount) {
        if (stack == null || stack.getFluid() == null) {
            throw new IllegalArgumentException("Fluid stack must contain a registered fluid");
        }
        return new FluidStackSpec(
            stack.getFluid()
                .getName(),
            includeAmount ? Math.max(0, stack.amount) : null,
            stack.tag == null ? null : GuideTextNbtCodec.writeTextSafeCompound(stack.tag));
    }
}
