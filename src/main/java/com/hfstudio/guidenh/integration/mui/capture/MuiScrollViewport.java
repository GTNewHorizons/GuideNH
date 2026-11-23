package com.hfstudio.guidenh.integration.mui.capture;

import com.gtnewhorizons.modularui.api.widget.Widget;
import com.gtnewhorizons.modularui.api.widget.scroll.IHorizontalScrollable;
import com.gtnewhorizons.modularui.api.widget.scroll.IVerticalScrollable;
import com.gtnewhorizons.modularui.common.widget.ListWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;

public record MuiScrollViewport(Widget widget) {

    public record Offset(int x, int y) {}

    public static boolean supports(Widget widget) {
        return widget instanceof Scrollable || widget instanceof ListWidget;
    }

    public Offset offset() {
        return new Offset(
            widget instanceof IHorizontalScrollable horizontal ? horizontal.getHorizontalScrollOffset() : 0,
            widget instanceof IVerticalScrollable vertical ? vertical.getVerticalScrollOffset() : 0);
    }

    public void offset(int x, int y) {
        if (widget instanceof IHorizontalScrollable horizontal)
            horizontal.setHorizontalScrollOffset(Math.max(0, Math.min(x, actualWidth() - width())));
        if (widget instanceof IVerticalScrollable vertical)
            vertical.setVerticalScrollOffset(Math.max(0, Math.min(y, actualHeight() - height())));
    }

    public int width() {
        return widget instanceof IHorizontalScrollable horizontal ? horizontal.getVisibleWidth()
            : widget.getSize().width;
    }

    public int height() {
        return widget instanceof IVerticalScrollable vertical ? vertical.getVisibleHeight() : widget.getSize().height;
    }

    public int actualWidth() {
        return widget instanceof IHorizontalScrollable horizontal ? Math.max(width(), horizontal.getActualWidth())
            : width();
    }

    public int actualHeight() {
        return widget instanceof IVerticalScrollable vertical ? Math.max(height(), vertical.getActualHeight())
            : height();
    }
}
