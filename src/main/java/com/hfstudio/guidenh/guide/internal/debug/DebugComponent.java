package com.hfstudio.guidenh.guide.internal.debug;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;

/**
 * Interface for nodes that contain sub-components (UI controls, interactive elements)
 * which should be debuggable but are not necessarily part of the LytNode tree.
 *
 * Implementations expose their internal components in a structured, extensible way.
 *
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
}
