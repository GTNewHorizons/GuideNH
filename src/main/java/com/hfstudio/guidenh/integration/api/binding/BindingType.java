package com.hfstudio.guidenh.integration.api.binding;

import java.util.Objects;
import java.util.function.UnaryOperator;

public record BindingType(String id, UnaryOperator<String> normalizer) implements GuideBindingType {

    public BindingType {
        id = BindingKey.normalizeType(id);
        Objects.requireNonNull(normalizer, "normalizer");
    }

    @Override
    public String normalizeId(String rawId) {
        return BindingKey.normalizeId(normalizer.apply(BindingKey.normalizeId(rawId)));
    }
}
