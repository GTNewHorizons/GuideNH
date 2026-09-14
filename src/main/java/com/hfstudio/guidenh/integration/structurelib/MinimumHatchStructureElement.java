package com.hfstudio.guidenh.integration.structurelib;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.gtnewhorizon.structurelib.structure.AutoPlaceEnvironment;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.hfstudio.guidenh.integration.gregtech.GregTechHelpers;

/** Delegates a GT hatch element while preserving casing fallbacks for optional positions. */
public final class MinimumHatchStructureElement<T> implements IStructureElement<T> {

    private final IStructureElement<T> delegate;
    private final boolean hasMinimumRequirement;
    private final int casingIndex;

    public MinimumHatchStructureElement(IStructureElement<T> delegate, boolean hasMinimumRequirement, int casingIndex) {
        this.delegate = delegate;
        this.hasMinimumRequirement = hasMinimumRequirement;
        this.casingIndex = casingIndex;
    }

    @Override
    public boolean check(T context, World world, int x, int y, int z) {
        boolean valid = delegate.check(context, world, x, y, z);
        if (valid) {
            GregTechHelpers.updatePreviewHatchTexture(world, x, y, z, casingIndex);
        }
        return valid;
    }

    @Override
    public boolean couldBeValid(T context, World world, int x, int y, int z, ItemStack trigger) {
        return delegate.couldBeValid(context, world, x, y, z, trigger);
    }

    @Nullable
    @Override
    public List<String> getDescription(T context) {
        return delegate.getDescription(context);
    }

    @Override
    public boolean spawnHint(T context, World world, int x, int y, int z, ItemStack trigger) {
        return delegate.spawnHint(context, world, x, y, z, trigger);
    }

    @Override
    public boolean placeBlock(T context, World world, int x, int y, int z, ItemStack trigger) {
        if (useFallback()) return false;
        boolean placed = delegate.placeBlock(context, world, x, y, z, trigger);
        if (placed) {
            GregTechHelpers.updatePreviewHatchTexture(world, x, y, z, casingIndex);
        }
        return placed;
    }

    @Deprecated
    @Override
    public PlaceResult survivalPlaceBlock(T context, World world, int x, int y, int z, ItemStack trigger,
        IItemSource source, EntityPlayerMP actor, Consumer<IChatComponent> chatter) {
        if (useFallback()) return PlaceResult.REJECT;
        PlaceResult result = delegate.survivalPlaceBlock(context, world, x, y, z, trigger, source, actor, chatter);
        if (result == PlaceResult.ACCEPT || result == PlaceResult.ACCEPT_STOP || result == PlaceResult.SKIP) {
            GregTechHelpers.updatePreviewHatchTexture(world, x, y, z, casingIndex);
        }
        return result;
    }

    @Override
    public PlaceResult survivalPlaceBlock(T context, World world, int x, int y, int z, ItemStack trigger,
        AutoPlaceEnvironment environment) {
        if (useFallback()) return PlaceResult.REJECT;
        PlaceResult result = delegate.survivalPlaceBlock(context, world, x, y, z, trigger, environment);
        if (result == PlaceResult.ACCEPT || result == PlaceResult.ACCEPT_STOP || result == PlaceResult.SKIP) {
            GregTechHelpers.updatePreviewHatchTexture(world, x, y, z, casingIndex);
        }
        return result;
    }

    @Nullable
    @Override
    public BlocksToPlace getBlocksToPlace(T context, World world, int x, int y, int z, ItemStack trigger,
        AutoPlaceEnvironment environment) {
        return delegate.getBlocksToPlace(context, world, x, y, z, trigger, environment);
    }

    @Override
    public int getStepA() {
        return delegate.getStepA();
    }

    @Override
    public int getStepB() {
        return delegate.getStepB();
    }

    @Override
    public int getStepC() {
        return delegate.getStepC();
    }

    @Override
    public boolean resetA() {
        return delegate.resetA();
    }

    @Override
    public boolean resetB() {
        return delegate.resetB();
    }

    @Override
    public boolean resetC() {
        return delegate.resetC();
    }

    @Override
    public boolean isNavigating() {
        return delegate.isNavigating();
    }

    private boolean useFallback() {
        return StructureLibMinimumHatchPlacement.shouldUseFallback(hasMinimumRequirement);
    }
}
