package com.hfstudio.guidenh.guide.render;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.GLAllocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Glyph atlas: manages a single GL texture containing rasterized glyphs from Rust cosmic-text.
 * <p>
 * Upload strategy: each glyph is packed into a growing atlas texture.
 * Phase 1 uses simple row-packing; future phases may use a proper bin-packing algorithm.
 */
public class GuideGlyphAtlas {

    private static final int ATLAS_SIZE = 1024;
    private static final int PADDING = 1;

    private int textureId = -1;
    private final ByteBuffer atlasBuffer = GLAllocation.createDirectByteBuffer(ATLAS_SIZE * ATLAS_SIZE * 4);
    private int cursorX = PADDING;
    private int cursorY = PADDING;
    private int currentRowHeight = 0;

    private final Map<Integer, GlyphUV> glyphCache = new HashMap<>();

    /** Upload a glyph to the atlas and return its UV coordinates. */
    public synchronized GlyphUV upload(int glyphId, byte[] rgba, int w, int h) {
        GlyphUV cached = glyphCache.get(glyphId);
        if (cached != null) return cached;

        if (cursorX + w + PADDING > ATLAS_SIZE) {
            cursorX = PADDING;
            cursorY += currentRowHeight + PADDING;
            currentRowHeight = 0;
        }
        if (cursorY + h + PADDING > ATLAS_SIZE) {
            // Atlas full — clear and re-pack (Phase 1: just clear)
            clear();
            cursorX = PADDING;
            cursorY = PADDING;
            currentRowHeight = 0;
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

        // Upload to GL texture
        ensureTexture();
        atlasBuffer.position(0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, u, v, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, atlasBuffer);

        float texSize = ATLAS_SIZE;
        GlyphUV uv = new GlyphUV(u / texSize, v / texSize, (u + w) / texSize, (v + h) / texSize);
        glyphCache.put(glyphId, uv);
        return uv;
    }

    /** Look up a glyph's UV coordinates in the atlas. */
    public @Nullable GlyphUV lookup(int glyphId) {
        return glyphCache.get(glyphId);
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
        if (textureId >= 0) {
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
        if (textureId >= 0) return;
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
