package com.hfstudio.guidenh.client.capture.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.render.CaptureTextureRenderer;

public class CapturePreviewRenderer implements AutoCloseable {

    private final Minecraft minecraft;
    private DynamicTexture texture;
    private ResourceLocation location;
    private long version = -1;
    private int textureWidth;
    private int textureHeight;

    public CapturePreviewRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public void draw(CaptureFrame frame, long frameVersion, CaptureViewport viewport, int backgroundArgb) {
        checkerboard(viewport.logicalWidth(), viewport.logicalHeight(), backgroundArgb);
        if (frame == null) return;
        if (texture == null || textureWidth != frame.width() || textureHeight != frame.height()) {
            close();
            textureWidth = frame.width();
            textureHeight = frame.height();
            texture = new DynamicTexture(frame.width(), frame.height());
            location = minecraft.getTextureManager()
                .getDynamicTextureLocation("gui_capture_preview", texture);
        }
        if (version != frameVersion) {
            int[] pixels = texture.getTextureData();
            for (int y = 0; y < frame.height(); y++) frame.copyRow(y, 0, frame.width(), pixels, y * frame.width());
            texture.updateDynamicTexture();
            version = frameVersion;
        }
        CaptureTextureRenderer
            .draw(texture.getGlTextureId(), viewport.logicalWidth(), viewport.logicalHeight(), false, false);
    }

    private static void checkerboard(int width, int height, int background) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int y = 0; y < height; y += 12) {
            for (int x = 0; x < width; x += 12) {
                int shade = ((x / 12 + y / 12) & 1) == 0 ? 0x252A31 : 0x1D2228;
                if ((background >>> 24) == 255) shade = background & 0xFFFFFF;
                tessellator.setColorOpaque_I(shade);
                tessellator.addVertex(x, Math.min(height, y + 12), 0);
                tessellator.addVertex(Math.min(width, x + 12), Math.min(height, y + 12), 0);
                tessellator.addVertex(Math.min(width, x + 12), y, 0);
                tessellator.addVertex(x, y, 0);
            }
        }
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    @Override
    public void close() {
        if (location != null) minecraft.getTextureManager()
            .deleteTexture(location);
        texture = null;
        location = null;
        version = -1;
    }
}
