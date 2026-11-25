package com.hfstudio.guidenh.client.capture.render;

public class CaptureMemoryBudget {

    private long limit;
    private long used;

    public CaptureMemoryBudget(long limit) {
        limit(limit);
    }

    public synchronized void limit(long bytes) {
        if (bytes < 0) throw new IllegalArgumentException("Capture memory limits cannot be negative");
        limit = bytes;
    }

    public synchronized long limit() {
        return limit;
    }

    public synchronized long used() {
        return used;
    }

    public synchronized void reserve(long bytes) {
        if (bytes < 0) throw new IllegalArgumentException("Capture memory reservations cannot be negative");
        if (used > limit || bytes > limit - used)
            throw new IllegalArgumentException("Retained capture resources exceed the memory budget");
        used += bytes;
    }

    public synchronized void release(long bytes) {
        if (bytes < 0 || bytes > used)
            throw new IllegalArgumentException("Released capture memory must match a live reservation");
        used -= bytes;
    }
}
