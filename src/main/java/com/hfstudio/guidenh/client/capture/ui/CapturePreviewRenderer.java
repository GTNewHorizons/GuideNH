package com.hfstudio.guidenh.client.capture.ui;

import java.util.List;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureExportRequest;
import com.hfstudio.guidenh.client.capture.api.CaptureFrame;
import com.hfstudio.guidenh.client.capture.api.CaptureOutputSize;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;
import com.hfstudio.guidenh.client.capture.export.CaptureComposition;
import com.hfstudio.guidenh.client.capture.export.CapturePlacement;
import com.hfstudio.guidenh.client.capture.geometry.CaptureSelectionAreas;
import com.hfstudio.guidenh.client.capture.render.CaptureTextureRenderer;

public class CapturePreviewRenderer implements AutoCloseable {

    private DynamicTexture texture;
    private long version = -1;
    private int textureWidth;
    private int textureHeight;
    private boolean hasFrame;
    private CaptureComposition composition;
    private List<CapturePlacement> visiblePlacements = List.of();
    private List<CaptureRect> erased = List.of();
    private List<CaptureRegion> selection = List.of();
    private List<CapturePlacement> selectionPlacements = List.of();
    private CaptureViewport selectionViewport;
    private boolean invertTransparencyGrid = true;

    public CapturePreviewRenderer() {}

    public void draw(CaptureFrame frame, long frameVersion, CaptureViewport viewport, int backgroundArgb) {
        draw(frame, frameVersion, viewport, backgroundArgb, List.of(), false);
    }

    public void draw(CaptureFrame frame, long frameVersion, CaptureViewport viewport, int backgroundArgb,
        List<CaptureRegion> regions, boolean selectedOnly) {
        draw(frame, frameVersion, viewport, backgroundArgb, regions, selectedOnly, true);
    }

    public void draw(CaptureFrame frame, long frameVersion, CaptureViewport viewport, int backgroundArgb,
        List<CaptureRegion> regions, boolean selectedOnly, boolean invertTransparencyGrid) {
        draw(frame, frameVersion, viewport, backgroundArgb, regions, selectedOnly, invertTransparencyGrid, null);
    }

    public void draw(CaptureFrame frame, long frameVersion, CaptureViewport viewport, int backgroundArgb,
        List<CaptureRegion> regions, boolean selectedOnly, boolean invertTransparencyGrid, CaptureRect candidate) {
        if (selectedOnly && !regions.isEmpty()) regions = CaptureSelectionAreas.preview(regions, candidate);
        this.invertTransparencyGrid = invertTransparencyGrid;
        CaptureTextureRenderer.prepareOverlay();
        boolean masked = selectedOnly && regions.stream()
            .anyMatch(
                region -> !region.erased() && !region.bounds()
                    .isEmpty());
        checkerboard(
            new CaptureRect(0, 0, viewport.logicalWidth(), viewport.logicalHeight()),
            masked ? 0 : backgroundArgb);
        if (frame == null) {
            hasFrame = false;
            return;
        }
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
        hasFrame = true;
        if (masked) {
            if (!regions.equals(selection) || !viewport.equals(selectionViewport)) {
                selection = List.copyOf(regions);
                selectionViewport = viewport;
                List<CapturePlacement> placements = regions.stream()
                    .filter(region -> !region.erased())
                    .map(region -> viewport.pixels(region.bounds()))
                    .filter(bounds -> !bounds.isEmpty())
                    .map(
                        bounds -> new CapturePlacement(
                            (int) bounds.x(),
                            (int) bounds.y(),
                            (int) bounds.width(),
                            (int) bounds.height(),
                            (int) bounds.x(),
                            (int) bounds.y()))
                    .toList();
                List<CaptureRect> masks = regions.stream()
                    .filter(CaptureRegion::erased)
                    .map(region -> viewport.pixels(region.bounds()))
                    .toList();
                selectionPlacements = new CaptureComposition(
                    new CaptureOutputSize(viewport.pixelWidth(), viewport.pixelHeight()),
                    placements).visiblePlacements(masks);
            }
            for (CapturePlacement placement : selectionPlacements) {
                CaptureRect pixels = new CaptureRect(
                    placement.sourceX(),
                    placement.sourceY(),
                    placement.width(),
                    placement.height());
                CaptureRect destination = new CaptureRect(
                    pixels.x() / viewport.scaleX(),
                    pixels.y() / viewport.scaleY(),
                    pixels.width() / viewport.scaleX(),
                    pixels.height() / viewport.scaleY());
                if (backgroundArgb >>> 24 != 0) Gui.drawRect(
                    (int) destination.x(),
                    (int) destination.y(),
                    (int) destination.right(),
                    (int) destination.bottom(),
                    backgroundArgb);
                CaptureTextureRenderer
                    .drawRegion(texture.getGlTextureId(), pixels, destination, textureWidth, textureHeight);
            }
            return;
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

    private void checkerboard(CaptureRect bounds, int background) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        for (int y = 0; y < bounds.height(); y += 12) {
            for (int x = 0; x < bounds.width(); x += 12) {
                boolean light = ((x / 12 + y / 12) & 1) == 0;
                int shade = invertTransparencyGrid ? light ? 0xE9EDF2 : 0xD5DCE5 : light ? 0x252A31 : 0x1D2228;
                if ((background >>> 24) == 255) shade = background & 0xFFFFFF;
                tessellator.setColorOpaque_I(shade);
                double left = bounds.x() + x;
                double right = bounds.x() + Math.min(bounds.width(), x + 12);
                double top = bounds.y() + y;
                double bottom = bounds.y() + Math.min(bounds.height(), y + 12);
                tessellator.addVertex(left, bottom, 0);
                tessellator.addVertex(right, bottom, 0);
                tessellator.addVertex(right, top, 0);
                tessellator.addVertex(left, top, 0);
            }
        }
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public void drawComposition(CaptureComposition composition, CaptureRect bounds, CaptureExportRequest request) {
        if (!hasFrame || texture == null) return;
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
        checkerboard(
            new CaptureRect(
                left,
                top,
                composition.size()
                    .width() * scale,
                composition.size()
                    .height() * scale),
            0);
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
        hasFrame = false;
    }
}
