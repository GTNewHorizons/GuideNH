package com.hfstudio.guidenh.guide.scene.outline;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Reusable per-frame container of outline batches. Batches and scratch positions are pooled, so a
 * steady scene does not allocate while its outlines are rebuilt every frame. The frame also keeps the
 * world-space union of everything its targets may draw, which lets backends skip untouched pixels.
 */
public final class OutlineFrame {

    private final List<OutlineBatch> batches = new ArrayList<>();
    private final List<int[]> positions = new ArrayList<>();
    private int batchCount;
    private int positionCount;
    private final float[] bounds = new float[6];
    private boolean hasBounds;
    private boolean unbounded;

    public void clear() {
        batchCount = 0;
        positionCount = 0;
        hasBounds = false;
        unbounded = false;
    }

    public boolean isUnbounded() {
        return unbounded;
    }

    @Nullable
    public float[] bounds() {
        return hasBounds && !unbounded ? bounds : null;
    }

    void include(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!hasBounds) {
            hasBounds = true;
            bounds[0] = (float) minX;
            bounds[1] = (float) minY;
            bounds[2] = (float) minZ;
            bounds[3] = (float) maxX;
            bounds[4] = (float) maxY;
            bounds[5] = (float) maxZ;
            return;
        }
        bounds[0] = Math.min(bounds[0], (float) minX);
        bounds[1] = Math.min(bounds[1], (float) minY);
        bounds[2] = Math.min(bounds[2], (float) minZ);
        bounds[3] = Math.max(bounds[3], (float) maxX);
        bounds[4] = Math.max(bounds[4], (float) maxY);
        bounds[5] = Math.max(bounds[5], (float) maxZ);
    }

    void includeEverything() {
        unbounded = true;
    }

    public int batchCount() {
        return batchCount;
    }

    public OutlineBatch batch(int index) {
        return batches.get(index);
    }

    public boolean isEmpty() {
        for (int i = 0; i < batchCount; i++) {
            if (!batches.get(i)
                .isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public float maxThickness() {
        float max = 0f;
        for (int i = 0; i < batchCount; i++) {
            max = Math.max(
                max,
                batches.get(i)
                    .getThickness());
        }
        return max;
    }

    OutlineBatch batchFor(int visibleArgb, int occludedArgb, float thickness) {
        for (int i = 0; i < batchCount; i++) {
            OutlineBatch batch = batches.get(i);
            if (batch.hasStyle(visibleArgb, occludedArgb, thickness)) {
                return batch;
            }
        }
        if (batchCount == batches.size()) {
            batches.add(new OutlineBatch());
        }
        OutlineBatch batch = batches.get(batchCount++);
        batch.reset(visibleArgb, occludedArgb, thickness);
        return batch;
    }

    int[] position(int x, int y, int z) {
        if (positionCount == positions.size()) {
            positions.add(new int[3]);
        }
        int[] position = positions.get(positionCount++);
        position[0] = x;
        position[1] = y;
        position[2] = z;
        return position;
    }
}
