package com.hfstudio.guidenh.guide.render;

public class GuiRenderVisibility implements AutoCloseable {

    private static final ThreadLocal<Boolean> DEBUG_VISIBLE = ThreadLocal.withInitial(() -> true);
    private static final ThreadLocal<Boolean> COLLECT_GEOMETRY = ThreadLocal.withInitial(() -> false);
    private final boolean previous;
    private final boolean previousGeometry;

    public GuiRenderVisibility(boolean debugVisible) {
        this(debugVisible, false);
    }

    public GuiRenderVisibility(boolean debugVisible, boolean collectGeometry) {
        previous = DEBUG_VISIBLE.get();
        DEBUG_VISIBLE.set(previous && debugVisible);
        previousGeometry = COLLECT_GEOMETRY.get();
        COLLECT_GEOMETRY.set(previousGeometry || collectGeometry);
    }

    public static boolean debugVisible() {
        return DEBUG_VISIBLE.get();
    }

    public static boolean geometryEnabled() {
        return COLLECT_GEOMETRY.get();
    }

    @Override
    public void close() {
        DEBUG_VISIBLE.set(previous);
        COLLECT_GEOMETRY.set(previousGeometry);
    }
}
