package com.hfstudio.guidenh.guide.internal.screen;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

public class GuideNavBarState {

    private static final GuideNavBarState DEFAULT_STATE = new GuideNavBarState(true, Set.of(), 0);

    private final boolean bookmarkGroupExpanded;
    private final Set<ResourceLocation> expandedPageIds;
    private final int scrollY;

    public GuideNavBarState(boolean bookmarkGroupExpanded, Set<ResourceLocation> expandedPageIds, int scrollY) {
        this.bookmarkGroupExpanded = bookmarkGroupExpanded;
        this.expandedPageIds = Set.copyOf(
            expandedPageIds == null ? new LinkedHashSet<ResourceLocation>()
                : new LinkedHashSet<ResourceLocation>(expandedPageIds));
        this.scrollY = Math.max(0, scrollY);
    }

    public static GuideNavBarState create(boolean bookmarkGroupExpanded, Set<ResourceLocation> expandedPageIds,
        int scrollY) {
        return new GuideNavBarState(bookmarkGroupExpanded, expandedPageIds, scrollY);
    }

    public static GuideNavBarState defaultState() {
        return DEFAULT_STATE;
    }

    public boolean bookmarkGroupExpanded() {
        return bookmarkGroupExpanded;
    }

    public Set<ResourceLocation> expandedPageIds() {
        return expandedPageIds;
    }

    public int scrollY() {
        return scrollY;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GuideNavBarState other)) {
            return false;
        }
        return bookmarkGroupExpanded == other.bookmarkGroupExpanded && expandedPageIds.equals(other.expandedPageIds)
            && scrollY == other.scrollY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmarkGroupExpanded, expandedPageIds, scrollY);
    }
}
