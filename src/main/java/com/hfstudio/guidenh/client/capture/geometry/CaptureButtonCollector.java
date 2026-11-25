package com.hfstudio.guidenh.client.capture.geometry;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryCatalog;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class CaptureButtonCollector {

    private final List<CaptureTarget> targets = new ArrayList<>();
    private final FloatBuffer model = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer pixels = BufferUtils.createIntBuffer(16);
    private final FloatBuffer point = BufferUtils.createFloatBuffer(3);

    public void clear() {
        targets.clear();
    }

    public void record(GuiButton button, CaptureViewport viewport, String parentId) {
        model.clear();
        projection.clear();
        pixels.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, model);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, pixels);
        double left = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.POSITIVE_INFINITY;
        double top = Double.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            float x = button.xPosition + ((corner & 1) == 0 ? 0 : button.width);
            float y = button.yPosition + ((corner & 2) == 0 ? 0 : button.height);
            point.clear();
            if (!GLU.gluProject(x, y, 0, model, projection, pixels, point)) return;
            left = Math.min(left, point.get(0));
            right = Math.max(right, point.get(0));
            bottom = Math.min(bottom, point.get(1));
            top = Math.max(top, point.get(1));
        }
        if (!Double.isFinite(left) || !Double.isFinite(right) || !Double.isFinite(bottom) || !Double.isFinite(top))
            return;
        CaptureRect bounds = CaptureRect.between(
            left / viewport.scaleX(),
            (viewport.pixelHeight() - top) / viewport.scaleY(),
            right / viewport.scaleX(),
            (viewport.pixelHeight() - bottom) / viewport.scaleY());
        CaptureRect visible = bounds.intersect(viewport.bounds());
        long owner = Integer.toUnsignedLong(System.identityHashCode(button));
        if (visible.isEmpty() || targets.stream()
            .anyMatch(
                target -> target.ownerIdentity() == owner && target.bounds()
                    .equals(bounds)))
            return;
        targets.add(
            new CaptureTarget(
                "minecraft:drawn_button:" + targets.size(),
                parentId,
                CaptureTarget.COMPONENT,
                button.getClass()
                    .getName(),
                button.displayString == null || button.displayString.isEmpty() ? button.getClass()
                    .getSimpleName() : button.displayString,
                bounds,
                visible,
                0,
                30,
                owner).withProvider(CaptureGeometryCatalog.DRAW_BUTTONS));
    }

    public CaptureGeometry merge(CaptureGeometry geometry) {
        if (targets.isEmpty()) return geometry;
        List<CaptureTarget> combined = new ArrayList<>(geometry.targets());
        combined.addAll(targets);
        return new CaptureGeometry(combined.hashCode(), geometry.viewport(), combined);
    }
}
