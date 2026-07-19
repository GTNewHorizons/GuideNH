package com.hfstudio.guidenh.guide.internal.debug;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;

/**
 * Interface for nodes that contain sub-components (UI controls, interactive elements)
 * which should be debuggable but are not necessarily part of the LytNode tree.
 * <p>
 * Implementations expose their internal components in a structured, extensible way.
 * <p>
 * Examples:
 * - GameScene: timeline slider, play/pause button, reset button, layer sliders
 * - ContentTabs: individual tab buttons with bounds and state
 * - Mermaid: individual mindmap nodes
 * - Chart: bars, columns, slices with data
 */
public interface DebugComponent {

    /**
     * Returns all debuggable sub-components within this node.
     * Called during hover detection to provide fine-grained element picking.
     */
    List<ComponentEntry> getDebugComponents();

    /**
     * Represents a debuggable sub-component with bounds and metadata.
     */
    interface ComponentEntry {

        /**
         * Display name for this component (e.g., "Play Button", "Tab: Overview", "Bar: Steel Production")
         */
        String getName();

        /**
         * Bounds of this component for hover detection
         */
        LytRect getBounds();

        /**
         * Optional extra debug information to display
         */
        @Nullable
        default String getExtraInfo() {
            return null;
        }

        /**
         * Optional priority for overlap resolution (higher = prefer this component)
         */
        default int getPriority() {
            return 0;
        }

        /**
         * Optional custom hit-testing. If not overridden, uses getBounds().contains(x, y).
         * Use this for non-rectangular shapes like pie chart slices.
         */
        default boolean containsPoint(int x, int y) {
            return getBounds().contains(x, y);
        }
    }

    /**
     * Simple implementation of ComponentEntry
     */
    record SimpleComponentEntry(String name, LytRect bounds, @Nullable String extraInfo, int priority)
        implements ComponentEntry {

        public SimpleComponentEntry(String name, LytRect bounds) {
            this(name, bounds, null, 0);
        }

        public SimpleComponentEntry(String name, LytRect bounds, String extraInfo) {
            this(name, bounds, extraInfo, 0);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public LytRect getBounds() {
            return bounds;
        }

        @Override
        public String getExtraInfo() {
            return extraInfo;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    record LineComponentEntry(String name, int x1, int y1, int x2, int y2, int tolerance, @Nullable String extraInfo,
        int priority) implements ComponentEntry {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public @Nullable String getExtraInfo() {
            return extraInfo;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public LytRect getBounds() {
            int padding = Math.max(1, tolerance);
            return new LytRect(
                Math.min(x1, x2) - padding,
                Math.min(y1, y2) - padding,
                Math.abs(x2 - x1) + padding * 2 + 1,
                Math.abs(y2 - y1) + padding * 2 + 1);
        }

        @Override
        public boolean containsPoint(int x, int y) {
            if (!getBounds().contains(x, y)) {
                return false;
            }
            float dx = x2 - x1;
            float dy = y2 - y1;
            float lengthSquared = dx * dx + dy * dy;
            if (lengthSquared == 0) {
                return Math.abs(x - x1) <= tolerance && Math.abs(y - y1) <= tolerance;
            }
            float progress = ((x - x1) * dx + (y - y1) * dy) / lengthSquared;
            progress = Math.clamp(progress, 0F, 1F);
            float nearestX = x1 + progress * dx;
            float nearestY = y1 + progress * dy;
            float offsetX = x - nearestX;
            float offsetY = y - nearestY;
            return offsetX * offsetX + offsetY * offsetY <= tolerance * tolerance;
        }
    }
}
