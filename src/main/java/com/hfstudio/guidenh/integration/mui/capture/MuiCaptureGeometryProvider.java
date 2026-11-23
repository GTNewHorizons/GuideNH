package com.hfstudio.guidenh.integration.mui.capture;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import com.gtnewhorizons.modularui.api.math.Pos2d;
import com.gtnewhorizons.modularui.api.math.Size;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.widget.IWidgetParent;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.widget.FluidSlotWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.config.Config;
import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.mixins.late.compat.mui.AccessorCaptureModularWindow;

public class MuiCaptureGeometryProvider implements CaptureGeometryProvider {

    @Override
    public String id() {
        return "modularui";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return context.screen() instanceof ModularGui;
    }

    @Override
    public CaptureGeometry collect(CaptureContext context) {
        ModularGui gui = (ModularGui) context.screen();
        List<ModularWindow> windows = new ArrayList<>();
        gui.getContext()
            .forEachWindowBottomToTop(windows::add);
        List<CaptureTarget> targets = new ArrayList<>();
        for (int index = 0; index < windows.size(); index++) {
            ModularWindow window = windows.get(index);
            if (!window.isEnabled() || window.getScale() <= 0 || window.getAlpha() == 0) continue;
            String id = "mui:window:" + index;
            AffineTransform transform = windowTransform(window);
            CaptureRect bounds = bounds(window.getPos(), window.getSize(), transform);
            targets.add(
                new CaptureTarget(
                    id,
                    "minecraft:screen",
                    CaptureTarget.WINDOW,
                    window.getClass()
                        .getName(),
                    window.getClass()
                        .getSimpleName(),
                    bounds,
                    bounds.intersect(
                        context.viewport()
                            .bounds()),
                    index + 1,
                    -10,
                    Integer.toUnsignedLong(System.identityHashCode(window))));
            List<Widget> children = window.getChildren();
            for (int child = 0; child < children.size(); child++) {
                collectWidget(
                    children.get(child),
                    id + "/" + child,
                    id,
                    index + 1,
                    0,
                    context.viewport()
                        .bounds(),
                    transform,
                    targets);
            }
        }
        return new CaptureGeometry(targets.hashCode(), context.viewport(), targets);
    }

    public static AffineTransform windowTransform(ModularWindow window) {
        AccessorCaptureModularWindow access = (AccessorCaptureModularWindow) window;
        double centerX = window.getPos().x + window.getSize().width / 2d;
        double centerY = window.getPos().y + window.getSize().height / 2d;
        AffineTransform transform = new AffineTransform();
        if (Config.openCloseRotateFast)
            transform.rotate(Math.toRadians(access.guidenh$getCaptureRotation()), centerX, centerY);
        transform.translate(access.guidenh$getCaptureTranslateX(), access.guidenh$getCaptureTranslateY());
        transform.translate(centerX, centerY);
        transform.scale(window.getScale(), window.getScale());
        transform.translate(-centerX, -centerY);
        return transform;
    }

    private static void collectWidget(Widget widget, String id, String parent, int layer, int depth, CaptureRect clip,
        AffineTransform transform, List<CaptureTarget> targets) {
        if (!widget.isEnabled()) return;
        CaptureRect bounds = bounds(widget.getAbsolutePos(), widget.getSize(), transform);
        CaptureRect visible = bounds.intersect(clip);
        if (!visible.isEmpty()) {
            String kind = widget instanceof SlotWidget || widget instanceof FluidSlotWidget ? CaptureTarget.SLOT
                : CaptureTarget.COMPONENT;
            targets.add(
                new CaptureTarget(
                    id,
                    parent,
                    kind,
                    widget.getClass()
                        .getName(),
                    widget.getClass()
                        .getSimpleName(),
                    bounds,
                    visible,
                    layer,
                    20 + depth + widget.getLayer(),
                    Integer.toUnsignedLong(System.identityHashCode(widget))));
        }
        if (widget instanceof IWidgetParent group) {
            CaptureRect childClip = group.childrenMustBeInBounds() ? clip.intersect(bounds) : clip;
            List<Widget> children = group.getChildren();
            for (int index = 0; index < children.size(); index++) {
                collectWidget(
                    children.get(index),
                    id + "/" + index,
                    id,
                    layer,
                    depth + 1,
                    childClip,
                    transform,
                    targets);
            }
        }
    }

    public static CaptureRect bounds(Pos2d position, Size size, AffineTransform transform) {
        double x0 = transform.getScaleX() * position.x + transform.getShearX() * position.y + transform.getTranslateX();
        double y0 = transform.getShearY() * position.x + transform.getScaleY() * position.y + transform.getTranslateY();
        double x1 = x0 + transform.getScaleX() * size.width;
        double y1 = y0 + transform.getShearY() * size.width;
        double x2 = x0 + transform.getShearX() * size.height;
        double y2 = y0 + transform.getScaleY() * size.height;
        double x3 = x1 + transform.getShearX() * size.height;
        double y3 = y1 + transform.getScaleY() * size.height;
        return CaptureRect.between(
            Math.min(Math.min(x0, x1), Math.min(x2, x3)),
            Math.min(Math.min(y0, y1), Math.min(y2, y3)),
            Math.max(Math.max(x0, x1), Math.max(x2, x3)),
            Math.max(Math.max(y0, y1), Math.max(y2, y3)));
    }
}
