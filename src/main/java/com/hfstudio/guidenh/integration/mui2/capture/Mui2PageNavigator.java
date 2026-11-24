package com.hfstudio.guidenh.integration.mui2.capture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;

import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.hfstudio.guidenh.capture.server.api.CaptureGuiLocation;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchContext;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchNavigator;
import com.hfstudio.guidenh.client.capture.api.CaptureBatchReadiness;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;
import com.hfstudio.guidenh.client.capture.api.CaptureTargetDescriptor;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2CaptureTree.Node;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2CaptureTree.PageStep;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2ScrollViewport.Offset;
import com.hfstudio.guidenh.integration.mui2.capture.Mui2ScrollViewport.State;

public class Mui2PageNavigator implements CaptureBatchNavigator {

    private static final int DEFAULT_EXPANSION_LIMIT = 500;
    private IMuiScreen screen;
    private Mui2CaptureTree tree;
    private final Map<String, Integer> originalPages;
    private final Map<String, Integer> latePages = new LinkedHashMap<>();
    private final Map<String, Integer> enumeratedPages = new LinkedHashMap<>();
    private final Map<String, State> originalScrolls = new LinkedHashMap<>();
    private final Set<String> initialScrolls = new HashSet<>();
    private final Set<String> enumeratedScrolls = new HashSet<>();
    private final CaptureGuiLocation location;
    private final int expansionLimit;
    private int expandedTargets;
    private CaptureTargetDescriptor selected;
    private Node node;
    private int page;
    private int x;
    private int y;
    private int clipX;
    private int clipY;

    public Mui2PageNavigator(IMuiScreen screen) {
        this(screen, DEFAULT_EXPANSION_LIMIT);
    }

    public Mui2PageNavigator(IMuiScreen screen, int expansionLimit) {
        if (expansionLimit < 1) throw new IllegalArgumentException("The MUI2 expansion limit must be positive");
        this.screen = screen;
        this.expansionLimit = expansionLimit;
        location = Minecraft.getMinecraft().thePlayer == null ? null
            : Mui2ServerContainerIdentity.location(Minecraft.getMinecraft().thePlayer.openContainer);
        tree = new Mui2CaptureTree(screen);
        originalPages = tree.pages();
        rememberState();
        initialScrolls.addAll(originalScrolls.keySet());
        for (Map.Entry<String, Node> entry : tree.nodes()
            .entrySet()) {
            if (entry.getValue()
                .widget() instanceof PagedWidget<?>paged)
                enumeratedPages.put(
                    entry.getKey(),
                    paged.getPages()
                        .size());
            else if (Mui2ScrollViewport.supports(
                entry.getValue()
                    .widget()))
                enumeratedScrolls.add(entry.getKey());
        }
    }

    private void rememberState() {
        for (Map.Entry<String, Node> entry : tree.nodes()
            .entrySet()) {
            if (entry.getValue()
                .widget() instanceof PagedWidget<?>paged && !originalPages.containsKey(entry.getKey()))
                latePages.putIfAbsent(entry.getKey(), paged.getCurrentPageIndex());
            if (Mui2ScrollViewport.supports(
                entry.getValue()
                    .widget()))
                originalScrolls.putIfAbsent(
                    entry.getKey(),
                    Mui2ScrollViewport.of(
                        entry.getValue()
                            .widget())
                        .state());
        }
    }

    @Override
    public void select(CaptureTargetDescriptor target) {
        selected = target;
        node = null;
        page = Integer.parseInt(
            target.data()
                .getOrDefault("page", "0"));
        x = Integer.parseInt(
            target.data()
                .getOrDefault("x", "0"));
        y = Integer.parseInt(
            target.data()
                .getOrDefault("y", "0"));
        clipX = Integer.parseInt(
            target.data()
                .getOrDefault("clip_x", "0"));
        clipY = Integer.parseInt(
            target.data()
                .getOrDefault("clip_y", "0"));
        tree.refresh();
        restoreScrollState(false, false);
        node = tree.activate(
            target.data()
                .get("path"),
            page);
        rememberState();
        restoreScrollState(false, false);
        if (target.data()
            .getOrDefault("kind", "page")
            .equals("page") && !(node.widget() instanceof PagedWidget<?>))
            throw new IllegalStateException("The selected MUI2 page changed type");
        applyScroll();
    }

    private boolean ancestor(IWidget widget) {
        if (node == null) return false;
        for (IWidget parent : node.lineage()) if (parent == widget) return true;
        return false;
    }

