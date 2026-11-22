package com.hfstudio.guidenh.client.capture.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureRegion;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureSelectionRenderer {

    public void draw(List<CaptureRegion> regions, CaptureRegion active, CaptureTarget hovered, CaptureViewport viewport,
        int opacity, FontRenderer font) {
        List<CaptureRect> holes = new ArrayList<>();
        for (CaptureRegion region : regions) holes.add(region.bounds());
        if (regions.isEmpty() && hovered != null) holes.add(hovered.visibleBounds());
        shade(holes, viewport, opacity * 255 / 100);
        if (hovered != null && regions.isEmpty()) outline(hovered.visibleBounds(), 0xFFFFFFFF, true);
        int number = 0;
        for (CaptureRegion region : regions) {
            number++;
            boolean selected = active != null && active.id() == region.id();
            CaptureRect rectangle = region.bounds();
            outline(rectangle, selected ? 0xFFFFFFFF : 0xFFABB3BC, false);
            if (selected) handles(rectangle);
            String text = "#" + number
                + "  "
                + (int) Math.round(rectangle.width())
                + " × "
                + (int) Math.round(rectangle.height());
            int labelX = Math
                .clamp((int) rectangle.x(), 4, Math.max(4, viewport.logicalWidth() - font.getStringWidth(text) - 8));
            int labelY = rectangle.y() >= 18 ? (int) rectangle.y() - 15 : (int) rectangle.bottom() + 6;
            labelY = Math.clamp(labelY, 4, viewport.logicalHeight() - 13);
            Gui.drawRect(labelX - 3, labelY - 3, labelX + font.getStringWidth(text) + 3, labelY + 11, 0xE51C222A);
            font.drawStringWithShadow(text, labelX, labelY, 0xFFFFFF);
        }
    }

    private static void shade(List<CaptureRect> holes, CaptureViewport viewport, int alpha) {
        TreeSet<Double> ys = new TreeSet<>();
        ys.add(0d);
        ys.add((double) viewport.logicalHeight());
        for (CaptureRect hole : holes) {
            ys.add(hole.y());
            ys.add(hole.bottom());
        }
        List<Double> boundaries = List.copyOf(ys);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setColorRGBA_I(0, alpha);
        for (int index = 1; index < boundaries.size(); index++) {
            double top = boundaries.get(index - 1);
            double bottom = boundaries.get(index);
            List<CaptureRect> intervals = holes.stream()
                .filter(rectangle -> rectangle.y() <= top && rectangle.bottom() >= bottom)
                .sorted(Comparator.comparingDouble(CaptureRect::x))
                .toList();
            double left = 0;
            for (CaptureRect interval : intervals) {
                if (interval.x() > left) quad(tessellator, left, top, interval.x(), bottom);
                left = Math.max(left, interval.right());
            }
            if (left < viewport.logicalWidth()) quad(tessellator, left, top, viewport.logicalWidth(), bottom);
        }
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void quad(Tessellator tessellator, double left, double top, double right, double bottom) {
        tessellator.addVertex(left, bottom, 0);
        tessellator.addVertex(right, bottom, 0);
        tessellator.addVertex(right, top, 0);
        tessellator.addVertex(left, top, 0);
    }

    public static void outline(CaptureRect rectangle, int color, boolean dashed) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(1);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        tessellator.setColorRGBA_I(color & 0xFFFFFF, color >>> 24);
        edge(tessellator, rectangle.x(), rectangle.y(), rectangle.right(), rectangle.y(), dashed);
        edge(tessellator, rectangle.right(), rectangle.y(), rectangle.right(), rectangle.bottom(), dashed);
        edge(tessellator, rectangle.right(), rectangle.bottom(), rectangle.x(), rectangle.bottom(), dashed);
        edge(tessellator, rectangle.x(), rectangle.bottom(), rectangle.x(), rectangle.y(), dashed);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    private static void edge(Tessellator tessellator, double x1, double y1, double x2, double y2, boolean dashed) {
        if (!dashed) {
            tessellator.addVertex(x1, y1, 0);
            tessellator.addVertex(x2, y2, 0);
            return;
        }
        double length = Math.hypot(x2 - x1, y2 - y1);
        for (double offset = 0; offset < length; offset += 8) {
            double end = Math.min(length, offset + 4);
            tessellator.addVertex(x1 + (x2 - x1) * offset / length, y1 + (y2 - y1) * offset / length, 0);
            tessellator.addVertex(x1 + (x2 - x1) * end / length, y1 + (y2 - y1) * end / length, 0);
        }
    }

    private static void handles(CaptureRect rectangle) {
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (x == 1 && y == 1) continue;
                int centerX = (int) Math.round(rectangle.x() + rectangle.width() * x / 2);
                int centerY = (int) Math.round(rectangle.y() + rectangle.height() * y / 2);
                Gui.drawRect(centerX - 3, centerY - 3, centerX + 4, centerY + 4, 0xFF10151A);
                Gui.drawRect(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFFFFFFF);
            }
        }
    }
}
