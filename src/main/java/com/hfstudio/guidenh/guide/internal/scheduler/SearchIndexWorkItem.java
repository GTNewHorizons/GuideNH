package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.GuideME;
import com.hfstudio.guidenh.guide.internal.search.GuideSearch;

public class SearchIndexWorkItem implements WorkItem {

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    @Override
    public boolean shouldRun() {
        return GuideME.getSearch()
            .hasPendingWork();
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        GuideSearch search = GuideME.getSearch();
        long budget = search.isSearchPriorityActive() ? GuideSearch.SEARCH_TIME_PER_TICK
            : GuideSearch.BACKGROUND_TIME_PER_TICK;
        search.processWork(budget);
        return WorkResult.YIELD;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SearchIndexWorkItem;
    }

    @Override
    public int hashCode() {
        return SearchIndexWorkItem.class.hashCode();
    }
}
