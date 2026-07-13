package com.hfstudio.guidenh.guide.render;

import java.util.List;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

/**
 * Sealed interface for all render primitives consumed by GuideRenderEngine.
 * <p>
 * 19 total (as of Phase 1): 5 state instructions + 14 draw primitives.
 * <p>
 * All coordinates are absolute document coordinates unless stated otherwise.
 * GuideRenderEngine applies the current transform stack to convert to screen coordinates.
 */
public sealed interface GuideRenderPrimitive permits GuideRenderPrimitive.PushTransform,GuideRenderPrimitive.PopTransform,GuideRenderPrimitive.PushScissor,GuideRenderPrimitive.PopScissor,GuideRenderPrimitive.SetBlendMode,GuideRenderPrimitive.SetColor,GuideRenderPrimitive.FillRect,GuideRenderPrimitive.GradientFill,GuideRenderPrimitive.DrawBorder,GuideRenderPrimitive.BlitTexture,GuideRenderPrimitive.DrawGlyphRun,GuideRenderPrimitive.DrawLine,GuideRenderPrimitive.DrawTriangle,GuideRenderPrimitive.DrawCircle,GuideRenderPrimitive.DrawCircleOutline,GuideRenderPrimitive.DrawPolygon,GuideRenderPrimitive.RenderItem,GuideRenderPrimitive.DrawText,GuideRenderPrimitive.RenderScene3D,GuideRenderPrimitive.HostDraw {

    /** Push a translation+scale onto the transform stack. */
    record PushTransform(int tx, int ty, float scale) implements GuideRenderPrimitive {}

    /** Pop the top transform from the stack. */
    record PopTransform() implements GuideRenderPrimitive {}

    /** Push a scissor rectangle (absolute document coords). Intersects with parent. */
    record PushScissor(int x, int y, int w, int h) implements GuideRenderPrimitive {}

    /** Pop the top scissor rectangle. */
    record PopScissor() implements GuideRenderPrimitive {}

    /** Set blend mode for subsequent primitives. */
    record SetBlendMode(int mode) implements GuideRenderPrimitive {}

    /** Set color for subsequent primitives. */
    record SetColor(int argb) implements GuideRenderPrimitive {}

    /** Filled rectangle. */
    record FillRect(int x, int y, int w, int h, int argb) implements GuideRenderPrimitive {}

    /** Vertical gradient fill. */
    record GradientFill(int x, int y, int w, int h, int argbTop, int argbBottom) implements GuideRenderPrimitive {}

    /** Border (4 sides, single color). */
    record DrawBorder(int x, int y, int w, int h, int top, int left, int bottom, int right, int argb)
        implements GuideRenderPrimitive {}

    /** Textured quad with UV coordinates. */
    record BlitTexture(int texId, int x, int y, int w, int h, float u, float v, float u2, float v2)
        implements GuideRenderPrimitive {}

    /** Glyph run from Rust cosmic-text shaping. Glyph positions are absolute document coords. */
    record DrawGlyphRun(int atlasId, List<PlacedGlyph> glyphs) implements GuideRenderPrimitive {}

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

    /** External GL callback. */
    record HostDraw(int callbackId, int x, int y, int w, int h) implements GuideRenderPrimitive {}

    /** A single glyph placed at an absolute document coordinate. Used by DrawGlyphRun. */
    record PlacedGlyph(int glyphId, float x, float y, float w, float h) {}
}
