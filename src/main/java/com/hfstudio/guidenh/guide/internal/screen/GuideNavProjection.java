package com.hfstudio.guidenh.guide.internal.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.internal.GuideBookmarkState;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.navigation.NavigationNode;
import com.hfstudio.guidenh.guide.navigation.NavigationTree;

public class GuideNavProjection {

    public static class ProjectionResult {

        private final List<ProjectedRow> rows;

        public ProjectionResult(List<ProjectedRow> rows) {
            this.rows = rows;
        }

        public List<ProjectedRow> rows() {
            return rows;
        }
    }

    public static class ProjectedRow {

        private final DisplayRow displayRow;
        private final int rowIndex;
        private final int parentRowIndex;
        private int subtreeEndRowIndexExclusive;

        public ProjectedRow(DisplayRow displayRow, int rowIndex, int parentRowIndex) {
            this.displayRow = displayRow;
            this.rowIndex = rowIndex;
            this.parentRowIndex = parentRowIndex;
            this.subtreeEndRowIndexExclusive = rowIndex + 1;
        }

        public DisplayRow displayRow() {
            return displayRow;
        }

        public int rowIndex() {
            return rowIndex;
        }

        public int parentRowIndex() {
            return parentRowIndex;
        }

        public int subtreeEndRowIndexExclusive() {
            return subtreeEndRowIndexExclusive;
        }

        public void setSubtreeEndRowIndexExclusive(int subtreeEndRowIndexExclusive) {
            this.subtreeEndRowIndexExclusive = Math.max(rowIndex + 1, subtreeEndRowIndexExclusive);
        }
    }

    public enum RowKind {
        BOOKMARK_GROUP,
        BOOKMARK_PAGE,
        TREE_PAGE,
        /**
         * The collapsible section the guide editor appends below the tree, holding every template page.
         */
        TEMPLATE_GROUP,
        TEMPLATE_PAGE
    }

    public static class DisplayRow {

        private final RowKind kind;
        private final int depth;
        private final String title;
        @Nullable
        private final GuidePageIcon icon;
        @Nullable
        private final ResourceLocation guideId;
        @Nullable
        private final ResourceLocation pageId;
        private final boolean hasChildren;
        private final boolean hasPage;

        public DisplayRow(RowKind kind, int depth, String title, @Nullable GuidePageIcon icon,
            @Nullable ResourceLocation guideId, @Nullable ResourceLocation pageId, boolean hasChildren,
            boolean hasPage) {
            this.kind = kind;
            this.depth = depth;
            this.title = title;
            this.icon = icon;
            this.guideId = guideId;
            this.pageId = pageId;
            this.hasChildren = hasChildren;
            this.hasPage = hasPage;
        }

        public RowKind kind() {
            return kind;
        }

        public int depth() {
            return depth;
        }

        public String title() {
            return title;
        }

        @Nullable
        public GuidePageIcon icon() {
            return icon;
        }

        @Nullable
        public ResourceLocation guideId() {
            return guideId;
        }

        @Nullable
        public ResourceLocation pageId() {
            return pageId;
        }

        public boolean hasChildren() {
            return hasChildren;
        }

        public boolean hasPage() {
            return hasPage;
        }
    }

    private static final Comparator<DisplayRow> BOOKMARK_ROW_COMPARATOR = Comparator
        .comparing(DisplayRow::title, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(
            row -> row.pageId() != null ? row.pageId()
                .toString() : "");

    @Nullable
    private NavigationTree cachedBookmarkTree;
    @Nullable
    private GuideBookmarkState cachedBookmarkState;
    private int cachedBookmarkVersion = -1;
    private List<DisplayRow> cachedBookmarkRows = List.of();

    public ProjectionResult project(@Nullable NavigationTree tree, GuideBookmarkState bookmarkState,
        Set<NavigationNode.Key> expandedTreeNodeKeys, boolean bookmarkGroupExpanded) {
        return project(tree, bookmarkState, expandedTreeNodeKeys, bookmarkGroupExpanded, null);
    }

    public ProjectionResult project(@Nullable NavigationTree tree, GuideBookmarkState bookmarkState,
        Set<NavigationNode.Key> expandedTreeNodeKeys, boolean bookmarkGroupExpanded,
        @Nullable Predicate<String> titleFilter) {
        if (tree == null) {
            return new ProjectionResult(List.of());
        }

        var rows = new ArrayList<ProjectedRow>();
        List<DisplayRow> bookmarkRows = getBookmarkRows(tree, bookmarkState);
        if (titleFilter != null) {
            bookmarkRows = bookmarkRows.stream()
                .filter(row -> titleFilter.test(row.title()))
                .toList();
        }
        if (!bookmarkRows.isEmpty()) {
            var bookmarkGroupRow = appendRow(
                rows,
                new DisplayRow(
                    RowKind.BOOKMARK_GROUP,
                    0,
                    GuidebookText.Bookmarks.text(),
                    null,
                    null,
                    null,
                    true,
                    false),
                -1);
            if (bookmarkGroupExpanded || titleFilter != null) {
                for (DisplayRow bookmarkRow : bookmarkRows) {
                    appendRow(rows, bookmarkRow, bookmarkGroupRow.rowIndex());
                }
            }
            bookmarkGroupRow.setSubtreeEndRowIndexExclusive(rows.size());
        }

        Set<NavigationNode> matchingNodes = null;
        if (titleFilter != null) {
            matchingNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var root : tree.getRootNodes()) {
                collectMatchingNodes(root, titleFilter, matchingNodes);
            }
        }
        for (var root : tree.getRootNodes()) {
            addTreeRows(rows, root, 0, expandedTreeNodeKeys, -1, matchingNodes);
        }
        return new ProjectionResult(rows);
    }

