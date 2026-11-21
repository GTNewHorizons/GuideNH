package com.hfstudio.guidenh.integration.api.binding;

public record BindingState(String id) {

    public static final BindingState VISIBLE = new BindingState("visible");
    public static final BindingState COMPLETED = new BindingState("completed");
    public static final BindingState LOCKED = new BindingState("locked");
    public static final BindingState HIDDEN = new BindingState("hidden");
    public static final BindingState MISSING = new BindingState("missing");
    public static final BindingState UNAVAILABLE = new BindingState("unavailable");
    public static final BindingState UNKNOWN = new BindingState("unknown");

    public BindingState {
        id = BindingKey.normalizeId(id);
    }
}
