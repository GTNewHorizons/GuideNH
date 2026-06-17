package com.hfstudio.guidenh.guide.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * General-purpose background computation pool.
 *
 * <p>
 * Submit heavy CPU/IO work that does NOT touch Minecraft registries
 * or main-thread data structures. Poll for completion from the main thread.
 *
 * <p>
 * 4 daemon threads. Threads die with the JVM.
 */
public class AsyncWorker {

    private static final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        var t = new Thread(r, "guidenh-async");
        t.setDaemon(true);
        return t;
    });

    private static final ConcurrentHashMap<String, Future<?>> tasks = new ConcurrentHashMap<>();

    /**
     * Submit a named task. If a task with the same name already exists
     * and is still running, the new submission replaces it.
     */
    public static void submit(String name, Runnable work) {
        var existing = tasks.get(name);
        if (existing != null && !existing.isDone()) {
            GuideDebugLog.info("[AsyncWorker] Replacing running task: {}", name);
            existing.cancel(true);
        }
        tasks.put(name, pool.submit(work));
    }

    /** True if the named task has completed (or never existed). */
    public static boolean isDone(String name) {
        var f = tasks.get(name);
        return f == null || f.isDone();
    }

    /** Remove completed task entry to free the name. */
    public static void clear(String name) {
        tasks.remove(name);
    }

    /** Cancel all running tasks and clear the task map. */
    public static void reset() {
        for (var f : tasks.values()) {
            f.cancel(true);
        }
        tasks.clear();
    }
}