    private List<DisplayRow> getBookmarkRows(NavigationTree tree, GuideBookmarkState bookmarkState) {
        int bookmarkVersion = bookmarkState.version();
        if (cachedBookmarkTree == tree && cachedBookmarkState == bookmarkState
            && cachedBookmarkVersion == bookmarkVersion) {
            return cachedBookmarkRows;
        }
        if (!bookmarkState.isEmpty()) {
            var validPageIds = collectValidPageIds(tree);
            bookmarkState.pruneInvalid(validPageIds);
        }
        var bookmarkRows = buildBookmarkRows(tree, bookmarkState);
        cachedBookmarkRows = List.copyOf(bookmarkRows);
        cachedBookmarkTree = tree;
        cachedBookmarkState = bookmarkState;
        cachedBookmarkVersion = bookmarkState.version();
        return cachedBookmarkRows;
    }

    /**
     * Appends editor-only template pages. This section is independent of where the pack places templates in
     * its navigation tree.
     *
     * @param templatePages         resolved pages in display order
     * @param templateGroupExpanded whether the section is expanded
     */
    public ProjectionResult withTemplates(ProjectionResult base, List<DisplayRow> templatePages,
        boolean templateGroupExpanded) {
        return withTemplates(base, templatePages, templateGroupExpanded, null);
    }

    public ProjectionResult withTemplates(ProjectionResult base, List<DisplayRow> templatePages,
        boolean templateGroupExpanded, @Nullable Predicate<String> titleFilter) {
        if (titleFilter != null) {
            templatePages = templatePages.stream()
                .filter(row -> titleFilter.test(row.title()))
                .toList();
        }
        if (templatePages.isEmpty()) {
            return base;
        }
        var rows = new ArrayList<>(base.rows());
        var groupRow = appendRow(
            rows,
            new DisplayRow(RowKind.TEMPLATE_GROUP, 0, GuidebookText.Templates.text(), null, null, null, true, false),
            -1);
        if (templateGroupExpanded || titleFilter != null) {
            for (DisplayRow templateRow : templatePages) {
                appendRow(rows, templateRow, groupRow.rowIndex());
            }
        }
        groupRow.setSubtreeEndRowIndexExclusive(rows.size());
        return new ProjectionResult(rows);
    }

    private Set<ResourceLocation> collectValidPageIds(NavigationTree tree) {
        var validPageIds = new HashSet<ResourceLocation>();
        for (var root : tree.getRootNodes()) {
            collectPageIds(root, validPageIds);
        }
        return validPageIds;
    }

    private void collectPageIds(NavigationNode node, Set<ResourceLocation> validPageIds) {
        if (node.pageId() != null) {
            validPageIds.add(node.pageId());
        }
        for (var child : node.children()) {
            collectPageIds(child, validPageIds);
        }
    }

    private List<DisplayRow> buildBookmarkRows(NavigationTree tree, GuideBookmarkState bookmarkState) {
        if (bookmarkState.isEmpty()) {
            return List.of();
        }
        var rows = new ArrayList<DisplayRow>();
        for (ResourceLocation bookmarkId : bookmarkState.getBookmarksView()) {
            NavigationNode node = tree.getNodeById(bookmarkId);
            if (node == null || node.pageId() == null || !node.hasPage()) {
                continue;
            }
            rows.add(
                new DisplayRow(
                    RowKind.BOOKMARK_PAGE,
                    1,
                    node.title(),
                    node.icon(),
                    node.guideId(),
                    node.pageId(),
                    false,
                    true));
        }
        rows.sort(BOOKMARK_ROW_COMPARATOR);
        return rows;
    }

    private ProjectedRow appendRow(List<ProjectedRow> rows, DisplayRow displayRow, int parentRowIndex) {
        ProjectedRow row = new ProjectedRow(displayRow, rows.size(), parentRowIndex);
        rows.add(row);
        return row;
    }

    private void addTreeRows(List<ProjectedRow> rows, NavigationNode node, int depth,
        Set<NavigationNode.Key> expandedTreeNodeKeys, int parentRowIndex, @Nullable Set<NavigationNode> matchingNodes) {
        if (matchingNodes != null && !matchingNodes.contains(node)) {
            return;
        }
        boolean hasChildren = matchingNodes == null ? !node.children()
            .isEmpty()
            : node.children()
                .stream()
                .anyMatch(matchingNodes::contains);
        ProjectedRow row = appendRow(
            rows,
            new DisplayRow(
                RowKind.TREE_PAGE,
                depth,
                node.title(),
                node.icon(),
                node.guideId(),
                node.pageId(),
                hasChildren,
                node.hasPage()),
            parentRowIndex);
        NavigationNode.Key nodeKey = node.key();
        if (matchingNodes != null || nodeKey != null && expandedTreeNodeKeys.contains(nodeKey)) {
            for (var child : node.children()) {
                addTreeRows(rows, child, depth + 1, expandedTreeNodeKeys, row.rowIndex(), matchingNodes);
            }
        }
        row.setSubtreeEndRowIndexExclusive(rows.size());
    }

    private boolean collectMatchingNodes(NavigationNode node, Predicate<String> titleFilter,
        Set<NavigationNode> matchingNodes) {
        boolean matches = titleFilter.test(node.title());
        for (NavigationNode child : node.children()) {
            matches |= collectMatchingNodes(child, titleFilter, matchingNodes);
        }
        if (matches) {
            matchingNodes.add(node);
        }
        return matches;
    }
}
