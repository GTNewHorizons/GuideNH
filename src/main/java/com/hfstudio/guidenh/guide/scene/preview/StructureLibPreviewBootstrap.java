package com.hfstudio.guidenh.guide.scene.preview;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

public class StructureLibPreviewBootstrap {

    private static final int PREWARM_PARALLELISM = 2;

    private final ExecutorService executor = Executors.newFixedThreadPool(PREWARM_PARALLELISM, runnable -> {
        Thread thread = new Thread(runnable, "guidenh-structurelib-prewarm");
        thread.setDaemon(true);
        return thread;
    });

    public void scheduleReloadPrewarm() {
        if (!Mods.StructureLib.isModLoaded()) {
            return;
        }
        List<StructureLibImportRequest> requests = collectRequests();
        if (requests.isEmpty()) {
            return;
        }
        executor.submit(() -> runPrewarm(requests));
    }

    private List<StructureLibImportRequest> collectRequests() {
        ArrayList<StructureLibImportRequest> requests = new ArrayList<>();
        for (MutableGuide guide : GuideRegistry.getAll()) {
            for (ParsedGuidePage page : guide.getPages()) {
                if (!SceneTagCompiler.likelyHasHeavySceneWork(page)) {
                    continue;
                }
                collectRequests(page, requests);
            }
        }
        return requests;
    }

    private void collectRequests(ParsedGuidePage page, List<StructureLibImportRequest> requests) {
        MdAstRoot astRoot;
        try {
            astRoot = page.getAstRoot();
        } catch (Throwable throwable) {
            GuideDebugLog.warn("[StructureLibPreviewBootstrap] Failed to parse page {}", page.getId(), throwable);
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
                if (sceneElement == null || !"ImportStructureLib".equals(sceneElement.name())) {
                    continue;
                }
                StructureLibImportRequest request = ImportStructureLibElementCompiler
                    .buildDefaultPreviewRequest(sceneElement);
                if (request != null) {
                    requests.add(request);
                }
            }
        }
    }

    private void runPrewarm(List<StructureLibImportRequest> requests) {
        long startedAt = System.nanoTime();
        int warmed = 0;
        for (StructureLibImportRequest request : requests) {
            try {
                StructureLibRuntimeFacade facade = new StructureLibRuntimeFacade();
                StructureLibRuntimeFacade.ResolvedController controller = StructureLibRuntimeFacade
                    .resolveController(request);
                StructureLibRuntimeFacade.ControlAnalysis analysis = StructureLibRuntimeFacade
                    .analyzeControls(request, controller);
                facade.buildPreviewSelection(request, analysis);
                facade.importScene(request);
                warmed++;
            } catch (Throwable throwable) {
                GuideDebugLog.warn(
                    "[StructureLibPreviewBootstrap] Failed to prewarm controller {}",
                    request.getController(),
                    throwable);
            }
        }
        GuideDebugLog.infoAlways(
            "[StructureLibPreviewBootstrap] Prewarmed {} StructureLib previews in {} ms",
            warmed,
            (System.nanoTime() - startedAt) / 1_000_000L);
    }
}
