package com.hfstudio.guidenh.guide.render;

public class GuiRenderVisibility implements AutoCloseable {

    private static final ThreadLocal<Boolean> DEBUG_VISIBLE = ThreadLocal.withInitial(() -> true);
    private final boolean previous;

    public GuiRenderVisibility(boolean debugVisible) {
        previous = DEBUG_VISIBLE.get();
        DEBUG_VISIBLE.set(previous && debugVisible);
    }

    public static boolean debugVisible() {
        return DEBUG_VISIBLE.get();
    }

    @Override
    public void close() {
        DEBUG_VISIBLE.set(previous);
    }
}
