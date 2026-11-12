package com.hfstudio.guidenh.guide.internal;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;

import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotationRenderer;

public class GuideReloadListener implements IResourceManagerReloadListener {

    /**
     * Boot triggers redundant reloads (listener registration fires immediately, FML refreshes
     * again after loadComplete); skip them all and let FML's final refresh do the one real load.
     */
    private static volatile boolean bootComplete;

    public static void markBootComplete() {
        bootComplete = true;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        if (!bootComplete) {
            return;
        }
        InWorldAnnotationRenderer.release();
        GuideLightweightReloadService.reloadGuides(resourceManager);
    }
}
