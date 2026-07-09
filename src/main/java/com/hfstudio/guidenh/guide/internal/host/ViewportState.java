package com.hfstudio.guidenh.guide.internal.host;

import com.hfstudio.guidenh.guide.document.LytRect;

import lombok.Getter;
import lombok.Setter;

public class ViewportState {

    private int scrollY;
    private int viewportWidth;
    private int viewportHeight;
    private int contentWidth;
    private int contentHeight;
    @Getter
    @Setter
    private boolean layoutDirty;

    public void updateViewport(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public void updateContent(int width, int height) {
        this.contentWidth = width;
        this.contentHeight = height;
    }

    public int scrollY() {
        return scrollY;
    }

    public void scrollTo(int y) {
        this.scrollY = clampScroll(y);
    }

    public void scrollBy(int delta) {
        scrollTo(scrollY + delta);
    }

    private int clampScroll(int y) {
        int max = getMaxScrollY();
        if (y < 0) return 0;
        return Math.min(y, max);
    }

    public void clampScroll() {
        scrollY = clampScroll(scrollY);
    }

    public int getMaxScrollY() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    public LytRect getRect() {
        return new LytRect(0, scrollY, viewportWidth, viewportHeight);
    }

    public int viewportWidth() {
        return viewportWidth;
    }

    public int viewportHeight() {
        return viewportHeight;
    }

    public int contentWidth() {
        return contentWidth;
    }

    public int contentHeight() {
        return contentHeight;
    }
}
