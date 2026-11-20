package com.hfstudio.guidenh.integration.api.binding;

import org.jetbrains.annotations.Nullable;

public record BindingOpenResult(boolean opened, @Nullable String messageKey) {

    public static BindingOpenResult success() {
        return new BindingOpenResult(true, null);
    }

    public static BindingOpenResult failure(String messageKey) {
        return new BindingOpenResult(false, messageKey);
    }
}
