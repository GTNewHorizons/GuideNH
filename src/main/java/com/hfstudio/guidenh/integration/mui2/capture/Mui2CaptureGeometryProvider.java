package com.hfstudio.guidenh.integration.mui2.capture;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.layout.IViewport;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.GuiViewportStack;
import com.cleanroommc.modularui.widget.sizer.Area;
import com.cleanroommc.modularui.widgets.slot.FluidSlot;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public class Mui2CaptureGeometryProvider implements CaptureGeometryProvider {

    private final GuiViewportStack transforms = new GuiViewportStack();

    @Override
    public String id() {
        return "modularui2";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return context.screen() instanceof IMuiScreen;
    }

    @Override
    public CaptureGeometry collect(CaptureContext context) {
        IMuiScreen screen = (IMuiScreen) context.screen();
        List<CaptureTarget> targets = new ArrayList<>();
        transforms.reset();
        transforms.pushViewport(
            null,
            new Area(
                0,
                0,
                context.viewport()
                    .logicalWidth(),
                context.viewport()
                    .logicalHeight()));
        try {
            int panelIndex = 0;
            for (ModularPanel panel : screen.getScreen()
                .getPanelManager()
                .getReverseOpenPanels()) {
                panelIndex++;
                if (panel.getAlpha() <= 0) continue;
                String name = panel.getName();
                String root = "mui2:" + (name == null ? Integer.toString(panelIndex) : name);
                collectWidget(panel, root, "", panelIndex, 0, targets);
            }
        } finally {
            transforms.popViewport(null);
        }
        return new CaptureGeometry(targets.hashCode(), context.viewport(), targets);
    }

    private void collectWidget(IWidget widget, String id, String parentId, int panelIndex, int depth,
        List<CaptureTarget> targets) {
        if (!widget.isEnabled()) return;
        transforms.pushMatrix();
        try {
            widget.transform(transforms);
            Area area = widget.getArea();
            CaptureRect bounds = transformedBounds(
                transforms.peek()
                    .getMatrix(),
                area.w(),
                area.h());
            Area viewport = transforms.getViewport();
            CaptureRect clip = new CaptureRect(
                viewport.x(),
                viewport.y(),
                Math.max(0, viewport.w()),
                Math.max(0, viewport.h()));
            CaptureRect visible = bounds.intersect(clip);
            String kind = widget instanceof ModularPanel ? CaptureTarget.WINDOW
                : widget instanceof ItemSlot || widget instanceof FluidSlot ? CaptureTarget.SLOT
                    : CaptureTarget.COMPONENT;
            String name = widget.getName();
            if (!visible.isEmpty()) {
                targets.add(
                    new CaptureTarget(
                        id,
                        parentId,
                        kind,
                        widget.getClass()
                            .getName(),
                        name == null ? widget.getClass()
                            .getSimpleName() : name,
                        bounds,
                        visible,
                        panelIndex,
                        kind.equals(CaptureTarget.WINDOW) ? -10 : 20 + depth + area.z(),
                        Integer.toUnsignedLong(System.identityHashCode(widget))));
            }
            IViewport childViewport = widget instanceof IViewport value ? value : null;
            if (childViewport != null) {
                transforms.pushViewport(childViewport, area);
                childViewport.transformChildren(transforms);
            }
            try {
                List<IWidget> children = widget.getChildren();
                for (int index = 0; index < children.size(); index++) {
                    IWidget child = children.get(index);
                    String childName = child.getName();
                    collectWidget(
                        child,
                        id + "/" + index + (childName == null ? "" : ":" + childName),
                        id,
                        panelIndex,
                        depth + 1,
                        targets);
                }
            } finally {
                if (childViewport != null) transforms.popViewport(childViewport);
            }
        } finally {
            transforms.popMatrix();
        }
    }

    private static CaptureRect transformedBounds(Matrix4f matrix, int width, int height) {
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
