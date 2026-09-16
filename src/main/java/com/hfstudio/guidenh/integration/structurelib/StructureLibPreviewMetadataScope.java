package com.hfstudio.guidenh.integration.structurelib;

import java.util.IdentityHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.common.blocks.ItemMachines;

public final class StructureLibPreviewMetadataScope implements AutoCloseable {

    private static final ThreadLocal<ScopeState> ACTIVE = new ThreadLocal<>();

    private final ScopeState previous;

    private StructureLibPreviewMetadataScope() {
        previous = ACTIVE.get();
        ACTIVE.set(new ScopeState());
    }

    public static StructureLibPreviewMetadataScope open() {
        return new StructureLibPreviewMetadataScope();
    }

    public static boolean isActive() {
        return ACTIVE.get() != null;
    }

    @Nullable
    public static IMetaTileEntity resolveMachine(ItemStack stack) {
        ScopeState state = ACTIVE.get();
        if (state == null) {
            return ItemMachines.getMetaTileEntity(stack);
        }
        if (state.machineCache.containsKey(stack)) {
            return state.machineCache.get(stack);
        }
        IMetaTileEntity machine = ItemMachines.getMetaTileEntity(stack);
        state.machineCache.put(stack, machine);
        return machine;
    }

    @Override
    public void close() {
        if (previous == null) {
            ACTIVE.remove();
        } else {
            ACTIVE.set(previous);
        }
    }

    private static final class ScopeState {

        private final Map<ItemStack, IMetaTileEntity> machineCache = new IdentityHashMap<>();
    }
}
