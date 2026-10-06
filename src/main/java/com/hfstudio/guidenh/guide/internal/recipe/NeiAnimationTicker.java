package com.hfstudio.guidenh.guide.internal.recipe;

import java.util.WeakHashMap;

import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.nei.NeiRecipePermutationController;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Drives {@code IRecipeHandler.onUpdate()} (used by some handlers for animation, e.g. the arrow
 * progress bar) once per client tick. Registered lazily on the first recipe box render and kept
 * alive as long as the handler object remains referenced anywhere. Handlers are held via
 * {@link WeakHashMap} so unused ones are garbage-collected with their entries.
 */
public class NeiAnimationTicker {

    public static final WeakHashMap<Object, Boolean> TRACKED = new WeakHashMap<>();
    public static boolean registered;

    private NeiAnimationTicker() {}

    private static final WeakHashMap<Object, Boolean> UPDATE_CAPABILITY = new WeakHashMap<>();

    public static void ensureUpdating(Object handler) {
        if (handler == null) return;
        synchronized (TRACKED) {
            Boolean cachedCapability = UPDATE_CAPABILITY.get(handler);
            if (cachedCapability == null) {
                cachedCapability = GuideNhIntegrationRegistry.global()
                    .canUpdateRecipeAnimation(handler);
                UPDATE_CAPABILITY.put(handler, cachedCapability);
            }
            if (!registered) {
                registered = true;
                FMLCommonHandler.instance()
                    .bus()
                    .register(new NeiAnimationTicker());
            }
            if (cachedCapability) {
                TRACKED.put(handler, Boolean.TRUE);
            }
        }
    }

    public static void clear() {
        synchronized (TRACKED) {
            TRACKED.clear();
            UPDATE_CAPABILITY.clear();
        }
        NeiRecipePermutationController.clear();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        NeiRecipePermutationController.tick();
        Object[] snapshot;
        synchronized (TRACKED) {
            if (TRACKED.isEmpty()) return;
            snapshot = TRACKED.keySet()
                .toArray();
        }
        GuideNhIntegrationRegistry registry = GuideNhIntegrationRegistry.global();
        for (Object o : snapshot) {
            registry.updateRecipeAnimation(o);
        }
    }
}
