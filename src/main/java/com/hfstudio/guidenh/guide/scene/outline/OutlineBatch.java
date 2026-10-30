package com.hfstudio.guidenh.guide.scene.outline;

import lombok.Getter;

@Getter
public final class OutlineBatch {

    private int visibleArgb;
    private int occludedArgb;
    private float thickness;
    private final OutlineTargetSet solid = new OutlineTargetSet();
    private final OutlineTargetSet cutout = new OutlineTargetSet();

    void reset(int visibleArgb, int occludedArgb, float thickness) {
        this.visibleArgb = visibleArgb;
        this.occludedArgb = occludedArgb;
        this.thickness = thickness;
        solid.clear();
        cutout.clear();
    }

    boolean hasStyle(int visibleArgb, int occludedArgb, float thickness) {
        return this.visibleArgb == visibleArgb && this.occludedArgb == occludedArgb && this.thickness == thickness;
    }

    public OutlineTargetSet targets(OutlineSilhouette silhouette) {
        return silhouette.usesTextureAlpha() ? cutout : solid;
    }

    public boolean isEmpty() {
        return solid.isEmpty() && cutout.isEmpty();
    }
}
