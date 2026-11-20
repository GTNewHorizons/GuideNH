package com.hfstudio.guidenh.integration.api.binding;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public record BindingViewContext(@Nullable UUID playerId, @Nullable String playerName, String language,
    boolean includeDescription, boolean authoring, boolean staticView) {}
