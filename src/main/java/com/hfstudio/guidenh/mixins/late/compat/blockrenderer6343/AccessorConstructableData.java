package com.hfstudio.guidenh.mixins.late.compat.blockrenderer6343;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;

import blockrenderer6343.client.utils.ConstructableData;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

@Mixin(value = ConstructableData.class, remap = false)
public interface AccessorConstructableData {

    @Accessor("constructableData")
    static Object2ObjectMap<IConstructable, ConstructableData> getConstructableDataMap() {
        throw new AssertionError();
    }
}
