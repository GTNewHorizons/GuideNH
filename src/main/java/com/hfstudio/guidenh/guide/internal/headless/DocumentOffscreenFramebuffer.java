package com.hfstudio.guidenh.guide.internal.headless;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GuideGlyphAtlas;
import com.hfstudio.guidenh.guide.render.GuideRenderEngine;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.GuidebookSceneRenderer;
import com.hfstudio.guidenh.guide.render.VanillaRenderContext;
import com.hfstudio.guidenh.guide.scene.GuidebookLevelRenderer;

/**
 * Renders a list of already-laid-out document primitives to an offscreen FBO and
 * reads back a {@link BufferedImage}. When the total height exceeds
 * {@code GL_MAX_TEXTURE_SIZE}, the document is split into tiles and composited.
 *
 * <p>This class does <em>not</em> perform layout — the caller is responsible for
 * laying out the document at the full render resolution and collecting primitives
 * via {@link com.hfstudio.guidenh.guide.render.PrimitiveCollector#result()}.
 * Each tile translates the viewport origin (camera pan equivalent) so that
 * the portion of the document starting at {@code (tileX, tileY)} is rendered
 * into the tile's framebuffer.
 */
public final class DocumentOffscreenFramebuffer {

    private static final int MAX_TILE_SIZE_CAP = 4096;
    /** 16384 * 4 */
    private static final int MAX_TOTAL_DIMENSION = 65536;

    private DocumentOffscreenFramebuffer() {
    }

    /**
     * Renders the given primitives at the specified total dimensions, tiling
     * transparently when the document exceeds {@code GL_MAX_TEXTURE_SIZE}.
     *
     * @param primitives  the already-collected primitives in document coordinates.
     * @param context     the render context whose document origin is adjusted per
     *                    tile (affects HostDraw callbacks).
     * @param totalWidth  full document width in document units (must be positive).
     * @param totalHeight full document height in document units (must be positive).
     * @param backgroundRgb opaque background colour packed as 0xRRGGBB.
     * @param scale       pixel-density multiplier (1-4). Output dimensions are
     *                    totalWidth × scale by totalHeight × scale.
     * @return a fully composited opaque {@code BufferedImage} of the document.
     * @throws IllegalArgumentException if dimensions are out of range.
     * @throws IllegalStateException    if the Minecraft client is not ready.
     * @throws RuntimeException         if any tile fails to render (FBO is cleaned
     *                                  up before rethrowing).
     */
    public static BufferedImage renderAll(
            List<GuideRenderPrimitive> primitives,
            VanillaRenderContext context,
            int totalWidth,
            int totalHeight,
            int backgroundRgb,
            int scale) {

        // ---- dimension validation -------------------------------------------
        if (totalWidth <= 0 || totalHeight <= 0) {
            throw new IllegalArgumentException(
                "totalWidth and totalHeight must be positive: " + totalWidth + " x " + totalHeight);
        }

        // Scale pixel dimensions
        int pxWidth = totalWidth * scale;
        int pxHeight = totalHeight * scale;

        if (pxWidth > MAX_TOTAL_DIMENSION || pxHeight > MAX_TOTAL_DIMENSION) {
            throw new IllegalArgumentException(
                "Pixel dimensions exceed maximum " + MAX_TOTAL_DIMENSION + ": "
                    + pxWidth + " x " + pxHeight);
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.gameSettings == null) {
            throw new IllegalStateException("Minecraft client is not ready for offscreen rendering.");
        }

        // ---- tile size ------------------------------------------------------
        int maxFboSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (maxFboSize <= 0) {
            maxFboSize = 8192; // safe fallback
        }
        int tileSize = Math.min(maxFboSize, MAX_TILE_SIZE_CAP);

        // ---- render engine (shared across tiles) ----------------------------
        GuideRenderEngine engine = new GuideRenderEngine(
            GuideGlyphAtlas.instance(),
            new GuidebookSceneRenderer());

        // ---- output image (scaled pixel dimensions) -------------------------
        BufferedImage output = new BufferedImage(pxWidth, pxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D outputG = output.createGraphics();
        outputG.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        boolean prevSkipLightmap = GuidebookLevelRenderer.skipLightmapForOffscreen;
        GuidebookLevelRenderer.skipLightmapForOffscreen = true;

        try {
            int prevDisplayWidth = minecraft.displayWidth;
            int prevDisplayHeight = minecraft.displayHeight;
            int prevGuiScale = minecraft.gameSettings.guiScale;
            context.setZoom(scale);

            // ---- tile loop (in pixel space, scaled) -------------------------
            for (int tileY = 0; tileY < pxHeight; tileY += tileSize) {
                int tileH = Math.min(tileSize, pxHeight - tileY);
                for (int tileX = 0; tileX < pxWidth; tileX += tileSize) {
                    int tileW = Math.min(tileSize, pxWidth - tileX);

                    Framebuffer fb = null;
                    boolean projectionPushed = false;
                    boolean modelviewPushed = false;

                    try {
                        fb = new Framebuffer(tileW, tileH, true);
                        fb.setFramebufferColor(0f, 0f, 0f, 0f);

                        minecraft.displayWidth = tileW;
                        minecraft.displayHeight = tileH;
                        minecraft.gameSettings.guiScale = 1;

                        fb.bindFramebuffer(true);

                        // Set up 2D orthographic projection for the tile dimensions
                        // (Vanilla GUI rendering relies on this, just like ItemPreviewService).
                        GL11.glMatrixMode(GL11.GL_PROJECTION);
                        GL11.glPushMatrix();
                        projectionPushed = true;
                        GL11.glLoadIdentity();
                        GL11.glOrtho(0.0D, tileW, tileH, 0.0D, 1000.0D, 3000.0D);

                        GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        GL11.glPushMatrix();
                        modelviewPushed = true;
                        GL11.glLoadIdentity();
                        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);

                        // Clear with the opaque background colour
                        float r = ((backgroundRgb >> 16) & 0xFF) / 255f;
                        float g = ((backgroundRgb >> 8) & 0xFF) / 255f;
                        float b = (backgroundRgb & 0xFF) / 255f;
                        GL11.glClearColor(r, g, b, 1f);
                        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

                        // Shift document origin so HostDraw callbacks see the tile offset
                        // Document-origin callback: pixel units (-tileX, -tileY)
                        context.setDocumentOrigin(-tileX, -tileY);

                        // Wrap primitives with scale + tile-offset transforms:
                        //   outer: PushTransform(0, 0, scale) — scales doc coords by N×
                        //   inner: PushTransform(-tileX/scale, -tileY/scale, 1.0f) — tile offset
                        //     in doc space, which after parent-scaling becomes -tileX, -tileY
                        // Combined: screen = doc * scale + (-tileX, -tileY)
                        List<GuideRenderPrimitive> wrapped = wrapForTile(primitives, tileX, tileY, scale);

                        engine.beginFrame(new LytRect(0, 0, tileW, tileH), 1.0f);
                        engine.execute(wrapped);
                        engine.endFrame();

                        // Read pixels (Y-flip to Java top-down)
                        BufferedImage tile = readPixels(tileW, tileH);
                        outputG.drawImage(tile, tileX, tileY, null);

                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException("Tile rendering failed at offset (" + tileX + ", " + tileY + ")", e);
                    } finally {
                        // Restore matrices in reverse order
                        if (modelviewPushed) {
                            GL11.glMatrixMode(GL11.GL_MODELVIEW);
                            GL11.glPopMatrix();
                        }
                        if (projectionPushed) {
                            GL11.glMatrixMode(GL11.GL_PROJECTION);
                            GL11.glPopMatrix();
                            GL11.glMatrixMode(GL11.GL_MODELVIEW);
                        }
                        // Release FBO resources
                        if (fb != null) {
                            fb.unbindFramebuffer();
                            fb.deleteFramebuffer();
                        }
                        // Restore original display dimensions
                        minecraft.displayWidth = prevDisplayWidth;
                        minecraft.displayHeight = prevDisplayHeight;
                        minecraft.gameSettings.guiScale = prevGuiScale;
                        GL11.glViewport(0, 0, prevDisplayWidth, prevDisplayHeight);
                    }
                }
            }
        } finally {
            GuidebookLevelRenderer.skipLightmapForOffscreen = prevSkipLightmap;
            outputG.dispose();
        }

        // ---- opaque background composite ------------------------------------
        return compositeOpaque(output, backgroundRgb);
    }

