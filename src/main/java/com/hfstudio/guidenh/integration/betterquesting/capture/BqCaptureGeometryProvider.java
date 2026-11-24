package com.hfstudio.guidenh.integration.betterquesting.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.integration.betterquesting.capture.BqCaptureCanvasTree.Node;

import betterquesting.api2.client.gui.GuiScreenCanvas;

public class BqCaptureGeometryProvider implements CaptureGeometryProvider {

    @Override
    public String id() {
        return "betterquesting";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return context.screen() instanceof GuiScreenCanvas;
    }

    @Override
    public CaptureGeometry collect(CaptureContext context) {
        List<CaptureTarget> targets = new ArrayList<>();
        for (Map.Entry<String, Node> entry : BqCaptureCanvasTree
            .collect(
                (GuiScreenCanvas) context.screen(),
                context.viewport()
                    .bounds(),
                false)
            .entrySet()) {
            Node node = entry.getValue();
            if (node.visibleBounds()
                .isEmpty()) continue;
            boolean window = entry.getKey()
                .equals("bq:screen")
                || entry.getKey()
                    .equals("bq:popup");
            targets.add(
                new CaptureTarget(
                    entry.getKey(),
                    node.parent(),
                    window ? CaptureTarget.WINDOW : CaptureTarget.COMPONENT,
                    node.panel()
                        .getClass()
                        .getName(),
                    node.panel()
                        .getClass()
                        .getSimpleName(),
                    node.bounds(),
                    node.visibleBounds(),
                    node.layer(),
                    window ? -10
                        : 20 + node.panel()
                            .getTransform()
                            .getDepth(),
                    Integer.toUnsignedLong(System.identityHashCode(node.panel()))));
        }
        return new CaptureGeometry(targets.hashCode(), context.viewport(), targets);
    }
}
