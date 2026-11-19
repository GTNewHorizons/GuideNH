package com.hfstudio.guidenh.guide.internal.binding;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.integration.api.binding.BindingKey;

public record BindingSpec(BindingKey key, @Nullable String text, @Nullable String title, String target,
    boolean showTooltip, boolean showDescription, boolean showIcon, boolean showParents, float scale) {

    public BindingSpec {
        if (!"auto".equals(target) && !"native".equals(target) && !"guide".equals(target))
            throw new IllegalArgumentException("Binding target must be auto, native, or guide");
        if (!Float.isFinite(scale) || scale <= 0)
            throw new IllegalArgumentException("Binding scale must be finite and positive");
    }

    public static BindingSpec quest(BindingKey key, String text, boolean showTooltip) {
        return new BindingSpec(key, text, null, "native", showTooltip, false, false, false, 1);
    }
}