    private void restoreScrollState(boolean keepAncestors, boolean requireOriginal) {
        for (Map.Entry<String, State> entry : originalScrolls.entrySet()) {
            Node original = tree.nodes()
                .get(entry.getKey());
            if (original == null || !Mui2ScrollViewport.supports(original.widget())) {
                if (requireOriginal && initialScrolls.contains(entry.getKey()))
                    throw new IllegalStateException("The original MUI2 scroll view disappeared: " + entry.getKey());
                continue;
            }
            if (!requireOriginal && (!original.widget()
                .isValid()
                || !original.widget()
                    .isEnabled()
                || keepAncestors && ancestor(original.widget()))) continue;
            Mui2ScrollViewport scroll = Mui2ScrollViewport.of(original.widget());
            scroll.shadows(
                entry.getValue()
                    .shadows());
            scroll.offset(
                entry.getValue()
                    .offset()
                    .x(),
                entry.getValue()
                    .offset()
                    .y());
        }
    }

    private void applyScroll() {
        if (!Mui2ScrollViewport.supports(node.widget())) return;
        Mui2ScrollViewport scroll = Mui2ScrollViewport.of(node.widget());
        State original = originalScrolls.get(
            selected.data()
                .get("path"));
        scroll.shadows(
            !selected.data()
                .containsKey("slice") && original.shadows());
        scroll.offset(x, y);
    }

    @Override
    public void tick(CaptureBatchContext context) {
        if (selected == null || context.capture()
            .screen() != screen) return;
        if (node == null || !node.widget()
            .isValid()) {
            tree.refresh();
            node = tree.require(
                selected.data()
                    .get("path"));
            rememberState();
        }
        if (!node.widget()
            .isValid()
            || node.widget()
                .getArea()
                .w() <= 0
            || node.widget()
                .getArea()
                .h() <= 0)
            return;
        restoreScrollState(true, false);
        applyScroll();
        revealParents(context);
    }

    private void revealParents(CaptureBatchContext context) {
        if (!node.axisAligned(
            context.capture()
                .viewport()))
            return;
        for (IWidget parent : node.lineage()) {
            if (parent == node.widget() || !Mui2ScrollViewport.supports(parent)) continue;
            Node parentNode = tree.node(parent);
            if (parentNode == null || parent.getArea()
                .w() <= 0
                || parent.getArea()
                    .h() <= 0
                || !parentNode.axisAligned(
                    context.capture()
                        .viewport()))
                continue;
            Mui2ScrollViewport scroll = Mui2ScrollViewport.of(parent);
            CaptureRect parentBounds = parentNode.bounds(
                context.capture()
                    .viewport());
            CaptureRect viewport = scroll.contentBounds(parentBounds);
            CaptureRect childBounds = node.bounds(
                context.capture()
                    .viewport());
            if (Mui2ScrollViewport.supports(node.widget())) {
                Mui2ScrollViewport child = Mui2ScrollViewport.of(node.widget());
                CaptureRect content = child.contentBounds(childBounds);
                childBounds = new CaptureRect(
                    content.x() + clipX * childBounds.width()
                        / node.widget()
                            .getArea()
                            .w(),
                    content.y() + clipY * childBounds.height()
                        / node.widget()
                            .getArea()
                            .h(),
                    Math.min(content.width(), viewport.width()),
                    Math.min(content.height(), viewport.height()));
            }
            double scaleX = parentBounds.width() / parent.getArea()
                .w();
            double scaleY = parentBounds.height() / parent.getArea()
                .h();
            if (scaleX <= 0 || scaleY <= 0 || viewport.isEmpty()) continue;
            double dx = childBounds.x() < viewport.x() ? childBounds.x() - viewport.x()
                : Math.max(0, childBounds.right() - viewport.right());
            double dy = childBounds.y() < viewport.y() ? childBounds.y() - viewport.y()
                : Math.max(0, childBounds.bottom() - viewport.bottom());
            scroll.offset(
                (int) Math.round(
                    scroll.offset()
                        .x() + dx / scaleX),
                (int) Math.round(
                    scroll.offset()
                        .y() + dy / scaleY));
        }
    }

    private CaptureTarget capturedTarget(CaptureBatchContext context) {
        if (node == null) return null;
        long owner = Integer.toUnsignedLong(System.identityHashCode(node.widget()));
        for (CaptureTarget target : context.geometry()
            .targets()) if (target.ownerIdentity() == owner) return target;
        return null;
    }

