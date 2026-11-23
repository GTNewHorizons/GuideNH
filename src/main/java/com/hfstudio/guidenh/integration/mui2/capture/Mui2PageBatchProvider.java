package com.hfstudio.guidenh.integration.mui2.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.hfstudio.guidenh.capture.server.api.CaptureGuiLocation;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;

public class Mui2PageBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "mui2_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() instanceof IMuiScreen;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.PAGES)) return List.of();
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (Map.Entry<String, PagePath> entry : paths(
            (IMuiScreen) context.capture()
                .screen()).entrySet()) {
            PagePath path = entry.getValue();
            for (int page = 0; page < path.widget()
                .getPages()
                .size(); page++) {
                String label = path.widget()
                    .getName();
                if (label == null) label = path.widget()
                    .getClass()
                    .getSimpleName();
                targets.add(
                    new CaptureTargetDescriptor(
                        "mui2:page:" + entry.getKey() + "/" + page,
                        id(),
                        CaptureBatchOptions.PAGES,
                        label + " · " + (page + 1),
                        "ready",
                        "",
                        Map.of("path", entry.getKey(), "page", Integer.toString(page))));
            }
        }
        return List.copyOf(targets);
    }

    private static Map<String, PagePath> paths(IMuiScreen screen) {
        Map<String, PagePath> paths = new LinkedHashMap<>();
        int index = 0;
        for (ModularPanel panel : screen.getScreen()
            .getPanelManager()
            .getReverseOpenPanels()) {
            collect(panel, "panel:" + index++, List.of(), paths);
        }
        return paths;
    }

    private static void collect(IWidget widget, String path, List<PageStep> ancestors, Map<String, PagePath> paths) {
        if (widget instanceof PagedWidget<?>paged) {
            paths.put(path, new PagePath(paged, ancestors));
            List<IWidget> pages = paged.getPages();
            for (int index = 0; index < pages.size(); index++) {
                List<PageStep> next = new ArrayList<>(ancestors);
                next.add(new PageStep(paged, index));
                collect(pages.get(index), path + "/page:" + index, List.copyOf(next), paths);
            }
        } else {
            List<IWidget> children = widget.getChildren();
            for (int index = 0; index < children.size(); index++)
                collect(children.get(index), path + "/" + index, ancestors, paths);
        }
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new PageNavigator(
            (IMuiScreen) context.capture()
                .screen());
    }

    public record PageStep(PagedWidget<?> widget, int page) {}

    public record PagePath(PagedWidget<?> widget, List<PageStep> ancestors) {

        public PagePath {
            ancestors = List.copyOf(ancestors);
        }
    }

    public static class PageNavigator implements CaptureBatchNavigator {

        private IMuiScreen screen;
        private Map<String, PagePath> paths;
        private final Map<String, Integer> original = new LinkedHashMap<>();
        private final CaptureGuiLocation location;
        private PagePath selected;
        private int page;

        public PageNavigator(IMuiScreen screen) {
            this.screen = screen;
            location = Minecraft.getMinecraft().thePlayer == null ? null
                : Mui2ServerContainerIdentity.location(Minecraft.getMinecraft().thePlayer.openContainer);
            paths = paths(screen);
            for (Map.Entry<String, PagePath> entry : paths.entrySet()) original.put(
                entry.getKey(),
                entry.getValue()
                    .widget()
                    .getCurrentPageIndex());
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            selected = paths.get(
                target.data()
                    .get("path"));
            page = Integer.parseInt(
                target.data()
                    .get("page"));
            for (PageStep ancestor : selected.ancestors()) {
                if (ancestor.widget()
                    .getCurrentPageIndex() != ancestor.page())
                    ancestor.widget()
                        .setPage(ancestor.page());
            }
            if (selected.widget()
                .getCurrentPageIndex() != page)
                selected.widget()
                    .setPage(page);
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == screen && selected != null
                && selected.widget()
                    .isValid()
                && selected.widget()
                    .isEnabled()
                && selected.widget()
                    .getCurrentPageIndex() == page;
            for (PageStep ancestor : selected.ancestors()) ready &= ancestor.widget()
                .getCurrentPageIndex() == ancestor.page();
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for the tab to become visible");
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            return List.of(
                context.capture()
                    .viewport()
                    .bounds());
        }

        @Override
        public void restore() {
            if (location != null && (Minecraft.getMinecraft().thePlayer == null || !location
                .equals(Mui2ServerContainerIdentity.location(Minecraft.getMinecraft().thePlayer.openContainer))))
                throw new IllegalStateException("The reopened MUI2 GUI belongs to a different target");
            if (!(Minecraft.getMinecraft().currentScreen instanceof IMuiScreen current))
                throw new IllegalStateException("The original MUI2 GUI could not be reopened");
            screen = current;
            paths = paths(screen);
            for (Map.Entry<String, Integer> entry : original.entrySet()) {
                PagePath path = paths.get(entry.getKey());
                if (path == null)
                    throw new IllegalStateException("The original MUI2 page path disappeared: " + entry.getKey());
                if (path.widget()
                    .getCurrentPageIndex() != entry.getValue())
                    path.widget()
                        .setPage(entry.getValue());
            }
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == screen;
            for (Map.Entry<String, Integer> entry : original.entrySet()) {
                PagePath path = paths.get(entry.getKey());
                ready &= path != null && path.widget()
                    .getCurrentPageIndex() == entry.getValue();
            }
            return new CaptureBatchReadiness(
                ready,
                context.geometry()
                    .revision(),
                ready ? "" : "Waiting for the original tabs");
        }
    }
}
