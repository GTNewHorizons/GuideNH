package com.hfstudio.guidenh.guide.scene.element;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Pre-parsed SNBT cache shared between AsyncWorker (writer) and main thread (reader).
 *
 * <p>
 * The cache is bounded: every structure a scene imports used to be kept for the rest of the session, so a
 * long session accumulated the parsed NBT of every structure it had ever shown. The oldest entries are
 * dropped once the bound is reached, which only costs a re-parse.
 */
public class SnbtPreParseCache {

    /** Structures kept at once, well above what a single scene imports. */
    private static final int MAX_ENTRIES = 64;

    private static final ConcurrentHashMap<ResourceLocation, NBTTagCompound> cache = new ConcurrentHashMap<>();
    /**
     * Insertion order of the cached ids, guarded by itself. A {@link LinkedHashMap} in access order would
     * need the reads to write, so the reader path stays lock-free and only insertion is recorded here.
     */
    private static final LinkedHashMap<ResourceLocation, Boolean> insertionOrder = new LinkedHashMap<>();

    public static void put(ResourceLocation id, NBTTagCompound root) {
        if (id == null || root == null) {
            return;
        }
        cache.put(id, root);
        ResourceLocation evicted;
        synchronized (insertionOrder) {
            insertionOrder.remove(id);
            insertionOrder.put(id, Boolean.TRUE);
            evicted = insertionOrder.size() > MAX_ENTRIES ? insertionOrder.keySet()
                .iterator()
                .next() : null;
            if (evicted != null) {
                insertionOrder.remove(evicted);
            }
        }
        if (evicted != null) {
            cache.remove(evicted);
        }
    }

    @Nullable
    public static NBTTagCompound get(ResourceLocation id) {
        return id != null ? cache.get(id) : null;
    }

    public static void clear() {
        cache.clear();
        synchronized (insertionOrder) {
            insertionOrder.clear();
        }
    }
}
