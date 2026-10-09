package com.hfstudio.guidenh.integration.structurelib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

public class StructureLibSceneOptions {

    public static final String GREGTECH_ACTIVE_CONTROLLER_OPTION = "gregtech.active_controller";
    public static final String GREGTECH_PLACE_HATCHES_OPTION = "gregtech.place_hatches";

    @Nullable
    private final String facing;
    @Nullable
    private final String rotation;
    @Nullable
    private final String flip;
    @Nullable
    private final Integer tier;
    @Getter
    private final Map<String, Integer> channelOverrides;
    @Getter
    private final boolean tierLocked;
    @Getter
    private final Set<String> lockedChannels;
    @Getter
    private final boolean gregTechActiveController;
    @Getter
    private final boolean gregTechPlaceHatches;

    public StructureLibSceneOptions(@Nullable String facing, @Nullable String rotation, @Nullable String flip,
        @Nullable Integer tier, @Nullable Map<String, Integer> channelOverrides, boolean gregTechActiveController,
        boolean gregTechPlaceHatches) {
        this(
            facing,
            rotation,
            flip,
            tier,
            channelOverrides,
            false,
            Collections.emptySet(),
            gregTechActiveController,
            gregTechPlaceHatches);
    }

    public StructureLibSceneOptions(@Nullable String facing, @Nullable String rotation, @Nullable String flip,
        @Nullable Integer tier, @Nullable Map<String, Integer> channelOverrides, boolean tierLocked,
        @Nullable Set<String> lockedChannels, boolean gregTechActiveController, boolean gregTechPlaceHatches) {
        this.facing = normalizeOptional(facing);
        this.rotation = normalizeOptional(rotation);
        this.flip = normalizeOptional(flip);
        this.tier = tier != null && tier > 0 ? tier : null;
        this.channelOverrides = StructureLibPreviewSelection.immutableChannelOverrides(channelOverrides);
        this.tierLocked = tier != null && tier > 0 && tierLocked;
        this.lockedChannels = immutableChannelIds(lockedChannels);
        this.gregTechActiveController = gregTechActiveController;
        this.gregTechPlaceHatches = gregTechPlaceHatches;
    }

    public static StructureLibSceneOptions empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Nullable
    public String getFacing() {
        return facing;
    }

    @Nullable
    public String getRotation() {
        return rotation;
    }

    @Nullable
    public String getFlip() {
        return flip;
    }

    @Nullable
    public Integer getTier() {
        return tier;
    }

    public boolean hasOverrides() {
        return facing != null || rotation != null
            || flip != null
            || tier != null
            || !channelOverrides.isEmpty()
            || tierLocked
            || !lockedChannels.isEmpty()
            || gregTechActiveController
            || gregTechPlaceHatches;
    }

    public StructureLibPreviewSelection createSelection(@Nullable Integer legacyChannel) {
        int masterTier = tier != null ? tier
            : legacyChannel != null && legacyChannel > 0 ? legacyChannel
                : StructureLibPreviewSelection.DEFAULT_MASTER_TIER;
        StructureLibPreviewSelection selection = new StructureLibPreviewSelection(masterTier, channelOverrides);
        selection = selection.withIntegrationOption(GREGTECH_ACTIVE_CONTROLLER_OPTION, gregTechActiveController);
        selection = selection.withIntegrationOption(GREGTECH_PLACE_HATCHES_OPTION, gregTechPlaceHatches);
        return selection;
    }

    public StructureLibSceneOptions merge(StructureLibSceneOptions overrides) {
        if (overrides == null || !overrides.hasOverrides()) {
            return this;
        }
        LinkedHashMap<String, Integer> channels = new LinkedHashMap<>(channelOverrides);
        channels.putAll(overrides.channelOverrides);
        return new StructureLibSceneOptions(
            overrides.facing != null ? overrides.facing : facing,
            overrides.rotation != null ? overrides.rotation : rotation,
            overrides.flip != null ? overrides.flip : flip,
            overrides.tier != null ? overrides.tier : tier,
            channels,
            overrides.tier != null ? overrides.tierLocked : tierLocked,
            mergeLockedChannels(overrides.lockedChannels),
            overrides.gregTechActiveController || gregTechActiveController,
            overrides.gregTechPlaceHatches || gregTechPlaceHatches);
    }