    @Override
    public CaptureBatchReadiness readiness(CaptureBatchContext context) {
        CaptureTarget target = capturedTarget(context);
        boolean ready = context.capture()
            .screen() == screen && node != null
            && node.widget()
                .isValid()
            && node.widget()
                .isEnabled()
            && node.widget()
                .getPanel()
                .getAlpha() == 1
            && target != null;
        if (!ready) return new CaptureBatchReadiness(false, 0, "Waiting for MUI2 layout and visible capture geometry");
        for (PageStep ancestor : node.ancestors()) {
            Node parent = tree.nodes()
                .get(ancestor.path());
            ready &= parent != null && parent.widget() instanceof PagedWidget<?>paged
                && paged.getCurrentPageIndex() == ancestor.page();
        }
        if (node.widget() instanceof PagedWidget<?>paged) ready &= paged.getCurrentPageIndex() == page;
        ready &= node.axisAligned(
            context.capture()
                .viewport())
            && target.bounds()
                .equals(
                    node.bounds(
                        context.capture()
                            .viewport()));
        long revision = CaptureBatchReadiness.viewRevision(context.capture()) * 31 + target.visibleBounds()
            .hashCode();
        if (Mui2ScrollViewport.supports(node.widget())) {
            Mui2ScrollViewport scroll = Mui2ScrollViewport.of(node.widget());
            ready &= scroll.width() > 0 && scroll.height() > 0
                && !scroll.viewport(target)
                    .isEmpty()
                && scroll.settled()
                && scroll.offset()
                    .equals(new Offset(x, y));
            revision = (revision * 31 + scroll.contentWidth()) * 31 + scroll.contentHeight();
        }
        return new CaptureBatchReadiness(
            ready,
            revision,
            ready ? "" : "Waiting for MUI2 page, scroll position, and axis-aligned viewport");
    }

    @Override
    public List<CaptureRect> regions(CaptureBatchContext context) {
        if (!selected.data()
            .containsKey("slice"))
            return List.of(
                context.capture()
                    .viewport()
                    .bounds());
        CaptureTarget target = capturedTarget(context);
        return target == null ? List.of()
            : List.of(
                Mui2ScrollViewport.of(node.widget())
                    .viewport(target));
    }

    @Override
    public CaptureScrollGeometry scrollGeometry(CaptureBatchContext context) {
        if (!selected.data()
            .containsKey("slice") || !Mui2ScrollViewport.supports(node.widget())) return null;
        CaptureTarget target = capturedTarget(context);
        return target == null ? null
            : Mui2ScrollViewport.of(node.widget())
                .geometry(
                    selected.data()
                        .get("path"),
                    target);
    }

    @Override
    public List<CaptureTargetDescriptor> additionalTargets(CaptureTargetDescriptor target,
        CaptureBatchContext context) {
        if (target.data()
            .containsKey("slice")) return List.of();
        if (Mui2ScrollViewport.supports(node.widget())) return scrollTargets(target, context);
        tree.refresh();
        rememberState();
        String prefix = target.data()
            .get("path") + "/page:"
            + page
            + "/";
        List<CaptureTargetDescriptor> targets = new ArrayList<>();
        long omitted = 0;
        for (Map.Entry<String, Node> entry : tree.nodes()
            .entrySet()) {
            if (!entry.getKey()
                .startsWith(prefix)) continue;
            if (entry.getValue()
                .widget() instanceof PagedWidget<?>paged) {
                int known = enumeratedPages.getOrDefault(entry.getKey(), 0);
                int count = paged.getPages()
                    .size();
                for (int next = known; next < count; next++) {
                    if (expandedTargets >= expansionLimit) {
                        omitted += count - next;
                        break;
                    }
                    targets.add(Mui2CaptureTargets.page(entry.getKey(), paged, next));
                    expandedTargets++;
                }
                enumeratedPages.put(entry.getKey(), count);
            } else if (Mui2ScrollViewport.supports(
                entry.getValue()
                    .widget())
                && enumeratedScrolls.add(entry.getKey())) {
                    if (expandedTargets >= expansionLimit) omitted++;
                    else {
                        targets.add(
                            Mui2CaptureTargets.scroll(
                                entry.getKey(),
                                entry.getValue()
                                    .widget()));
                        expandedTargets++;
                    }
                }
        }
        if (omitted > 0) targets.add(omitted(target, omitted));
        return List.copyOf(targets);
    }

