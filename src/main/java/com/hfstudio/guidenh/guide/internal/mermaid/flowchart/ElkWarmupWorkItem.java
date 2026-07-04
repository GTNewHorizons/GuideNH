package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import com.hfstudio.guidenh.guide.internal.scheduler.Priority;
import com.hfstudio.guidenh.guide.internal.scheduler.WorkItem;
import com.hfstudio.guidenh.guide.internal.scheduler.WorkResult;

public class ElkWarmupWorkItem implements WorkItem {

    private boolean done;

    @Override
    public Priority priority() {
        return Priority.LOW;
    }

    @Override
    public boolean shouldRun() {
        return !done;
    }

    @Override
    public WorkResult tick(long deadlineNs) {
        ElkLayoutStrategy.warmup();
        done = true;
        return WorkResult.DONE;
    }
}
