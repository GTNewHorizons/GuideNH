package com.hfstudio.guidenh.guide.editor;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Describes one item contributed to a registered Scene Editor dropdown menu.
 *
 * <p>
 * An item is identified by its id within the menu it is registered under. The registry evaluates
 * the visibility and enabled suppliers when it builds a menu, orders visible items by {@link #order()},
 * and invokes {@link #activate(SceneEditorActionContext)} for an accepted interaction. A non-null
 * checked supplier makes the item a checkable menu entry; a non-null {@link #widget()} replaces the
 * normal text-only interaction with an embedded control while retaining the item's registration and
 * enabled state.
 * </p>
 *
 * <p>
 * Suppliers and the action may be evaluated on the client thread more than once. Implementations
 * should therefore keep them side-effect free except for the action itself, and should use the action
 * context for all editor state changes.
 * </p>
 */
public class SceneEditorMenuItem {

    protected String id;
    protected Supplier<String> label;
    protected int order;
    protected BooleanSupplier visible;
    protected BooleanSupplier enabled;
    protected BooleanSupplier checked;
    protected boolean checkBox;
    protected SceneEditorMenuWidget widget;
    protected Consumer<SceneEditorActionContext> action;

    protected SceneEditorMenuItem() {}

    public SceneEditorMenuItem(String id, Supplier<String> label, Consumer<SceneEditorActionContext> action) {
        this(id, label, 0, () -> true, () -> true, null, null, action);
    }

    public SceneEditorMenuItem(String id, Supplier<String> label, int order, BooleanSupplier visible,
        BooleanSupplier enabled, BooleanSupplier checked, Consumer<SceneEditorActionContext> action) {
        this(id, label, order, visible, enabled, checked, null, action);
    }

    public SceneEditorMenuItem(String id, Supplier<String> label, int order, BooleanSupplier visible,
        BooleanSupplier enabled, BooleanSupplier checked, SceneEditorMenuWidget widget,
        Consumer<SceneEditorActionContext> action) {
        this.id = SceneEditorToolbarButton.requireId(id);
        this.label = Objects.requireNonNull(label, "label");
        this.order = order;
        this.visible = Objects.requireNonNull(visible, "visible");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        this.checked = checked;
        this.checkBox = checked != null;
        this.widget = widget;
        this.action = Objects.requireNonNull(action, "action");
    }

    public String id() {
        return id;
    }

    public String label() {
        String value = label.get();
        return value == null ? id : value;
    }

    public int order() {
        return order;
    }

    public boolean visible() {
        return visible.getAsBoolean();
    }

    public boolean enabled() {
        return enabled.getAsBoolean();
    }

    public boolean checked() {
        return checked != null && checked.getAsBoolean();
    }

    public boolean hasCheckBox() {
        return checkBox;
    }

    public SceneEditorMenuWidget widget() {
        return widget;
    }

    public void activate(SceneEditorActionContext context) {
        action.accept(context);
    }

    public void triggerClick(SceneEditorActionContext context) {
        activate(context);
    }

    public void triggerSelection(SceneEditorActionContext context) {
        activate(context);
    }
}
