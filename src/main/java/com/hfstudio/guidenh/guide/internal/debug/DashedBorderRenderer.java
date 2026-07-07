package com.hfstudio.guidenh.guide.internal.debug;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;

/**
 * Renders animated dashed borders for debug overlay, similar to GuideME's DashedRectangle.
 * Optimized for zero overhead when not rendering.
 */
public class DashedBorderRenderer {

    public void renderDashedBorder(int x, int y, int width, int height, int color) {
        if (!ModConfig.debug.guiDebugMode || !ModConfig.debug.showHoveredOutline) {
            return;
        }

        float lineWidth = ModConfig.debug.debugDashWidth;
        float onLength = ModConfig.debug.debugDashOnLength;
        float offLength = ModConfig.debug.debugDashOffLength;
        float animationCycleMs = ModConfig.debug.debugDashAnimationCycleMs;

        float t = 0f;
        if (animationCycleMs > 0) {
            t = (System.currentTimeMillis() % (long) animationCycleMs) / animationCycleMs;
        }

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(lineWidth);

        float alpha = ((color >> 24) & 0xFF) / 255.0f;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        Tessellator tessellator = Tessellator.instance;

        float patternLength = onLength + offLength;
        float phase = t * patternLength;

        // Top edge (left to right)
        renderDashedLineHorizontal(
            tessellator,
            x,
            y,
            x + width,
            phase,
            patternLength,
            onLength,
            red,
            green,
            blue,
            alpha,
            false);

        // Bottom edge (right to left)
        renderDashedLineHorizontal(
            tessellator,
            x,
            y + height,
            x + width,
            phase,
            patternLength,
            onLength,
            red,
            green,
            blue,
            alpha,
            true);

        // Left edge (bottom to top)
        renderDashedLineVertical(
            tessellator,
            x,
            y,
            y + height,
            phase,
            patternLength,
            onLength,
            red,
            green,
            blue,
            alpha,
            true);

        // Right edge (top to bottom)
        renderDashedLineVertical(
            tessellator,
            x + width,
            y,
            y + height,
            phase,
            patternLength,
            onLength,
            red,
            green,
            blue,
            alpha,
            false);

        GL11.glPopAttrib();
    }

    private void renderDashedLineHorizontal(Tessellator tessellator, float x1, float y, float x2, float phase,
        float patternLength, float onLength, float r, float g, float b, float a, boolean reverse) {
        if (!reverse) {
            phase = patternLength - phase;
        }

        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_F(r, g, b, a);

        for (float x = x1 - phase; x < x2; x += patternLength) {
            float segmentStart = Math.max(x, x1);
            float segmentEnd = Math.min(x + onLength, x2);
            if (segmentStart < segmentEnd) {
                tessellator.addVertex(segmentStart, y, 0.0);
                tessellator.addVertex(segmentEnd, y, 0.0);
            }
        }

        tessellator.draw();
    }

    private void renderDashedLineVertical(Tessellator tessellator, float x, float y1, float y2, float phase,
        float patternLength, float onLength, float r, float g, float b, float a, boolean reverse) {
        if (!reverse) {
            phase = patternLength - phase;
        }

        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_F(r, g, b, a);

        for (float y = y1 - phase; y < y2; y += patternLength) {
            float segmentStart = Math.max(y, y1);
            float segmentEnd = Math.min(y + onLength, y2);
            if (segmentStart < segmentEnd) {
                tessellator.addVertex(x, segmentStart, 0.0);
                tessellator.addVertex(x, segmentEnd, 0.0);
            }
        }

        tessellator.draw();
    }
}
