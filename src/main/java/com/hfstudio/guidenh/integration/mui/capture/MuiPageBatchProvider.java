package com.hfstudio.guidenh.integration.mui.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizons.modularui.api.screen.ModularUIContext;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.common.internal.wrapper.ModularGui;
import com.gtnewhorizons.modularui.common.widget.PageControlWidget;
import com.hfstudio.guidenh.capture.server.api.CaptureGuiLocation;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchOptions;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchProvider;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.integration.mui.capture.MuiCaptureTree.Node;
import com.hfstudio.guidenh.integration.mui.capture.MuiCaptureTree.PageStep;
import com.hfstudio.guidenh.integration.mui.capture.MuiScrollViewport.Offset;
import com.hfstudio.guidenh.mixins.late.compat.mui.AccessorCaptureModularWindow;

public class MuiPageBatchProvider implements CaptureBatchProvider {

    @Override
    public String id() {
        return "mui1_pages";
    }

    @Override
    public boolean supports(CaptureBatchContext context) {
        return context.capture()
            .screen() instanceof ModularGui;
    }

    @Override
    public List<CaptureTargetDescriptor> enumerate(CaptureBatchContext context, CaptureBatchOptions options) {
        if (!options.scopes()
            .contains(CaptureBatchOptions.PAGES)) return List.of();
        MuiCaptureTree tree = new MuiCaptureTree(
            (ModularGui) context.capture()
                .screen());
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        for (Map.Entry<String, Node> entry : tree.nodes()
            .entrySet()) {
            Node node = entry.getValue();
            if (node.widget() instanceof PageControlWidget paged) {
                for (int page = 0; page < paged.getChildren()
                    .size(); page++) targets.add(target(entry.getKey(), node, "page", page));
            } else
                if (MuiScrollViewport.supports(node.widget())) targets.add(target(entry.getKey(), node, "scroll", 0));
        }
        return List.copyOf(targets);
    }

    private CaptureTargetDescriptor target(String path, Node node, String kind, int page) {
        return new CaptureTargetDescriptor(
            "mui1:" + kind + ":" + path + "/" + page,
            id(),
            CaptureBatchOptions.PAGES,
            node.widget()
                .getClass()
                .getSimpleName() + " · "
                + (page + 1),
            "ready",
            "",
            Map.of("path", path, "kind", kind, "page", Integer.toString(page), "x", "0", "y", "0"));
    }

    @Override
    public CaptureBatchNavigator navigator(CaptureBatchContext context, CaptureBatchOptions options) {
        return new PageNavigator(
            (ModularGui) context.capture()
                .screen());
    }

    public static class PageNavigator implements CaptureBatchNavigator {

        private ModularGui screen;
        private MuiCaptureTree tree;
        private final ModularUIContext originalContext;
        private final CaptureGuiLocation location;
        private final Map<String, Integer> originalPages;
        private final Map<String, Offset> originalScrolls = new LinkedHashMap<>();
        private CaptureTargetDescriptor selected;
        private Node node;
        private int page;
        private int x;
        private int y;

        public PageNavigator(ModularGui screen) {
            this.screen = screen;
            originalContext = screen.getContext();
            location = location(screen);
            tree = new MuiCaptureTree(screen);
            originalPages = tree.pages();
            for (Map.Entry<String, Node> entry : tree.nodes()
                .entrySet()) {
                if (MuiScrollViewport.supports(
                    entry.getValue()
                        .widget()))
                    originalScrolls.put(
                        entry.getKey(),
                        new MuiScrollViewport(
                            entry.getValue()
                                .widget()).offset());
            }
        }

        private static CaptureGuiLocation location(ModularGui screen) {
            for (Object value : screen.inventorySlots.inventorySlots) {
                if (((Slot) value).inventory instanceof TileEntity tile) return new CaptureGuiLocation(
                    tile.getWorldObj().provider.dimensionId,
                    tile.xCoord,
                    tile.yCoord,
                    tile.zCoord);
            }
            return null;
        }

        @Override
        public void select(CaptureTargetDescriptor target) {
            selected = target;
            node = tree.nodes()
                .get(
                    target.data()
                        .get("path"));
            if (node == null) throw new IllegalStateException("The selected MUI widget disappeared");
            page = Integer.parseInt(
                target.data()
                    .get("page"));
            x = Integer.parseInt(
                target.data()
                    .get("x"));
            y = Integer.parseInt(
                target.data()
                    .get("y"));
            tree.activate(node, page);
            if (MuiScrollViewport.supports(node.widget())) new MuiScrollViewport(node.widget()).offset(x, y);
        }

