package com.hfstudio.guidenh.guide.editor;

/**
 * Client-side rendering and input contract for an embedded control in a Scene Editor menu item.
 *
 * <p>
 * The editor allocates one row for the widget and calls {@link #render(SceneEditorMenuWidgetContext, int, int,
 * int, int, boolean, boolean)} with the row's pixel bounds. The widget owns its visual state and decides whether
 * a mouse event belongs to the control. Returning {@code true} from {@link #mouseClicked(SceneEditorActionContext,
 * int, int, int, int, int, int, int)} or {@link #mouseDragged(SceneEditorActionContext, int, int, int, int, int,
 * int, int)} consumes that event and prevents the menu from handling it as a regular item.
 * </p>
 *
 * <p>
 * Override {@link #height()} and {@link #preferredWidth()} when the control needs more space than the default
 * row. The default {@link #triggerClick(SceneEditorActionContext, int, int, int, int, int, int, int)} and
 * {@link #triggerDrag(SceneEditorActionContext, int, int, int, int, int, int, int)} methods deliberately delegate
 * to the corresponding mouse methods so custom controls can expose the same behavior to programmatic menu actions.
 * </p>
 */
public interface SceneEditorMenuWidget {

    /** Returns the pixel height reserved for this widget's menu row. */
    default int height() {
        return 18;
    }

    /** Returns a preferred row width, or zero when the menu may choose the width. */
    default int preferredWidth() {
        return 0;
    }

    /** Draws the widget for the supplied row bounds and current hover/enabled state. */
    void render(SceneEditorMenuWidgetContext context, int x, int y, int width, int height, boolean hovered,
        boolean enabled);

    /** Handles a mouse click and returns whether the widget consumed it. */
    default boolean mouseClicked(SceneEditorActionContext actionContext, int x, int y, int width, int height,
        int mouseX, int mouseY, int button) {
        return false;
    }

    /** Handles a mouse drag and returns whether the widget consumed it. */
    default boolean mouseDragged(SceneEditorActionContext actionContext, int x, int y, int width, int height,
        int mouseX, int mouseY, int button) {
        return false;
    }

    /** Programmatically dispatches a click using the same path as a mouse click. */
    default boolean triggerClick(SceneEditorActionContext actionContext, int x, int y, int width, int height,
        int mouseX, int mouseY, int button) {
        return mouseClicked(actionContext, x, y, width, height, mouseX, mouseY, button);
    }

    /** Programmatically dispatches a drag using the same path as a mouse drag. */
    default boolean triggerDrag(SceneEditorActionContext actionContext, int x, int y, int width, int height, int mouseX,
        int mouseY, int button) {
        return mouseDragged(actionContext, x, y, width, height, mouseX, mouseY, button);
    }
}
