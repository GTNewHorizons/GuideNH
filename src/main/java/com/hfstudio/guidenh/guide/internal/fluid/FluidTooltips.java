package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.api.client.FluidTooltipProvider;
import com.hfstudio.guidenh.integration.api.client.GuideNhClientIntegrationRegistry;

public class FluidTooltips {

    private static final Set<Class<?>> FAILED_PROVIDERS = ConcurrentHashMap.newKeySet();

    protected FluidTooltips() {}

    public static List<String> lines(FluidStackSpec spec, FluidStack stack, boolean advanced) {
        List<String> lines = new ArrayList<>(8);
        lines.add(FluidStacks.displayName(stack));
        for (FluidTooltipProvider provider : GuideNhClientIntegrationRegistry.global()
            .fluidTooltipProviders()) {
            List<String> extra = new ArrayList<>(4);
            try {
                provider.appendTooltip(stack.copy(), extra);
                for (String line : extra) {
                    if (line != null) lines.add(EnumChatFormatting.GRAY + line + EnumChatFormatting.RESET);
                }
            } catch (RuntimeException | LinkageError exception) {
                reportFailure(provider.getClass(), exception);
            }
        }
        if (advanced) lines.add(
            EnumChatFormatting.DARK_GRAY
                + StatCollector.translateToLocalFormatted("guideme.fluid.tooltip.registry", spec.id())
                + EnumChatFormatting.RESET);
        if (spec.amount() != null) lines.add(
            EnumChatFormatting.BLUE + StatCollector
                .translateToLocalFormatted("guideme.fluid.tooltip.amount", NumberFormatUtil.formatFluid(stack.amount))
                + EnumChatFormatting.RESET);
        var fluid = stack.getFluid();
        try {
            lines.add(
                EnumChatFormatting.RED + StatCollector.translateToLocalFormatted(
                    "guideme.fluid.tooltip.temperature",
                    NumberFormatUtil.formatNumber(fluid.getTemperature(stack))) + EnumChatFormatting.RESET);
        } catch (RuntimeException | LinkageError exception) {
            reportFailure(fluid.getClass(), exception);
        }
        try {
            String state = StatCollector.translateToLocal(
                fluid.isGaseous(stack) ? "guideme.fluid.tooltip.state.gas" : "guideme.fluid.tooltip.state.liquid");
            lines.add(
                EnumChatFormatting.GREEN + StatCollector.translateToLocalFormatted("guideme.fluid.tooltip.state", state)
                    + EnumChatFormatting.RESET);
        } catch (RuntimeException | LinkageError exception) {
            reportFailure(fluid.getClass(), exception);
        }
        return lines;
    }

    private static void reportFailure(Class<?> provider, Throwable exception) {
        if (FAILED_PROVIDERS.add(provider)) {
            GuideDebugLog.warnAlways("[GuideNH] Fluid tooltip provider {} failed", provider.getName(), exception);
        }
    }
}
