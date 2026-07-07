package com.hfstudio.guidenh.guide.internal.host;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.ClientProxy;
import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.PageCollection;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.compile.CompileWorker;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import lombok.Getter;
import lombok.Setter;

public class LytHost {

    @Nullable
    private LytDocument document;
    @Nullable
    private PageCollection currentPageCollection;
    private final Map<String, LytScript> scripts = new HashMap<>();
    // Compiled pages are now in CompileWorker. LytHost only tracks node-level results.
    private final Map<String, Map<String, Object>> nodeResults = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<String, AtomicInteger> pageNodeCounters = new HashMap<>();
    private static final int MAX_NODE_RESULT_CACHE = 32;
    @Setter
    String currentPageId;

    @Getter
    private final ViewportState viewport = new ViewportState();
    private final NavigationState nav = new NavigationState();
    private final Deque<LytEvent> eventQueue = new ArrayDeque<>();
    private final Deque<DeferredTask> taskQueue = new ArrayDeque<>();

    // Debug implementation

    /**
     * Full processing: UID allocation, onAttach, MOUNT dispatch. Resets the node counter so the
     * same page always gets the same UIDs across remounts (enabling node-level cache hits).
     */
    public void mountDocument(@Nullable LytDocument newDoc) {
        if (this.document != null && this.document != newDoc) {
            this.document.setLive(false); // onDetach cascade on old doc
            taskQueue.clear();
        }
        this.document = newDoc;
        if (newDoc != null) {
            pageNodeCounters.remove(currentPageId); // reset for stable UIDs
            ensureNodeResultStore(currentPageId);
            long tUid = System.nanoTime();
            allocateNodeUids(newDoc);
            long tAttach = System.nanoTime();
            newDoc.setLive(true); // onAttach cascade
            dispatchMountEvents(newDoc); // MOUNT events → scripts materialize placeholders
            long tMount = System.nanoTime();
            viewport.updateContent(newDoc.getAvailableWidth(), newDoc.getContentHeight());
            long tViewport = System.nanoTime();
            GuideDebugLog.infoAlways(
                "[LytHost] mountDocument uidMs={} attachAndScriptMs={} viewportMs={}",
                (tAttach - tUid) / 1_000_000L,
                (tMount - tAttach) / 1_000_000L,
                (tViewport - tMount) / 1_000_000L);
        }
    }

    @Nullable
    public LytDocument getDocument() {
        return document;
    }

    public NavigationState getNavigation() {
        return nav;
    }

    public void registerScript(String styleClass, LytScript script) {
        scripts.put(styleClass, script);
    }

    @Nullable
    public GuidePage getCachedGuidePage(String pageId) {
        CompileWorker worker = ClientProxy.getWorker();
        if (worker == null) return null;
        return worker.getCompiledPage(new ResourceLocation(pageId));
    }

    public void recordNodeResult(String pageId, String nodeUid, Object result) {
        Map<String, Object> results = nodeResults.get(pageId);
        if (results != null) {
            results.put(nodeUid, result);
        }
    }

    @Nullable
    Object getNodeResult(String pageId, String nodeUid) {
        Map<String, Object> results = nodeResults.get(pageId);
        return results != null ? results.get(nodeUid) : null;
    }

    public void ensureNodeResultStore(String pageId) {
        if (nodeResults.containsKey(pageId)) return;
        while (nodeResults.size() >= MAX_NODE_RESULT_CACHE) {
            String oldest = nodeResults.keySet()
                .iterator()
                .next();
            nodeResults.remove(oldest);
        }
        nodeResults.put(pageId, new HashMap<>());
    }

    public void invalidatePage(String pageId) {
        nodeResults.remove(pageId);
        pageNodeCounters.remove(pageId);
    }

    public void clearPageCaches() {
        nodeResults.clear();
        pageNodeCounters.clear();
    }

    public void setCurrentPageCollection(@Nullable PageCollection pageCollection) {
        this.currentPageCollection = pageCollection;
    }

    @Nullable
    public PageCollection getCurrentPageCollection() {
        return currentPageCollection;
    }

    String allocateNodeUid(String pageId, String prefix) {
        int seq = pageNodeCounters.computeIfAbsent(pageId, k -> new AtomicInteger())
            .incrementAndGet();
        return pageId + "::" + prefix + ":" + seq;
    }

