package com.hfstudio.guidenh.guide.scene.outline;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.GuidebookSceneLayerSelection;
import com.hfstudio.guidenh.guide.scene.LytGuidebookScene;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotation;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldAnnotationRenderer;
import com.hfstudio.guidenh.guide.scene.annotation.InWorldOutlineAnnotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockBoundsResolver;

public final class OutlineTargetResolver {

    public static final int DEFAULT_ARGB = 0xFFFFFFFF;
    private static final int HOVER_LIGHTEN_PERCENT = 50;
    private static final int OCCLUDED_DARKEN_PERCENT = 50;
    private static final double DRAW_MARGIN = 0.5;
    private static final String CHAT_COLOR_CODES = "0123456789abcdef";
    private static final int[] CHAT_COLOR_RGB = { 0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00,
        0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };

    public static void collect(GuidebookLevel level, Iterable<? extends InWorldAnnotation> annotations,
        @Nullable GuidebookSceneLayerSelection layers, long timeMillis, OutlineFrame frame) {
        frame.clear();
        GuidebookSceneLayerSelection effectiveLayers = layers != null ? layers : GuidebookSceneLayerSelection.all();
        for (InWorldAnnotation annotation : annotations) {
            if (annotation instanceof InWorldOutlineAnnotation outline) {
                collect(level, outline, effectiveLayers, timeMillis, frame);
            }
        }
    }

    public static void collect(GuidebookLevel level, InWorldOutlineAnnotation outline,
        GuidebookSceneLayerSelection layers, long timeMillis, OutlineFrame frame) {
        float intensity = outline.pulseIntensity(timeMillis);
        if (outline.outlinesRegionBlocks()) {
            OutlineRegion region = outline.getRegion();
            OutlineBatch batch = batchFor(frame, outline, intensity, blockColor(outline));
            if (region.volume() <= level.getFilledBlocks()
                .size()) {
                collectRegionCells(level, outline, region, layers, batch, frame);
            } else {
                collectFilledBlocks(level, outline, region, layers, batch, frame);
            }
        }
        if (outline.targetsEntities()) {
            collectEntities(level, outline, layers, intensity, frame);
        }
    }

    public static boolean intersectsRay(GuidebookLevel level, InWorldOutlineAnnotation outline, float[] ray) {
        if (outline.outlinesRegionBlocks() && intersectsRegionBlocks(level, outline.getRegion(), ray)) {
            return true;
        }
        if (!outline.targetsEntities()) {
            return false;
        }
        for (Entity entity : level.getEntities()) {
            AxisAlignedBB box = entity.boundingBox;
            if (box != null && matchesEntity(level, outline, entity)
                && hitsBox(
                    ray,
                    (float) box.minX,
                    (float) box.minY,
                    (float) box.minZ,
                    (float) box.maxX,
                    (float) box.maxY,
                    (float) box.maxZ)) {
                return true;
            }
        }
        return false;
    }

    public static int teamColor(Entity entity, int fallback) {
        if (!(entity instanceof EntityLivingBase living)) {
            return fallback;
        }
        Team team;
        try {
            team = living.getTeam();
        } catch (RuntimeException ignored) {
            return fallback;
        }
        if (!(team instanceof ScorePlayerTeam playerTeam)) {
            return fallback;
        }
        String prefix = playerTeam.getColorPrefix();
        for (int i = prefix.length() - 2; i >= 0; i--) {
            if (prefix.charAt(i) != '§') {
                continue;
            }
            int code = CHAT_COLOR_CODES.indexOf(Character.toLowerCase(prefix.charAt(i + 1)));
            if (code >= 0) {
                return 0xFF000000 | CHAT_COLOR_RGB[code];
            }
        }
        return fallback;
    }

