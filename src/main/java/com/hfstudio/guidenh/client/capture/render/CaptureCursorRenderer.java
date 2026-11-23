package com.hfstudio.guidenh.client.capture.render;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.cursor.CaptureCursorImage;

public class CaptureCursorRenderer implements AutoCloseable {

    private final CaptureCursorImage image;
    private DynamicTexture texture;

    public CaptureCursorRenderer(CaptureCursorImage image) {
        this.image = image;
    }

    public void draw(CaptureViewport viewport, double x, double y) {
        if (texture == null) {
            texture = new DynamicTexture(image.width(), image.height());
            image.copyPixels(texture.getTextureData());
            texture.updateDynamicTexture();
        }
        double left = x - image.hotspotX() / viewport.scaleX();
        double top = y - image.hotspotY() / viewport.scaleY();
        try (CaptureRenderScope scope = new CaptureRenderScope()) {
            CaptureTextureRenderer.drawRegion(
                texture.getGlTextureId(),
                new CaptureRect(0, 0, image.width(), image.height()),
                new CaptureRect(left, top, image.width() / viewport.scaleX(), image.height() / viewport.scaleY()),
                image.width(),
                image.height());
            if (!image.inverted()
                .isEmpty()) {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_BLEND);
                OpenGlHelper
                    .glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();
                tessellator.setColorOpaque_I(0xFFFFFF);
                for (CaptureRect rectangle : image.inverted()) {
                    double x0 = left + rectangle.x() / viewport.scaleX();
                    double x1 = left + rectangle.right() / viewport.scaleX();
                    double y0 = top + rectangle.y() / viewport.scaleY();
                    double y1 = top + rectangle.bottom() / viewport.scaleY();
                    tessellator.addVertex(x0, y1, 0);
                    tessellator.addVertex(x1, y1, 0);
                    tessellator.addVertex(x1, y0, 0);
                    tessellator.addVertex(x0, y0, 0);
                }
                tessellator.draw();
            }
        }
    }

    @Override
    public void close() {
        if (texture != null) texture.deleteGlTexture();
        texture = null;
    }
}
