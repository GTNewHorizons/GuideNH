package com.hfstudio.guidenh.integration.materiallib;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.ruling_0.materiallib.api.StackResolver;

/// Resolves the `ml:<MaterialName>:<shapeToken>` item references guide pages carry, so a page keeps its target
/// across sessions that renumber MaterialLib's item metadata.
///
/// Sole holder of MaterialLib types in GuideNH: every call site gates on [com.hfstudio.guidenh.integration.Mods]
/// `.MaterialLib.isModLoaded()` before reaching this class, so it never classloads without MaterialLib present.
public final class MaterialLibItemRefs {

    private static final int CACHE_MAX = 512;
    private static final Map<String, ItemStack> RESOLVED = Collections
        .synchronizedMap(new LinkedHashMap<String, ItemStack>(64, 0.75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ItemStack> eldest) {
                return size() > CACHE_MAX;
            }
        });
    private static final Set<String> MISSES = Collections.synchronizedSet(new HashSet<>());
    private static boolean warnedTooEarly;

    private MaterialLibItemRefs() {}

    /// A fresh single-item stack of the named material in the named shape, or null when either name matches
    /// nothing. [StackResolver] logs the miss itself, so callers must not log it again.
    @Nullable
    public static ItemStack getStack(String materialName, String shapeToken) {
        String key = materialName + ":" + shapeToken;
        if (MISSES.contains(key)) {
            return null;
        }
        ItemStack cached = RESOLVED.get(key);
        if (cached != null) {
            return cached.copy();
        }

        ItemStack stack;
        try {
            stack = StackResolver.getStack(materialName, shapeToken, 1);
        } catch (Throwable t) {
            // Shapes only become resolvable during MaterialLib's preInit; the outcome stays uncached so a
            // lookup that ran ahead of it succeeds on the next attempt.
            if (!warnedTooEarly) {
                warnedTooEarly = true;
                GuideDebugLog.warnAlways("[GuideNH] [MaterialLib] Cannot resolve '{}' yet", key, t);
            }
            return null;
        }

        if (stack == null || stack.getItem() == null) {
            MISSES.add(key);
            return null;
        }
        RESOLVED.put(key, stack);
        return stack.copy();
    }
}
