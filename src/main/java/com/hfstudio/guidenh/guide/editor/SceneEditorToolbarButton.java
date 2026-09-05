package com.hfstudio.guidenh.guide.editor;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Describes a toolbar button contributed to the Scene Editor.
 *
 * <p>The button is identified by a unique id, displayed with an atlas {@link SceneEditorIcon}, and
 * sorted with its order value after built-in controls. Visibility and enabled state are evaluated when
 * the toolbar is laid out or interacted with. When {@code menuId} is non-null, clicking the button
 * opens that dropdown menu instead of requiring the action to perform the menu behavior itself.</p>
 *
 * <p>Registered actions run on the client thread and receive the current {@link SceneEditorActionContext}.
 * Suppliers may be evaluated repeatedly and should not mutate editor state.</p>
 */
public class SceneEditorToolbarButton {

    protected String id;
    protected Supplier<String> label;
    protected SceneEditorIcon icon;
    protected String menuId;
    protected int order;
    protected BooleanSupplier visible;
    protected BooleanSupplier enabled;
    protected Consumer<SceneEditorActionContext> action;

    protected SceneEditorToolbarButton() {}

    public SceneEditorToolbarButton(String id, Supplier<String> label, SceneEditorIcon icon,
        Consumer<SceneEditorActionContext> action) {
        this(id, label, icon, null, 0, () -> true, () -> true, action);
    }

    public SceneEditorToolbarButton(String id, Supplier<String> label, SceneEditorIcon icon, BooleanSupplier enabled,
        Consumer<SceneEditorActionContext> action) {
        this(id, label, icon, null, 0, () -> true, enabled, action);
    }

    public SceneEditorToolbarButton(String id, Supplier<String> label, SceneEditorIcon icon, int order,
        BooleanSupplier visible, BooleanSupplier enabled, Consumer<SceneEditorActionContext> action) {
        this(id, label, icon, null, order, visible, enabled, action);
    }

    public SceneEditorToolbarButton(String id, Supplier<String> label, SceneEditorIcon icon, String menuId, int order,
        BooleanSupplier visible, BooleanSupplier enabled, Consumer<SceneEditorActionContext> action) {
        this.id = requireId(id);
        this.label = Objects.requireNonNull(label, "label");
        this.icon = Objects.requireNonNull(icon, "icon");
        this.menuId = menuId;
        this.order = order;
        this.visible = Objects.requireNonNull(visible, "visible");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        this.action = Objects.requireNonNull(action, "action");
    }

    /** Returns this button's registry-unique identifier. */
    public String id() {
        return id;
    }

    /** Resolves and returns the current display label, falling back to the id when the supplier returns null. */
    public String label() {
        String value = label.get();
        return value == null ? id : value;
    }

    /** Returns the texture-atlas sprite rendered for this button. */
    public SceneEditorIcon icon() {
        return icon;
    }

    /** Returns the dropdown menu id opened by this button, or null for a direct action. */
    public String menuId() {
        return menuId;
    }

    /** Returns the relative ordering among contributed toolbar buttons. */
    public int order() {
        return order;
    }

    /** Returns whether this button should currently be included in the toolbar. */
    public boolean visible() {
        return visible.getAsBoolean();
    }

    /** Returns whether this button currently accepts clicks. */
    public boolean enabled() {
        return enabled.getAsBoolean();
    }

    /** Invokes the contributed action with the supplied editor context. */
    public void activate(SceneEditorActionContext context) {
        action.accept(context);
    }

    /** Dispatches a toolbar click to the contributed action. */
    public void triggerClick(SceneEditorActionContext context) {
        activate(context);
    }

    protected static String requireId(String id) {
        if (id == null || id.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Scene Editor registration id must not be blank");
        }
        return id;
    }
}
