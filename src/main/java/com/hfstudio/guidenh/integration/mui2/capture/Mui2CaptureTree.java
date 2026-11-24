package com.hfstudio.guidenh.integration.mui2.capture;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureViewport;

public class Mui2CaptureTree {

    private final IMuiScreen screen;
    private Map<String, Node> nodes;

    public record PageStep(String path, int page) {}

    public record Node(IWidget widget, List<PageStep> ancestors, List<IWidget> lineage) {

        public Node {
            ancestors = List.copyOf(ancestors);
            lineage = List.copyOf(lineage);
            if (lineage.isEmpty() || lineage.getLast() != widget)
                throw new IllegalArgumentException("A widget lineage must end at the selected widget");
        }

        public boolean axisAligned(CaptureViewport viewport) {
            return Mui2CaptureProjection.axisAligned(Mui2CaptureProjection.transform(lineage, viewport));
        }

        public CaptureRect bounds(CaptureViewport viewport) {
            return Mui2CaptureProjection.bounds(
                Mui2CaptureProjection.transform(lineage, viewport),
                widget.getArea()
                    .w(),
                widget.getArea()
                    .h());
        }
    }

    public Mui2CaptureTree(IMuiScreen screen) {
        this.screen = screen;
        refresh();
    }

    public void refresh() {
        Map<String, Node> collected = new LinkedHashMap<>();
        Set<IWidget> visiting = Collections.newSetFromMap(new IdentityHashMap<>());
        int index = 0;
        for (ModularPanel panel : screen.getScreen()
            .getPanelManager()
            .getReverseOpenPanels()) {
            String name = panel.getName();
            String path = name == null || name.isBlank() ? "panel:" + index
                : "panel:name:" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            index++;
            collect(panel, path, List.of(), List.of(), collected, visiting);
        }
        nodes = Collections.unmodifiableMap(collected);
    }

    private static void collect(IWidget widget, String path, List<PageStep> ancestors, List<IWidget> parents,
        Map<String, Node> nodes, Set<IWidget> visiting) {
        if (!visiting.add(widget)) return;
        try {
            List<IWidget> lineage = new ArrayList<>(parents);
            lineage.add(widget);
            nodes.put(path, new Node(widget, ancestors, lineage));
            List<IWidget> children = widget instanceof PagedWidget<?>paged ? paged.getPages() : widget.getChildren();
            for (int index = 0; index < children.size(); index++) {
                List<PageStep> next = ancestors;
                String childPath = path + "/" + index;
                if (widget instanceof PagedWidget<?>) {
                    next = new ArrayList<>(ancestors);
                    next.add(new PageStep(path, index));
                    childPath = path + "/page:" + index;
                }
                collect(children.get(index), childPath, next, lineage, nodes, visiting);
            }
        } finally {
            visiting.remove(widget);
        }
    }

    public Map<String, Node> nodes() {
        return nodes;
    }

    public Node node(IWidget widget) {
        for (Node node : nodes.values()) if (node.widget() == widget) return node;
        return null;
    }

    public Map<String, Integer> pages() {
        Map<String, Integer> pages = new LinkedHashMap<>();
        for (Map.Entry<String, Node> entry : nodes.entrySet()) {
            if (entry.getValue()
                .widget() instanceof PagedWidget<?>paged) pages.put(entry.getKey(), paged.getCurrentPageIndex());
        }
        return Collections.unmodifiableMap(pages);
    }

    public Node activate(String path, int page) {
        refresh();
        Node selected = require(path);
        for (PageStep ancestor : selected.ancestors()) {
            Node parent = require(ancestor.path());
            if (!(parent.widget() instanceof PagedWidget<?>paged))
                throw new IllegalStateException("The MUI2 page parent changed: " + ancestor.path());
            if (paged.getCurrentPageIndex() != ancestor.page()) {
                paged.setPage(ancestor.page());
                refresh();
            }
        }
        selected = require(path);
        if (selected.widget() instanceof PagedWidget<?>paged && paged.getCurrentPageIndex() != page) {
            paged.setPage(page);
            refresh();
        }
        return require(path);
    }

    public Node require(String path) {
        Node node = nodes.get(path);
        if (node == null) throw new IllegalStateException("The selected MUI2 widget disappeared: " + path);
        return node;
    }

    public void restorePages(Map<String, Integer> pages) {
        restorePages(pages, true);
    }

    public void restorePages(Map<String, Integer> pages, boolean requirePresent) {
        for (Map.Entry<String, Integer> entry : pages.entrySet()) {
            Node node = nodes.get(entry.getKey());
            if (node == null || !(node.widget() instanceof PagedWidget<?>paged)) {
                if (requirePresent)
                    throw new IllegalStateException("The original MUI2 page disappeared: " + entry.getKey());
                continue;
            }
            if (paged.getCurrentPageIndex() != entry.getValue()) {
                paged.setPage(entry.getValue());
                refresh();
            }
        }
    }
}