    private List<CaptureTargetDescriptor> scrollTargets(CaptureTargetDescriptor target, CaptureBatchContext context) {
        Mui2ScrollViewport scroll = Mui2ScrollViewport.of(node.widget());
        CaptureTarget captured = capturedTarget(context);
        CaptureRect visible = scroll.viewport(captured);
        int width = Math.max(
            1,
            (int) Math.floor(
                visible.width() * node.widget()
                    .getArea()
                    .w()
                    / captured.bounds()
                        .width()));
        int height = Math.max(
            1,
            (int) Math.floor(
                visible.height() * node.widget()
                    .getArea()
                    .h()
                    / captured.bounds()
                        .height()));
        int maximumX = Math.max(0, scroll.contentWidth() - width);
        int maximumY = Math.max(0, scroll.contentHeight() - height);
        long columns = positions(maximumX, width);
        long rows = positions(maximumY, height);
        long count = Math.multiplyExact(columns, rows);
        int included = (int) Math.min(count, expansionLimit - (long) expandedTargets);
        List<CaptureTargetDescriptor> targets = new ArrayList<>(included + 1);
        for (int index = 0; index < included; index++) {
            int left = position(index % columns, maximumX, width);
            int top = position(index / columns, maximumY, height);
            int horizontal = Math.min(left, scroll.maximumX());
            int vertical = Math.min(top, scroll.maximumY());
            Map<String, String> data = new LinkedHashMap<>(target.data());
            data.put("x", Integer.toString(horizontal));
            data.put("y", Integer.toString(vertical));
            data.put("clip_x", Integer.toString(left - horizontal));
            data.put("clip_y", Integer.toString(top - vertical));
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
        expandedTargets += included;
        if (count > included) targets.add(omitted(target, count - included));
        return List.copyOf(targets);
    }

    private static long positions(int maximum, int step) {
        return maximum <= 0 ? 1 : (maximum + (long) step - 1) / step + 1;
    }

    private static int position(long index, int maximum, int step) {
        return (int) Math.min(maximum, index * step);
    }

    private static CaptureTargetDescriptor omitted(CaptureTargetDescriptor parent, long count) {
        Map<String, String> data = new LinkedHashMap<>(parent.data());
        data.put("remaining_targets", Long.toString(count));
        return new CaptureTargetDescriptor(
            parent.id() + "/remaining",
            parent.providerId(),
            parent.scope(),
            parent.label() + " [" + count + " remaining]",
            "not_processed",
            count + " additional MUI2 capture positions exceed the configured expansion budget",
            data);
    }

    @Override
    public void restore() {
        if (location != null && (Minecraft.getMinecraft().thePlayer == null || !location
            .equals(Mui2ServerContainerIdentity.location(Minecraft.getMinecraft().thePlayer.openContainer))))
            throw new IllegalStateException("The reopened MUI2 GUI belongs to a different target");
        if (!(Minecraft.getMinecraft().currentScreen instanceof IMuiScreen current))
            throw new IllegalStateException("The original MUI2 GUI could not be reopened");
        screen = current;
        tree = new Mui2CaptureTree(current);
        tree.restorePages(originalPages);
        tree.restorePages(latePages, false);
        node = null;
        restoreScrollState(false, true);
    }

    @Override
    public CaptureBatchReadiness restorationReadiness(CaptureBatchContext context) {
        boolean ready = context.capture()
            .screen() == screen;
        for (Map.Entry<String, Integer> entry : originalPages.entrySet()) {
            Node original = tree.nodes()
                .get(entry.getKey());
            ready &= original != null && original.widget() instanceof PagedWidget<?>paged
                && paged.getCurrentPageIndex() == entry.getValue();
        }
        restoreScrollState(false, true);
        for (Map.Entry<String, State> entry : originalScrolls.entrySet()) {
            Node original = tree.nodes()
                .get(entry.getKey());
            if (original == null && !initialScrolls.contains(entry.getKey())) continue;
            if (original == null || !Mui2ScrollViewport.supports(original.widget())) {
                ready = false;
                continue;
            }
            Mui2ScrollViewport scroll = Mui2ScrollViewport.of(original.widget());
            ready &= scroll.settled() && scroll.offset()
                .equals(
                    entry.getValue()
                        .offset())
                && scroll.widget()
                    .isShowScrollShadows()
                    == entry.getValue()
                        .shadows();
        }
        return new CaptureBatchReadiness(
            ready,
            CaptureBatchReadiness.viewRevision(context.capture()),
            ready ? "" : "Waiting for the original MUI2 pages and scroll positions");
    }
}
