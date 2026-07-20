package com.hfstudio.guidenh.guide.render;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.GLAllocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Glyph atlas: manages a single GL texture containing rasterized glyphs from Rust cosmic-text.
 * <p>
 * Upload strategy: each glyph is packed into a growing atlas texture.
 * Phase 1 uses simple row-packing; future phases may use a proper bin-packing algorithm.
 */
public class GuideGlyphAtlas {

    /**
     * Default atlas instance for measureLayout processing.
     * The render engine can override with setGlobalInstance().
     */
    private static GuideGlyphAtlas globalInstance = new GuideGlyphAtlas();

    public static GuideGlyphAtlas instance() {
        return globalInstance;
    }

    public static void setGlobalInstance(GuideGlyphAtlas atlas) {
        globalInstance = atlas;
    }

    public GuideGlyphAtlas() {}

    private static final int ATLAS_SIZE = 2048;
    private static final int PADDING = 1;

    private int textureId = -1;
    private final ByteBuffer atlasBuffer = GLAllocation.createDirectByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
    private int cursorX = PADDING;
    private int cursorY = PADDING;
    private int currentRowHeight = 0;

    private final Map<Long, GlyphUV> glyphCache = new HashMap<>();

    /** Test hook: when true, all GL calls are skipped (packing/UV bookkeeping still runs). */
    private boolean headless;

    /** Test hook for headless environments (unit tests without a GL context). */
    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    /**
     * Upload a glyph bitmap to the atlas and return its UV coordinates.
     * {@code key} is the opaque bitmap key from LayoutResult (content-stable
     * across layout rebuilds); repeated uploads of the same key are no-ops.
     */
    @Nullable
    public synchronized GlyphUV upload(long key, byte[] rgba, int w, int h) {
        GlyphUV cached = glyphCache.get(key);
        if (cached != null) return cached;

        if (cursorX + w + PADDING > ATLAS_SIZE) {
            cursorX = PADDING;
            cursorY += currentRowHeight + PADDING;
            currentRowHeight = 0;
        }
        if (cursorY + h + PADDING > ATLAS_SIZE) {
            // Atlas full: drop the NEW glyph instead of wiping the cache.
            // Clearing would invalidate every atlasKey already emitted this
            // frame — whole paragraphs vanished until the next layout (C-3).
            // Returning null makes the glyph miss silently (one glyph, once),
            // which is strictly better than mass eviction.
            GuideDebugLog.warnAlways("[GuideNH] glyph atlas full, dropping glyph key={} ({}x{})", key, w, h);
            return null;
        }

        int u = cursorX;
        int v = cursorY;

        // Write glyph pixels into atlas buffer
        for (int gy = 0; gy < h; gy++) {
            for (int gx = 0; gx < w; gx++) {
                int srcIdx = (gy * w + gx) * 4;
                int dstIdx = ((cursorY + gy) * ATLAS_SIZE + (cursorX + gx)) * 4;
                atlasBuffer.put(dstIdx, rgba[srcIdx]);
                atlasBuffer.put(dstIdx + 1, rgba[srcIdx + 1]);
                atlasBuffer.put(dstIdx + 2, rgba[srcIdx + 2]);
                atlasBuffer.put(dstIdx + 3, rgba[srcIdx + 3]);
            }
        }

        if (h > currentRowHeight) currentRowHeight = h;
        cursorX += w + PADDING;

        if (!headless) {
            // Upload to GL texture
            ensureTexture();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            // Set UNPACK_ROW_LENGTH so glTexSubImage2D reads rows matching the atlas stride,
            // not the sub-image width. Position the buffer to the start of the glyph's row.
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, ATLAS_SIZE);
            atlasBuffer.position((v * ATLAS_SIZE + u) * 4);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, u, v, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, atlasBuffer);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            atlasBuffer.position(0);
        }

        float texSize = ATLAS_SIZE;
        GlyphUV uv = new GlyphUV(u / texSize, v / texSize, (u + w) / texSize, (v + h) / texSize);
        glyphCache.put(key, uv);
        return uv;
    }

    /** Look up a glyph bitmap's UV coordinates in the atlas. */
    public @Nullable GlyphUV lookup(long key) {
        return glyphCache.get(key);
    }

    /** Get the GL texture name for this atlas. */
    public int getTextureId() {
        ensureTexture();
        return textureId;
    }

    /** Clear the atlas. */
    public void clear() {
        glyphCache.clear();
        for (int i = 0; i < atlasBuffer.capacity(); i++) {
            atlasBuffer.put(i, (byte) 0);
        }
        cursorX = PADDING;
        cursorY = PADDING;
        currentRowHeight = 0;
        if (textureId >= 0 && !headless) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                ATLAS_SIZE,
                ATLAS_SIZE,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                atlasBuffer);
        }
    }

    /** Delete the GL texture. */
    public void delete() {
        if (textureId >= 0) {
            GL11.glDeleteTextures(textureId);
            textureId = -1;
        }
        glyphCache.clear();
    }

    private void ensureTexture() {
        if (textureId >= 0 || headless) return;
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            ATLAS_SIZE,
            ATLAS_SIZE,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);
        GL11.glTexSubImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            0,
            0,
            ATLAS_SIZE,
            ATLAS_SIZE,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            atlasBuffer);
    }

    public record GlyphUV(float u, float v, float u2, float v2) {}
}
