package com.hfstudio.guidenh.guide.render;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Sealed interface for all render primitives consumed by GuideRenderEngine.
 * <p>
 * 20 total: 6 state instructions + 14 draw primitives.
 * <p>
 * All coordinates are absolute document coordinates unless stated otherwise.
 * GuideRenderEngine applies the current transform stack to convert to screen coordinates:
 * {@code screen = doc * scale + (tx, ty)}.
 */
public sealed interface GuideRenderPrimitive permits GuideRenderPrimitive.PushTransform,GuideRenderPrimitive.PopTransform,GuideRenderPrimitive.PushScissor,GuideRenderPrimitive.PopScissor,GuideRenderPrimitive.PushScreenScissor,GuideRenderPrimitive.PopScreenScissor,GuideRenderPrimitive.FillRect,GuideRenderPrimitive.GradientFill,GuideRenderPrimitive.DrawBorder,GuideRenderPrimitive.BlitTexture,GuideRenderPrimitive.DrawGlyphRun,GuideRenderPrimitive.DrawLine,GuideRenderPrimitive.DrawTriangle,GuideRenderPrimitive.DrawCircle,GuideRenderPrimitive.DrawCircleOutline,GuideRenderPrimitive.DrawPolygon,GuideRenderPrimitive.RenderItem,GuideRenderPrimitive.DrawText,GuideRenderPrimitive.RenderScene3D,GuideRenderPrimitive.HostDraw {

    /**
     * Push a translate+scale onto the transform stack. The new frame maps
     * {@code screen = doc * scale + (tx, ty)} and composes with its parent.
     */
    record PushTransform(float tx, float ty, float scale) implements GuideRenderPrimitive {}

    /** Pop the top transform from the stack. */
    record PopTransform() implements GuideRenderPrimitive {}

    /** Push a scissor rectangle (absolute document coords). Intersects with parent. */
    record PushScissor(int x, int y, int w, int h) implements GuideRenderPrimitive {}

    /** Pop the top scissor rectangle. */
    record PopScissor() implements GuideRenderPrimitive {}

    /**
     * Push a scissor rectangle in screen GUI coordinates (i.e. after the
     * document-to-screen transform). Unlike {@link PushScissor}, this is NOT
     * converted through the transform stack — it is only intersected with the
     * current scissor and multiplied by the display scale factor before
     * {@code glScissor}. Used for the fixed viewport clip, which must not move
     * with scroll or zoom (a document-space rect cannot express it exactly
     * when zoom != 1).
     */
    record PushScreenScissor(int x, int y, int w, int h) implements GuideRenderPrimitive {}

    /** Pop a screen-scissor rectangle. */
    record PopScreenScissor() implements GuideRenderPrimitive {}

    /** Filled rectangle. */
    record FillRect(int x, int y, int w, int h, int argb) implements GuideRenderPrimitive {}

    /** Vertical gradient fill. */
    record GradientFill(int x, int y, int w, int h, int argbTop, int argbBottom) implements GuideRenderPrimitive {}

    /** Border (4 sides, single color). */
    record DrawBorder(int x, int y, int w, int h, int top, int left, int bottom, int right, int argb)
        implements GuideRenderPrimitive {}

    /** Textured quad with UV coordinates, tinted by {@code argb} (0xFFFFFFFF = untinted). */
    record BlitTexture(int texId, int x, int y, int w, int h, float u, float v, float u2, float v2, int argb)
        implements GuideRenderPrimitive {

        /** Untinted (white) variant. */
        public BlitTexture(int texId, int x, int y, int w, int h, float u, float v, float u2, float v2) {
            this(texId, x, y, w, h, u, v, u2, v2, 0xFFFFFFFF);
        }
    }

    /**
     * Glyph run from Rust cosmic-text shaping. Glyph positions are absolute
     * document coords. {@code argb} tints the (white) atlas bitmaps — the
     * single color channel for unified-pipeline text. {@code shear} applies a
     * synthetic-italic slant at draw time (fake-oblique, mirroring MC §o).
     */
    record DrawGlyphRun(int atlasId, List<PlacedGlyph> glyphs, int argb, boolean shear)
        implements GuideRenderPrimitive {

        public DrawGlyphRun(int atlasId, List<PlacedGlyph> glyphs) {
            this(atlasId, glyphs, 0xFFFFFFFF, false);
        }

        public DrawGlyphRun(int atlasId, List<PlacedGlyph> glyphs, int argb) {
            this(atlasId, glyphs, argb, false);
        }
    }

    /** Line (thick or thin). */
    record DrawLine(float x1, float y1, float x2, float y2, float thickness, int argb)
        implements GuideRenderPrimitive {}

    /** Filled triangle. */
    record DrawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argb)
        implements GuideRenderPrimitive {}

    /** Circle (filled or outline). */
    record DrawCircle(float cx, float cy, float radius, int argb, boolean filled) implements GuideRenderPrimitive {}

    /** Circle outline only. */
    record DrawCircleOutline(float cx, float cy, float radius, float thickness, int argb)
        implements GuideRenderPrimitive {}

    /** Filled polygon. */
    record DrawPolygon(float[] xs, float[] ys, int argb) implements GuideRenderPrimitive {}

    /** Minecraft item stack rendering. */
    record RenderItem(ItemStack stack, int x, int y) implements GuideRenderPrimitive {}

    /** Text via Minecraft FontRenderer with full ResolvedTextStyle. */
    record DrawText(String text, int x, int y, ResolvedTextStyle style) implements GuideRenderPrimitive {}

    /** 3D scene rendering. */
    record RenderScene3D(Object level, Object camera, List<?> annotations, List<?> particles, List<?> weatherEffects,
        float weatherAnimationTick, Object lightDarkMode, int clipX, int clipY, int clipW, int clipH)
        implements GuideRenderPrimitive {}

    /**
     * Legacy subtree rendering escape hatch. Emitted by PrimitiveCollector for
     * blocks whose {@code usePrimitives()} is false. The engine sets up the GL
     * modelview from its current transform stack (so {@code draw} can render in
     * document coordinates), seeds {@code context}'s scissor stack with the
     * current engine scissor, invokes {@code draw}, and restores GL state
     * afterwards.
     */
    record HostDraw(RenderContext context, Runnable draw) implements GuideRenderPrimitive {}

    /**
     * A glyph quad at an absolute document coordinate (top-left origin), with the
     * key of its bitmap in the GuideGlyphAtlas. Used by DrawGlyphRun.
     * {@code lineIndex} is the visual (wrapped) line the glyph belongs to within
     * its paragraph, carried so line geometry can be rebuilt from glyph runs.
     */
    record PlacedGlyph(long atlasKey, float x, float y, float w, float h, int lineIndex) {}
}
