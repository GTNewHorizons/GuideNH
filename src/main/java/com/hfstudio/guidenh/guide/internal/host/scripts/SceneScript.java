package com.hfstudio.guidenh.guide.internal.host.scripts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureSnapshot;
import com.hfstudio.guidenh.guide.scene.element.ImportStructureElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.ImportStructureLibElementCompiler;
import com.hfstudio.guidenh.guide.scene.element.SceneElementTagCompiler;
import com.hfstudio.guidenh.guide.scene.element.SnbtPreParseCache;
import com.hfstudio.guidenh.guide.scene.element.StructureLibSceneOptionParser;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.preview.StructureLibDefinitionCache;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstRoot;
import com.hfstudio.guidenh.libs.unist.UnistNode;

public class SceneScript implements LytScript {

    private static final String KEY_STATE = "scene.state";
    private static final String STATE_SCAN = "SCAN";
    private static final String STATE_COMPILE = "COMPILE";
    private static final String KEY_AST = "scene.ast";
    private static final String KEY_SCENE = "scene.object";
    private static final String KEY_TICKETS = "scene.tickets";
    private static final String KEY_BINDINGS = "scene.structurelib.bindings";

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
            case STATE_COMPILE -> doCompile(ph, ctx);
            default -> ctx.replace(LytParagraph.error("[Scene] Unknown async state: " + state));
        }
    }

    private void doScan(ScenePlaceholder ph, ScriptContext ctx) {
        if (ph.childrenSource == null || ph.childrenSource.trim()
            .isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Empty scene: no scene elements"));
            return;
        }

        MdAstRoot ast = readOrCreateAst(ph, ctx);
        if (ast == null) {
            ctx.replace(LytParagraph.error("[Scene] Failed to parse scene elements"));
            return;
        }

        LytGuidebookScene scene = createSceneShell(ph);
        PageCollection pageCollection = ctx.getPageCollection();
        List<String> tickets = new ArrayList<>();
        LinkedHashMap<String, StructureBindingState> bindingStates = new LinkedHashMap<>();
        String sceneKey = sceneKeyOf(ph, scene);

        for (UnistNode child : ast.children()) {
            MdxJsxElementFields el = SceneTagCompiler.unwrapSceneElement(child);
            if (el == null) {
                continue;
            }

            if ("ImportStructureLib".equals(el.name())) {
                StructureBindingState bindingState = registerDefaultStructureBinding(ph, scene, el, sceneKey);
                if (bindingState != null) {
                    bindingStates.put(bindingState.bindingKey, bindingState);
                    if (bindingState.defaultFailureMessage != null) {
                        scene.setLoadFailure(bindingState.defaultFailureMessage);
                    }
                }
                continue;
            }

            if ("ImportStructure".equals(el.name())) {
                queueSnbtPreparse(ph, pageCollection, el, tickets);
            }
        }

        ctx.data()
            .put(KEY_SCENE, scene);
        ctx.data()
            .put(KEY_TICKETS, tickets);
        ctx.data()
            .put(KEY_BINDINGS, bindingStates);
        ctx.data()
            .put(KEY_STATE, STATE_COMPILE);

        doCompile(ph, ctx);
    }

    private void doCompile(ScenePlaceholder ph, ScriptContext ctx) {
        if (ph.childrenSource == null || ph.childrenSource.trim()
            .isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Empty scene: no scene elements"));
            return;
        }

        LytGuidebookScene existingScene = (LytGuidebookScene) ctx.data()
            .get(KEY_SCENE);
        LytGuidebookScene scene = existingScene != null ? existingScene : new LytGuidebookScene();
        GuidebookLevel level = scene.getLevel();
        if (level == null) {
            level = new GuidebookLevel();
            scene.setLevel(level);
        }
        CameraSettings camera = scene.getCamera();
        if (camera == null) {
            camera = new CameraSettings();
            scene.setCamera(camera);
        }

        applyCameraAndViewport(ph, scene, level, camera);

        ExceptionCollector errorSink = new ExceptionCollector();
        PageCollection pageCollection = ctx.getPageCollection();
        ExtensionCollection extensions = pageCollection instanceof Guide guide ? guide.getExtensions()
            : ExtensionCollection.EMPTY;
        PageCompiler runtimeCompiler = new PageCompiler(
            pageCollection != null ? pageCollection : new StubPageCollection(),
            extensions,
            ph.sourcePack,
            ph.language,
            new ResourceLocation(ph.pageDomain, ph.pagePath),
            ph.childrenSource != null ? ph.childrenSource : "");

        MdAstRoot ast = (MdAstRoot) ctx.data()
            .get(KEY_AST);
        if (ast == null) {
            ast = readOrCreateAst(ph, ctx);
            if (ast == null) {
                ctx.replace(LytParagraph.error("[Scene] Failed to parse scene elements"));
                return;
            }
        }

        Map<String, SceneElementTagCompiler> elementCompilers = new HashMap<>();
        if (ph.sceneElementCompilers != null) {
            for (SceneElementTagCompiler compiler : ph.sceneElementCompilers) {
                if (compiler instanceof ImportStructureLibElementCompiler) {
                    continue;
                }
                for (String name : compiler.getTagNames()) {
                    elementCompilers.put(name, compiler);
                }
            }
        }

        boolean[] blockStatsExplicitlySet = { false };
        final GuidebookLevel compileLevel = level;
        final CameraSettings compileCamera = camera;
        final MdAstRoot compileAst = ast;
        LytGuidebookScene previousScene = AnnotationTagCompiler.CURRENT_SCENE.get();
        AnnotationTagCompiler.CURRENT_SCENE.set(scene);
        try {
            GuideSceneStructureCompileScope.run(true, () -> {
                for (UnistNode child : compileAst.children()) {
                    MdxJsxElementFields el = SceneTagCompiler.unwrapSceneElement(child);
                    if (el == null) {
                        continue;
                    }
                    if ("ImportStructureLib".equals(el.name())) {
                        continue;
                    }
                    if ("BlockStats".equals(el.name())) {
                        applyBlockStatsConfig(scene, el);
                        blockStatsExplicitlySet[0] = true;
                        continue;
                    }
                    SceneElementTagCompiler compiler = elementCompilers.get(el.name());
                    if (compiler != null) {
                        compiler.compile(compileLevel, compileCamera, runtimeCompiler, errorSink, el);
                    }
                }
            });
        } finally {
            if (previousScene != null) {
                AnnotationTagCompiler.CURRENT_SCENE.set(previousScene);
            } else {
                AnnotationTagCompiler.CURRENT_SCENE.remove();
            }
        }

        @SuppressWarnings("unchecked")
        LinkedHashMap<String, StructureBindingState> bindingStates = (LinkedHashMap<String, StructureBindingState>) ctx
            .data()
            .get(KEY_BINDINGS);

        dispatchSceneSubtrees(scene, ctx);

        // Only rebuild if there are StructureLib bindings.
        // Scenes without bindings rely on element-compiler-placed blocks (ImportStructure).
        if (bindingStates != null && !bindingStates.isEmpty()) {
            scene.rebuildStructureLib();
        }
        // Set metadata on scene from binding results
        bindingStates = (LinkedHashMap<String, StructureBindingState>) ctx.data()
            .get(KEY_BINDINGS);
        if (bindingStates != null) {
            for (StructureBindingState bindingState : bindingStates.values()) {
                StructureLibSceneMetadata metadata = bindingState.binding.getMetadata();
                if (metadata != null) {
                    scene.setStructureLibSceneMetadata(bindingState.binding.getName(), metadata);
                }
            }
        }
        if (level.isEmpty()) {
            ctx.replace(LytParagraph.error("[Scene] Scene has no supported elements"));
            return;
        }

        if (!blockStatsExplicitlySet[0]) {
            scene.setBlockStatsEnabled(true);
            scene.setBlockStatsVisible(ModConfig.ui.sceneBlockStatsVisible);
            scene.setBlockStatsButtonEnabled(ModConfig.ui.sceneBlockStatsButtonEnabled);
        }

        finalizeSceneGeometry(ph, scene, level, camera);
        scene.setInitialLevelSnapshot(GuideSceneStructureSnapshot.capture(level));
        scene.clearLoadState();
        attachSelectionListeners(scene, ctx);
        scene.initializePonderTimelineBaseline();
        scene.captureInitialInteractiveState();
        scene.snapshotInitialCamera();

        ctx.replace(scene);
        ctx.markComplete();
    }

    @Nullable
    private MdAstRoot readOrCreateAst(ScenePlaceholder ph, ScriptContext ctx) {
        MdAstRoot cached = (MdAstRoot) ctx.data()
            .get(KEY_AST);
        if (cached != null) {
            return cached;
        }
        MdAstRoot ast = ph.childrenAst;
        if (ast != null) {
            ctx.data()
                .put(KEY_AST, ast);
            return ast;
        }
        try {
            ast = MdAst.fromMarkdown(ph.childrenSource, GuideMarkdownOptions.runtime());
            MdAstToMdxConverter.convert(ast, Collections.emptyMap());
            ctx.data()
                .put(KEY_AST, ast);
            return ast;
        } catch (Exception exception) {
            GuideDebugLog.error("[SceneScript] Failed to parse scene children", exception);
            return null;
        }
    }

    private LytGuidebookScene createSceneShell(ScenePlaceholder ph) {
        LytGuidebookScene scene = new LytGuidebookScene();
        scene.setSceneSize(ph.width > 0 ? ph.width : 320, ph.height > 0 ? ph.height : 180);
        scene.setInteractive(ph.interactive);
        scene.setShowBackground(ph.showBackground);
        scene.setVisibleLayerSliderEnabled(ph.allowLayerSlider);
        scene.setGridButtonEnabled(ph.gridButtonEnabled);
        scene.setGridVisible(ph.showGrid);
        return scene;
    }

    private void queueSnbtPreparse(ScenePlaceholder ph, @Nullable PageCollection pageCollection, MdxJsxElementFields el,
        List<String> tickets) {
        String src = el.getAttributeString("src", null);
        if (src == null || src.isEmpty()) {
            return;
        }
        ResourceLocation absoluteSrc;
        try {
            absoluteSrc = IdUtils.resolveLink(src, new ResourceLocation(ph.pageDomain, ph.pagePath));
        } catch (IllegalArgumentException exception) {
            return;
        }
        byte[] data = pageCollection != null ? pageCollection.loadAsset(absoluteSrc) : null;
        if (data == null) {
            return;
        }
        String ticket = "snbt:" + absoluteSrc;
        AsyncWorker.submit(ticket, () -> {
            try {
                SnbtPreParseCache.put(absoluteSrc, ImportStructureElementCompiler.readStructureNbt(data));
            } catch (Exception exception) {
                GuideDebugLog.warn("[SceneScript] SNBT pre-parse failed: {}", absoluteSrc, exception);
            }
        });
        tickets.add(ticket);
    }

    @Nullable
    private StructureBindingState registerDefaultStructureBinding(ScenePlaceholder ph, LytGuidebookScene scene,
        MdxJsxElementFields el, String sceneKey) {
        String controller = el.getAttributeString("controller", null);
        if (controller == null || controller.trim()
            .isEmpty()) {
            return null;
        }
        String structureName = el.getAttributeString("name", null);
        StructureLibSceneBinding binding = scene.registerStructureLibBinding(structureName);
        StructureLibSceneOptions childOptions = StructureLibSceneOptionParser
            .parseChildren(null, NoopErrorSink.INSTANCE, el);
        StructureLibSceneOptions legacyOptions = StructureLibSceneOptionParser
            .parseAttributes(null, NoopErrorSink.INSTANCE, el);
        StructureLibSceneOptions sceneOptions = legacyOptions.merge(childOptions);
        StructureLibImportRequest defaultRequest = ImportStructureLibElementCompiler.buildDefaultPreviewRequest(el);
        if (defaultRequest == null) {
            return null;
        }
        StructureLibPreviewSelection defaultSelection = defaultRequest.getPreviewSelection();
        StructureLibPreviewSelection effectiveSelection = ImportStructureLibElementCompiler
            .applyControllerDefaults(controller, defaultSelection, sceneOptions);
        binding.applyPreviewSelection(effectiveSelection);
        binding.setPendingSelection(effectiveSelection);

        int offsetX = parseIntAttribute(el, "offsetX", 0);
        int offsetY = parseIntAttribute(el, "offsetY", 0);
        int offsetZ = parseIntAttribute(el, "offsetZ", 0);
        boolean formed = parseBooleanAttribute(el, "formed", false);
        binding.setRebuildRecipe(
            defaultRequest.getChannel(),
            sceneOptions,
            offsetX,
            offsetY,
            offsetZ,
            formed,
            effectiveSelection.getIntegrationOptions());

        // Verify IConstructable exists and build initial metadata from ConstructableData
        StructureLibDefinitionCache cache = StructureLibDefinitionCache.getInstance();
        com.gtnewhorizon.structurelib.alignment.constructable.IConstructable constructable = cache
            .findConstructable(controller);
        if (constructable == null) {
            StructureBindingState state = new StructureBindingState(sceneKey + "::" + binding.getBindingKey(), binding);
            state.defaultFailureMessage = "NEI加载未成功";
            return state;
        }

        // Build minimal metadata so rebuildStructureLib can call buildRebuildRequest()
        blockrenderer6343.client.utils.ConstructableData data = cache.getConstructableData(constructable);
        StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(
            controller,
            defaultRequest.getPiece(),
            defaultRequest.getFacing(),
            defaultRequest.getRotation(),
            defaultRequest.getFlip());
        int maxTier = data.getMaxTotalTier();
        metadata = metadata
            .withTierData(1, maxTier, effectiveSelection.getMasterTier(), effectiveSelection.getMasterTier());
        it.unimi.dsi.fastutil.objects.Object2IntMap<String> channelData = data.getChannelData();
        if (channelData != null) {
            for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<String> entry : channelData.object2IntEntrySet()) {
                int channelMax = entry.getIntValue();
                int currentValue = effectiveSelection.getChannelOverrides() != null
                    ? effectiveSelection.getChannelOverrides()
                        .getOrDefault(entry.getKey(), channelMax)
                    : channelMax;
                metadata = metadata.withChannelData(entry.getKey(), entry.getKey(), channelMax, currentValue);
            }
        }
        binding.setMetadata(metadata);

        StructureBindingState state = new StructureBindingState(sceneKey + "::" + binding.getBindingKey(), binding);
        return state;
    }

    private void applyCameraAndViewport(ScenePlaceholder ph, LytGuidebookScene scene, GuidebookLevel level,
        CameraSettings camera) {
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
        scene.setLevel(level);
        scene.setCamera(camera);
        scene.setInteractive(ph.interactive);
        scene.setShowBackground(ph.showBackground);
        scene.setVisibleLayerSliderEnabled(ph.allowLayerSlider);
        scene.setGridButtonEnabled(ph.gridButtonEnabled);
        scene.setGridVisible(ph.showGrid);
    }

    private void finalizeSceneGeometry(ScenePlaceholder ph, LytGuidebookScene scene, GuidebookLevel level,
        CameraSettings camera) {
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
        int width = ph.width > 0 ? ph.width : 320;
        int height = ph.height > 0 ? ph.height : 180;

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
            if (explicitOffX) camera.setOffsetX(ph.offsetX);
            if (explicitOffY) camera.setOffsetY(ph.offsetY);
        }

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

        if (!ph.explicitCenter && !explicitOffX && !explicitOffY) {
            camera.setOffsetX(0f);
            camera.setOffsetY(0f);
            var screenCenter = camera.worldToScreen(center[0], center[1], center[2]);
            camera.setOffsetX(-screenCenter.x);
            camera.setOffsetY(screenCenter.y);
        }

    }

    private void dispatchSceneSubtrees(LytGuidebookScene scene, ScriptContext ctx) {
        for (var annotation : scene.getAnnotations()) {
            var tooltip = annotation.getTooltip();
            if (tooltip instanceof ContentTooltip contentTooltip) {
                if (contentTooltip.getContent() instanceof LytNode root) {
                    ctx.dispatchSubtree(root);
                }
            }
        }
        for (LytParagraph paragraph : scene.collectTextAnnotationRichContent()) {
            ctx.dispatchSubtree(paragraph);
        }
    }

    private void attachSelectionListeners(LytGuidebookScene scene, ScriptContext ctx) {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, StructureBindingState> bindingStates = (LinkedHashMap<String, StructureBindingState>) ctx
            .data()
            .get(KEY_BINDINGS);
        if (bindingStates == null || bindingStates.isEmpty()) {
            return;
        }
        for (StructureBindingState bindingState : bindingStates.values()) {
            bindingState.binding.setSelectionChangeListener(
                selection -> scene.queueStructureLibSelectionBuild(bindingState.binding, selection));
        }
        scene.setStructureLibSelectionChangeListener(selection -> {
            StructureLibSceneBinding primaryBinding = scene.resolveStructureLibBinding(null);
            if (primaryBinding == null) {
                return;
            }
            scene.queueStructureLibSelectionBuild(primaryBinding, selection);
        });
    }

    private static int parseIntAttribute(MdxJsxElementFields el, String name, int defaultValue) {
        String value = el.getAttributeString(name, null);
        if (value == null || value.trim()
            .isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static boolean parseBooleanAttribute(MdxJsxElementFields el, String name, boolean defaultValue) {
        String value = el.getAttributeString(name, null);
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim()
            .toLowerCase();
        return switch (normalized) {
            case "", "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> defaultValue;
        };
    }

    private static String sceneKeyOf(ScenePlaceholder ph, LytGuidebookScene scene) {
        return ph.pageDomain + ":" + ph.pagePath + "::scene@" + System.identityHashCode(scene);
    }

    private static String firstError(StructureLibImportResult result) {
        if (result == null || result.getErrors()
            .isEmpty()) {
            return "StructureLib preview failed";
        }
        String first = result.getErrors()
            .getFirst();
        return first != null && !first.trim()
            .isEmpty() ? first.trim() : "StructureLib preview failed";
    }

    private static void applyBlockStatsConfig(LytGuidebookScene scene, MdxJsxElementFields el) {
        String visibleStr = el.getAttributeString("visible", null);
        if (visibleStr != null) scene.setBlockStatsVisible(Boolean.parseBoolean(visibleStr));
        String enabledStr = el.getAttributeString("buttonEnabled", null);
        if (enabledStr != null) scene.setBlockStatsButtonEnabled(Boolean.parseBoolean(enabledStr));
    }

    private static class StructureBindingState {

        final String bindingKey;
        final StructureLibSceneBinding binding;
        @Nullable
        String defaultFailureMessage;

        private StructureBindingState(String bindingKey, StructureLibSceneBinding binding) {
            this.bindingKey = bindingKey;
            this.binding = binding;
        }
    }

    private static class ExceptionCollector implements LytErrorSink {

        @Override
        public void appendError(PageCompiler compiler, String text, UnistNode node) {
            GuideDebugLog.warnAlways("[GuideNH] [SceneScript] {}", text);
        }
    }

    private static class NoopErrorSink implements LytErrorSink {

        private static final NoopErrorSink INSTANCE = new NoopErrorSink();

        @Override
        public void appendError(PageCompiler compiler, String text, UnistNode node) {}
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