    private static boolean intersectsRegionBlocks(GuidebookLevel level, OutlineRegion region, float[] ray) {
        if (!hitsCell(ray, region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ())) {
            return false;
        }
        if (region.volume() > level.getFilledBlocks()
            .size()) {
            for (int[] position : level.getFilledBlocks()) {
                if (region.contains(position[0], position[1], position[2])
                    && hitsCell(ray, position[0], position[1], position[2], position[0], position[1], position[2])) {
                    return true;
                }
            }
            return false;
        }
        for (int x = region.minX(); x <= region.maxX(); x++) {
            for (int y = region.minY(); y <= region.maxY(); y++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    if (level.getBlock(x, y, z) != Blocks.air && hitsCell(ray, x, y, z, x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void collectRegionCells(GuidebookLevel level, InWorldOutlineAnnotation outline, OutlineRegion region,
        GuidebookSceneLayerSelection layers, OutlineBatch batch, OutlineFrame frame) {
        for (int y = region.minY(); y <= region.maxY(); y++) {
            if (!layers.isLayerVisible(y)) {
                continue;
            }
            for (int x = region.minX(); x <= region.maxX(); x++) {
                for (int z = region.minZ(); z <= region.maxZ(); z++) {
                    Block block = level.getBlock(x, y, z);
                    if (block != Blocks.air) {
                        addBlock(level, outline, block, frame.position(x, y, z), batch, frame);
                    }
                }
            }
        }
    }

    private static void collectFilledBlocks(GuidebookLevel level, InWorldOutlineAnnotation outline,
        OutlineRegion region, GuidebookSceneLayerSelection layers, OutlineBatch batch, OutlineFrame frame) {
        for (int[] position : level.getFilledBlocks()) {
            if (!region.contains(position[0], position[1], position[2]) || !layers.isLayerVisible(position[1])) {
                continue;
            }
            Block block = level.getBlock(position[0], position[1], position[2]);
            if (block != Blocks.air) {
                addBlock(level, outline, block, position, batch, frame);
            }
        }
    }

    private static void addBlock(GuidebookLevel level, InWorldOutlineAnnotation outline, Block block, int[] position,
        OutlineBatch batch, OutlineFrame frame) {
        TileEntity tile = level.getTileEntity(position[0], position[1], position[2]);
        boolean hasSpecialRenderer = tile != null && TileEntityRendererDispatcher.instance.hasSpecialRenderer(tile);
        OutlineSilhouette silhouette = OutlineSilhouetteClassifier
            .forBlock(block, hasSpecialRenderer, outline.getMode());
        OutlineTargetSet targets = batch.targets(silhouette);
        if (silhouette == OutlineSilhouette.BOUNDS) {
            AxisAlignedBB bounds = GuideBlockBoundsResolver
                .resolveSelectedBounds(level, position[0], position[1], position[2]);
            if (bounds != null) {
                addBox(targets, frame, bounds);
            } else {
                targets
                    .addBox(position[0], position[1], position[2], position[0] + 1, position[1] + 1, position[2] + 1);
                frame.include(position[0], position[1], position[2], position[0] + 1, position[1] + 1, position[2] + 1);
            }
            return;
        }
        targets.addBlock(position);
        frame.include(
            position[0] - DRAW_MARGIN,
            position[1] - DRAW_MARGIN,
            position[2] - DRAW_MARGIN,
            position[0] + 1 + DRAW_MARGIN,
            position[1] + 1 + DRAW_MARGIN,
            position[2] + 1 + DRAW_MARGIN);
        if (hasSpecialRenderer) {
            targets.addTile(tile);
            includeRenderBounds(frame, tile);
        }
    }

    private static void addBox(OutlineTargetSet targets, OutlineFrame frame, AxisAlignedBB box) {
        targets.addBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        frame.include(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    private static void includeRenderBounds(OutlineFrame frame, TileEntity tile) {
        AxisAlignedBB box;
        try {
            box = tile.getRenderBoundingBox();
        } catch (RuntimeException ignored) {
            box = null;
        }
        if (box == null || box == TileEntity.INFINITE_EXTENT_AABB) {
            frame.includeEverything();
            return;
        }
        includeWithMargin(frame, box);
    }

    private static void includeWithMargin(OutlineFrame frame, AxisAlignedBB box) {
        frame.include(
            box.minX - DRAW_MARGIN,
            box.minY - DRAW_MARGIN,
            box.minZ - DRAW_MARGIN,
            box.maxX + DRAW_MARGIN,
            box.maxY + DRAW_MARGIN,
            box.maxZ + DRAW_MARGIN);
    }

    private static void collectEntities(GuidebookLevel level, InWorldOutlineAnnotation outline,
        GuidebookSceneLayerSelection layers, float intensity, OutlineFrame frame) {
        OutlineSilhouette silhouette = OutlineSilhouetteClassifier.forEntity(outline.getMode());
        for (Entity entity : level.getEntities()) {
            if (entity == null || entity.isDead || !matchesEntity(level, outline, entity)) {
                continue;
            }
            AxisAlignedBB box = entity.boundingBox;
            if (box != null && !intersectsLayers(box, layers)) {
                continue;
            }
            OutlineTargetSet targets = batchFor(frame, outline, intensity, entityColor(outline, entity))
                .targets(silhouette);
            if (silhouette == OutlineSilhouette.BOUNDS && box != null) {
                addBox(targets, frame, box);
                continue;
            }
            targets.addEntity(entity);
            if (box == null || entity.ignoreFrustumCheck) {
                frame.includeEverything();
            } else {
                includeWithMargin(frame, box);
            }
        }
    }

    private static boolean matchesEntity(GuidebookLevel level, InWorldOutlineAnnotation outline, Entity entity) {
        String sceneEntityId = outline.getSceneEntityId();
        if (sceneEntityId != null && sceneEntityId.equals(level.getSceneEntityId(entity.getEntityId()))) {
            return true;
        }
        if (!outline.getScope()
            .isEntities()) {
            return false;
        }
        OutlineRegion region = outline.getRegion();
        if (region == null) {
            return sceneEntityId == null;
        }
        return entity.boundingBox != null && region.intersects(entity.boundingBox);
    }

    private static boolean intersectsLayers(AxisAlignedBB box, GuidebookSceneLayerSelection layers) {
        if (layers.getMode() == GuidebookSceneLayerSelection.Mode.ALL) {
            return true;
        }
        int minLayer = (int) Math.floor(box.minY);
        int maxLayer = Math.max(minLayer, (int) Math.ceil(box.maxY) - 1);
        for (int layer = minLayer; layer <= maxLayer; layer++) {
            if (layers.isLayerVisible(layer)) {
                return true;
            }
        }
        return false;
    }

    private static int blockColor(InWorldOutlineAnnotation outline) {
        return outline.getColor() != null ? outline.getColor()
            .resolve() : DEFAULT_ARGB;
    }

    private static int entityColor(InWorldOutlineAnnotation outline, Entity entity) {
        return outline.getColor() != null ? outline.getColor()
            .resolve() : teamColor(entity, DEFAULT_ARGB);
    }

    private static OutlineBatch batchFor(OutlineFrame frame, InWorldOutlineAnnotation outline, float intensity,
        int baseArgb) {
        int argb = baseArgb;
        if (outline.isHovered()) {
            argb = InWorldAnnotationRenderer.lighter(argb, HOVER_LIGHTEN_PERCENT);
        }
        int visible = scaleAlpha(argb, intensity);
        int occluded = outline.isAlwaysOnTop() ? visible
            : scaleAlpha(InWorldAnnotationRenderer.darker(visible, OCCLUDED_DARKEN_PERCENT), 0.5f);
        return frame.batchFor(visible, occluded, outline.getThickness());
    }

    private static int scaleAlpha(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * factor);
        return (Math.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    private static boolean hitsCell(float[] ray, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return hitsBox(ray, minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    private static boolean hitsBox(float[] ray, float minX, float minY, float minZ, float maxX, float maxY,
        float maxZ) {
        float t = LytGuidebookScene
            .rayAabb(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5], minX, minY, minZ, maxX, maxY, maxZ);
        return !Float.isNaN(t) && t >= 0f;
    }
}
