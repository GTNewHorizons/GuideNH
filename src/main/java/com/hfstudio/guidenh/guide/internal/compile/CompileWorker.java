package com.hfstudio.guidenh.guide.internal.compile;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    private static final int MAX_COMPILED_PAGES = 64;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "guidenh-compile");
        t.setDaemon(true);
        return t;
    });

    /** Runtime worlds are released on the client thread; compilation itself runs in the worker thread. */
    private final ConcurrentLinkedQueue<GuidePage> pendingRuntimeReleases = new ConcurrentLinkedQueue<>();

    private final Map<ResourceLocation, GuidePage> compiledPages = new LinkedHashMap<>(
        MAX_COMPILED_PAGES,
        0.75f,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<ResourceLocation, GuidePage> eldest) {
            if (size() > MAX_COMPILED_PAGES) {
                requestRuntimeRelease(eldest.getValue());
                return true;
            }
            return false;
        }
    };

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

    /** Invalidates work that was started against a previous resource-pack generation. */
    private volatile long generation;

    /**
     * Greedy compilation queue. Worker polls from the head; new items are
     * added at the tail. Synchronized access via the queue monitor.
     */
    private final Deque<ResourceLocation> bulkQueue = new ArrayDeque<>();

    private volatile boolean shutdown;

    public CompileWorker() {
        executor.submit(this::runLoop);
    }

    /**
     * Start greedy compilation of the given pages. Called after world load or
     * F3+T resource reload. Any previously queued items are discarded.
     */
    public void startBulk(Collection<ResourceLocation> pageIds) {
        generation++;
        this.priorityId = null;
        this.currentPageId = null;
        synchronized (bulkQueue) {
            bulkQueue.clear();
            bulkQueue.addAll(pageIds);
        }
        GuideDebugLog.info("[CompileWorker] startBulk {} pages", pageIds.size());
    }

    /**
     * Non-blocking read of a previously compiled page. Returns {@code null}
     * if the page has not been compiled yet.
     */
    @Nullable
    public GuidePage getCompiledPage(ResourceLocation pageId) {
        synchronized (compiledPages) {
            return compiledPages.get(pageId);
        }
    }

    public void invalidate(ResourceLocation pageId) {
        if (pageId != null) {
            synchronized (compiledPages) {
                requestRuntimeRelease(compiledPages.remove(pageId));
            }
        }
    }

    /**
     * Promote a page to the front of the compilation queue. No-op if the page
     * is already compiled or currently being compiled.
     */
    public void prioritize(ResourceLocation pageId) {
        synchronized (compiledPages) {
            if (compiledPages.containsKey(pageId)) {
                return;
            }
        }
        if (Objects.equals(currentPageId, pageId)) {
            return;
        }
        priorityId = pageId;
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
        clearCompiledPages();
        startBulk(newPageIds);
    }

    /**
     * Clears compiled pages and queued work without scheduling a bulk compile. Pages are compiled
     * on demand when opened, which keeps resource reloads cheap even for large guide packs.
     */
    public void clearCompiledPages() {
        generation++;
        priorityId = null;
        currentPageId = null;
        synchronized (bulkQueue) {
            bulkQueue.clear();
        }
        synchronized (compiledPages) {
            requestRuntimeReleases(compiledPages.values());
            compiledPages.clear();
        }
    }

    /**
     * Shut down the worker thread. No more compilation will be performed.
     */
    public void shutdown() {
        shutdown = true;
        generation++;
        priorityId = null;
        synchronized (bulkQueue) {
            bulkQueue.clear();
        }
        synchronized (compiledPages) {
            requestRuntimeReleases(compiledPages.values());
            compiledPages.clear();
        }
        executor.shutdownNow();
    }

    private void requestRuntimeReleases(Collection<GuidePage> pages) {
        for (GuidePage page : pages) {
            requestRuntimeRelease(page);
        }
    }

    private void requestRuntimeRelease(@Nullable GuidePage page) {
        if (page == null) {
            return;
        }
        pendingRuntimeReleases.add(page);
    }

    /** Drains runtime release requests and must be called from the Minecraft client thread. */
    public void drainRuntimeReleases() {
        GuidePage page;
        while ((page = pendingRuntimeReleases.poll()) != null) {
            releaseRuntimePage(page);
        }
    }

    /**
     * Drops queued page references after the client world has been unloaded. Their runtime
     * worlds have already been released through GuidebookLevel's weak live-level registry.
     */
    public void clearPendingRuntimeReleases() {
        pendingRuntimeReleases.clear();
    }

    private static void releaseRuntimePage(GuidePage page) {
        try {
            page.releaseRuntimeScenes();
        } catch (Throwable t) {
            GuideDebugLog.warn("[CompileWorker] Failed to release runtime scenes for cached page", t);
        }
    }

    private void runLoop() {
        while (!shutdown) {
            ResourceLocation target = null;

            // 1. Check priorityId
            ResourceLocation prio = priorityId;
            if (prio != null) {
                priorityId = null; // grab it, clear it
                synchronized (compiledPages) {
                    if (!compiledPages.containsKey(prio)) {
                        target = prio;
                    }
                }
            }

            // 2. If no priority target, poll the bulk queue
            if (target == null) {
                synchronized (bulkQueue) {
                    target = bulkQueue.pollFirst();
                }
            }

            // 3. Nothing to do — sleep briefly, then check again
            if (target == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                    return;
                }
                continue;
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
        long compileGeneration = generation;
        MutableGuide guide = findGuideForPage(pageId);
        if (guide == null) {
            return;
        }

        ParsedGuidePage parsed = guide.getParsedPage(pageId);
        if (parsed == null) {
            return;
        }

        long t0 = System.nanoTime();
        // Parse (no-op if already parsed — getAstRoot is lazy + cached)
        parsed.getAstRoot();
        long tParsed = System.nanoTime();

        // Compile
        GuidePage compiled = PageCompiler.compile(guide, guide.getExtensions(), parsed);
        long tCompiled = System.nanoTime();
        synchronized (compiledPages) {
            if (compileGeneration != generation || shutdown) {
                // A reload won while this page was compiling. Do not publish a stale page that
                // retains runtime scene state from the previous resource-pack generation.
                requestRuntimeRelease(compiled);
                return;
            }
            compiledPages.put(pageId, compiled);
        }

        GuideDebugLog.info(
            "[CompileWorker] Compiled page={} parseMs={} compileMs={}",
            pageId,
            (tParsed - t0) / 1_000_000L,
            (tCompiled - tParsed) / 1_000_000L);
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
