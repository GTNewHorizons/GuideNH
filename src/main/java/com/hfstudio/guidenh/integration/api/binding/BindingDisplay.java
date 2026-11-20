package com.hfstudio.guidenh.integration.api.binding;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public record BindingDisplay(BindingKey key, BindingState state, @Nullable String title, @Nullable String description,
    @Nullable BindingIcon icon, List<String> tooltip, List<BindingKey> prerequisites, boolean nativeOpenable) {

    public BindingDisplay {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        tooltip = List.copyOf(tooltip);
        prerequisites = List.copyOf(prerequisites);
        if (state.equals(BindingState.HIDDEN)) {
            title = null;
            description = null;
            icon = null;
            tooltip = List.of();
            prerequisites = List.of();
            nativeOpenable = false;
        }
    }
}
