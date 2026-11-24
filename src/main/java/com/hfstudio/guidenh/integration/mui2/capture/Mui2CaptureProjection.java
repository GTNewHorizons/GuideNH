package com.hfstudio.guidenh.integration.mui2.capture;

import java.util.List;

import org.joml.Matrix4f;

import com.cleanroommc.modularui.api.layout.IViewport;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.GuiViewportStack;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class Mui2CaptureProjection {

    private Mui2CaptureProjection() {}

    public static Matrix4f transform(List<IWidget> lineage, CaptureViewport viewport) {
        if (lineage.isEmpty()) throw new IllegalArgumentException("A widget transform requires its parent lineage");
        GuiViewportStack transforms = new GuiViewportStack();
        transforms.reset();
        transforms.pushViewport(null, new Area(0, 0, viewport.logicalWidth(), viewport.logicalHeight()));
        try {
            return transform(transforms, lineage, 0);
        } finally {
            transforms.popViewport(null);
        }
    }

    private static Matrix4f transform(GuiViewportStack transforms, List<IWidget> lineage, int index) {
        IWidget widget = lineage.get(index);
        transforms.pushMatrix();
        try {
            widget.transform(transforms);
            if (index == lineage.size() - 1) return new Matrix4f(
                transforms.peek()
                    .getMatrix());
            IViewport childViewport = widget instanceof IViewport value ? value : null;
            if (childViewport != null) {
                transforms.pushViewport(childViewport, widget.getArea());
                childViewport.transformChildren(transforms);
            }
            try {
                return transform(transforms, lineage, index + 1);
            } finally {
                if (childViewport != null) transforms.popViewport(childViewport);
            }
        } finally {
            transforms.popMatrix();
        }
    }

    public static boolean axisAligned(Matrix4f matrix) {
        return Float.isFinite(matrix.m00()) && Float.isFinite(matrix.m11())
            && matrix.m00() > 0
            && matrix.m11() > 0
            && Math.abs(matrix.m01()) < 0.0001f
            && Math.abs(matrix.m10()) < 0.0001f;
    }

    public static CaptureRect bounds(Matrix4f matrix, int width, int height) {
        double x0 = matrix.m30();
        double y0 = matrix.m31();
        double x1 = x0 + matrix.m00() * width;
        double y1 = y0 + matrix.m01() * width;
        double x2 = x0 + matrix.m10() * height;
        double y2 = y0 + matrix.m11() * height;
        double x3 = x1 + matrix.m10() * height;
        double y3 = y1 + matrix.m11() * height;
        return CaptureRect.between(
            Math.min(Math.min(x0, x1), Math.min(x2, x3)),
            Math.min(Math.min(y0, y1), Math.min(y2, y3)),
            Math.max(Math.max(x0, x1), Math.max(x2, x3)),
            Math.max(Math.max(y0, y1), Math.max(y2, y3)));
    }
}
