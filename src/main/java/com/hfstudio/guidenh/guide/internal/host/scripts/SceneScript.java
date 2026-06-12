package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.compiler.IdUtils;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.extensions.ExtensionCollection;
import com.hfstudio.guidenh.guide.indices.PageIndex;
import com.hfstudio.guidenh.guide.internal.AsyncWorker;
import com.hfstudio.guidenh.guide.internal.host.EventType;
import com.hfstudio.guidenh.guide.internal.host.LytEvent;
import com.hfstudio.guidenh.guide.internal.host.LytScript;
import com.hfstudio.guidenh.guide.internal.host.ScriptContext;
import com.hfstudio.guidenh.guide.internal.host.ScriptType;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.PerspectivePreset;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler;
import com.hfstudio.guidenh.guide.scene.SceneTagCompiler.ScenePlaceholder;
import com.hfstudio.guidenh.guide.scene.SceneViewportMetrics;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.element.ImportStructureElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.element.SnbtPreParseCache;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class SceneScript implements LytScript {

    private static final String KEY_STATE = "scene.state";
    private static final String STATE_SCAN = "SCAN";
    private static final String STATE_POLL = "POLL";
    private static final String STATE_COMPILE = "COMPILE";
    private static final String KEY_AST = "scene.ast";
    private static final String KEY_SCENE = "scene.object";
    private static final String KEY_TICKETS = "scene.tickets";

    public SceneScript() {}

    @Override
    public ScriptType type() {
        return ScriptType.JAVA;
    }

    @Override
    public String styleClass() {
        return "Scene";
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void onEvent(Object node, LytEvent event, ScriptContext ctx) {
        if (event.type() != EventType.MOUNT) return;
        if (!(node instanceof ScenePlaceholder ph)) return;

        var state = (String) ctx.data()
            .getOrDefault(KEY_STATE, STATE_SCAN);
        switch (state) {
            case STATE_SCAN -> doScan(ph, ctx);
            case STATE_POLL -> doPoll(ph, ctx);
            case STATE_COMPILE -> doCompile(ph, ctx);
        }
    }

    private void doScan(ScenePlaceholder ph, ScriptContext ctx) {
        if (ph.childrenSource == null || ph.childrenSource.trim()
            .isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Empty scene: no scene elements"));
            return;
        }

        // Parse AST once; cache in ctx.data() so it survives re-entry
        var ast = ph.childrenAst;
        if (ast == null) {
            try {
                ast = MdAst.fromMarkdown(ph.childrenSource, GuideMarkdownOptions.runtime());
                MdAstToMdxConverter.convert(ast, Collections.emptyMap());
                ctx.data()
                    .put(KEY_AST, ast);
            } catch (Exception e) {
                GuideDebugLog.error("[SceneScript] Failed to parse scene children", e);
                ctx.replace(LytParagraph.error("[Scene] Failed to parse scene elements"));
                return;
            }
        }

        // Create empty scene skeleton immediately so the user sees a frame.
        // Use same dimensions doCompile will compute from ph.
        var scene = new LytGuidebookScene();
        scene.setSceneSize(
            ph.width > 0 ? ph.width : 320,
            ph.height > 0 ? ph.height : 180);
        scene.setInteractive(ph.interactive);
        scene.setShowBackground(ph.showBackground);
        scene.setVisibleLayerSliderEnabled(ph.allowLayerSlider);
        scene.setGridButtonEnabled(ph.gridButtonEnabled);
        scene.setGridVisible(ph.showGrid);

        var pc = ctx.getPageCollection();
        var tickets = new java.util.ArrayList<String>();

        for (var child : ast.children()) {
            var el = SceneTagCompiler.unwrapSceneElement(child);
            if (el == null) continue;

            switch (el.name()) {
                case "ImportStructureLib" -> {
                    var controller = el.getAttributeString("controller", null);
                    if (controller == null || controller.trim()
                        .isEmpty()) continue;
                    controller = controller.trim();
                    var request = buildImportRequest(ph, el);
                    if (request != null) {
                        var ticket = "lib:" + controller;
                        AsyncWorker.submit(ticket, () -> { new StructureLibRuntimeFacade().importScene(request); });
                        tickets.add(ticket);
                    }
                }
                case "ImportStructure" -> {
                    var src = el.getAttributeString("src", null);
                    if (src == null || src.isEmpty()) continue;
                    ResourceLocation absSrc;
                    try {
                        absSrc = IdUtils.resolveLink(src, new ResourceLocation(ph.pageDomain, ph.pagePath));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    var data = pc != null ? pc.loadAsset(absSrc) : null;
                    if (data == null) continue;
                    var ticket = "snbt:" + absSrc;
                    AsyncWorker.submit(ticket, () -> {
                        try {
                            var root = ImportStructureElementCompiler.readStructureNbt(data);
                            SnbtPreParseCache.put(absSrc, root);
                        } catch (Exception e) {
                            GuideDebugLog.warn("[SceneScript] SNBT pre-parse failed: {}", absSrc, e);
                        }
                    });
                    tickets.add(ticket);
                }
            }
        }

        if (tickets.isEmpty()) {
            ctx.data()
                .put(KEY_STATE, STATE_COMPILE);
            ctx.data()
                .put(KEY_SCENE, scene);
            doCompile(ph, ctx);
            return;
        }

        // Show placeholder immediately with progress bar
        scene.setLoading(true);
        scene.setLoadProgress(0, tickets.size());
        ctx.data()
            .put(KEY_SCENE, scene);
        ctx.data()
            .put(KEY_TICKETS, tickets);
        ctx.data()
            .put(KEY_STATE, STATE_POLL);
        ctx.replace(scene);
        ctx.yield();
    }

    @Nullable
    private static StructureLibImportRequest buildImportRequest(ScenePlaceholder ph, MdxJsxElementFields el) {
        var controller = el.getAttributeString("controller", null);
        if (controller == null || controller.trim()
            .isEmpty()) return null;

        return new StructureLibImportRequest(
            controller.trim(),
            el.getAttributeString("piece", null),
            StructureLibSceneOptions.resolveFacing(el.getAttributeString("facing", null), null),
            StructureLibSceneOptions.resolveRotation(el.getAttributeString("rotation", null), null),
            StructureLibSceneOptions.resolveFlip(el.getAttributeString("flip", null), null),
            1,
            null);
    }

    @SuppressWarnings("unchecked")
    private void doPoll(ScenePlaceholder ph, ScriptContext ctx) {
        var tickets = (java.util.List<String>) ctx.data()
            .get(KEY_TICKETS);
        var scene = (LytGuidebookScene) ctx.data()
            .get(KEY_SCENE);
        if (tickets == null || scene == null) {
            ctx.data()
                .put(KEY_STATE, STATE_COMPILE);
            doCompile(ph, ctx);
            return;
        }

        // Count completed tickets and update progress bar
        int done = 0;
        for (var ticket : tickets) {
            if (AsyncWorker.isDone(ticket)) done++;
        }
        scene.setLoadProgress(done, tickets.size());

        if (done >= tickets.size()) {
            ctx.data()
                .put(KEY_STATE, STATE_COMPILE);
            doCompile(ph, ctx);
        } else {
            ctx.yield();
        }
    }

    private void doCompile(ScenePlaceholder ph, ScriptContext ctx) {
        if (ph.childrenSource == null || ph.childrenSource.trim()
            .isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Empty scene: no scene elements"));
            return;
        }

        // Reuse scene from doScan if available, else create fresh
        var existingScene = (LytGuidebookScene) ctx.data()
            .get(KEY_SCENE);
        final var hadTickets = ctx.data()
            .containsKey(KEY_TICKETS);
        final LytGuidebookScene scene = existingScene != null ? existingScene : new LytGuidebookScene();
        var level = scene.getLevel();
        if (level == null) {
            level = new GuidebookLevel();
            scene.setLevel(level);
        }
        final GuidebookLevel finalLevel = level;
        var camera = scene.getCamera();
        if (camera == null) {
            camera = new CameraSettings();
            scene.setCamera(camera);
        }
        final CameraSettings finalCamera = camera;

        // Apply camera settings from placeholder (if not already set by doScan)
        if (ph.perspective != null && !ph.perspective.trim()
            .isEmpty()) {
            camera.setPerspectivePreset(PerspectivePreset.fromSerializedName(ph.perspective.trim()));
        }
        if (!Float.isNaN(ph.zoom)) camera.setZoom(ph.zoom);
        if (!Float.isNaN(ph.rotateX)) camera.setRotationX(ph.rotateX);
        if (!Float.isNaN(ph.rotateY)) camera.setRotationY(ph.rotateY);
        if (!Float.isNaN(ph.rotateZ)) camera.setRotationZ(ph.rotateZ);
        if (!Float.isNaN(ph.offsetX)) camera.setOffsetX(ph.offsetX);
        if (!Float.isNaN(ph.offsetY)) camera.setOffsetY(ph.offsetY);
        if (ph.explicitCenter) {
            camera.setRotationCenter(
                Float.isNaN(ph.centerX) ? 0 : ph.centerX,
                Float.isNaN(ph.centerY) ? 0 : ph.centerY,
                Float.isNaN(ph.centerZ) ? 0 : ph.centerZ);
        }

        int width = ph.width > 0 ? ph.width : 320;
        int height = ph.height > 0 ? ph.height : 180;
        camera.setViewportSize(width, height);
        scene.setSceneSize(width, height);

        // Parse children source
        ExceptionCollector errorSink = new ExceptionCollector();
        PageCollection pc = ctx.getPageCollection();
        ExtensionCollection extensions = pc instanceof Guide guide ? guide.getExtensions() : ExtensionCollection.EMPTY;
        PageCompiler runtimeCompiler = new PageCompiler(
            pc != null ? pc : new StubPageCollection(),
            extensions,
            ph.sourcePack,
            new ResourceLocation(ph.pageDomain, ph.pagePath),
            ph.childrenSource != null ? ph.childrenSource : "");
        // Use cached AST from doScan if available, else parse from source
        MdAstRoot ast = (MdAstRoot) ctx.data()
            .get(KEY_AST);
        if (ast == null) {
            ast = ph.childrenAst;
        }
        if (ast == null) {
            try {
                ast = MdAst.fromMarkdown(ph.childrenSource, GuideMarkdownOptions.runtime());
                MdAstToMdxConverter.convert(ast, Collections.emptyMap());
            } catch (Exception e) {
                GuideDebugLog.error("[GuideNH] [SceneScript] Failed to parse scene children", e);
                ctx.replace(LytParagraph.error("[Scene] Failed to parse scene elements"));
                return;
            }
        }
        final MdAstRoot finalAst = ast;

        // Build element compiler map from placeholder (set at compile time by SceneTagCompiler)
        Map<String, SceneElementTagCompiler> elementCompilers = new HashMap<>();
        if (ph.sceneElementCompilers != null) {
            for (var ec : ph.sceneElementCompilers) {
                for (String name : ec.getTagNames()) {
                    elementCompilers.put(name, ec);
                }
            }
        }

        // Update scene properties (created in doScan for async path, or fresh here)
        scene.setLevel(level);
        scene.setCamera(camera);
        scene.setSceneSize(width, height);
        scene.setInteractive(ph.interactive);
        scene.setShowBackground(ph.showBackground);
        scene.setVisibleLayerSliderEnabled(ph.allowLayerSlider);
        scene.setGridButtonEnabled(ph.gridButtonEnabled);
        scene.setGridVisible(ph.showGrid);

        // NB: Phase 2 used GuideSceneStructureCache (fingerprint-based) to avoid
        // recompiling complex scenes on every page visit. Phase 3 compiles from scratch
        // each mount. The cache requires StructureFingerprintResolver + compile-time
        // fingerprint computation, which is not practical to restore in a MOUNT-time script.
        // Low priority — scene compilation is usually fast enough that recompilation
        // per mount is acceptable.

        // Compile scene elements with CURRENT_SCENE set so that element compilers
        // (ImportPonderElementCompiler, ImportStructureLibElementCompiler, annotations, etc.)
        // can call scene.attachPonderData(), scene.addAnnotation(), etc.
        var prevScene = AnnotationTagCompiler.CURRENT_SCENE.get();
        AnnotationTagCompiler.CURRENT_SCENE.set(scene);
        final boolean[] blockStatsExplicitlySet = { false };
        try {
            GuideSceneStructureCompileScope.run(true, () -> {
                for (UnistNode child : finalAst.children()) {
                    MdxJsxElementFields el = SceneTagCompiler.unwrapSceneElement(child);
                    if (el == null) continue;
                    // Handle BlockStats — not a SceneElementTagCompiler, special-cased in Phase 2
                    if ("BlockStats".equals(el.name())) {
                        applyBlockStatsConfig(scene, el);
                        blockStatsExplicitlySet[0] = true;
                        continue;
                    }
                    SceneElementTagCompiler ec = elementCompilers.get(el.name());
                    if (ec != null) {
                        ec.compile(finalLevel, finalCamera, runtimeCompiler, errorSink, el);
                    }
                }
            });
        } finally {
            if (prevScene != null) {
                AnnotationTagCompiler.CURRENT_SCENE.set(prevScene);
            } else {
                AnnotationTagCompiler.CURRENT_SCENE.remove();
            }
        }

        // Dispatch MOUNT events into annotation tooltip subtrees (Recipe/Scene placeholders)
        for (var annotation : scene.getAnnotations()) {
            var tooltip = annotation.getTooltip();
            if (tooltip instanceof ContentTooltip ct) {
                var content = ct.getContent();
                if (content instanceof LytNode root) {
                    ctx.dispatchSubtree(root);
                }
            }
        }

        if (level.isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Scene has no supported elements"));
            return;
        }

        // Apply implicit block stats for non-empty scenes without explicit BlockStats
        if (!blockStatsExplicitlySet[0]) {
            scene.setBlockStatsEnabled(true);
            scene.setBlockStatsVisible(ModConfig.ui.sceneBlockStatsVisible);
            scene.setBlockStatsButtonEnabled(ModConfig.ui.sceneBlockStatsButtonEnabled);
        }

        // Determine rotation center; fall back to level center
        float[] center;
        if (!ph.explicitCenter) {
            center = level.getCenter();
            camera.setRotationCenter(center[0], center[1], center[2]);
        } else {
            center = new float[] { Float.isNaN(ph.centerX) ? 0f : ph.centerX, Float.isNaN(ph.centerY) ? 0f : ph.centerY,
                Float.isNaN(ph.centerZ) ? 0f : ph.centerZ };
        }

        boolean explicitOffX = !Float.isNaN(ph.offsetX);
        boolean explicitOffY = !Float.isNaN(ph.offsetY);

        // Auto-zoom: measure at zoom=1/offset=0, then fit to viewport at 85% fill
        if (Float.isNaN(ph.zoom)) {
            camera.setZoom(1f);
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            if (!level.isEmpty()) {
                int[] bounds = level.getBounds();
                SceneViewportMetrics metrics = SceneViewportMetrics.measure(camera, bounds);
                float spanX = metrics.spanX();
                float spanY = metrics.spanY();
                if (spanX > 0.5f || spanY > 0.5f) {
                    float zX = spanX > 0.5f ? (float) width / spanX : Float.MAX_VALUE;
                    float zY = spanY > 0.5f ? (float) height / spanY : Float.MAX_VALUE;
                    float autoZoom = Math.min(zX, zY) * 0.85f;
                    autoZoom = Math.max(LytGuidebookScene.MIN_ZOOM, Math.min(LytGuidebookScene.MAX_ZOOM, autoZoom));
                    camera.setZoom(autoZoom);
                }
            }
            // Restore explicit offsets zeroed for measurement
            if (explicitOffX) camera.setOffsetX(ph.offsetX);
            if (explicitOffY) camera.setOffsetY(ph.offsetY);
        }

        // Auto-size: save offsets, measure at offset=0, restore
        if (!ph.explicitWidth || !ph.explicitHeight) {
            float savedOffX = camera.getOffsetX();
            float savedOffY = camera.getOffsetY();
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            if (!level.isEmpty()) {
                int[] bounds = level.getBounds();
                SceneViewportMetrics metrics = SceneViewportMetrics.measure(camera, bounds);
                if (!ph.explicitWidth && metrics.spanX() > 0.5f) {
                    width = SceneViewportMetrics.clampDimension(metrics.spanX());
                }
                if (!ph.explicitHeight && metrics.spanY() > 0.5f) {
                    height = SceneViewportMetrics.clampDimension(metrics.spanY());
                }
                scene.setSceneSize(width, height);
                camera.setViewportSize(width, height);
            }
            camera.setOffsetX(savedOffX);
            camera.setOffsetY(savedOffY);
        }

        // Auto-center: shift projected scene center to viewport origin.
        // Applied only when neither rotation center nor screen offsets are author-specified.
        if (!ph.explicitCenter && !explicitOffX && !explicitOffY) {
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            var sc = camera.worldToScreen(center[0], center[1], center[2]);
            camera.setOffsetX(-sc.x);
            camera.setOffsetY(sc.y);
        }

        scene.initializePonderTimelineBaseline();
        scene.captureInitialInteractiveState();
        scene.snapshotInitialCamera();
        scene.setStructureLibSelectionChangeListener(selection -> scene.rebuildStructureLib());
        for (StructureLibSceneBinding binding : scene.getStructureLibBindings()) {
            if (binding.getName() != null && binding.hasRebuildRecipe()) {
                binding.setSelectionChangeListener(selection -> scene.rebuildStructureLib());
            }
        }
        scene.setLoading(false);
        if (!hadTickets) {
            ctx.replace(scene);
        }
        ctx.markComplete();
    }

    /**
     * Applies BlockStats element attributes to the scene.
     * <p>
     * NB: Full BlockStats restoration (BlockStat sub-elements, filters, implicit enable)
     * requires the Phase 2 compileBlockStatsElement() logic (~100 lines). This minimal
     * restoration handles the most common attribute-only use case.
     */
    private static void applyBlockStatsConfig(LytGuidebookScene scene, MdxJsxElementFields el) {
        String visibleStr = el.getAttributeString("visible", null);
        if (visibleStr != null) scene.setBlockStatsVisible(Boolean.parseBoolean(visibleStr));
        String enabledStr = el.getAttributeString("buttonEnabled", null);
        if (enabledStr != null) scene.setBlockStatsButtonEnabled(Boolean.parseBoolean(enabledStr));
    }

    private static class ExceptionCollector implements LytErrorSink {

        @Override
        public void appendError(PageCompiler compiler, String text, UnistNode node) {
            GuideDebugLog.warnAlways("[GuideNH] [SceneScript] {}", text);
        }
    }

    private static class StubPageCollection implements PageCollection {

        @Override
        public <T extends PageIndex> T getIndex(Class<T> c) {
            return null;
        }

        @Override
        public Collection<ParsedGuidePage> getPages() {
            return Collections.emptyList();
        }

        @Override
        public ParsedGuidePage getParsedPage(ResourceLocation id) {
            return null;
        }

        @Override
        public GuidePage getPage(ResourceLocation id) {
            return null;
        }

        @Override
        public byte[] loadAsset(ResourceLocation id) {
            return null;
        }

        @Override
        public NavigationTree getNavigationTree() {
            return new NavigationTree();
        }

        @Override
        public boolean pageExists(ResourceLocation pageId) {
            return false;
        }
    }
}
