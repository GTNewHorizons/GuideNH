package com.hfstudio.guidenh.guide.internal.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.document.LytRect;

/**
 * Collects visible screen-space components for the current debug frame without driving layout or input.
 */
public class ScreenDebugComponents implements DebugComponent {

    public static final int CONTENT_LAYER = 10;
    public static final int CONTENT_POPUP_LAYER = 50;
    public static final int CHROME_LAYER = 100;
    public static final int NAVIGATION_LAYER = 200;
    public static final int POPUP_LAYER = 300;
    public static final int TOOLTIP_LAYER = 400;

    private final List<ComponentEntry> entries = new ArrayList<>();
    private final List<ComponentEntry> view = Collections.unmodifiableList(entries);
    private boolean enabled;

    /**
     * Discards the previous frame, including when debugging has just been disabled.
     */
    public void beginFrame() {
        entries.clear();
        enabled = ModConfig.debug.guiDebugMode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Nullable
    public ComponentEntry getRoot() {
        return entries.isEmpty() ? null : entries.getFirst();
    }

    @Nullable
    public ComponentEntry add(String name, LytRect bounds, @Nullable String extraInfo, int priority,
        @Nullable ComponentEntry parent) {
        if (!enabled || bounds.width() <= 0 || bounds.height() <= 0) {
            return null;
        }
        ComponentEntry entry = new Entry(name, bounds, extraInfo, priority, parent);
        entries.add(entry);
        return entry;
    }

    @Nullable
    public ComponentEntry addClipped(String name, LytRect bounds, LytRect clip, @Nullable String extraInfo,
        int priority, @Nullable ComponentEntry parent) {
        if (!enabled) {
            return null;
        }
        int left = Math.max(bounds.x(), clip.x());
        int top = Math.max(bounds.y(), clip.y());
        int right = Math.min(bounds.right(), clip.right());
        int bottom = Math.min(bounds.bottom(), clip.bottom());
        return add(name, new LytRect(left, top, right - left, bottom - top), extraInfo, priority, parent);
    }

    public void include(DebugComponent component) {
        if (enabled) {
            entries.addAll(component.getDebugComponents());
        }
    }

    @Override
    public List<ComponentEntry> getDebugComponents() {
        return view;
    }

    public record Entry(String name, LytRect bounds, @Nullable String extraInfo, int priority,
        @Nullable ComponentEntry parent) implements ComponentEntry {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public LytRect getBounds() {
            return bounds;
        }

        @Override
        @Nullable
        public String getExtraInfo() {
            return extraInfo;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        @Nullable
        public ComponentEntry getParent() {
            return parent;
        }
    }
}
