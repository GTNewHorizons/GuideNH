package com.hfstudio.guidenh.integration.structurelib;

import java.util.Map;

import javax.annotation.Nullable;

public record StructureLibBuildRequest(
    String controllerId,
    @Nullable String piece,
    @Nullable String facing,
    @Nullable String rotation,
    @Nullable String flip,
    int tier,
    Map<String, Integer> channels,
    Map<String, Boolean> options) {

    public StructureLibBuildRequest {
        channels = channels != null ? Map.copyOf(channels) : Map.of();
        options = options != null ? Map.copyOf(options) : Map.of();
    }
}
