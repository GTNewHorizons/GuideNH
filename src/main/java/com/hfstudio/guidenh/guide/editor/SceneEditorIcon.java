package com.hfstudio.guidenh.guide.editor;

import java.util.Objects;

import net.minecraft.util.ResourceLocation;

/**
 * Identifies a rectangular sprite in a texture atlas used by a Scene Editor toolbar button.
 *
 * <p>
 * All source coordinates and dimensions are measured in atlas pixels. The atlas dimensions are
 * supplied separately because the renderer uses them to convert the source rectangle to texture
 * coordinates; they do not imply that the resource must be a particular image size.
 * </p>
 */
public class SceneEditorIcon {

    protected ResourceLocation texture;
    protected int textureWidth;
    protected int textureHeight;
    protected int sourceX;
    protected int sourceY;
    protected int sourceWidth;
    protected int sourceHeight;

    protected SceneEditorIcon() {}

    public SceneEditorIcon(ResourceLocation texture, int textureWidth, int textureHeight, int sourceX, int sourceY,
        int sourceWidth, int sourceHeight) {
        this.texture = Objects.requireNonNull(texture, "texture");
        if (textureWidth <= 0 || textureHeight <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            throw new IllegalArgumentException("Icon dimensions must be positive");
        }
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    /** Returns the texture resource containing this sprite. */
    public ResourceLocation texture() {
        return texture;
    }

    /** Returns the full texture width in pixels used for UV conversion. */
    public int textureWidth() {
        return textureWidth;
    }

    /** Returns the full texture height in pixels used for UV conversion. */
    public int textureHeight() {
        return textureHeight;
    }

    /** Returns the sprite's left coordinate in the texture atlas. */
    public int sourceX() {
        return sourceX;
    }

    /** Returns the sprite's top coordinate in the texture atlas. */
    public int sourceY() {
        return sourceY;
    }

    /** Returns the sprite width in atlas pixels. */
    public int sourceWidth() {
        return sourceWidth;
    }

    /** Returns the sprite height in atlas pixels. */
    public int sourceHeight() {
        return sourceHeight;
    }
}
