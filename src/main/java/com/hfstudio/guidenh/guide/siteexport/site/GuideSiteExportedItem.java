package com.hfstudio.guidenh.guide.siteexport.site;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.fluid.FluidStackSpec;

public class GuideSiteExportedItem {

    private final String itemId;
    private final String displayName;
    private final String iconSrc;
    private final int count;
    private final @Nullable FluidStackSpec fluid;

    public GuideSiteExportedItem(String itemId, String displayName, String iconSrc) {
        this(itemId, displayName, iconSrc, null);
    }

    public GuideSiteExportedItem(String itemId, String displayName, String iconSrc, @Nullable FluidStackSpec fluid) {
        this(itemId, displayName, iconSrc, 1, fluid);
    }

    public GuideSiteExportedItem(String itemId, String displayName, String iconSrc, int count,
        @Nullable FluidStackSpec fluid) {
        this.itemId = itemId != null ? itemId : "";
        this.displayName = displayName != null ? displayName : "";
        this.iconSrc = iconSrc != null ? iconSrc : "";
        this.count = count;
        this.fluid = fluid;
    }

    public @Nullable FluidStackSpec fluid() {
        return fluid;
    }

    public String itemId() {
        return itemId;
    }

    public String displayName() {
        return displayName;
    }

    public String iconSrc() {
        return iconSrc;
    }

    public int count() {
        return count;
    }

    public boolean hasIcon() {
        return !iconSrc.isEmpty();
    }

    public boolean isEmpty() {
        return itemId.isEmpty() && displayName.isEmpty() && iconSrc.isEmpty();
    }
}
