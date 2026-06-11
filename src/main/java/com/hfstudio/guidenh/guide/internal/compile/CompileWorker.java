package com.hfstudio.guidenh.guide.internal.compile;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.GuideRegistry;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Background compilation worker that compiles parsed guide pages in a dedicated
 * daemon thread. Supports greedy bulk compilation (post-world-load / F3+T
 * reload) and priority-based insertion for pages the user navigates to before
 * bulk compilation finishes.
 *
 * <p>
 * The worker thread exits when there is no work to do and is resubmitted
 * by {@link #startBulk(Collection)} or {@link #prioritize(ResourceLocation)}.
 */
public class CompileWorker {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "guidenh-compile");
        t.setDaemon(true);
        return t;
    });

    private final ConcurrentHashMap<ResourceLocation, GuidePage> compiledPages = new ConcurrentHashMap<>();

    /**
     * Main thread sets this to force a page to compile next (插队).
     * Worker clears it after processing.
     */
    private volatile ResourceLocation priorityId;

    /**
     * Worker sets this while compiling a page. Main thread reads it for UI
     * feedback (e.g. "Compiling..." spinner or tooltip).
     */
    private volatile ResourceLocation currentPageId;

    /**
     * Greedy compilation queue. Worker polls from the head; new items are
     * added at the tail. Synchronized access via the queue monitor.
     */
    private final Deque<ResourceLocation> bulkQueue = new ArrayDeque<>();

    private volatile boolean shutdown;

    /**
     * Start greedy compilation of the given pages. Called after world load or
     * F3+T resource reload. Any previously queued items are discarded.
     */
    public void startBulk(Collection<ResourceLocation> pageIds) {
        this.priorityId = null;
        this.currentPageId = null;
        synchronized (bulkQueue) {
            bulkQueue.clear();
            bulkQueue.addAll(pageIds);
        }
        submitIfIdle();
    }

    /**
     * Non-blocking read of a previously compiled page. Returns {@code null}
     * if the page has not been compiled yet.
     */
    @Nullable
    public GuidePage getCompiledPage(ResourceLocation pageId) {
        return compiledPages.get(pageId);
    }

    /**
     * Promote a page to the front of the compilation queue. No-op if the page
     * is already compiled or currently being compiled.
     */
    public void prioritize(ResourceLocation pageId) {
        if (compiledPages.containsKey(pageId)) {
            return;
        }
        if (Objects.equals(currentPageId, pageId)) {
            return;
        }
        priorityId = pageId;
        submitIfIdle();
    }

    /**
     * Returns the page currently being compiled, or {@code null} if the worker
     * is idle. For UI display.
     */
    @Nullable
    public ResourceLocation getCurrentPageId() {
        return currentPageId;
    }

    /**
     * Clear all compiled results, discard any queued work, and restart
     * compilation for the given set of pages. Called during F3+T reload.
     */
    public void reset(Collection<ResourceLocation> newPageIds) {
        compiledPages.clear();
        startBulk(newPageIds);
    }

    /**
     * Shut down the worker thread. No more compilation will be performed.
     */
    public void shutdown() {
        shutdown = true;
        priorityId = null;
        synchronized (bulkQueue) {
            bulkQueue.clear();
        }
        compiledPages.clear();
        executor.shutdownNow();
    }

    // ---- internal -------------------------------------------------------

    private void submitIfIdle() {
        if (shutdown) {
            return;
        }
        executor.submit(this::runLoop);
    }

    private void runLoop() {
        while (!shutdown) {
            ResourceLocation target = null;

            // 1. Check priorityId
            ResourceLocation prio = priorityId;
            if (prio != null) {
                priorityId = null; // grab it, clear it
                if (!compiledPages.containsKey(prio)) {
                    target = prio;
                }
            }

            // 2. If no priority target, poll the bulk queue
            if (target == null) {
                synchronized (bulkQueue) {
                    target = bulkQueue.pollFirst();
                }
            }

            // 3. Nothing to do — exit; will be resubmitted by prioritize/startBulk
            if (target == null) {
                return;
            }

            // 4. Set current page id
            currentPageId = target;

            // 5. Compile
            try {
                compileOne(target);
            } catch (Throwable t) {
                GuideDebugLog.error("[CompileWorker] Failed to compile page {}", target, t);
            } finally {
                // 6. Clear current page id (always)
                currentPageId = null;
            }

            // 7. If priorityId matches target, clear it (just compiled it)
            if (Objects.equals(priorityId, target)) {
                priorityId = null;
            }
        }
    }

    private void compileOne(ResourceLocation pageId) {
        MutableGuide guide = findGuideForPage(pageId);
        if (guide == null) {
            return;
        }

        ParsedGuidePage parsed = guide.getParsedPage(pageId);
        if (parsed == null) {
            return;
        }

        // Parse (no-op if already parsed — getAstRoot is lazy + cached)
        parsed.getAstRoot();

        // Compile
        GuidePage compiled = PageCompiler.compile(guide, guide.getExtensions(), parsed);
        compiledPages.put(pageId, compiled);
    }

    /**
     * Iterate all registered guides and return the first one that owns the
     * given page.
     */
    @Nullable
    private static MutableGuide findGuideForPage(ResourceLocation pageId) {
        for (MutableGuide guide : GuideRegistry.getAll()) {
            if (guide.pageExists(pageId)) {
                return guide;
            }
        }
        return null;
    }
}
