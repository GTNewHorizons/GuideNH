package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.hfstudio.guidenh.guide.internal.structure.GuideTextNbtCodec;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class FluidStacks {

    private static final Set<Class<?>> FAILED_NAME_PROVIDERS = ConcurrentHashMap.newKeySet();

    protected FluidStacks() {}

    public static FluidStack resolve(FluidStackSpec spec) {
        Fluid fluid = FluidRegistry.getFluid(spec.id());
        if (fluid == null) {
            throw new IllegalArgumentException("Fluid not found: " + spec.id());
        }
        FluidStack stack = new FluidStack(
            fluid,
            spec.amount() == null ? FluidContainerRegistry.BUCKET_VOLUME : spec.amount());
        if (spec.nbt() != null) {
            try {
                stack.tag = GuideTextNbtCodec.readTextSafeCompound(spec.nbt());
            } catch (Exception exception) {
                throw new IllegalArgumentException("Invalid fluid NBT: " + exception.getMessage(), exception);
            }
        }
        return stack;
    }

    public static String displayName(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        try {
            String name = fluid.getLocalizedName(stack);
            return name == null || name.isEmpty() ? fluid.getName() : name;
        } catch (RuntimeException | LinkageError exception) {
            if (FAILED_NAME_PROVIDERS.add(fluid.getClass())) {
                GuideDebugLog.warnAlways(
                    "[GuideNH] Fluid name provider {} failed",
                    fluid.getClass()
                        .getName(),
                    exception);
            }
            return fluid.getName();
        }
    }
}
