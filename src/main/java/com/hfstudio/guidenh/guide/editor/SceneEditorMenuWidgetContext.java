package com.hfstudio.guidenh.guide.editor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Rendering services supplied by the Scene Editor to a {@link SceneEditorMenuWidget}.
 *
 * <p>
 * The context is created for a menu draw pass and provides the client font renderer, Minecraft
 * instance, and the primitive operations permitted to a registered widget. Coordinates passed to
 * drawing methods are screen pixels in the widget's menu coordinate space.
 * </p>
 */
public interface SceneEditorMenuWidgetContext {

    /** Returns the active Minecraft client instance. */
    Minecraft minecraft();

    /** Returns the font renderer used by the editor menu. */
    FontRenderer fontRenderer();

    /** Draws a filled, half-open rectangle. */
    void drawRect(int left, int top, int right, int bottom, int color);

    /** Draws a one-pixel border around a rectangle. */
    void drawBorder(int left, int top, int width, int height, int color);

    /** Draws a string at the supplied pixel position. */
    void drawString(String text, int x, int y, int color);
}
