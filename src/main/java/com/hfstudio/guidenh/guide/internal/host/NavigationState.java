package com.hfstudio.guidenh.guide.internal.host;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.GuideScreenRoute;
import com.hfstudio.guidenh.guide.internal.GuideScreenViewState;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.screen.GuideNavBarState;

public class NavigationState {

    /** Sentinel key for the home page's nav bar state (home has no guideId). */
    static final ResourceLocation HOME_KEY = new ResourceLocation("guidenh", "_home");

    @Nullable
    private ResourceLocation currentGuideId;
    @Nullable
    private PageAnchor currentAnchor;

    private final List<GuideScreenViewState> pageHistory = new ArrayList<>();
    private int pageHistoryIndex = -1;

    private final Map<ResourceLocation, GuideNavBarState> navBarStates = new LinkedHashMap<>();

    private final Set<ResourceLocation> bookmarks = new LinkedHashSet<>();

    private final List<HomeHistoryEntry> homeHistory = new ArrayList<>();

    public static class HomeHistoryEntry {

        public final ResourceLocation guideId;
        public final ResourceLocation pageId;

        public HomeHistoryEntry(ResourceLocation guideId, ResourceLocation pageId) {
            this.guideId = guideId;
            this.pageId = pageId;
        }
    }

    public void setCurrent(ResourceLocation guideId, PageAnchor anchor) {
        this.currentGuideId = guideId;
        this.currentAnchor = anchor;
    }

    @Nullable
    public ResourceLocation currentGuideId() {
        return currentGuideId;
    }

    @Nullable
    public PageAnchor currentAnchor() {
        return currentAnchor;
    }

    // ---- Page history (browser-style linear history with back/forward) ----

    public void recordPageHistory(GuideScreenViewState state) {
        if (state == null) return;
        if (pageHistoryIndex >= 0 && pageHistoryIndex < pageHistory.size()
            && isSamePage(pageHistory.get(pageHistoryIndex), state)) {
            pageHistory.set(pageHistoryIndex, state);
            return;
        }
        while (pageHistory.size() > pageHistoryIndex + 1) {
            pageHistory.remove(pageHistory.size() - 1);
        }
        pageHistory.add(state);
        pageHistoryIndex = pageHistory.size() - 1;
    }

    @Nullable
    public GuideScreenViewState navigateBack() {
        if (pageHistoryIndex <= 0) return null;
        pageHistoryIndex--;
        return pageHistory.get(pageHistoryIndex);
    }

    @Nullable
    public GuideScreenViewState navigateForward() {
        if (pageHistoryIndex >= pageHistory.size() - 1) return null;
        pageHistoryIndex++;
        return pageHistory.get(pageHistoryIndex);
    }

    public boolean canGoBack() {
        return pageHistoryIndex > 0;
    }

    public boolean canGoForward() {
        return pageHistoryIndex < pageHistory.size() - 1;
    }

    @Nullable
    public GuideScreenViewState getMostRecentPageHistory() {
        if (pageHistoryIndex < 0 || pageHistoryIndex >= pageHistory.size()) return null;
        GuideScreenViewState state = pageHistory.get(pageHistoryIndex);
        if (!isValidContentRoute(state.route())) return null;
        return state;
    }

    @Nullable
    public GuideScreenViewState recallLastContentState() {
        return getMostRecentPageHistory();
    }

    @Nullable
    public GuideScreenViewState consumeValidLastContentState() {
        return getMostRecentPageHistory();
    }

    private static boolean isSamePage(GuideScreenViewState a, GuideScreenViewState b) {
        GuideScreenRoute routeA = a.route();
        GuideScreenRoute routeB = b.route();
        if (routeA == null || routeB == null) return false;
        if (!routeA.isContent() || !routeB.isContent()) return false;
        ResourceLocation guideIdA = routeA.guideId();
        ResourceLocation guideIdB = routeB.guideId();
        PageAnchor anchorA = routeA.anchor();
        PageAnchor anchorB = routeB.anchor();
        if (guideIdA == null || guideIdB == null || anchorA == null || anchorB == null) return false;
        return guideIdA.equals(guideIdB) && anchorA.pageId() != null
            && anchorA.pageId()
                .equals(anchorB.pageId());
    }

    // ---- Legacy content state (delegates to page history) ----

    public void rememberContentState(@Nullable GuideScreenViewState state) {
        if (!isRememberable(state)) return;
        recordPageHistory(state);
    }

    // ---- Nav bar state ----

    public void rememberNavBarState(ResourceLocation guideId, GuideNavBarState state) {
        if (state != null) {
            ResourceLocation key = guideId != null ? guideId : HOME_KEY;
            navBarStates.put(key, state);
        }
    }

    @Nullable
    public GuideNavBarState recallNavBarState(ResourceLocation guideId) {
        return navBarStates.get(guideId);
    }

    public GuideNavBarState recallNavigationState(@Nullable ResourceLocation guideId) {
        ResourceLocation key = guideId != null ? guideId : HOME_KEY;
        GuideNavBarState state = navBarStates.get(key);
        return state != null ? state : GuideNavBarState.defaultState();
    }

    // ---- Bookmarks ----

    public boolean isBookmarked(ResourceLocation pageId) {
        return bookmarks.contains(pageId);
    }

    public void toggleBookmark(ResourceLocation pageId) {
        if (!bookmarks.remove(pageId)) {
            bookmarks.add(pageId);
        }
    }

    public Set<ResourceLocation> bookmarks() {
        return bookmarks;
    }

    // ---- Home history (for home page widget display) ----

    public void recordHomeHistory(ResourceLocation guideId, ResourceLocation pageId) {
        homeHistory.add(0, new HomeHistoryEntry(guideId, pageId));
    }

    public List<HomeHistoryEntry> homeHistory() {
        return homeHistory;
    }

    // ---- Validation ----

    public boolean isRememberable(@Nullable GuideScreenViewState state) {
        if (state == null) return false;
        GuideScreenRoute route = state.route();
        if (route == null || !route.isContent()) return false;
        PageAnchor anchor = route.anchor();
        return anchor != null && isSupportedContentAnchor(anchor) && isValidContentRoute(route);
    }

    public static boolean isSupportedContentAnchor(@Nullable PageAnchor anchor) {
        return anchor != null;
    }

    public boolean isValidContentRoute(@Nullable GuideScreenRoute route) {
        if (route == null || !route.isContent()) return false;
        ResourceLocation guideId = route.guideId();
        PageAnchor anchor = route.anchor();
        if (guideId == null || anchor == null) return false;
        MutableGuide guide = GuideRegistry.getById(guideId);
        return guide != null && guide.pageExists(anchor.pageId());
    }

    public void clear() {
        pageHistory.clear();
        pageHistoryIndex = -1;
        navBarStates.clear();
        bookmarks.clear();
        homeHistory.clear();
    }
}
