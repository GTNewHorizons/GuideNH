package com.hfstudio.guidenh.guide.render;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

import lombok.Getter;
import lombok.Setter;

public class VanillaRenderContext implements RenderContext {

    public static final RenderItem ITEM_RENDERER = new RenderItem();

    private final FontRenderer fontRenderer;
    @Getter
    @Setter
    private int screenHeight;

    @Setter
    private LightDarkMode lightDarkMode;
    @Setter
    private LytRect viewport;

    /**
     * Fixed on-screen viewport rect (screen GUI coordinates), set by the screen
     * alongside {@link #viewport}. Used by the primitive pipeline for the outer
     * scissor clip and culling; falls back to {@link #viewport} when unset.
     */
    @Setter
    private LytRect screenViewport;

    private final Deque<LytRect> scissorStack = new ArrayDeque<>();

    @Getter
    private int documentOriginX = 0;
    private int documentOriginY = 0;

    private int scrollOffsetY = 0;
    private float preciseScrollOffsetY = 0f;

    @Getter
    private float zoom = 1.0f;

    public VanillaRenderContext(LightDarkMode mode, LytRect viewport, int screenHeight) {
        this.lightDarkMode = mode;
        this.viewport = viewport;
        this.screenHeight = screenHeight;
        this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
    }

    public void setDocumentOrigin(int absX, int absY) {
        this.documentOriginX = absX;
        this.documentOriginY = absY;
    }

    @Override
    public int getDocumentOriginY() {
        return documentOriginY;
    }

    public void setScrollOffsetY(int scrollOffsetY) {
        this.scrollOffsetY = scrollOffsetY;
        this.preciseScrollOffsetY = scrollOffsetY;
    }

    public void setPreciseScrollOffsetY(float scrollOffsetY) {
        this.preciseScrollOffsetY = scrollOffsetY;
        this.scrollOffsetY = Math.round(scrollOffsetY);
    }

    @Override
    public int getScrollOffsetY() {
        return scrollOffsetY;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom > 0f ? zoom : 1.0f;
    }

    @Override
    public LytRect toScreenRect(LytRect rect) {
        return new LytRect(
            Math.round(rect.x() * zoom) + documentOriginX,
            Math.round((rect.y() - preciseScrollOffsetY) * zoom) + documentOriginY,
            Math.max(1, Math.round(rect.width() * zoom)),
            Math.max(1, Math.round(rect.height() * zoom)));
    }

    @Override
    public LightDarkMode lightDarkMode() {
        return lightDarkMode;
    }

    @Override
    public LytRect viewport() {
        return viewport;
    }

    @Override
    public LytRect getScreenViewport() {
        return screenViewport != null ? screenViewport : viewport;
    }

    @Override
    public float getPreciseScrollOffsetY() {
        return preciseScrollOffsetY;
    }

    @Override
    public int resolveColor(ColorValue ref) {
        return ref.resolve(lightDarkMode);
    }

    @Override
    public void fillRect(LytRect rect, int argbColor) {
        Gui.drawRect(rect.x(), rect.y(), rect.right(), rect.bottom(), argbColor);
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argbColor) {
        Gui.drawRect(x, y, x + width, y + height, argbColor);
    }

    @Override
    public void drawBorder(LytRect rect, int argbColor, int thickness) {
        Gui.drawRect(rect.x(), rect.y(), rect.right(), rect.y() + thickness, argbColor);
        Gui.drawRect(rect.x(), rect.bottom() - thickness, rect.right(), rect.bottom(), argbColor);
        Gui.drawRect(rect.x(), rect.y() + thickness, rect.x() + thickness, rect.bottom() - thickness, argbColor);
        Gui.drawRect(
            rect.right() - thickness,
            rect.y() + thickness,
            rect.right(),
            rect.bottom() - thickness,
            argbColor);
    }

    @Override
    public void drawBorder(int x, int y, int width, int height, int argbColor, int thickness) {
        int right = x + width;
        int bottom = y + height;
        Gui.drawRect(x, y, right, y + thickness, argbColor);
        Gui.drawRect(x, bottom - thickness, right, bottom, argbColor);
        Gui.drawRect(x, y + thickness, x + thickness, bottom - thickness, argbColor);
        Gui.drawRect(right - thickness, y + thickness, right, bottom - thickness, argbColor);
    }

