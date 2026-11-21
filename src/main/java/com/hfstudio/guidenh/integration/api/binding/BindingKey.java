package com.hfstudio.guidenh.integration.api.binding;

import java.util.Locale;
import java.util.Objects;

import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;

public record BindingKey(String type, String id) {

    public BindingKey {
        type = normalizeType(type);
        id = normalizeId(id);
    }

    public static BindingKey parse(String type, String id) {
        String normalizedType = normalizeType(type);
        GuideBindingType definition = GuideNhIntegrationRegistry.global()
            .bindingType(normalizedType);
        return new BindingKey(normalizedType, definition == null ? normalizeId(id) : definition.normalizeId(id));
    }

    public static String normalizeId(String value) {
        String normalized = Objects.requireNonNull(value, "id")
            .trim();
        if (normalized.isEmpty() || normalized.chars()
            .anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                "Binding identifiers must be nonempty and contain no control characters");
        }
        return normalized;
    }

    public static String normalizeType(String type) {
        return normalizeId(type).toLowerCase(Locale.ROOT);
    }
}
