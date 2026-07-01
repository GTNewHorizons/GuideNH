package com.hfstudio.guidenh.guide.internal.debug;

/**
 * Interface for containers that have animated viewport transformations (scrolling, panning, zooming).
 *
 * <p>
 * This interface exposes the current interpolated (visual) transformation state, which is used
 * by rendering and interaction systems to ensure smooth animations. The visual state may differ
 * from the target state during animation transitions.
 *
 * <p>
 * Implementing this interface allows:
 * <ul>
 *   <li>Debug overlays to position element frames correctly during scrolling animations</li>
 *   <li>Hit-testing to account for animated viewport transformations</li>
 *   <li>Child elements to query their container's current visual state</li>
 * </ul>
 *
 * <p>
 * Common use cases:
 * <ul>
 *   <li>Scrollable containers (CodeBlock, DetailsBlock, SizeBox) - return vertical scroll offset</li>
 *   <li>Pannable canvases (MermaidCanvas) - return pan offset and zoom (handled separately)</li>
 *   <li>Multi-axis scrollable areas - return both horizontal and vertical offsets</li>
 * </ul>
 */
public interface InterpolatedViewport {

    /**
     * Returns the current visual (interpolated) horizontal offset of the viewport.
     * This is the offset currently being rendered, not the target scroll position.
     *
     * <p>
     * For scrollable containers, positive values indicate the viewport has scrolled right
     * (content appears to move left). For example, if the user scrolled right by 50 pixels
     * and the animation is halfway complete, this returns 25.
     *
     * @return horizontal viewport offset in pixels (positive = scrolled/panned right)
     */
    default float getVisualScrollOffsetX() {
        return 0f;
    }

    /**
     * Returns the current visual (interpolated) vertical offset of the viewport.
     * This is the offset currently being rendered, not the target scroll position.
     *
     * <p>
     * For scrollable containers, positive values indicate the viewport has scrolled down
     * (content appears to move up). For example, if the user scrolled down by 100 pixels
     * and the animation is 80% complete, this returns 80.
     *
     * @return vertical viewport offset in pixels (positive = scrolled/panned down)
     */
    default float getVisualScrollOffsetY() {
        return 0f;
    }

    /**
     * Returns the current visual (interpolated) zoom level of the viewport.
     * This is the zoom currently being rendered, not the target zoom level.
     *
     * <p>
     * A value of 1.0 means no zoom (100%), 2.0 means 200% zoom (content appears larger),
     * and 0.5 means 50% zoom (content appears smaller). This is used by zoomable containers
     * like interactive diagrams or maps.
     *
     * <p>
     * Most scrollable containers do not support zoom and should leave this as the default.
     *
     * @return zoom scale factor (1.0 = no zoom, >1.0 = zoomed in, <1.0 = zoomed out)
     */
    default float getVisualZoom() {
        return 1f;
    }
}
