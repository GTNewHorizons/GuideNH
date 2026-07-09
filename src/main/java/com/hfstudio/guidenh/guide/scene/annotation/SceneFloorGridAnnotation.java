package com.hfstudio.guidenh.guide.scene.annotation;

import lombok.Getter;
import lombok.Setter;

/**
 * An in-world annotation that renders a flat grid on a horizontal plane (typically Y=0),
 * covering the structure's XZ footprint extended by one block in each direction.
 *
 * <p>
 * Grid lines are drawn at every integer X and Z coordinate within the annotation's bounds.
 * When {@link #showDebugLabels} is {@code true}, coordinate numbers (X and Z axis) and
 * cardinal direction initials (N/S/E/W) are additionally rendered in 3D world space, flat
 * on the ground plane — exactly like the Ponder editor mode overlay.
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
