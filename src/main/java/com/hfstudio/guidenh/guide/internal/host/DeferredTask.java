package com.hfstudio.guidenh.guide.internal.host;

import com.hfstudio.guidenh.guide.document.block.LytDocument;

public interface DeferredTask {

    enum Priority {
        HIGH,
        LOW
    }

    enum TaskResult {
        YIELD,
        DONE
    }

    Priority priority();

    TaskResult step(long deadlineNs);

    /** Whether this task belongs to a document, so unmounting that document drops only its own work. */
    default boolean belongsTo(LytDocument owner) {
        return true;
    }
}
