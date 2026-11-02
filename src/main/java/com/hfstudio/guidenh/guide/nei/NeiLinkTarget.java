package com.hfstudio.guidenh.guide.nei;

import org.jetbrains.annotations.Nullable;

/** Immutable NEI navigation intent without native handlers or mutable item stacks. */
public record NeiLinkTarget(String action, @Nullable String itemId, @Nullable String handlerId) {

    public NeiLinkTarget {
        if (action == null) {
            throw new IllegalArgumentException("NEI action is required.");
        }
        itemId = trimToNull(itemId);
        handlerId = trimToNull(handlerId);
        switch (action) {
            case "handler" -> {
                if (handlerId == null || itemId != null) {
                    throw new IllegalArgumentException("NEI handler mode requires handlerId and does not accept id.");
                }
            }
            case "recipes", "usage" -> {
                if (itemId == null) {
                    throw new IllegalArgumentException("NEI " + action + " mode requires id.");
                }
            }
            default -> throw new IllegalArgumentException("Unknown NEI action: " + action);
        }
    }

    private static @Nullable String trimToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
