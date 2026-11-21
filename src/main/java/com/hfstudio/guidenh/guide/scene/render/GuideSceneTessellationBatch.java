package com.hfstudio.guidenh.guide.scene.render;

import net.minecraft.client.renderer.Tessellator;

public class GuideSceneTessellationBatch implements AutoCloseable {

    private static final int DEFAULT_BLOCK_LIMIT = 4096;
    private static final int PREVIEW_BRIGHTNESS = (15 << 20) | (15 << 4);

    private final Tessellator tessellator;
    private final int blockLimit;
    private int submittedBlocks;
    private boolean drawing;

    public GuideSceneTessellationBatch(Tessellator tessellator) {
        this(tessellator, DEFAULT_BLOCK_LIMIT);
    }

    public GuideSceneTessellationBatch(Tessellator tessellator, int blockLimit) {
        if (blockLimit <= 0) {
            throw new IllegalArgumentException("Block batch limit must be positive");
        }
        this.tessellator = tessellator;
        this.blockLimit = blockLimit;
        begin();
    }

    public void blockSubmitted() {
        if (++submittedBlocks >= blockLimit) {
            finish();
            begin();
        }
    }

    private void begin() {
        tessellator.startDrawingQuads();
        drawing = true;
        tessellator.setBrightness(PREVIEW_BRIGHTNESS);
        submittedBlocks = 0;
    }

    private void finish() {
        if (drawing) {
            drawing = false;
            tessellator.draw();
        }
    }

    @Override
    public void close() {
        finish();
    }
}
