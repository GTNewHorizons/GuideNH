package com.hfstudio.guidenh.guide.internal;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class GuideReloadListener implements IResourceManagerReloadListener {

    /**
     * Boot triggers redundant reloads (listener registration fires immediately, FML refreshes
     * again after loadComplete); skip them all and let FML's final refresh do the one real load.
     */
    private static volatile boolean bootComplete;

    /**
     * That first load runs on its own thread next to the rest of the startup resource reload.
     * Nothing reads the guides before the game has finished loading, so it is joined on the
     * first client tick. Its caches are still empty then, so clearing them makes no GL calls.
     */
    private static Thread bootLoad;
    private static boolean bootLoadStarted;

    public static void markBootComplete() {
        bootComplete = true;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        if (!bootComplete) {
            return;
        }
        if (startBootLoad(resourceManager)) {
            return;
        }
        awaitBootLoad();
        GuideLightweightReloadService.reloadGuides(resourceManager);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            awaitBootLoad();
        }
    }

    private static synchronized boolean startBootLoad(IResourceManager resourceManager) {
        if (bootLoadStarted) {
            return false;
        }
        bootLoadStarted = true;
        bootLoad = new Thread(() -> {
            try {
                GuideLightweightReloadService.reloadGuides(resourceManager);
            } catch (Throwable t) {
                GuideDebugLog.error("[GuideNH] [GuideReloadListener] Failed to load guides at startup", t);
            }
        }, "GuideNH Boot Reload");
        bootLoad.setDaemon(true);
        bootLoad.start();
        return true;
    }

    private static void awaitBootLoad() {
        Thread running;
        synchronized (GuideReloadListener.class) {
            bootLoadStarted = true;
            running = bootLoad;
            bootLoad = null;
        }
        if (running == null) {
            return;
        }
        boolean interrupted = false;
        while (running.isAlive()) {
            try {
                running.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread()
                .interrupt();
        }
    }
}
