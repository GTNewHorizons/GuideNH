package com.hfstudio.guidenh.integration.mui2.capture;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.AbstractScrollWidget;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;
import com.cleanroommc.modularui.widget.scroll.ScrollData;
import com.hfstudio.guidenh.client.capture.api.CaptureRect;
import com.hfstudio.guidenh.client.capture.api.CaptureScrollGeometry;
import com.hfstudio.guidenh.client.capture.api.CaptureTarget;

public record Mui2ScrollViewport(AbstractScrollWidget<?, ?> widget) {

    public record Offset(int x, int y) {}

    public record State(Offset offset, boolean shadows) {}

    public static boolean supports(IWidget widget) {
        return widget instanceof AbstractScrollWidget<?, ?>;
    }

    public static Mui2ScrollViewport of(IWidget widget) {
        if (!(widget instanceof AbstractScrollWidget<?, ?>scroll))
            throw new IllegalArgumentException("This MUI2 widget does not expose scroll data");
        return new Mui2ScrollViewport(scroll);
    }

    public Offset offset() {
        return new Offset(widget.getScrollX(), widget.getScrollY());
    }

    public State state() {
        return new State(offset(), widget.isShowScrollShadows());
    }

    public void shadows(boolean enabled) {
        widget.showScrollShadows(enabled);
    }

    public void offset(int x, int y) {
        ScrollArea area = widget.getScrollArea();
        offset(area, area.getScrollX(), x);
        offset(area, area.getScrollY(), y);
    }

    private static void offset(ScrollArea area, ScrollData data, int value) {
        if (data == null) return;
        int target = Math.clamp(value, 0, maximum(area, data));
        if (data.isAnimating()) {
            if (data.getAnimatingTo() != target) data.animateTo(area, target);
        } else data.scrollTo(area, target);
    }

    public boolean settled() {
        ScrollArea area = widget.getScrollArea();
        return (area.getScrollX() == null || !area.getScrollX()
            .isAnimating()) && (area.getScrollY() == null
                || !area.getScrollY()
                    .isAnimating())
            && (!area.isScrollBarXActive() || area.getScrollX()
                .getThickness() > 0)
            && (!area.isScrollBarYActive() || area.getScrollY()
                .getThickness() > 0);
    }

    private static int maximum(ScrollArea area, ScrollData data) {
        return data == null ? 0 : Math.max(0, data.getScrollSize() - data.getFullVisibleSize(area));
    }

    public int maximumX() {
        return maximum(
            widget.getScrollArea(),
            widget.getScrollArea()
                .getScrollX());
    }

    public int maximumY() {
        return maximum(
            widget.getScrollArea(),
            widget.getScrollArea()
                .getScrollY());
    }

    public int width() {
        ScrollArea area = widget.getScrollArea();
        return Math.max(
            0,
            area.w() - (area.isScrollBarYActive() ? area.getScrollY()
                .getThickness() : 0));
    }

    public int height() {
        ScrollArea area = widget.getScrollArea();
        return Math.max(
            0,
            area.h() - (area.isScrollBarXActive() ? area.getScrollX()
                .getThickness() : 0));
    }

    public int contentWidth() {
        ScrollData data = widget.getScrollArea()
            .getScrollX();
        return Math.max(width(), data == null ? 0 : data.getScrollSize());
    }

    public int contentHeight() {
        ScrollData data = widget.getScrollArea()
            .getScrollY();
        return Math.max(height(), data == null ? 0 : data.getScrollSize());
    }

    public CaptureRect contentBounds(CaptureRect bounds) {
        ScrollArea area = widget.getScrollArea();
        if (area.w() <= 0 || area.h() <= 0) return new CaptureRect(0, 0, 0, 0);
        ScrollData vertical = area.getScrollY();
        ScrollData horizontal = area.getScrollX();
        int left = area.isScrollBarYActive() && vertical.isOnAxisStart() ? vertical.getThickness() : 0;
        int top = area.isScrollBarXActive() && horizontal.isOnAxisStart() ? horizontal.getThickness() : 0;
        double scaleX = bounds.width() / area.w();
        double scaleY = bounds.height() / area.h();
        return new CaptureRect(
            bounds.x() + left * scaleX,
            bounds.y() + top * scaleY,
            width() * scaleX,
            height() * scaleY);
    }

    public CaptureRect viewport(CaptureTarget target) {
        return contentBounds(target.bounds()).intersect(target.visibleBounds());
    }

    public CaptureScrollGeometry geometry(String group, CaptureTarget target) {
        ScrollArea area = widget.getScrollArea();
        CaptureRect bounds = contentBounds(target.bounds());
        CaptureRect visible = bounds.intersect(target.visibleBounds());
        double scaleX = target.bounds()
            .width() / area.w();
        double scaleY = target.bounds()
            .height() / area.h();
        return new CaptureScrollGeometry(
            group,
            visible,
            Math.max(0, widget.getScrollX() * scaleX + visible.x() - bounds.x()),
            Math.max(0, widget.getScrollY() * scaleY + visible.y() - bounds.y()),
            contentWidth() * scaleX,
            contentHeight() * scaleY);
    }
}
