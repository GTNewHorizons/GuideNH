package com.hfstudio.guidenh.guide.editor;

/**
 * A control rendered within one editor menu row. Render and input methods receive the row bounds in GUI
 * pixels. Returning {@code true} from a mouse handler consumes the event.
 *
 * {@link #height()} and {@link #preferredWidth()} request additional space. Trigger methods delegate to
 * mouse handlers by default.
 */
public interface SceneEditorMenuWidget {

    /**
     * Returns the pixel height reserved for this widget's menu row.
     */
    default int height() {
        return 18;
    }

    /**
     * Returns a preferred row width, or zero when the menu may choose the width.
     */
    default int preferredWidth() {
        return 0;
    }

    /**
     * Draws the widget for the supplied row bounds and current hover/enabled state.
     */
    void render(SceneEditorMenuWidgetContext context, int x, int y, int width, int height, boolean hovered,
        boolean enabled);

    /**
     * Handles a mouse click and returns whether the widget consumed it.
     */
    default boolean mouseClicked(SceneEditorActionContext actionContext, int x, int y, int width, int height,
        int mouseX, int mouseY, int button) {
        return false;
    }

    /**
     * Handles a mouse drag and returns whether the widget consumed it.
     */
    default boolean mouseDragged(SceneEditorActionContext actionContext, int x, int y, int width, int height,
        int mouseX, int mouseY, int button) {
        return false;
    }

    /**
     * Programmatically dispatches a click using the same path as a mouse click.
     */
    default boolean triggerClick(SceneEditorActionContext actionContext, int x, int y, int width, int height,
        int mouseX, int mouseY, int button) {
        return mouseClicked(actionContext, x, y, width, height, mouseX, mouseY, button);
    }

    /**
     * Programmatically dispatches a drag using the same path as a mouse drag.
     */
    default boolean triggerDrag(SceneEditorActionContext actionContext, int x, int y, int width, int height, int mouseX,
        int mouseY, int button) {
        return mouseDragged(actionContext, x, y, width, height, mouseX, mouseY, button);
    }
}
