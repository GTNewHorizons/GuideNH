package com.hfstudio.guidenh.integration.mui.capture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.widget.IWidgetParent;
import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.widget.PageControlWidget;

public class MuiCaptureTree {

    private final Map<String, Node> nodes;

    public record PageStep(PageControlWidget widget, int page) {}

    public record Node(Widget widget, List<PageStep> ancestors) {

        public Node {
            ancestors = List.copyOf(ancestors);
        }
    }

    public MuiCaptureTree(ModularGui gui) {
        Map<String, Node> collected = new LinkedHashMap<>();
        List<ModularWindow> windows = new ArrayList<>();
        gui.getContext()
            .forEachWindowBottomToTop(windows::add);
        for (int window = 0; window < windows.size(); window++) {
            List<Widget> children = windows.get(window)
                .getChildren();
            for (int index = 0; index < children.size(); index++)
                collect(children.get(index), "mui:window:" + window + "/" + index, List.of(), collected);
        }
        nodes = Collections.unmodifiableMap(collected);
    }

    private static void collect(Widget widget, String path, List<PageStep> ancestors, Map<String, Node> nodes) {
        nodes.put(path, new Node(widget, ancestors));
        if (!(widget instanceof IWidgetParent parent)) return;
        List<Widget> children = parent.getChildren();
        for (int index = 0; index < children.size(); index++) {
            List<PageStep> next = ancestors;
            if (widget instanceof PageControlWidget paged) {
                next = new ArrayList<>(ancestors);
                next.add(new PageStep(paged, index));
            }
            collect(children.get(index), path + "/" + index, next, nodes);
        }
    }

    public Map<String, Node> nodes() {
        return nodes;
    }

    public Map<String, Integer> pages() {
        Map<String, Integer> pages = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            if (entry.getValue()
                .widget() instanceof PageControlWidget paged) pages.put(entry.getKey(), paged.getCurrentPage());
        }
        return Collections.unmodifiableMap(pages);
    }

    public void activate(Node node, int page) {
        for (PageStep step : node.ancestors()) step.widget()
            .setActivePage(step.page());
        if (node.widget() instanceof PageControlWidget paged) paged.setActivePage(page);
        for (Node child : nodes.values()) {
            if (child.widget() instanceof PageControlWidget paged) paged.setActivePage(paged.getCurrentPage());
        }
    }

    public void restorePages(Map<String, Integer> pages) {
        for (Map.Entry<String, Integer> entry : pages.entrySet()) {
            Node node = nodes.get(entry.getKey());
            if (node == null || !(node.widget() instanceof PageControlWidget paged))
                throw new IllegalStateException("The original MUI page disappeared: " + entry.getKey());
            paged.setActivePage(entry.getValue());
        }
    }
}
