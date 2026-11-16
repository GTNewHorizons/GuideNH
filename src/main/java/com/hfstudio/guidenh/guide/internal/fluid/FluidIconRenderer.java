package com.hfstudio.guidenh.guide.internal.fluid;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.gtnhlib.util.numberformatting.NumberFormatUtil;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class FluidIconRenderer {

    protected FluidIconRenderer() {}

    public static void draw(RenderContext context, FluidStack stack, int x, int y, int size, boolean showAmount) {
        context.beginLocalView();
        try {
            draw(stack, x, y, size, showAmount);
        } finally {
            context.restoreExternalRenderState();
            context.endLocalView();
        }
    }

    public static void draw(FluidStack stack, int x, int y, int size, boolean showAmount) {
        draw(stack, x, y, size, showAmount, 1f);
    }

    public static void draw(FluidStack stack, int x, int y, int size, boolean showAmount, float opacity) {
        if (stack == null || stack.getFluid() == null || size <= 0) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        IIcon icon = stack.getFluid()
            .getIcon(stack);
        if (icon == null) icon = minecraft.getTextureMapBlocks()
            .getAtlasSprite("missingno");
        int color = stack.getFluid()
            .getColor(stack);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_TEXTURE_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            minecraft.getTextureManager()
                .bindTexture(TextureMap.locationBlocksTexture);
            GL11.glColor4f((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, opacity);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(x, y + size, 0, icon.getMinU(), icon.getMaxV());
            tessellator.addVertexWithUV(x + size, y + size, 0, icon.getMaxU(), icon.getMaxV());
            tessellator.addVertexWithUV(x + size, y, 0, icon.getMaxU(), icon.getMinV());
            tessellator.addVertexWithUV(x, y, 0, icon.getMinU(), icon.getMinV());
            tessellator.draw();
            GL11.glColor4f(1f, 1f, 1f, 1f);
            if (showAmount) {
                String text = NumberFormatUtil.formatNumberCompact(stack.amount);
                float scale = Math.min(1f, size / 16f);
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(x + size, y + size, 0);
                    GL11.glScalef(scale, scale, 1f);
                    minecraft.fontRenderer
                        .drawStringWithShadow(text, -minecraft.fontRenderer.getStringWidth(text), -8, 0xffffff);
                } finally {
                    GL11.glPopMatrix();
                }
            }
        } finally {
            GL11.glPopAttrib();
        }
    }
}
