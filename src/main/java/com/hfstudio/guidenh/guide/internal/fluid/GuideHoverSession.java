package com.hfstudio.guidenh.guide.internal.fluid;

import java.util.List;
import java.util.Objects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuideAnchor;
import com.hfstudio.guidenh.guide.indices.FluidIndex;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.search.GuideTargetSelection;

public class GuideHoverSession {

    private static final GuideTargetSelection EMPTY_SELECTION = new GuideTargetSelection("", List.of(), List.of());

    private GuiScreen screen;
    private Object source;
    private ItemStack item;
    private String fluidId;
    private NBTTagCompound fluidTag;
    private FluidStackSpec directSpec;
    private FluidStack directStack;
    private String locale;
    private long revision = Long.MIN_VALUE;
    private long resourceRevision = Long.MIN_VALUE;
    private GuideTargetSelection selection = EMPTY_SELECTION;
    private int ticksHeld;

    public GuideTargetSelection update(@Nullable ItemStack stack, @Nullable FluidStackSpec directFluid,
        @Nullable Object sourceKey) {
        if (stack == null && directFluid == null) {
            reset();
            return selection;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        long currentResourceRevision = FluidCatalog.INSTANCE.resourceRevision();
        boolean resourcesChanged = resourceRevision != currentResourceRevision;
        if (resourcesChanged || !Objects.equals(directSpec, directFluid)) {
            directSpec = directFluid;
            try {
                directStack = directFluid == null ? null : FluidStacks.resolve(directFluid);
            } catch (IllegalArgumentException exception) {
                directStack = null;
            }
            resourceRevision = currentResourceRevision;
        }
        FluidStack fluid = directFluid == null ? FluidStackResolver.getFluid(stack) : directStack;
        String currentFluid = fluid == null ? null
            : fluid.getFluid()
                .getName();
        String currentLocale = minecraft.getLanguageManager()
            .getCurrentLanguage()
            .getLanguageCode();
        long currentRevision = GuideRegistry.getNavigationRevision();
        for (var guide : GuideRegistry.getAll()) {
            try {
                currentRevision = currentRevision * 31 + guide.getIndex(FluidIndex.class)
                    .getRevision();
            } catch (IllegalArgumentException exception) {
                continue;
            }
        }
        boolean sourceChanged = screen != minecraft.currentScreen || !Objects.equals(source, sourceKey);
        boolean fluidChanged = !Objects.equals(fluidId, currentFluid);
        boolean itemChanged = !sameItem(item, stack);
        boolean fluidTagChanged = !Objects.equals(fluidTag, fluid == null ? null : fluid.tag);
        boolean localeChanged = !Objects.equals(locale, currentLocale);
        if (sourceChanged || fluidChanged
            || fluidTagChanged
            || itemChanged
            || resourcesChanged
            || localeChanged
            || revision != currentRevision) {
            GuideTargetSelection previous = selection;
            var fluids = currentFluid == null ? List.<GuideAnchor>of()
                : FluidGuideTargetResolver.findAll(currentFluid, GuideRegistry.getAll());
            var items = GuideContentTargetResolver.findItemTargets(stack);
            String title = fluid != null ? FluidStacks.displayName(fluid) : stack == null ? "" : stack.getDisplayName();
            selection = new GuideTargetSelection(title, fluids, items);
            if (sourceChanged || fluidChanged
                || fluidTagChanged
                || itemChanged
                || resourcesChanged
                || localeChanged
                || !selection.fluidTargets()
                    .equals(previous.fluidTargets())
                || !selection.itemTargets()
                    .equals(previous.itemTargets()))
                ticksHeld = 0;
            screen = minecraft.currentScreen;
            source = sourceKey;
            item = stack == null ? null : stack.copy();
            fluidId = currentFluid;
            fluidTag = fluid == null || fluid.tag == null ? null : (NBTTagCompound) fluid.tag.copy();
            locale = currentLocale;
            revision = currentRevision;
        }
        return selection;
    }

    public boolean advance(boolean held, int requiredTicks) {
        if (!held || selection.size() == 0) {
            ticksHeld = 0;
            return false;
        }
        return ticksHeld < requiredTicks && ++ticksHeld == requiredTicks;
    }

    public int ticksHeld() {
        return ticksHeld;
    }

    public void reset() {
        screen = null;
        source = null;
        item = null;
        fluidId = null;
        fluidTag = null;
        directSpec = null;
        directStack = null;
        locale = null;
        revision = Long.MIN_VALUE;
        resourceRevision = Long.MIN_VALUE;
        selection = EMPTY_SELECTION;
        ticksHeld = 0;
    }

    private static boolean sameItem(@Nullable ItemStack first, @Nullable ItemStack second) {
        if (first == second) return true;
        return first != null && second != null
            && first.getItem() == second.getItem()
            && first.getItemDamage() == second.getItemDamage()
            && ItemStack.areItemStackTagsEqual(first, second);
    }
}
