package com.hfstudio.guidenh.guide.internal.scheduler;

import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;

public class DevWatchWorkItem implements WorkItem {

    static final int DEFAULT_INTERVAL_TICKS = 20;

    private int tickCounter;

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    @Override
    public boolean shouldRun() {
        return hasDevelopmentSources();
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        tickCounter++;
        if (tickCounter >= DEFAULT_INTERVAL_TICKS) {
            tickCounter = 0;
            pollDevelopmentSources();
        }
        return WorkResult.YIELD;
    }

    private static boolean hasDevelopmentSources() {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            if (guide.hasDevelopmentSources()) {
                return true;
            }
        }
        return false;
    }

    private static void pollDevelopmentSources() {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            if (guide.hasDevelopmentSources()) {
                guide.tickDevelopmentSources();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DevWatchWorkItem;
    }

    @Override
    public int hashCode() {
        return DevWatchWorkItem.class.hashCode();
    }
}