    // ---- helper: primitive list wrapping ------------------------------------

    /**
     * Wraps the original primitive list with:
     * <ol>
     *   <li>Outer {@link PushTransform}(0, 0, scale) — scales document coordinates
     *       by N× for pixel-density amplification.</li>
     *   <li>Inner {@link PushTransform}(-tileX/scale, -tileY/scale, 1.0f) — tile
     *       offset in document space.  The engine's composition applies parent
     *       scale to child translation, yielding effective offset
     *       {@code (-tileX, -tileY)} in the scaled pixel space.</li>
     * </ol>
     *
     * <p>Combined effective transform:
     * {@code screen = doc * scale + (-tileX, -tileY)}.
     *
     * <p>Tile coordinates are in pixel (scaled) space; the inner translation
     * divides by scale so the tile-border crossing matches document units.
     */
    private static List<GuideRenderPrimitive> wrapForTile(
            List<GuideRenderPrimitive> primitives,
            int tileX,
            int tileY,
            int scale) {
        List<GuideRenderPrimitive> wrapped = new ArrayList<>(primitives.size() + 4);
        // Outer: scale document coordinates
        wrapped.add(new GuideRenderPrimitive.PushTransform(0f, 0f, (float) scale));
        // Inner: tile offset in document space
        wrapped.add(new GuideRenderPrimitive.PushTransform(
            (float) -tileX / scale, (float) -tileY / scale, 1.0f));
        wrapped.addAll(primitives);
        wrapped.add(new GuideRenderPrimitive.PopTransform());
        wrapped.add(new GuideRenderPrimitive.PopTransform());
        return wrapped;
    }

    // ---- helper: read pixels from currently bound FBO -----------------------

    private static BufferedImage readPixels(int width, int height) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int flippedY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                int index = (x + y * width) * 4;
                int r = buffer.get(index) & 0xFF;
                int g = buffer.get(index + 1) & 0xFF;
                int b = buffer.get(index + 2) & 0xFF;
                int a = buffer.get(index + 3) & 0xFF;
                image.setRGB(x, flippedY, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    // ---- helper: composite onto opaque background ---------------------------

    private static BufferedImage compositeOpaque(BufferedImage source, int backgroundRgb) {
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(backgroundRgb));
            g.fillRect(0, 0, source.getWidth(), source.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return image;
    }
}