    private void allocateNodeUids(LytNode node) {
        if (node.getStyleClass() != null && node.getNodeUid() == null) {
            String prefix = node.getStyleClass()
                .toLowerCase();
            int seq = pageNodeCounters.computeIfAbsent(currentPageId, k -> new AtomicInteger())
                .incrementAndGet();
            node.setNodeUid(currentPageId + "::" + prefix + ":" + seq);
        }
        for (var child : node.getChildren()) {
            allocateNodeUids(child);
        }
        // Also traverse into flow content (LytParagraph, LytFlowSpan children)
        allocateFlowNodeUids(node);
    }

    private void allocateFlowNodeUids(LytNode node) {
        if (node instanceof LytParagraph para) {
            for (var fcChild : para.getContent()) {
                allocateFlowNodeUidsRecursive(fcChild);
            }
        }
    }

    private void allocateFlowNodeUidsRecursive(LytFlowContent fc) {
        if (fc.getStyleClass() != null && fc.getNodeUid() == null) {
            String prefix = fc.getStyleClass()
                .toLowerCase();
            int seq = pageNodeCounters.computeIfAbsent(currentPageId, k -> new AtomicInteger())
                .incrementAndGet();
            fc.setNodeUid(currentPageId + "::" + prefix + ":" + seq);
        }
        if (fc instanceof LytFlowSpan span) {
            for (var child : span.getChildren()) {
                allocateFlowNodeUidsRecursive(child);
            }
        }
    }

    /**
     * Two-phase MOUNT dispatch:
     * <p>
     * <strong>Phase 1 (sync):</strong> walk the entire tree and execute
     * every synchronous script immediately. This guarantees that all
     * setup and initialization work (e.g. establishing CURRENT_SCENE,
     * compiling child elements) is finished before any asynchronous
     * work begins.
     * <p>
     * <strong>Phase 2 (async):</strong> walk the tree a second time and
     * queue every asynchronous script as a {@link MaterializeTask} for
     * execution on subsequent ticks (see {@link #step}).
     * <p>
     * Within each phase the original document order (parent before children)
     * is preserved. The node-level result cache is consulted: if a node
     * already has a cached result from a previous mount, the cached content
     * is restored directly and the script is skipped in <em>both</em>
     * phases.
     */
    private void dispatchMountEvents(LytNode node) {
        dispatchPhase(node, false); // Phase 1: sync scripts only
        dispatchPhase(node, true); // Phase 2: queue async scripts only
    }

    private void dispatchPhase(LytNode node, boolean asyncPhase) {
        String cls = node.getStyleClass();
        if (cls != null) {
            LytScript script = scripts.get(cls);
            if (script != null) {
                dispatchScriptInPhase(script, node, asyncPhase);
            }
        }
        for (var child : node.getChildren()) {
            dispatchPhase(child, asyncPhase);
        }
        dispatchPhaseFlow(node, asyncPhase);
    }

    private void dispatchPhaseFlow(LytNode node, boolean asyncPhase) {
        if (node instanceof LytParagraph para) {
            for (var fcChild : para.getContent()) {
                dispatchPhaseFlowRecursive(fcChild, asyncPhase);
            }
        }
    }

    private void dispatchPhaseFlowRecursive(LytFlowContent fc, boolean asyncPhase) {
        String cls = fc.getStyleClass();
        if (cls != null) {
            LytScript script = scripts.get(cls);
            if (script != null) {
                dispatchScriptInPhase(script, fc, asyncPhase);
            }
        }
        if (fc instanceof LytFlowSpan span) {
            for (var child : span.getChildren()) {
                dispatchPhaseFlowRecursive(child, asyncPhase);
            }
        } else if (fc instanceof LytFlowInlineBlock inlineBlock && inlineBlock.getBlock() != null) {
            LytBlock inner = inlineBlock.getBlock();
            String innerCls = inner.getStyleClass();
            if (innerCls != null) {
                LytScript script = scripts.get(innerCls);
                if (script != null) {
                    dispatchScriptInPhase(script, inlineBlock, asyncPhase);
                }
            }
        }
    }

