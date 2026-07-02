package com.hfstudio.guidenh.guide.scene.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.guide.scene.element.ImportStructureLibElementCompiler;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;

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

        List<PrewarmTask> tasks = collectPrewarmTasks();
        if (tasks.isEmpty()) {
            return;
        }

        isPrewarming = true;
        CompletableFuture.runAsync(
            () -> runPrewarm(tasks),
            GameSceneConcurrentManager.getInstance()
                .getAnalysisExecutor())
            .whenComplete((v, throwable) -> {
                isPrewarming = false;
                if (throwable != null) {
                    GuideDebugLog.error("[GameScenePrewarmBootstrap] Prewarm failed", throwable);
                }
            });
    }

    private List<PrewarmTask> collectPrewarmTasks() {
        ArrayList<PrewarmTask> tasks = new ArrayList<>();

        for (MutableGuide guide : GuideRegistry.getAll()) {
            for (ParsedGuidePage page : guide.getPages()) {
                if (!SceneTagCompiler.likelyHasHeavySceneWork(page)) {
                    continue;
                }
                collectTasksFromPage(page, tasks);
            }
        }

        return tasks;
    }

    private void collectTasksFromPage(ParsedGuidePage page, List<PrewarmTask> tasks) {
        MdAstRoot astRoot;
        try {
            astRoot = page.getAstRoot();
        } catch (Throwable throwable) {
            GuideDebugLog.warn("[GameScenePrewarmBootstrap] Failed to parse page {}", page.getId(), throwable);
            return;
        }

        if (astRoot == null || astRoot.children() == null) {
            return;
        }

        for (MdAstAnyContent child : astRoot.children()) {
            MdxJsxElementFields rootElement = SceneTagCompiler.unwrapSceneElement(child);
            if (rootElement == null) {
                continue;
            }

            String rootName = rootElement.name();
            if (!"GameScene".equals(rootName) && !"Scene".equals(rootName)) {
                continue;
            }

            if (rootElement.children() == null || rootElement.children()
                .isEmpty()) {
                continue;
            }

            for (MdAstAnyContent sceneChild : rootElement.children()) {
                MdxJsxElementFields sceneElement = SceneTagCompiler.unwrapSceneElement(sceneChild);
                if (sceneElement == null) {
                    continue;
                }

                if ("ImportStructureLib".equals(sceneElement.name()) && Mods.StructureLib.isModLoaded()) {
                    StructureLibImportRequest request = ImportStructureLibElementCompiler
                        .buildDefaultPreviewRequest(sceneElement);
                    if (request != null) {
                        tasks.add(new PrewarmTask(PrewarmTaskType.STRUCTURE_LIB, request));
                    }
                }
                // Future: Add other heavy scene element types here
            }
        }
    }

    private void runPrewarm(List<PrewarmTask> tasks) {
        long startedAt = System.nanoTime();
        AtomicInteger completed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        GuideDebugLog.infoAlways(
            "[GameScenePrewarmBootstrap] Starting prewarm of {} tasks with concurrent execution",
            tasks.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>(tasks.size());

        for (PrewarmTask task : tasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    executePrewarmTask(task);
                    int count = completed.incrementAndGet();
                    if (count % 10 == 0) {
                        GuideDebugLog
                            .infoAlways("[GameScenePrewarmBootstrap] Progress: {}/{} completed", count, tasks.size());
                    }
                } catch (Throwable throwable) {
                    failed.incrementAndGet();
                    GuideDebugLog
                        .warn("[GameScenePrewarmBootstrap] Failed to prewarm task type={}", task.type, throwable);
                }
            },
                GameSceneConcurrentManager.getInstance()
                    .getAnalysisExecutor());

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .join();

        long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000L;
        GuideDebugLog.infoAlways(
            "[GameScenePrewarmBootstrap] Prewarm completed: {} succeeded, {} failed in {} ms (avg {} ms/task)",
            completed.get(),
            failed.get(),
            elapsedMs,
            tasks.isEmpty() ? 0 : elapsedMs / tasks.size());
    }

    private void executePrewarmTask(PrewarmTask task) {
        switch (task.type) {
            case STRUCTURE_LIB -> prewarmStructureLib((StructureLibImportRequest) task.data);
            default -> GuideDebugLog.warn("[GameScenePrewarmBootstrap] Unknown task type: {}", task.type);
        }
    }

    private void prewarmStructureLib(StructureLibImportRequest request) {
        StructureLibRuntimeFacade facade = new StructureLibRuntimeFacade();
        StructureLibRuntimeFacade.ResolvedController controller = StructureLibRuntimeFacade.resolveController(request);
        StructureLibRuntimeFacade.ControlAnalysis analysis = StructureLibRuntimeFacade
            .analyzeControls(request, controller);
        facade.buildPreviewSelection(request, analysis);
        facade.importScene(request);
    }

    private enum PrewarmTaskType {
        STRUCTURE_LIB
    }

    private static class PrewarmTask {

        private final PrewarmTaskType type;
        private final Object data;

        public PrewarmTask(PrewarmTaskType type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
}
