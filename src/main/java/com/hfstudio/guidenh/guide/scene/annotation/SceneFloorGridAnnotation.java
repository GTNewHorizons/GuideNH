package com.hfstudio.guidenh.guide.scene.annotation;

import lombok.Getter;
import lombok.Setter;

/**
 * Horizontal grid over the structure's XZ footprint with a one-block margin. Optional debug labels show
 * coordinates and cardinal directions.
 */
@Getter
public class SceneFloorGridAnnotation extends InWorldAnnotation {

    private final int minX;
    private final int minZ;
    private final int maxX;
    private final int maxZ;
    private final float y;
    @Setter
    private boolean showDebugLabels;

    /**
     * @param minX inclusive grid start X (one block outside structure minX)
     * @param minZ inclusive grid start Z (one block outside structure minZ)
     * @param maxX inclusive grid end X (one block outside structure maxX+1)
     * @param maxZ inclusive grid end Z (one block outside structure maxZ+1)
     * @param y    world Y at which the grid plane is rendered
     */
    public SceneFloorGridAnnotation(int minX, int minZ, int maxX, int maxZ, float y) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.y = y;
        setAlwaysOnTop(false);
    }

}
