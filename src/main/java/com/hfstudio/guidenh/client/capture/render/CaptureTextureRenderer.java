package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;

public class CaptureTextureRenderer {

    private CaptureTextureRenderer() {}

    public static void draw(int texture, double width, double height, boolean flipped, boolean premultiplied) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(premultiplied ? GL11.GL_ONE : GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        double top = flipped ? 1 : 0;
        double bottom = flipped ? 0 : 1;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(0, height, 0, 0, bottom);
        tessellator.addVertexWithUV(width, height, 0, 1, bottom);
        tessellator.addVertexWithUV(width, 0, 0, 1, top);
        tessellator.addVertexWithUV(0, 0, 0, 0, top);
        tessellator.draw();
    }

    public static void drawRegion(int texture, CaptureRect source, CaptureRect destination, int textureWidth,
        int textureHeight) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        double left = source.x() / textureWidth;
        double right = source.right() / textureWidth;
        double top = source.y() / textureHeight;
        double bottom = source.bottom() / textureHeight;
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(destination.x(), destination.bottom(), 0, left, bottom);
        tessellator.addVertexWithUV(destination.right(), destination.bottom(), 0, right, bottom);
        tessellator.addVertexWithUV(destination.right(), destination.y(), 0, right, top);
        tessellator.addVertexWithUV(destination.x(), destination.y(), 0, left, top);
        tessellator.draw();
    }
}
