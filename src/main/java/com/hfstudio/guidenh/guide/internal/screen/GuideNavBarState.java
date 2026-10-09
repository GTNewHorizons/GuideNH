package com.hfstudio.guidenh.guide.internal.screen;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import com.hfstudio.guidenh.guide.navigation.NavigationNode;

public class GuideNavBarState {

    private static final GuideNavBarState DEFAULT_STATE = createExpandedNodes(true, Set.of(), 0);

    private final boolean bookmarkGroupExpanded;
    private final Set<NavigationNode.Key> expandedNodeKeys;
    private final int scrollY;

    public GuideNavBarState(boolean bookmarkGroupExpanded, Set<ResourceLocation> expandedPageIds, int scrollY) {
        this(bookmarkGroupExpanded, fromPageIds(expandedPageIds), scrollY);
    }

    private GuideNavBarState(boolean bookmarkGroupExpanded, Collection<NavigationNode.Key> expandedNodeKeys,
        int scrollY) {
        this.bookmarkGroupExpanded = bookmarkGroupExpanded;
        this.expandedNodeKeys = Set
            .copyOf(expandedNodeKeys == null ? new LinkedHashSet<>() : new LinkedHashSet<>(expandedNodeKeys));
        this.scrollY = Math.max(0, scrollY);
    }

    public static GuideNavBarState create(boolean bookmarkGroupExpanded, Set<ResourceLocation> expandedPageIds,
        int scrollY) {
        return new GuideNavBarState(bookmarkGroupExpanded, expandedPageIds, scrollY);
    }

    public static GuideNavBarState createExpandedNodes(boolean bookmarkGroupExpanded,
        Set<NavigationNode.Key> expandedNodeKeys, int scrollY) {
        return new GuideNavBarState(bookmarkGroupExpanded, expandedNodeKeys, scrollY);
    }

    public static GuideNavBarState defaultState() {
        return DEFAULT_STATE;
    }

    public boolean bookmarkGroupExpanded() {
        return bookmarkGroupExpanded;
    }

    public Set<ResourceLocation> expandedPageIds() {
        Set<ResourceLocation> pageIds = new LinkedHashSet<>();
        for (NavigationNode.Key key : expandedNodeKeys) {
            pageIds.add(key.pageId());
        }
        return Set.copyOf(pageIds);
    }

    public Set<NavigationNode.Key> expandedNodeKeys() {
        return expandedNodeKeys;
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
        return bookmarkGroupExpanded == other.bookmarkGroupExpanded && expandedNodeKeys.equals(other.expandedNodeKeys)
            && scrollY == other.scrollY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookmarkGroupExpanded, expandedNodeKeys, scrollY);
    }

    private static Set<NavigationNode.Key> fromPageIds(Set<ResourceLocation> pageIds) {
        Set<NavigationNode.Key> keys = new LinkedHashSet<>();
        if (pageIds != null) {
            for (ResourceLocation pageId : pageIds) {
                if (pageId != null) {
                    keys.add(new NavigationNode.Key(null, pageId));
                }
            }
        }
        return keys;
    }
}