    @Override
    public void fillRoundedRect(LytRect rect, int argbColor, int radius) {
        int clampedRadius = Math.max(0, Math.min(radius, Math.min(rect.width(), rect.height()) / 2));
        if (clampedRadius == 0) {
            fillRect(rect, argbColor);
            return;
        }
        fillRect(rect.x() + clampedRadius, rect.y(), rect.width() - clampedRadius * 2, rect.height(), argbColor);
        fillRect(rect.x(), rect.y() + clampedRadius, clampedRadius, rect.height() - clampedRadius * 2, argbColor);
        fillRect(
            rect.right() - clampedRadius,
            rect.y() + clampedRadius,
            clampedRadius,
            rect.height() - clampedRadius * 2,
            argbColor);
        fillCircle(rect.x() + clampedRadius, rect.y() + clampedRadius, clampedRadius, argbColor);
        fillCircle(rect.right() - clampedRadius, rect.y() + clampedRadius, clampedRadius, argbColor);
        fillCircle(rect.x() + clampedRadius, rect.bottom() - clampedRadius, clampedRadius, argbColor);
        fillCircle(rect.right() - clampedRadius, rect.bottom() - clampedRadius, clampedRadius, argbColor);
    }

    @Override
    public void drawRoundedBorder(LytRect rect, int argbColor, int thickness, int radius) {
        int clampedRadius = Math.max(0, Math.min(radius, Math.min(rect.width(), rect.height()) / 2));
        if (clampedRadius == 0) {
            drawBorder(rect, argbColor, thickness);
            return;
        }
        fillRect(rect.x() + clampedRadius, rect.y(), rect.width() - clampedRadius * 2, thickness, argbColor);
        fillRect(
            rect.x() + clampedRadius,
            rect.bottom() - thickness,
            rect.width() - clampedRadius * 2,
            thickness,
            argbColor);
        fillRect(rect.x(), rect.y() + clampedRadius, thickness, rect.height() - clampedRadius * 2, argbColor);
        fillRect(
            rect.right() - thickness,
            rect.y() + clampedRadius,
            thickness,
            rect.height() - clampedRadius * 2,
            argbColor);
        drawCircleOutline(
            rect.x() + clampedRadius,
            rect.y() + clampedRadius,
            clampedRadius - thickness * 0.5f,
            thickness,
            argbColor);
        drawCircleOutline(
            rect.right() - clampedRadius,
            rect.y() + clampedRadius,
            clampedRadius - thickness * 0.5f,
            thickness,
            argbColor);
        drawCircleOutline(
            rect.x() + clampedRadius,
            rect.bottom() - clampedRadius,
            clampedRadius - thickness * 0.5f,
            thickness,
            argbColor);
        drawCircleOutline(
            rect.right() - clampedRadius,
            rect.bottom() - clampedRadius,
            clampedRadius - thickness * 0.5f,
            thickness,
            argbColor);
    }

