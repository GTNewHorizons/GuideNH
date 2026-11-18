package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;

public class FluidStackResolver {

    private static final Set<Class<?>> FAILED_PROVIDERS = ConcurrentHashMap.newKeySet();

    protected FluidStackResolver() {}

    public static @Nullable FluidStack getFluid(@Nullable ItemStack source) {
        if (source == null || source.getItem() == null) return null;
        ItemStack stack = source.copy();
        FluidStack fluid = null;
        try {
            fluid = FluidContainerRegistry.getFluidForFilledItem(stack);
            if (fluid == null && stack.getItem() instanceof IFluidContainerItem container)
                fluid = container.getFluid(stack);
        } catch (RuntimeException | LinkageError exception) {
            warnFailure(source.getItem(), exception);
        }
        if (fluid != null && fluid.getFluid() != null) return fluid.copy();
        for (var extractor : GuideNhClientIntegrationRegistry.global()
            .fluidStackExtractors()) {
            try {
                fluid = extractor.getFluid(source.copy());
                if (fluid != null && fluid.getFluid() != null) return fluid.copy();
            } catch (RuntimeException | LinkageError exception) {
                warnFailure(extractor, exception);
            }
        }
        return null;
    }

    public static @Nullable FluidStack getDisplayedFluid(@Nullable ItemStack source) {
        if (source == null || source.getItem() == null) return null;
        for (var adapter : GuideNhClientIntegrationRegistry.global()
            .fluidDisplayAdapters()) {
            try {
                FluidStack fluid = adapter.getDisplayedFluid(source.copy());
                if (fluid != null && fluid.getFluid() != null) return fluid.copy();
            } catch (RuntimeException | LinkageError exception) {
                warnFailure(adapter, exception);
            }
        }
        return null;
    }

    public static @Nullable FluidStack getHoveredFluid(GuiScreen screen, int x, int y) {
        for (var provider : GuideNhClientIntegrationRegistry.global()
            .hoveredFluidProviders()) {
            try {
                FluidStack fluid = provider.getHoveredFluid(screen, x, y);
                if (fluid != null && fluid.getFluid() != null) return fluid.copy();
            } catch (RuntimeException | LinkageError exception) {
                warnFailure(provider, exception);
            }
        }
        return null;
    }

    private static void warnFailure(Object provider, Throwable exception) {
        if (FAILED_PROVIDERS.add(provider.getClass())) {
            GuideDebugLog.warnAlways(
                "[GuideNH] Fluid provider {} failed",
                provider.getClass()
                    .getName(),
                exception);
        }
    }
}