    public static String resolveFacing(@Nullable String attributeFacing, StructureLibSceneOptions options) {
        String normalized = normalizeOptional(attributeFacing);
        return normalized != null ? normalized : options != null ? options.getFacing() : null;
    }

    public static String resolveRotation(@Nullable String attributeRotation, StructureLibSceneOptions options) {
        String normalized = normalizeOptional(attributeRotation);
        return normalized != null ? normalized : options != null ? options.getRotation() : null;
    }

    public static String resolveFlip(@Nullable String attributeFlip, StructureLibSceneOptions options) {
        String normalized = normalizeOptional(attributeFlip);
        return normalized != null ? normalized : options != null ? options.getFlip() : null;
    }

    @Nullable
    public static String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Set<String> immutableChannelIds(@Nullable Set<String> source) {
        if (source == null || source.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : source) {
            String channel = StructureLibPreviewSelection.normalizeChannelId(value);
            if (channel != null) {
                normalized.add(channel);
            }
        }
        return normalized.isEmpty() ? Set.of() : Set.copyOf(normalized);
    }

    private Set<String> mergeLockedChannels(Set<String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return lockedChannels;
        }
        LinkedHashSet<String> merged = new LinkedHashSet<>(lockedChannels);
        merged.addAll(overrides);
        return merged;
    }

    public static class Builder {

        @Nullable
        private String facing;
        @Nullable
        private String rotation;
        @Nullable
        private String flip;
        @Nullable
        private Integer tier;
        private final Map<String, Integer> channels = new LinkedHashMap<>();
        private boolean tierLocked;
        private final Set<String> lockedChannels = new LinkedHashSet<>();
        private boolean gregTechActiveController;
        private boolean gregTechPlaceHatches;

        public Builder facing(@Nullable String facing) {
            this.facing = normalizeOptional(facing);
            return this;
        }

        public Builder rotation(@Nullable String rotation) {
            this.rotation = normalizeOptional(rotation);
            return this;
        }

        public Builder flip(@Nullable String flip) {
            this.flip = normalizeOptional(flip);
            return this;
        }

        public Builder tier(@Nullable Integer tier) {
            this.tier = tier != null && tier > 0 ? tier : null;
            return this;
        }

        public Builder tierLocked(boolean tierLocked) {
            this.tierLocked = tierLocked;
            return this;
        }

        public Builder channel(String name, int value) {
            String normalized = StructureLibPreviewSelection.normalizeChannelId(name);
            if (normalized != null && value > 0) {
                channels.put(normalized, value);
            }
            return this;
        }

        public Builder channelLocked(String name, boolean locked) {
            String normalized = StructureLibPreviewSelection.normalizeChannelId(name);
            if (normalized != null) {
                if (locked) {
                    lockedChannels.add(normalized);
                } else {
                    lockedChannels.remove(normalized);
                }
            }
            return this;
        }

        public Builder channels(@Nullable Map<String, Integer> channels) {
            if (channels != null) {
                for (Map.Entry<String, Integer> entry : channels.entrySet()) {
                    Integer value = entry.getValue();
                    if (value != null) {
                        channel(entry.getKey(), value);
                    }
                }
            }
            return this;
        }

        public Builder gregTechActiveController(boolean gregTechActiveController) {
            this.gregTechActiveController = gregTechActiveController;
            return this;
        }

        public Builder gregTechPlaceHatches(boolean gregTechPlaceHatches) {
            this.gregTechPlaceHatches = gregTechPlaceHatches;
            return this;
        }

        public StructureLibSceneOptions build() {
            return new StructureLibSceneOptions(
                facing,
                rotation,
                flip,
                tier,
                channels.isEmpty() ? Collections.emptyMap() : channels,
                tierLocked,
                lockedChannels,
                gregTechActiveController,
                gregTechPlaceHatches);
        }
    }
}
