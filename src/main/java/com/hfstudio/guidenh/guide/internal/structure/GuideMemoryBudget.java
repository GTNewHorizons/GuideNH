package com.hfstudio.guidenh.guide.internal.structure;

public class GuideMemoryBudget {

    private final long maxBytes;
    private long usedBytes;

    public GuideMemoryBudget(long maxBytes) {
        if (maxBytes <= 0) throw new IllegalArgumentException("A positive memory budget is required");
        this.maxBytes = maxBytes;
    }

    public synchronized boolean reserve(long bytes) {
        if (bytes < 0 || bytes > maxBytes - usedBytes) return false;
        usedBytes += bytes;
        return true;
    }

    public synchronized void release(long bytes) {
        if (bytes < 0 || bytes > usedBytes) throw new IllegalStateException("Invalid memory budget release");
        usedBytes -= bytes;
    }
}