        @Override
        public CaptureBatchReadiness readiness(CaptureBatchContext context) {
            ModularWindow window = node.widget()
                .getWindow();
            AccessorCaptureModularWindow animation = (AccessorCaptureModularWindow) window;
            boolean ready = context.capture()
                .screen() == screen && node.widget()
                    .isEnabled()
                && window.isEnabled()
                && window.getScale() == 1
                && animation.guidenh$getCaptureRotation() == 0
                && animation.guidenh$getCaptureTranslateX() == 0
                && animation.guidenh$getCaptureTranslateY() == 0;
            ready &= !viewport(context).isEmpty();
            for (PageStep ancestor : node.ancestors()) ready &= ancestor.widget()
                .getCurrentPage() == ancestor.page();
            if (node.widget() instanceof PageControlWidget paged) ready &= paged.getCurrentPage() == page;
            if (MuiScrollViewport.supports(node.widget())) {
                MuiScrollViewport scroll = new MuiScrollViewport(node.widget());
                ready &= scroll.width() > 0 && scroll.height() > 0
                    && scroll.offset()
                        .equals(new Offset(x, y));
            }
            return new CaptureBatchReadiness(
                ready,
                CaptureBatchReadiness.viewRevision(context.capture()),
                ready ? "" : "Waiting for MUI page layout and scrolling");
        }

        private CaptureRect viewport(CaptureBatchContext context) {
            String path = selected.data()
                .get("path");
            for (CaptureTarget target : context.geometry()
                .targets()) {
                if (target.id()
                    .equals(path)) return target.visibleBounds();
            }
            return new CaptureRect(0, 0, 0, 0);
        }

        @Override
        public List<CaptureRect> regions(CaptureBatchContext context) {
            return List.of(
                selected.data()
                    .containsKey("slice") ? viewport(context)
                        : context.capture()
                            .viewport()
                            .bounds());
        }

        @Override
        public CaptureScrollGeometry scrollGeometry(CaptureBatchContext context) {
            if (!MuiScrollViewport.supports(node.widget())) return null;
            MuiScrollViewport scroll = new MuiScrollViewport(node.widget());
            CaptureRect visible = viewport(context);
            CaptureRect bounds = MuiCaptureGeometryProvider.bounds(
                node.widget()
                    .getAbsolutePos(),
                node.widget()
                    .getSize(),
                MuiCaptureGeometryProvider.windowTransform(
                    node.widget()
                        .getWindow()));
            double scale = node.widget()
                .getWindow()
                .getScale();
            return new CaptureScrollGeometry(
                selected.data()
                    .get("path"),
                visible,
                x * scale + visible.x() - bounds.x(),
                y * scale + visible.y() - bounds.y(),
                scroll.actualWidth() * scale,
                scroll.actualHeight() * scale);
        }

        @Override
        public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
            CaptureBatchContext context) {
            if (!MuiScrollViewport.supports(node.widget()) || target.data()
                .containsKey("slice")) return List.of();
            MuiScrollViewport scroll = new MuiScrollViewport(node.widget());
            List<CaptureTargetDescriptor> targets = new ArrayList<>();
            for (int top : offsets(scroll.actualHeight() - scroll.height(), scroll.height())) {
                for (int left : offsets(scroll.actualWidth() - scroll.width(), scroll.width())) {
                    if (left == 0 && top == 0) continue;
                    Map<String, String> data = new LinkedHashMap<>(target.data());
                    data.put("x", Integer.toString(left));
                    data.put("y", Integer.toString(top));
                    data.put("slice", "true");
                    targets.add(
                        new CaptureTargetDescriptor(
                            target.id() + "/scroll:" + left + ":" + top,
                            target.providerId(),
                            target.scope(),
                            target.label() + " [" + left + ", " + top + "]",
                            "ready",
                            "",
                            data));
                }
            }
            return List.copyOf(targets);
        }

        private static List<Integer> offsets(int maximum, int step) {
            List<Integer> positions = new ArrayList<>();
            positions.add(0);
            for (int offset = step; offset < maximum; offset += step) positions.add(offset);
            if (maximum > 0) positions.add(maximum);
            return positions;
        }

        @Override
        public void restore() {
            if (!(Minecraft.getMinecraft().currentScreen instanceof ModularGui current))
                throw new IllegalStateException("The original MUI GUI could not be reopened");
            if (current.getContext() != originalContext && (location == null || !location.equals(location(current))))
                throw new IllegalStateException("The reopened MUI GUI belongs to a different target");
            screen = current;
            tree = new MuiCaptureTree(current);
            tree.restorePages(originalPages);
            for (Map.Entry<String, Offset> entry : originalScrolls.entrySet()) {
                Node original = tree.nodes()
                    .get(entry.getKey());
                if (original == null || !MuiScrollViewport.supports(original.widget()))
                    throw new IllegalStateException("The original MUI scroll view disappeared");
                new MuiScrollViewport(original.widget()).offset(
                    entry.getValue()
                        .x(),
                    entry.getValue()
                        .y());
            }
        }

        @Override
        public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
            boolean ready = context.capture()
                .screen() == screen && tree.pages()
                    .equals(originalPages);
            for (Map.Entry<String, Offset> entry : originalScrolls.entrySet()) {
                Node original = tree.nodes()
                    .get(entry.getKey());
                ready &= original != null && new MuiScrollViewport(original.widget()).offset()
                    .equals(entry.getValue());
            }
            return new CaptureBatchReadiness(
                ready,
                CaptureBatchReadiness.viewRevision(context.capture()),
                ready ? "" : "Waiting for the original MUI pages and scroll positions");
        }
    }
}
