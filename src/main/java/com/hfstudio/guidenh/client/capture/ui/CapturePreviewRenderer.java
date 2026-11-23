package com.hfstudio.guidenh.client.capture.ui;

import java.util.List;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CapturePlacement;
import com.hfstudio.guidenh.client.capture.render.CaptureTextureRenderer;

public class CapturePreviewRenderer implements AutoCloseable {

    private DynamicTexture texture;
    private long version = -1;
    private int textureWidth;
    private int textureHeight;
    private CaptureComposition composition;
    private List<CapturePlacement> visiblePlacements = List.of();
    private List<CaptureRect> erased = List.of();

    public CapturePreviewRenderer() {}

    public void draw(CaptureFrame frame, long frameVersion, CaptureViewport viewport, int backgroundArgb) {
        checkerboard(viewport.logicalWidth(), viewport.logicalHeight(), backgroundArgb);
        if (frame == null) return;
        if (texture == null || textureWidth != frame.width() || textureHeight != frame.height()) {
            close();
            textureWidth = frame.width();
            textureHeight = frame.height();
            texture = new DynamicTexture(frame.width(), frame.height());
        }
        if (version != frameVersion) {
            int[] pixels = texture.getTextureData();
            for (int y = 0; y < frame.height(); y++) frame.copyRow(y, 0, frame.width(), pixels, y * frame.width());
            texture.updateDynamicTexture();
            version = frameVersion;
        }
        CaptureTextureRenderer.draw(
            texture.getGlTextureId(),
            viewport.bounds()
                .width(),
            viewport.bounds()
                .height(),
            false,
            false);
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

    public void drawComposition(CaptureComposition composition, CaptureRect bounds, CaptureExportRequest request) {
        if (texture == null) return;
        List<CaptureRect> current = request.regions()
            .stream()
            .filter(region -> region.erased())
            .map(
                region -> request.viewport()
                    .pixels(region.bounds()))
            .toList();
        if (!composition.equals(this.composition) || !current.equals(erased)) {
            this.composition = composition;
            erased = current;
            visiblePlacements = composition.visiblePlacements(erased);
        }
        double scale = Math.min(
            bounds.width() / composition.size()
                .width(),
            bounds.height() / composition.size()
                .height());
        double left = bounds.x() + (bounds.width() - composition.size()
            .width() * scale) / 2;
        double top = bounds.y() + (bounds.height() - composition.size()
            .height() * scale) / 2;
        for (CapturePlacement placement : visiblePlacements) {
            CaptureTextureRenderer.drawRegion(
                texture.getGlTextureId(),
                new CaptureRect(placement.sourceX(), placement.sourceY(), placement.width(), placement.height()),
                new CaptureRect(
                    left + placement.destinationX() * scale,
                    top + placement.destinationY() * scale,
                    placement.width() * scale,
                    placement.height() * scale),
                textureWidth,
                textureHeight);
        }
    }

    @Override
    public void close() {
        if (texture != null) texture.deleteGlTexture();
        texture = null;
        version = -1;
    }
}
