package com.hfstudio.guidenh.guide.scene.element;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.MdxAttrs;
import com.hfstudio.guidenh.guide.document.LytErrorSink;
import com.hfstudio.guidenh.guide.scene.CameraSettings;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneBinding;
import com.hfstudio.guidenh.guide.scene.annotation.compiler.AnnotationTagCompiler;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCompileScope;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.SceneStructureOptions;
import com.hfstudio.guidenh.integration.structurelib.StructureLibBuildRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneMetadata;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneOptions;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.unist.UnistNode;

/**
 * Compiler for &lt;ImportStructureLib&gt; MDX tags. Pure parse — no world operations.
 * Registers a binding and sets its rebuild recipe; actual block placement
 * happens later in {@link LytGuidebookScene#rebuildStructureLib()}.
 */
public class ImportStructureLibElementCompiler implements SceneElementTagCompiler {

    public ImportStructureLibElementCompiler() {}

    @Override
    public Set<String> getTagNames() {
        return Collections.singleton("ImportStructureLib");
    }

    @Override
    public void compile(GuidebookLevel level, CameraSettings camera, PageCompiler compiler, LytErrorSink errorSink,
        MdxJsxElementFields el) {
        if (!GuideSceneStructureCompileScope.isStructureMutationEnabled()) return;

        LytGuidebookScene scene = AnnotationTagCompiler.CURRENT_SCENE.get();
        if (scene == null) {
            errorSink.appendError(compiler, "ImportStructureLib used outside <GameScene>", el);
            return;
        }

        String controller = MdxAttrs.getString(compiler, errorSink, el, "controller", null);
        if (controller == null || controller.trim()
            .isEmpty()) {
            errorSink.appendError(compiler, "Missing controller attribute.", el);
            return;
        }

        int offsetX = MdxAttrs.getInt(compiler, errorSink, el, "offsetX", 0);
        int offsetY = MdxAttrs.getInt(compiler, errorSink, el, "offsetY", 0);
        int offsetZ = MdxAttrs.getInt(compiler, errorSink, el, "offsetZ", 0);
        boolean formed = SceneStructureOptions.isFormed(compiler, errorSink, el);
        String structureName = MdxAttrs.getString(compiler, errorSink, el, "name", null);

        StructureLibSceneOptions childOptions = StructureLibSceneOptionParser.parseChildren(compiler, errorSink, el);
        StructureLibSceneOptions legacyOptions = StructureLibSceneOptionParser.parseAttributes(compiler, errorSink, el);
        StructureLibSceneOptions mergedOptions = legacyOptions.merge(childOptions);

        String facing = StructureLibSceneOptions
            .resolveFacing(MdxAttrs.getString(compiler, errorSink, el, "facing", null), mergedOptions);
        String rotation = StructureLibSceneOptions
            .resolveRotation(MdxAttrs.getString(compiler, errorSink, el, "rotation", null), mergedOptions);
        String flip = StructureLibSceneOptions
            .resolveFlip(MdxAttrs.getString(compiler, errorSink, el, "flip", null), mergedOptions);

        int requestedChannel = MdxAttrs.getInt(compiler, errorSink, el, "channel", Integer.MIN_VALUE);
        StructureLibPreviewSelection selection = mergedOptions
            .createSelection(requestedChannel == Integer.MIN_VALUE ? null : requestedChannel);
        int tier = selection.getMasterTier();

        StructureLibBuildRequest request = new StructureLibBuildRequest(
            controller,
            /* piece */ null,
            facing,
            rotation,
            flip,
            tier,
            selection.getChannelOverrides(),
            selection.getIntegrationOptions());

        StructureLibSceneBinding binding = scene.registerStructureLibBinding(structureName);
        binding.setRebuildRecipe(request, offsetX, offsetY, offsetZ, formed);

        // Build initial metadata from ConstructableData (not a world operation).
        com.hfstudio.guidenh.guide.scene.preview.StructureLibDefinitionCache cache = com.hfstudio.guidenh.guide.scene.preview.StructureLibDefinitionCache
            .getInstance();
        blockrenderer6343.client.utils.ConstructableData data = cache.getConstructableDataFor(controller);
        if (data != null) {
            StructureLibSceneMetadata metadata = new StructureLibSceneMetadata(
                controller,
                null,
                facing,
                rotation,
                flip);
            int maxTier = Math.max(1, data.getMaxTotalTier());
            metadata = metadata.withTierData(1, maxTier, tier, tier);
            var channelData = data.getChannelData();
            if (channelData != null) {
                for (var entry : channelData.object2IntEntrySet()) {
                    String ch = com.hfstudio.guidenh.integration.structurelib.StructureLibPreviewSelection
                        .normalizeChannelId(entry.getKey());
                    if (ch != null) {
                        int cv = selection.getChannelOverrides()
                            .getOrDefault(ch, -1);
                        metadata = metadata.withChannelData(ch, ch, entry.getIntValue(), cv);
                    }
                }
            }
            binding.setMetadata(metadata);
            scene.setStructureLibSceneMetadata(structureName, metadata);
        }
    }

    // ========== Utility for callers that need to replicate parsing ==========

    @Nullable
    public static StructureLibBuildRequest buildDefaultPreviewRequest(MdxJsxElementFields el) {
        return buildDefaultPreviewRequest(null, NoopErrorSink.INSTANCE, el);
    }

    @Nullable
    public static StructureLibBuildRequest buildDefaultPreviewRequest(@Nullable PageCompiler compiler,
        LytErrorSink errorSink, MdxJsxElementFields el) {
        String controller = MdxAttrs.getString(compiler, errorSink, el, "controller", null);
        if (controller == null || controller.trim()
            .isEmpty()) return null;

        String facing = MdxAttrs.getString(compiler, errorSink, el, "facing", null);
        String rotation = MdxAttrs.getString(compiler, errorSink, el, "rotation", null);
        String flip = MdxAttrs.getString(compiler, errorSink, el, "flip", null);
        int requestedChannel = MdxAttrs.getInt(compiler, errorSink, el, "channel", Integer.MIN_VALUE);
        StructureLibSceneOptions childOptions = StructureLibSceneOptionParser.parseChildren(compiler, errorSink, el);
        StructureLibSceneOptions legacyOptions = StructureLibSceneOptionParser.parseAttributes(compiler, errorSink, el);
        StructureLibSceneOptions merged = legacyOptions.merge(childOptions);
        StructureLibPreviewSelection selection = merged
            .createSelection(requestedChannel == Integer.MIN_VALUE ? null : requestedChannel);

        return new StructureLibBuildRequest(
            controller,
            MdxAttrs.getString(compiler, errorSink, el, "piece", null),
            StructureLibSceneOptions.resolveFacing(facing, merged),
            StructureLibSceneOptions.resolveRotation(rotation, merged),
            StructureLibSceneOptions.resolveFlip(flip, merged),
            selection.getMasterTier(),
            selection.getChannelOverrides(),
            selection.getIntegrationOptions());
    }

    public static String resolveFailureMessage(List<String> errors, String controller) {
        if (errors != null && !errors.isEmpty()) {
            String first = errors.getFirst();
            if (first != null && !first.trim()
                .isEmpty()) return first;
        }
        return "StructureLib import failed for controller: " + controller;
    }

    private static class NoopErrorSink implements LytErrorSink {

        static final NoopErrorSink INSTANCE = new NoopErrorSink();

        @Override
        public void appendError(PageCompiler compiler, String text, UnistNode node) {}
    }
}
