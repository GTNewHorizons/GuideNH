package com.hfstudio.guidenh.guide.internal.debug;

/**
 * Viewport transform currently displayed during scrolling, panning, or zoom animation. Debug outlines and
 * hit testing use these visual values rather than animation targets.
 */
public interface InterpolatedViewport {

    /**
     * Rendered horizontal offset in pixels. Positive values move content left.
     */
    default float getVisualScrollOffsetX() {
        return 0f;
    }

    /**
     * Rendered vertical offset in pixels. Positive values move content up.
     */
    default float getVisualScrollOffsetY() {
        return 0f;
    }

    /**
     * Rendered zoom scale. Defaults to 1 for viewports without zoom.
     */
    default float getVisualZoom() {
        return 1f;
    }
}
