package com.hfstudio.guidenh.guide.scene.element;

import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Pre-parsed SNBT cache shared between AsyncWorker (writer) and main thread (reader).
 */
public class SnbtPreParseCache {

    private static final ConcurrentHashMap<ResourceLocation, NBTTagCompound> cache = new ConcurrentHashMap<>();

    public static void put(ResourceLocation id, NBTTagCompound root) {
        cache.put(id, root);
    }

    @Nullable
    public static NBTTagCompound get(ResourceLocation id) {
        return cache.get(id);
    }

    public static void clear() {
        cache.clear();
    }
}
