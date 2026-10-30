package com.hfstudio.guidenh.guide.scene.outline;

import java.util.List;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;

/**
 * Draws outline targets with the scene's own render paths, so whatever a block or entity looks like in
 * the scene (ISBRH, multipart, TESR, OBJ model) is exactly what ends up in the mask. Callers own the GL
 * state; {@code beforeTarget} lets them restore it after renderers that toggle blending or programs.
 */
public interface OutlineTargetDrawer {

    /** Renders static block models of both render passes with all faces enabled. */
    void drawBlocks(GuidebookLevel level, List<int[]> blocks);

    void drawTileEntity(TileEntity tile, float partialTicks);

    /** Renders the entity model only, without shadow, fire or name plate, and ignoring invisibility. */
    void drawEntity(Entity entity, float partialTicks);

    default void draw(GuidebookLevel level, OutlineTargetSet targets, float partialTicks, Runnable beforeTarget) {
        if (!targets.blocks()
            .isEmpty()) {
            beforeTarget.run();
            drawBlocks(level, targets.blocks());
        }
        for (TileEntity tile : targets.tiles()) {
            beforeTarget.run();
            drawTileEntity(tile, partialTicks);
        }
        for (Entity entity : targets.entities()) {
            beforeTarget.run();
            drawEntity(entity, partialTicks);
        }
        if (targets.boxCount() > 0) {
            beforeTarget.run();
            drawBoxes(targets);
        }
    }

    static void drawBoxes(OutlineTargetSet targets) {
        float[] boxes = targets.boxes();
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int i = 0; i < targets.boxCount(); i++) {
            int o = i * 6;
            addBox(tessellator, boxes[o], boxes[o + 1], boxes[o + 2], boxes[o + 3], boxes[o + 4], boxes[o + 5]);
        }
        tessellator.draw();
    }

    private static void addBox(Tessellator t, float x0, float y0, float z0, float x1, float y1, float z1) {
        t.addVertex(x0, y0, z0);
        t.addVertex(x1, y0, z0);
        t.addVertex(x1, y0, z1);
        t.addVertex(x0, y0, z1);
        t.addVertex(x0, y1, z0);
        t.addVertex(x0, y1, z1);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y1, z0);
        t.addVertex(x0, y0, z0);
        t.addVertex(x0, y1, z0);
        t.addVertex(x1, y1, z0);
        t.addVertex(x1, y0, z0);
        t.addVertex(x0, y0, z1);
        t.addVertex(x1, y0, z1);
        t.addVertex(x1, y1, z1);
        t.addVertex(x0, y1, z1);
        t.addVertex(x0, y0, z0);
        t.addVertex(x0, y0, z1);
        t.addVertex(x0, y1, z1);
        t.addVertex(x0, y1, z0);
        t.addVertex(x1, y0, z0);
        t.addVertex(x1, y1, z0);
        t.addVertex(x1, y1, z1);
        t.addVertex(x1, y0, z1);
    }
}
