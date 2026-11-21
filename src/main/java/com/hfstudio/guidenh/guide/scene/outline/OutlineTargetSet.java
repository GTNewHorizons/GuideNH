package com.hfstudio.guidenh.guide.scene.outline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public final class OutlineTargetSet {

    private static final int BOX_STRIDE = 6;

    private final List<int[]> blocks = new ArrayList<>();
    private final List<TileEntity> tiles = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private float[] boxes = new float[BOX_STRIDE * 8];
    private int boxCount;

    void clear() {
        blocks.clear();
        tiles.clear();
        entities.clear();
        boxCount = 0;
    }

    public boolean isEmpty() {
        return blocks.isEmpty() && tiles.isEmpty() && entities.isEmpty() && boxCount == 0;
    }

    void addBlock(int[] position) {
        blocks.add(position);
    }

    void addTile(TileEntity tile) {
        tiles.add(tile);
    }

    void addEntity(Entity entity) {
        entities.add(entity);
    }

    void addBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        int offset = boxCount * BOX_STRIDE;
        if (offset + BOX_STRIDE > boxes.length) {
            boxes = Arrays.copyOf(boxes, boxes.length * 2);
        }
        boxes[offset] = (float) minX;
        boxes[offset + 1] = (float) minY;
        boxes[offset + 2] = (float) minZ;
        boxes[offset + 3] = (float) maxX;
        boxes[offset + 4] = (float) maxY;
        boxes[offset + 5] = (float) maxZ;
        boxCount++;
    }

    public List<int[]> blocks() {
        return blocks;
    }

    public List<TileEntity> tiles() {
        return tiles;
    }

    public List<Entity> entities() {
        return entities;
    }

    public int boxCount() {
        return boxCount;
    }

    public float[] boxes() {
        return boxes;
    }
}
