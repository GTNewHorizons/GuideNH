package com.hfstudio.guidenh.integration.capture;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.client.capture.api.CaptureContext;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureGeometryProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent.ComponentEntry;
import com.hfstudio.guidenh.guide.internal.debug.DebugComponent.DocumentGeometry;

public class GuideCaptureGeometryProvider implements CaptureGeometryProvider {

    @Override
    public String id() {
        return "guidenh";
    }

    @Override
    public boolean supports(CaptureContext context) {
        return context.screen() instanceof DebugComponent;
    }

    @Override
    public CaptureGeometry collect(CaptureContext context) {
        DebugComponent source = (DebugComponent) context.screen();
        List<CaptureTarget> targets = new ArrayList<>();
        List<ComponentEntry> components = source.getDebugComponents();
        Map<ComponentEntry, String> ids = new IdentityHashMap<>();
        for (int index = 0; index < components.size(); index++)
            ids.put(components.get(index), "guide:component:" + index);
        for (ComponentEntry component : components) {
            CaptureRect rectangle = rectangle(component.getBounds());
            if (rectangle.isEmpty()) continue;
            targets.add(
                new CaptureTarget(
                    ids.get(component),
                    ids.getOrDefault(component.getParent(), "minecraft:screen"),
                    component.getParent() == null ? CaptureTarget.WINDOW : CaptureTarget.COMPONENT,
                    context.screen()
                        .getClass()
                        .getName(),
                    component.getName(),
                    rectangle,
                    rectangle.intersect(
                        context.viewport()
                            .bounds()),
                    1,
                    component.getPriority()));
        }
        int index = 0;
        for (DocumentGeometry document : source.getDebugDocuments()) {
            collectNode(document.document(), "guide:document:" + index++, "minecraft:screen", document, targets, 0);
        }
        return new CaptureGeometry(targets.hashCode(), context.viewport(), targets);
    }

    private static void collectNode(LytNode node, String id, String parent, DocumentGeometry document,
        List<CaptureTarget> targets, int depth) {
        LytRect bounds = node.getBounds();
        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            CaptureRect positioned = new CaptureRect(
                document.viewport()
                    .x() + bounds.x() * document.zoom(),
                document.viewport()
                    .y() + (bounds.y() - document.scrollY()) * document.zoom(),
                bounds.width() * document.zoom(),
                bounds.height() * document.zoom());
            CaptureRect visible = positioned.intersect(rectangle(document.clip()));
            if (!visible.isEmpty()) targets.add(
                new CaptureTarget(
                    id,
                    parent,
                    CaptureTarget.COMPONENT,
                    node.getClass()
                        .getName(),
                    node.getClass()
                        .getSimpleName(),
                    positioned,
                    visible,
                    1,
                    depth,
                    Integer.toUnsignedLong(System.identityHashCode(node))));
        }
        List<? extends LytNode> children = node.getChildren();
        for (int index = 0; index < children.size(); index++) {
            collectNode(children.get(index), id + "/" + index, id, document, targets, depth + 1);
        }
    }

    private static CaptureRect rectangle(LytRect bounds) {
        return new CaptureRect(bounds.x(), bounds.y(), Math.max(0, bounds.width()), Math.max(0, bounds.height()));
    }
}
