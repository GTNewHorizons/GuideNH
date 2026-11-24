package com.hfstudio.guidenh.integration.betterquesting.capture;

import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.mixins.late.compat.betterquesting.AccessorCaptureBqCanvas;

import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.IGuiCanvas;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;

public class BqCaptureCanvasTree {

    private BqCaptureCanvasTree() {}

    public record Node(IGuiPanel panel, String parent, CaptureRect bounds, CaptureRect visibleBounds, double scaleX,
        double scaleY, int layer) {}

    public static Map<String, Node> collect(GuiScreenCanvas screen, CaptureRect viewport, boolean includeDisabled) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        collect(screen, "bq:screen", "minecraft:screen", viewport, new AffineTransform(), 1, includeDisabled, nodes);
        IGuiPanel popup = ((AccessorCaptureBqCanvas) screen).guidenh$getCapturePopup();
        if (popup != null) collect(
            popup,
            "bq:popup",
            "minecraft:screen",
            viewport,
            new AffineTransform(),
            1000,
            includeDisabled,
            nodes);
        return Collections.unmodifiableMap(nodes);
    }

    private static void collect(IGuiPanel panel, String id, String parent, CaptureRect clip, AffineTransform transform,
        int layer, boolean includeDisabled, Map<String, Node> nodes) {
        if (!includeDisabled && !panel.isEnabled()) return;
        IGuiRect rectangle = panel.getTransform();
        CaptureRect bounds = bounds(rectangle, transform);
        CaptureRect visible = bounds.intersect(clip);
        nodes.put(id, new Node(panel, parent, bounds, visible, transform.getScaleX(), transform.getScaleY(), layer));
        if (!(panel instanceof IGuiCanvas canvas)) return;
        AffineTransform childrenTransform = transform;
        CaptureRect childrenClip = clip;
        if (panel instanceof CanvasScrolling scrolling) {
            if (!includeDisabled && visible.isEmpty()) return;
            childrenClip = visible;
            childrenTransform = new AffineTransform(transform);
            childrenTransform.translate(rectangle.getX(), rectangle.getY());
            double zoom = scrolling.getZoom();
            childrenTransform.scale(zoom, zoom);
            childrenTransform.translate(-scrolling.getScrollX(), -scrolling.getScrollY());
        }
        List<IGuiPanel> children = canvas.getChildren();
        for (int index = 0; index < children.size(); index++) collect(
            children.get(index),
            id + "/" + index,
            id,
            childrenClip,
            childrenTransform,
            layer,
            includeDisabled,
            nodes);
    }

    private static CaptureRect bounds(IGuiRect rectangle, AffineTransform transform) {
        return new CaptureRect(
            rectangle.getX() * transform.getScaleX() + transform.getTranslateX(),
            rectangle.getY() * transform.getScaleY() + transform.getTranslateY(),
            Math.max(0, rectangle.getWidth()) * transform.getScaleX(),
            Math.max(0, rectangle.getHeight()) * transform.getScaleY());
    }
}
