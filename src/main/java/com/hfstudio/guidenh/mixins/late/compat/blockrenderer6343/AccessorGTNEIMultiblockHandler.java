package com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.integration.gregtech.GTNEIMultiblockHandler;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@Mixin(value = GTNEIMultiblockHandler.class, remap = false)
public interface AccessorGTNEIMultiblockHandler {

    @Accessor("multiBlockComponents")
    static Long2ObjectMap<ObjectSet<IConstructable>> getMultiBlockComponents() {
        throw new AssertionError();
    }
}
