package com.hfstudio.guidenh.guide.scene.outline;

/**
 * How a single target is rasterized into the outline mask.
 *
 * <ul>
 * <li>{@link #GEOMETRY}: every rendered fragment counts, so glass, ice, water and other closed but
 * see-through shapes keep their full model silhouette instead of their texture's frame.</li>
 * <li>{@link #CUTOUT}: fragments are kept only where the texture is opaque, so crossed-quad plants,
 * torches, rails and entity skins follow their drawn shape instead of their quad rectangles.</li>
 * <li>{@link #BOUNDS}: the selection box is drawn instead of the model, for blocks that render nothing
 * or when an author asks for a clean box.</li>
 * </ul>
 */
public enum OutlineSilhouette {

    GEOMETRY,
    CUTOUT,
    BOUNDS;

    public boolean usesTextureAlpha() {
        return this == CUTOUT;
    }
}
