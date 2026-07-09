package com.hfstudio.guidenh.guide.internal.item;

import lombok.Getter;

public enum RegionWandExportMode {

    SNBT("snbt", false, "snbt"),
    SNBT_ENTITIES("snbt_e", true, "snbt+entities"),
    BLOCKS("blocks", false, "blocks"),
    BLOCKS_ENTITIES("blocks_e", true, "blocks+entities");

    @Getter
    private final String cliValue;
    private final boolean includeEntities;
    @Getter
    private final String displayName;

    RegionWandExportMode(String cliValue, boolean includeEntities, String displayName) {
        this.cliValue = cliValue;
        this.includeEntities = includeEntities;
        this.displayName = displayName;
    }

    public boolean includeEntities() {
        return includeEntities;
    }

    public RegionWandExportMode next() {
        RegionWandExportMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static RegionWandExportMode fromCliValue(String value) {
        if (value == null || value.isEmpty()) {
            return SNBT;
        }
        for (RegionWandExportMode mode : values()) {
            if (mode.cliValue.equals(value)) {
                return mode;
            }
        }
        return SNBT;
    }
}
