package com.hfstudio.guidenh.integration.structurelib;

import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;

import com.gtnewhorizon.structurelib.fluid.IFluidSource;

/**
 * A fluid source that always holds as much of the requested fluid as asked for.
 * <p>
 * Guide book scenes are built with a creative item source, and this is its fluid counterpart: it lets a scene fill the
 * positions a multiblock wants a fluid at, e.g. the water an ore washing plant needs, without anybody having to carry
 * that fluid.
 */
public class CreativeFluidSource implements IFluidSource {

    public static final CreativeFluidSource instance = new CreativeFluidSource();

    @Nonnull
    @Override
    public FluidStack take(FluidStack resource, boolean simulate) {
        if (resource == null || resource.getFluid() == null) throw new IllegalArgumentException();
        return new FluidStack(resource, resource.amount);
    }
}