    /**
     * Dispatch a single script in the given phase.
     * <ul>
     * <li>If the node has a cached result from a previous mount, the
     * cached content is restored directly and the script is skipped
     * entirely (both phases).
     * <li>In the <em>sync</em> phase ({@code asyncPhase == false}), only
     * non-async scripts are executed synchronously.
     * <li>In the <em>async</em> phase ({@code asyncPhase == true}), only
     * async scripts are enqueued as {@link MaterializeTask}s.
     * </ul>
     */
    private void dispatchScriptInPhase(LytScript script, Object node, boolean asyncPhase) {
        String nodeUid = nodeUidOf(node);
        if (nodeUid != null) {
            Object cached = getNodeResult(currentPageId, nodeUid);
            if (cached != null) {
                new ScriptContextImpl(node, this, document).replace(cached);
                return;
            }
        }
        if (asyncPhase) {
            if (script.isAsync()) {
                taskQueue.addLast(new MaterializeTask(script, node, new ScriptContextImpl(node, this, document)));
            }
        } else {
            if (!script.isAsync()) {
                try {
                    ScriptContextImpl ctx = new ScriptContextImpl(node, this, document);
                    script.onEvent(node, new LytEvent(EventType.MOUNT, node), ctx);
                } catch (Exception e) {
                    GuideDebugLog
                        .error("[LytHost] Sync script {} failed on node {}", script.styleClass(), nodeUidOf(node), e);
                }
            }
        }
    }

    @Nullable
    private static String nodeUidOf(Object node) {
        if (node instanceof LytNode ln) return ln.getNodeUid();
        if (node instanceof LytFlowContent fc) return fc.getNodeUid();
        return null;
    }

    private static class MaterializeTask implements DeferredTask {

        private final LytScript script;
        private final Object node;
        private final ScriptContextImpl ctx;
        private final LytEvent event;

        MaterializeTask(LytScript script, Object node, ScriptContextImpl ctx) {
            this.script = script;
            this.node = node;
            this.ctx = ctx;
            this.event = new LytEvent(EventType.MOUNT, node);
        }

        @Override
        public Priority priority() {
            return Priority.HIGH;
        }

        @Override
        public TaskResult step(long deadlineNs) {
            ctx.setYieldDeadline(deadlineNs);
            ctx.resetYieldState();
            try {
                script.onEvent(node, event, ctx);
            } catch (Exception e) {
                GuideDebugLog
                    .error("[MaterializeTask] Script {} failed on node {}", script.styleClass(), nodeUidOf(node), e);
                ctx.replaceError(e);
                ctx.markComplete();
            }
            if (ctx.isComplete()) return TaskResult.DONE;
            if (ctx.isYieldRequested()) return TaskResult.YIELD;
            // Script returned without declaring complete or yield
            // -> auto-complete (backward-compatible default for one-shot scripts)
            ctx.markComplete();
            return TaskResult.DONE;
        }
    }

    // Debug implementation

    public void pushEvent(LytEvent event) {
        eventQueue.addLast(event);
        processEventsNow();
    }

    private void processEventsNow() {
        while (!eventQueue.isEmpty()) {
            LytEvent event = eventQueue.pollFirst();
            if (document == null || event.target() == null) continue;
            Object rawTarget = event.target();
            if (rawTarget instanceof InteractiveElement interactive) {
                switch (event.type()) {
                    case CLICK:
                    case DOUBLE_CLICK:
                        if (event.data()
                            .containsKey("x")
                            && event.data()
                                .containsKey("y")) {
                            interactive.mouseClicked(
                                null,
                                ((Number) event.data()
                                    .get("x")).intValue(),
                                ((Number) event.data()
                                    .get("y")).intValue(),
                                event.data()
                                    .containsKey("button")
                                        ? ((Number) event.data()
                                            .get("button")).intValue()
                                        : 0,
                                event.type() == EventType.DOUBLE_CLICK);
                        }
                        break;
                    case MOUSE_SCROLL:
                        // InteractiveElement does not expose mouseScrolled yet
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // Debug implementation

    public void submitTask(DeferredTask task) {
        taskQueue.addLast(task);
    }

    /** Recursively dispatch MOUNT events into a detached subtree. */
    public void dispatchToSubtree(LytNode root) {
        allocateNodeUids(root);
        dispatchMountEvents(root);
    }

    public boolean hasWork() {
        return !taskQueue.isEmpty();
    }

    public void step(long deadlineNs) {
        while (!taskQueue.isEmpty() && System.nanoTime() < deadlineNs) {
            DeferredTask task = taskQueue.peekFirst();
            DeferredTask.TaskResult result = task.step(deadlineNs);
            if (result == DeferredTask.TaskResult.DONE) {
                taskQueue.pollFirst();
            }
            if (result == DeferredTask.TaskResult.YIELD) {
                break;
            }
        }
    }

    public int pendingTaskCount() {
        return taskQueue.size();
    }

    public void clear() {
        document = null;
        currentPageCollection = null;
        scripts.clear();
        nodeResults.clear();
        pageNodeCounters.clear();
        currentPageId = null;
        eventQueue.clear();
        taskQueue.clear();
        nav.clear();
    }
}