    @Override
    public void drawText(String text, int x, int y, ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) return;
        int color = resolveColor(style.color());
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }
        String drawn = GuideFontCompat.prepareRenderedText(text, style);

        float scale = style.fontScale();
        boolean scaled = Math.abs(scale - 1f) > 1e-4f;
        if (scaled) {
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0f);
            GL11.glScalef(scale, scale, 1f);
            if (style.dropShadow()) {
                fontRenderer.drawStringWithShadow(drawn, 0, 0, color);
            } else {
                fontRenderer.drawString(drawn, 0, 0, color);
            }
            GL11.glPopMatrix();
        } else if (style.dropShadow()) {
            fontRenderer.drawStringWithShadow(drawn, x, y, color);
        } else {
            fontRenderer.drawString(drawn, x, y, color);
        }

        boolean hasUnderline = style.underlined();
        boolean hasWavyUnderline = style.wavyUnderline();
        boolean hasDottedUnderline = style.dottedUnderline();
        if (!hasUnderline && !hasWavyUnderline && !hasDottedUnderline) {
            return;
        }

        int scaledFontHeight = Math.round(fontRenderer.FONT_HEIGHT * scale);
        int decorationY = y + scaledFontHeight - 1;
        int decoratedWidth = GuideFontCompat.getPreparedStringWidth(fontRenderer, drawn, style);
        if (hasUnderline) {
            Gui.drawRect(x, decorationY, x + decoratedWidth, decorationY + 1, color);
        }
        if (hasWavyUnderline) {
            // M3: the old wavy was 1x1px with ±1 amplitude — swallowed by the
            // dark page background. Use a ±2px, 2px-thick 8-phase sine, tinted
            // lighter than the body gray.
            int waveColor = brightenDecorationColor(color);
            for (int i = 0; i < decoratedWidth; i++) {
                int dy = (int) Math.round(Math.sin(i * Math.PI / 4.0) * 2.0);
                Gui.drawRect(x + i, decorationY + dy, x + i + 1, decorationY + dy + 2, waveColor);
            }
        }
        if (hasDottedUnderline) {
            // M3: one 2x2 dot per character was too sparse; draw 3x3 dots on a
            // fixed 4px cadence, tinted lighter than the body gray.
            int dotColor = brightenDecorationColor(color);
            int dotSize = 3;
            int step = 4;
            for (int dotX = x + step / 2; dotX + dotSize <= x + decoratedWidth; dotX += step) {
                Gui.drawRect(dotX, decorationY - 1, dotX + dotSize, decorationY + dotSize - 1, dotColor);
            }
        }
    }

    /**
     * Wavy/dots decorations are thin; a faint tint is swallowed by the page
     * background (M3). In dark mode blend the text color toward white so the
     * decoration reads against the body gray; light mode keeps the original
     * dark color, which already contrasts on the light page.
     */
    private int brightenDecorationColor(int color) {
        if (lightDarkMode != LightDarkMode.DARK_MODE) {
            return color;
        }
        int a = color & 0xFF000000;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        r = r + (255 - r) * 3 / 4;
        g = g + (255 - g) * 3 / 4;
        b = b + (255 - b) * 3 / 4;
        return a | (r << 16) | (g << 8) | b;
    }

    @Override
    public int getStringWidth(String text, ResolvedTextStyle style) {
        return GuideFontCompat.getStringWidth(fontRenderer, text, style);
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        float scale = style != null ? style.fontScale() : 1f;
        return (int) Math.ceil((fontRenderer.FONT_HEIGHT + 1) * scale);
    }

    @Override
    public void renderItem(ItemStack stack, int x, int y) {
        if (stack == null) return;
        renderItemInternal(stack, x, y, true);
    }

    @Override
    public void renderItemIcon(ItemStack stack, int x, int y) {
        if (stack == null) return;
        renderItemInternal(stack, x, y, false);
    }

    private void renderItemInternal(ItemStack stack, int x, int y, boolean drawOverlay) {
        var mc = Minecraft.getMinecraft();
        // Avoid glPushAttrib/glPopAttrib: those are slow on modern drivers (cause pipeline
        // flushes). We instead explicitly restore every state we touch in the finally block.
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1f, 1f, 1f, 1f);

            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_NORMALIZE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_ALPHA_TEST);

            ITEM_RENDERER.zLevel = 100f;
            ITEM_RENDERER.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), stack, x, y);
            if (drawOverlay) {
                ITEM_RENDERER.renderItemOverlayIntoGUI(fontRenderer, mc.getTextureManager(), stack, x, y);
            }
            RenderHelper.disableStandardItemLighting();
        } finally {
            ITEM_RENDERER.zLevel = 0f;
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_NORMALIZE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    @Override
    public void blitTexture(ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        var tess = Tessellator.instance;
        float texW = 256f;
        float texH = 256f;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + height, 0, u / texW, (v + height) / texH);
        tess.addVertexWithUV(x + width, y + height, 0, (u + width) / texW, (v + height) / texH);
        tess.addVertexWithUV(x + width, y, 0, (u + width) / texW, v / texH);
        tess.addVertexWithUV(x, y, 0, u / texW, v / texH);
        tess.draw();
    }

    @Override
    public void fillIcon(LytRect rect, GuiSprite sprite, ColorValue color) {
        if (sprite == null || rect == null) {
            return;
        }
        int resolved = resolveColor(color);
        int a = (resolved >>> 24) & 0xFF;
        int r = (resolved >>> 16) & 0xFF;
        int g = (resolved >>> 8) & 0xFF;
        int b = resolved & 0xFF;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        try {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(r / 255f, g / 255f, b / 255f, a / 255f);
            Minecraft.getMinecraft()
                .getTextureManager()
                .bindTexture(sprite.getTexture());
            var tess = Tessellator.instance;
            float texW = sprite.getTexWidth();
            float texH = sprite.getTexHeight();
            int x = rect.x();
            int y = rect.y();
            int u = sprite.getU();
            int v = sprite.getV();
            int spriteWidth = sprite.getWidth();
            int spriteHeight = sprite.getHeight();
            tess.startDrawingQuads();
            tess.addVertexWithUV(x, y + spriteHeight, 0, u / texW, (v + spriteHeight) / texH);
            tess.addVertexWithUV(
                x + spriteWidth,
                y + spriteHeight,
                0,
                (u + spriteWidth) / texW,
                (v + spriteHeight) / texH);
            tess.addVertexWithUV(x + spriteWidth, y, 0, (u + spriteWidth) / texW, v / texH);
            tess.addVertexWithUV(x, y, 0, u / texW, v / texH);
            tess.draw();
        } finally {
            GL11.glColor4f(1f, 1f, 1f, 1f);
            GL11.glPopAttrib();
        }
    }

    @Override
    public void fillTexturedRect(LytRect rect, GuidePageTexture texture) {
        if (texture == null || texture.isMissing()) {
            fillRect(rect, 0xFF333333);
            drawBorder(rect, 0xFFFF00FF, 1);
            return;
        }
        ResourceLocation resolvedTexture = texture.getTexture();
        if (resolvedTexture == null) {
            fillRect(rect, 0xFF333333);
            drawBorder(rect, 0xFFFF00FF, 1);
            return;
        }
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(resolvedTexture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        int x = rect.x();
        int y = rect.y();
        int w = rect.width();
        int h = rect.height();
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, 0, 0f, 1f);
        tess.addVertexWithUV(x + w, y + h, 0, 1f, 1f);
        tess.addVertexWithUV(x + w, y, 0, 1f, 0f);
        tess.addVertexWithUV(x, y, 0, 0f, 0f);
        tess.draw();
    }

    @Override
    public void fillTexturedRect(LytRect rect, GuidePageTexture texture, int sourceX, int sourceY, int sourceWidth,
        int sourceHeight) {
        if (texture == null || texture.isMissing()) {
            fillRect(rect, 0xFF333333);
            drawBorder(rect, 0xFFFF00FF, 1);
            return;
        }
        ResourceLocation resolvedTexture = texture.getTexture();
        if (resolvedTexture == null) {
            fillRect(rect, 0xFF333333);
            drawBorder(rect, 0xFFFF00FF, 1);
            return;
        }
        int naturalWidth = Math.max(
            1,
            texture.getSize()
                .width());
        int naturalHeight = Math.max(
            1,
            texture.getSize()
                .height());
        float u0 = sourceX / (float) naturalWidth;
        float v0 = sourceY / (float) naturalHeight;
        float u1 = (sourceX + sourceWidth) / (float) naturalWidth;
        float v1 = (sourceY + sourceHeight) / (float) naturalHeight;
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(resolvedTexture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        int x = rect.x();
        int y = rect.y();
        int w = rect.width();
        int h = rect.height();
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.addVertexWithUV(x, y + h, 0, u0, v1);
        tess.addVertexWithUV(x + w, y + h, 0, u1, v1);
        tess.addVertexWithUV(x + w, y, 0, u1, v0);
        tess.addVertexWithUV(x, y, 0, u0, v0);
        tess.draw();
    }

    @Override
    public void pushScissor(LytRect rect) {
        LytRect effective;
        if (!scissorStack.isEmpty()) {
            var parent = scissorStack.peek();
            int x1 = Math.max(rect.x(), parent.x());
            int y1 = Math.max(rect.y(), parent.y());
            int x2 = Math.min(rect.right(), parent.right());
            int y2 = Math.min(rect.bottom(), parent.bottom());
            int w = Math.max(0, x2 - x1);
            int h = Math.max(0, y2 - y1);
            if (x1 == rect.x() && y1 == rect.y() && w == rect.width() && h == rect.height()) {
                effective = rect;
            } else if (x1 == parent.x() && y1 == parent.y() && w == parent.width() && h == parent.height()) {
                effective = parent;
            } else {
                effective = new LytRect(x1, y1, w, h);
            }
        } else {
            effective = rect;
        }
        scissorStack.push(effective);
        applyScissor(effective);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void popScissor() {
        scissorStack.pop();
        if (!scissorStack.isEmpty()) {
            applyScissor(scissorStack.peek());
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public LytRect currentScissor() {
        return scissorStack.peek();
    }

    @Override
    public void restoreExternalRenderState() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        RenderHelper.disableStandardItemLighting();

        if (!scissorStack.isEmpty()) {
            applyScissor(scissorStack.peek());
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private void applyScissor(LytRect rect) {
        var mc = Minecraft.getMinecraft();
        int scale = DisplayScale.scaleFactor();
        int sx = rect.x() * scale;
        int sy = mc.displayHeight - rect.bottom() * scale;
        int sw = rect.width() * scale;
        int sh = rect.height() * scale;
        GL11.glScissor(sx, Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }

    private static final int CIRCLE_SEGMENTS = 32;

    private static void beginShapeDraw() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private static void endShapeDraw() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopAttrib();
    }

    private static void applyArgb(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        GL11.glColor4f(r / 255f, g / 255f, b / 255f, a / 255f);
    }

    /**
     * Apply color to the Tessellator's per-vertex color channel. Required because Tessellator.draw() in
     * 1.7.10 ignores the global GL color when hasColor is false; we must explicitly set the vertex color
     * via setColorRGBA_I (which is what vanilla Gui.drawRect does).
     */
    private static void tessColor(Tessellator tess, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        if (a == 0) {
            a = 0xFF;
        }
        tess.setColorRGBA_I((r << 16) | (g << 8) | b, a);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int argbColor) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-4f) {
            return;
        }
        float half = Math.max(0.5f, thickness * 0.5f);
        float nx = -dy / len * half;
        float ny = dx / len * half;
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        tessColor(tess, argbColor);
        tess.addVertex(x1 - nx, y1 - ny, 0);
        tess.addVertex(x2 - nx, y2 - ny, 0);
        tess.addVertex(x2 + nx, y2 + ny, 0);
        tess.addVertex(x1 + nx, y1 + ny, 0);
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int argbColor) {
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLES);
        tessColor(tess, argbColor);
        tess.addVertex(x1, y1, 0);
        tess.addVertex(x2, y2, 0);
        tess.addVertex(x3, y3, 0);
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillPolygon(float[] xs, float[] ys, int argbColor) {
        if (xs == null || ys == null || xs.length < 3 || ys.length < xs.length) {
            return;
        }
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessColor(tess, argbColor);
        tess.addVertex(xs[0], ys[0], 0);
        for (int i = 0; i < xs.length; i++) {
            tess.addVertex(xs[i], ys[i], 0);
        }
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillCircle(float cx, float cy, float radius, int argbColor) {
        if (radius <= 0f) {
            return;
        }
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessColor(tess, argbColor);
        tess.addVertex(cx, cy, 0);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            tess.addVertex(cx + (float) (Math.cos(a) * radius), cy + (float) (Math.sin(a) * radius), 0);
        }
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void fillEllipse(float cx, float cy, float rx, float ry, int argbColor) {
        if (rx <= 0f || ry <= 0f) {
            return;
        }
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_FAN);
        tessColor(tess, argbColor);
        tess.addVertex(cx, cy, 0);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            tess.addVertex(cx + (float) (Math.cos(a) * rx), cy + (float) (Math.sin(a) * ry), 0);
        }
        tess.draw();
        endShapeDraw();
    }

    @Override
    public void drawCircleOutline(float cx, float cy, float radius, float thickness, int argbColor) {
        if (radius <= 0f) {
            return;
        }
        float half = Math.max(0.5f, thickness * 0.5f);
        float inner = Math.max(0f, radius - half);
        float outer = radius + half;
        beginShapeDraw();
        applyArgb(argbColor);
        var tess = Tessellator.instance;
        tess.startDrawing(GL11.GL_TRIANGLE_STRIP);
        tessColor(tess, argbColor);
        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            double a = (Math.PI * 2.0 * i) / CIRCLE_SEGMENTS;
            float cosA = (float) Math.cos(a);
            float sinA = (float) Math.sin(a);
            tess.addVertex(cx + cosA * inner, cy + sinA * inner, 0);
            tess.addVertex(cx + cosA * outer, cy + sinA * outer, 0);
        }
        tess.draw();
        endShapeDraw();
    }
}
