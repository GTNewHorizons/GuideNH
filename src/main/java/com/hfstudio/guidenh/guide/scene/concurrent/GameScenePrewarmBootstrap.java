package com.hfstudio.guidenh.guide.scene.concurrent;

import java.util.concurrent.CompletableFuture;

import com.hfstudio.guidenh.guide.scene.preview.StructureLibDefinitionCache;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import lombok.Getter;

/**
 * Universal GameScene prewarm system that preloads and caches all scene data including StructureLib.
 * Runs in background after resource reload without blocking the game.
 */
@Getter
public class GameScenePrewarmBootstrap {

    private static final GameScenePrewarmBootstrap INSTANCE = new GameScenePrewarmBootstrap();

    private volatile boolean isPrewarming = false;

    private GameScenePrewarmBootstrap() {}

    public static GameScenePrewarmBootstrap getInstance() {
        return INSTANCE;
    }

    public void scheduleReloadPrewarm() {
        if (isPrewarming) {
            GuideDebugLog.infoAlways("[GameScenePrewarmBootstrap] Prewarm already in progress, skipping");
            return;
        }

        isPrewarming = true;
        CompletableFuture.runAsync(
            this::runPrewarm,
            GameSceneConcurrentManager.getInstance()
                .getAnalysisExecutor())
            .whenComplete((v, throwable) -> {
                isPrewarming = false;
                if (throwable != null) {
                    GuideDebugLog.error("[GameScenePrewarmBootstrap] Prewarm failed", throwable);
                }
            });
    }

    private void runPrewarm() {
        StructureLibDefinitionCache.getInstance()
            .refresh();
    }
}
