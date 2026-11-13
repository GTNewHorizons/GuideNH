package com.hfstudio.guidenh.guide.siteexport.site;

import static com.hfstudio.guidenh.guide.siteexport.site.GuideSiteHtml.toCssColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.StructureLibSceneCondition;
import com.hfstudio.guidenh.guide.scene.annotation.AnnotationColorStyle;
import com.hfstudio.guidenh.guide.scene.annotation.DiamondAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBlockFaceOverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldLineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldOutlineAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.OverlayAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.PonderInputAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.SceneAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.TextAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.outline.OutlineBatch;
import com.hfstudio.guidenh.guide.scene.outline.OutlineFrame;
import com.hfstudio.guidenh.guide.scene.outline.OutlineTargetResolver;
import com.hfstudio.guidenh.guide.scene.outline.OutlineTargetSet;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideSiteSceneAnnotationSerializer {

    private static final float ANNOTATION_THICKNESS_SCALE = 32.0f;
    private static final float MIN_EXPORTED_WORLD_THICKNESS = 1.0f / 256.0f;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .serializeNulls()
        .create();
    private static final ThreadLocal<Map<LytGuidebookScene, GuideSiteExportedScene>> EXPORTED_SCENE_LOOKUP = ThreadLocal
        .withInitial(Collections::emptyMap);

    private static final GuideSiteTooltipRenderer TOOLTIP_RENDERER = new GuideSiteTooltipRenderer(
        GuideSiteSceneAnnotationSerializer::resolveExportedScene);

    private GuideSiteSceneAnnotationSerializer() {}

    public static ExportedSceneLookupScope pushExportedSceneLookup(
        @Nullable Map<LytGuidebookScene, GuideSiteExportedScene> exportedScenesByScene) {
        Map<LytGuidebookScene, GuideSiteExportedScene> previous = EXPORTED_SCENE_LOOKUP.get();
        EXPORTED_SCENE_LOOKUP.set(exportedScenesByScene != null ? exportedScenesByScene : Map.of());
        return new ExportedSceneLookupScope(previous);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates) {
        return serialize(scene, templates, null, null, GuideSiteItemIconResolver.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId) {
        return serialize(scene, templates, currentPageId, null, GuideSiteItemIconResolver.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter) {
        return serialize(scene, templates, currentPageId, assetExporter, GuideSiteItemIconResolver.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        return serialize(
            scene,
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver,
            GuideSiteOutlineMeshExporter.NONE);
    }

    public static AnnotationPayload serialize(LytGuidebookScene scene, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver, GuideSiteOutlineMeshExporter outlineMeshes) {
        List<Map<String, Object>> inWorld = new ArrayList<>();
        List<Map<String, Object>> overlay = new ArrayList<>();
        List<InWorldOutlineAnnotation> outlines = new ArrayList<>();

        if (scene != null) {
            GuidebookSceneLayerSelection layerSelection = GuidebookSceneLayerSelection
                .fromVisibleLayer(scene.getVisibleLayerYForExport());
            for (InWorldAnnotation annotation : scene.collectInWorldAnnotationsForExport(
                true,
                false,
                layerSelection,
                false)) {
                switch (annotation) {
                    case InWorldBoxAnnotation box -> inWorld.add(serializeBox(box, templates, currentPageId, assetExporter, itemIconResolver));
                    case InWorldLineAnnotation line -> inWorld.add(serializeLine(line, templates, currentPageId, assetExporter, itemIconResolver));
                    case InWorldBlockFaceOverlayAnnotation blockOverlay -> inWorld.add(
                            serializeBlockOverlay(blockOverlay, templates, currentPageId, assetExporter, itemIconResolver));
                    case InWorldOutlineAnnotation outline -> outlines.add(outline);
                    default -> addContributedPayload(inWorld, annotation);
                }
            }
            for (List<InWorldOutlineAnnotation> group : groupOutlines(outlines)) {
                inWorld.add(
                    serializeOutline(
                        scene,
                        group,
                        layerSelection,
                        outlineMeshes,
                        templates,
                        currentPageId,
                        assetExporter,
                        itemIconResolver));
            }
            for (InWorldAnnotation annotation : scene.collectStructureLibHatchOverlaysForExport(layerSelection)) {
                if (annotation instanceof InWorldBlockFaceOverlayAnnotation blockOverlay) {
                    Map<String, Object> payload = serializeBlockOverlay(
                        blockOverlay,
                        templates,
                        currentPageId,
                        assetExporter,
                        itemIconResolver);
                    payload.put("siteControl", "structureLibHatches");
                    inWorld.add(payload);
                }
            }
            for (OverlayAnnotation annotation : scene.collectOverlayAnnotationsForExport(layerSelection)) {
                switch (annotation) {
                    case DiamondAnnotation diamond -> overlay.add(serializeDiamond(diamond, templates, currentPageId, assetExporter, itemIconResolver));
                    case TextAnnotation textAnnotation -> overlay.add(
                            serializeTextAnnotation(
                                    textAnnotation,
                                    templates,
                                    currentPageId,
                                    assetExporter,
                                    itemIconResolver));
                    case PonderInputAnnotation inputAnnotation -> overlay.add(
                            serializeInputAnnotation(
                                    inputAnnotation,
                                    templates,
                                    currentPageId,
                                    assetExporter,
                                    itemIconResolver));
                    default -> addContributedPayload(overlay, annotation);
                }
            }
        }

        return new AnnotationPayload(GSON.toJson(inWorld), GSON.toJson(overlay));
    }

    private static void addContributedPayload(List<Map<String, Object>> target, SceneAnnotation annotation) {
        Map<String, Object> payload;
        try {
            payload = annotation.toSitePayload();
        } catch (RuntimeException e) {
            GuideDebugLog.error(
                "[GuideNH] [SceneExport] {} failed to describe itself for the site: {}",
                annotation.getClass()
                    .getSimpleName(),
                e.toString());
            return;
        }
        if (payload != null) {
            target.add(payload);
            return;
        }
        if (annotation instanceof InWorldAnnotation || annotation instanceof OverlayAnnotation) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [SceneExport] {} is not exported to the site: it does not describe a site payload",
                annotation.getClass()
                    .getSimpleName());
        }
    }

    private static Map<String, Object> serializeBox(InWorldBoxAnnotation box, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseInWorldAnnotation(
            "box",
            box.color(),
            box.thickness(),
            box.isAlwaysOnTop(),
            box.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        appendStructureLibCondition(data, box);
        data.put("minCorner", toVector(box.min()));
        data.put("maxCorner", toVector(box.max()));
        appendColorStyle(data, box.colorStyle());
        return data;
    }

    private static Map<String, Object> serializeLine(InWorldLineAnnotation line, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseInWorldAnnotation(
            "line",
            line.color(),
            line.thickness(),
            line.isAlwaysOnTop(),
            line.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        appendStructureLibCondition(data, line);
        data.put("from", toVector(line.from()));
        data.put("to", toVector(line.to()));
        data.put("points", toVectors(line.points()));
        if (line.arrow() != InWorldLineAnnotation.Arrow.NONE) {
            data.put(
                "arrow",
                line.arrow()
                    .serializedName());
        }
        if (line.showPoints()) {
            data.put("showPoints", true);
        }
        data.put("pointColor", toCssColor(line.pointColor()));
        data.put("pointSize", exportWorldAnnotationSize(line.pointSize()));
        if (!line.pointStyles()
            .isEmpty()) {
            List<Map<String, Object>> pointStyles = new ArrayList<>();
            for (InWorldLineAnnotation.PointStyle style : line.pointStyles()) {
                Map<String, Object> pointStyle = new LinkedHashMap<>();
                pointStyle.put("index", style.index());
                if (style.show() != null) {
                    pointStyle.put("show", style.show());
                }
                if (style.color() != null) {
                    pointStyle.put("color", toCssColor(style.color()));
                }
                if (style.size() != null) {
                    pointStyle.put("size", exportWorldAnnotationSize(style.size()));
                }
                pointStyles.add(pointStyle);
            }
            data.put("pointStyles", pointStyles);
        }
        return data;
    }

    private static Map<String, Object> serializeBlockOverlay(InWorldBlockFaceOverlayAnnotation blockOverlay,
        GuideSiteTemplateRegistry templates, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseInWorldAnnotation(
            "box",
            blockOverlay.color(),
            InWorldBoxAnnotation.DEFAULT_THICKNESS,
            blockOverlay.isAlwaysOnTop(),
            blockOverlay.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        appendStructureLibCondition(data, blockOverlay);
        data.put(
            "minCorner",
            new float[] { blockOverlay.getBlockX(), blockOverlay.getBlockY(), blockOverlay.getBlockZ() });
        data.put(
            "maxCorner",
            new float[] { blockOverlay.getBlockX() + 1f, blockOverlay.getBlockY() + 1f,
                blockOverlay.getBlockZ() + 1f });
        return data;
    }

    private static List<List<InWorldOutlineAnnotation>> groupOutlines(List<InWorldOutlineAnnotation> outlines) {
        Map<OutlineGroupKey, List<InWorldOutlineAnnotation>> groups = new LinkedHashMap<>();
        for (InWorldOutlineAnnotation outline : outlines) {
            groups.computeIfAbsent(OutlineGroupKey.of(outline), key -> new ArrayList<>())
                .add(outline);
        }
        return new ArrayList<>(groups.values());
    }

    private static Map<String, Object> serializeOutline(LytGuidebookScene scene, List<InWorldOutlineAnnotation> group,
        GuidebookSceneLayerSelection layerSelection, GuideSiteOutlineMeshExporter outlineMeshes,
        GuideSiteTemplateRegistry templates, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        InWorldOutlineAnnotation first = group.getFirst();
        Map<String, Object> data = createBaseInWorldAnnotation(
            "outline",
            first.getColor(),
            first.getThickness(),
            first.isAlwaysOnTop(),
            first.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        // Outline widths are screen pixels, unlike the world-space thickness of boxes and lines.
        data.put("thickness", first.getThickness());
        appendColorStyle(data, first.getColorStyle());
        if (first.getColorStyle()
            .isRainbow()) {
            OutlineFrame frame = new OutlineFrame();
            OutlineTargetResolver.collect(scene.getLevel(), first, layerSelection, 0L, frame);
            if (frame.batchCount() > 0) {
                data.put(
                    "colorCenter",
                    toVector(
                        frame.batch(0)
                            .getColorCenter()));
            }
        }
        appendStructureLibCondition(data, first);
        if (first.isPulse()) {
            data.put("pulse", true);
            data.put("pulsePeriod", first.getPulsePeriod());
        }
        GuideSiteOutlineMeshExporter.Meshes meshes = exportOutlineMeshes(outlineMeshes, group);
        if (meshes.geometryPath() != null) {
            data.put("geometrySrc", GuideSitePageAssetExporter.ROOT_PREFIX + meshes.geometryPath());
        }
        if (meshes.cutoutPath() != null) {
            data.put("cutoutSrc", GuideSitePageAssetExporter.ROOT_PREFIX + meshes.cutoutPath());
        }
        if (data.containsKey("contentTemplateId")) {
            data.put("pickBoxes", collectOutlinePickBoxes(scene, group, layerSelection));
        }
        return data;
    }

    private static void appendColorStyle(Map<String, Object> data, AnnotationColorStyle colorStyle) {
        data.put(
            "colorMode",
            colorStyle.mode()
                .serializedName());
        data.put("colorPeriod", colorStyle.period());
    }

    private static GuideSiteOutlineMeshExporter.Meshes exportOutlineMeshes(GuideSiteOutlineMeshExporter exporter,
        List<InWorldOutlineAnnotation> group) {
        try {
            return exporter.export(group);
        } catch (Exception e) {
            GuideDebugLog.warnAlways("[GuideNH] [SceneExport] Outline meshes could not be exported: {}", e.toString());
            return GuideSiteOutlineMeshExporter.Meshes.EMPTY;
        }
    }

    private static List<float[]> collectOutlinePickBoxes(LytGuidebookScene scene, List<InWorldOutlineAnnotation> group,
        GuidebookSceneLayerSelection layerSelection) {
        GuidebookLevel level = scene.getLevel();
        OutlineFrame frame = new OutlineFrame();
        for (InWorldOutlineAnnotation outline : group) {
            OutlineTargetResolver.collect(level, outline, layerSelection, 0L, frame);
        }
        List<float[]> boxes = new ArrayList<>();
        for (int i = 0; i < frame.batchCount(); i++) {
            OutlineBatch batch = frame.batch(i);
            appendPickBoxes(level, batch.getSolid(), boxes);
            appendPickBoxes(level, batch.getCutout(), boxes);
        }
        return boxes;
    }

    private static void appendPickBoxes(GuidebookLevel level, OutlineTargetSet targets, List<float[]> boxes) {
        for (int[] position : targets.blocks()) {
            AxisAlignedBB bounds = GuideBlockBoundsResolver
                .resolveSelectedBounds(level, position[0], position[1], position[2]);
            if (bounds != null) {
                boxes.add(toBox(bounds));
            }
        }
        for (Entity entity : targets.entities()) {
            if (entity.boundingBox != null) {
                boxes.add(toBox(entity.boundingBox));
            }
        }
        float[] raw = targets.boxes();
        for (int i = 0; i < targets.boxCount(); i++) {
            boxes.add(Arrays.copyOfRange(raw, i * 6, i * 6 + 6));
        }
    }

    private static float[] toBox(AxisAlignedBB box) {
        return new float[] { (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY,
            (float) box.maxZ };
    }

    private record OutlineGroupKey(@Nullable Integer color, float thickness, boolean alwaysOnTop, boolean pulse,
        float pulsePeriod, AnnotationColorStyle colorStyle, @Nullable GuideTooltip tooltip,
        @Nullable StructureLibSceneCondition condition, @Nullable InWorldOutlineAnnotation colorOrigin) {

        static OutlineGroupKey of(InWorldOutlineAnnotation outline) {
            return new OutlineGroupKey(
                outline.getColor() != null ? outline.getColor()
                    .resolve() : null,
                outline.getThickness(),
                outline.isAlwaysOnTop(),
                outline.isPulse(),
                outline.getPulsePeriod(),
                outline.getColorStyle(),
                outline.getTooltip(),
                outline.getStructureLibCondition(),
                outline.getColorStyle()
                    .isRainbow() ? outline : null);
        }
    }

    private static Map<String, Object> serializeDiamond(DiamondAnnotation diamond, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseOverlayAnnotation("diamond");
        data.put("position", toVector(diamond.getPos()));
        data.put("color", toCssColor(diamond.getColor()));
        appendStructureLibCondition(data, diamond);
        String templateId = createTemplateId(
            diamond.getTooltip(),
            templates,
            currentPageId,
            assetExporter,
            itemIconResolver);
        if (templateId != null) {
            data.put("contentTemplateId", templateId);
        }
        return data;
    }

    private static Map<String, Object> serializeTextAnnotation(TextAnnotation textAnnotation,
        GuideSiteTemplateRegistry templates, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseOverlayAnnotation("text");
        data.put("position", toVector(textAnnotation.getWorldPos()));
        data.put("color", toCssColor(textAnnotation.getBorderColor()));
        data.put("text", renderTextAnnotationHtml(textAnnotation, currentPageId, assetExporter, itemIconResolver));
        data.put("plainText", textAnnotation.getText());
        data.put("maxWidth", Math.max(0, textAnnotation.getMaxWidth()));
        data.put("backgroundAlpha", textAnnotation.getBackgroundAlpha());
        data.put("independent", textAnnotation.isIndependent());
        data.put("screenYOffset", textAnnotation.getScreenYOffset());
        data.put(
            "connectorSide",
            textAnnotation.getConnectorSide()
                .serializedName());
        data.put("connectorOffset", textAnnotation.getConnectorOffset());
        data.put("connectorLength", textAnnotation.getConnectorLength());
        appendStructureLibCondition(data, textAnnotation);
        return data;
    }

    private static Map<String, Object> serializeInputAnnotation(PonderInputAnnotation inputAnnotation,
        GuideSiteTemplateRegistry templates, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = createBaseOverlayAnnotation("input");
        data.put("position", toVector(inputAnnotation.getWorldPos()));
        data.put(
            "inputType",
            inputAnnotation.getInputType()
                .name()
                .toLowerCase());
        String modifier = inputAnnotation.getModifier();
        if (modifier != null && !modifier.trim()
            .isEmpty()) {
            data.put("modifier", modifier.trim());
        }
        ItemStack itemStack = inputAnnotation.getItemStack();
        if (itemStack != null && itemStack.stackSize > 0) {
            GuideSiteExportedItem exportedItem = GuideSiteItemSupport.export(itemStack, itemIconResolver);
            if (!exportedItem.isEmpty()) {
                data.put("item", serializeExportedItem(exportedItem));
            }
        }
        appendStructureLibCondition(data, inputAnnotation);
        return data;
    }

    private static void appendStructureLibCondition(Map<String, Object> data, SceneAnnotation annotation) {
        if (data == null || annotation == null
            || annotation.getStructureLibCondition() == null
            || !annotation.getStructureLibCondition()
                .hasAnyConstraint()) {
            return;
        }
        data.put(
            "structureLibCondition",
            annotation.getStructureLibCondition()
                .toSiteExportData());
    }

    private static Map<String, Object> createBaseInWorldAnnotation(String type, ColorValue color, float thickness,
        boolean alwaysOnTop, @Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("color", toCssColor(color));
        data.put("thickness", exportWorldAnnotationSize(thickness));
        data.put("alwaysOnTop", alwaysOnTop);
        String templateId = createTemplateId(tooltip, templates, currentPageId, assetExporter, itemIconResolver);
        if (templateId != null) {
            data.put("contentTemplateId", templateId);
        }
        return data;
    }

    private static Map<String, Object> createBaseOverlayAnnotation(String type) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        return data;
    }

    private static Map<String, Object> serializeExportedItem(GuideSiteExportedItem item) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("itemId", item.itemId());
        data.put("displayName", item.displayName());
        data.put("iconSrc", item.iconSrc());
        return data;
    }

    private static String renderTextAnnotationHtml(TextAnnotation textAnnotation,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        LytParagraph richContent = textAnnotation.getRichContent();
        if (richContent != null) {
            return TOOLTIP_RENDERER
                .renderBlock(richContent, currentPageId, assetExporter, itemIconResolver, null, false);
        }
        return GuideSiteLegacyTextRenderer.renderPlainTextFragment(textAnnotation.getText());
    }

    public static float exportWorldAnnotationSize(float size) {
        return Math.max(size / ANNOTATION_THICKNESS_SCALE, MIN_EXPORTED_WORLD_THICKNESS);
    }

    @Nullable
    private static String createTemplateId(@Nullable GuideTooltip tooltip, GuideSiteTemplateRegistry templates,
        @Nullable ResourceLocation currentPageId, @Nullable GuideSitePageAssetExporter assetExporter,
        GuideSiteItemIconResolver itemIconResolver) {
        return TOOLTIP_RENDERER.createTemplateId(tooltip, templates, currentPageId, assetExporter, itemIconResolver);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId) {
        return renderTooltipHtml(tooltip, currentPageId, null, GuideSiteItemIconResolver.NONE);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter) {
        return renderTooltipHtml(tooltip, currentPageId, assetExporter, GuideSiteItemIconResolver.NONE);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver) {
        return renderTooltipHtml(tooltip, currentPageId, assetExporter, itemIconResolver, null);
    }

    public static String renderTooltipHtml(@Nullable GuideTooltip tooltip, @Nullable ResourceLocation currentPageId,
        @Nullable GuideSitePageAssetExporter assetExporter, GuideSiteItemIconResolver itemIconResolver,
        @Nullable GuideSiteTemplateRegistry templates) {
        return TOOLTIP_RENDERER
            .render(tooltip, currentPageId, assetExporter, itemIconResolver, templates, templates != null);
    }

    public static String renderPlainTextFragment(@Nullable String text) {
        return GuideSiteLegacyTextRenderer.renderPlainTextFragment(text);
    }

    private static float[] toVector(Vector3f vector) {
        return new float[] { vector.x, vector.y, vector.z };
    }

    private static List<float[]> toVectors(List<Vector3f> vectors) {
        List<float[]> result = new ArrayList<>(vectors.size());
        for (Vector3f vector : vectors) {
            result.add(toVector(vector));
        }
        return result;
    }

    @Nullable
    private static GuideSiteExportedScene resolveExportedScene(@Nullable LytGuidebookScene scene) {
        if (scene == null) {
            return null;
        }
        return EXPORTED_SCENE_LOOKUP.get()
            .get(scene);
    }

    public static final class AnnotationPayload {

        private final String inWorldJson;
        private final String overlayJson;

        public AnnotationPayload(String inWorldJson, String overlayJson) {
            this.inWorldJson = inWorldJson;
            this.overlayJson = overlayJson;
        }

        public String inWorldJson() {
            return inWorldJson;
        }

        public String overlayJson() {
            return overlayJson;
        }
    }

    public static final class ExportedSceneLookupScope implements AutoCloseable {

        private final Map<LytGuidebookScene, GuideSiteExportedScene> previous;

        private ExportedSceneLookupScope(Map<LytGuidebookScene, GuideSiteExportedScene> previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            EXPORTED_SCENE_LOOKUP.set(previous != null ? previous : Map.of());
        }
    }

}
