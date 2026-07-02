package com.hfstudio.guidenh.guide.scene.preview;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibImportResult;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibSceneImportService;

public class StructureLibPreviewWorker {

    private static final int WORKER_THREADS = 4;
    private static final int QUEUE_POLL_SLEEP_MS = 25;

    private final ExecutorService executor;
    private final Deque<StructureLibPreviewTask> queue = new ArrayDeque<>();
    private final Deque<StructureLibPreviewResult> completedResults = new ArrayDeque<>();
    private final ConcurrentHashMap<String, Integer> latestSelectionVersionByBinding = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, StructureLibPreviewTask> runningTasksByBinding = new ConcurrentHashMap<>();
    private final ReentrantLock queueLock = new ReentrantLock();
    private final ReentrantLock resultsLock = new ReentrantLock();

    private volatile boolean shutdown;
    private final AtomicInteger activeThreadCount = new AtomicInteger(0);

    public StructureLibPreviewWorker() {
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r, "guidenh-structurelib-preview-" + System.nanoTime());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };
        executor = Executors.newFixedThreadPool(WORKER_THREADS, threadFactory);
        for (int i = 0; i < WORKER_THREADS; i++) {
            executor.submit(this::runLoop);
        }
    }

    public void submit(StructureLibPreviewTask task) {
        if (task == null) {
            return;
        }
        if (task.isSelectionBuild()) {
            latestSelectionVersionByBinding.put(task.getBindingKey(), task.getRequestVersion());
        }
        queueLock.lock();
        try {
            if (task.getPriority() == StructureLibPreviewTask.Priority.HIGH) {
                queue.addFirst(task);
            } else {
                queue.addLast(task);
            }
        } finally {
            queueLock.unlock();
        }
        GuideDebugLog.infoAlways(
            "[StructureLibPreviewWorker] queued type={} binding={} version={} selection={}",
            task.getType(),
            task.getBindingKey(),
            task.getRequestVersion(),
            task.getSelectionKey());
    }

    public List<StructureLibPreviewResult> drainResultsFor(Iterable<String> bindingKeys) {
        Set<String> acceptedBindingKeys = new HashSet<>();
        for (String bindingKey : bindingKeys) {
            if (bindingKey != null) {
                acceptedBindingKeys.add(bindingKey);
            }
        }
        List<StructureLibPreviewResult> drained = new ArrayList<>();
        resultsLock.lock();
        try {
            if (acceptedBindingKeys.isEmpty() || completedResults.isEmpty()) {
                return drained;
            }
            Deque<StructureLibPreviewResult> retained = new ArrayDeque<>();
            while (!completedResults.isEmpty()) {
                StructureLibPreviewResult result = completedResults.pollFirst();
                if (acceptedBindingKeys.contains(result.getBindingKey())) {
                    drained.add(result);
                } else {
                    retained.addLast(result);
                }
            }
            completedResults.addAll(retained);
        } finally {
            resultsLock.unlock();
        }
        return drained;
    }

    public void reset() {
        queueLock.lock();
        try {
            queue.clear();
        } finally {
            queueLock.unlock();
        }
        resultsLock.lock();
        try {
            completedResults.clear();
        } finally {
            resultsLock.unlock();
        }
        latestSelectionVersionByBinding.clear();
        runningTasksByBinding.clear();
    }

    public void shutdown() {
        shutdown = true;
        reset();
        executor.shutdownNow();
    }

    public StructureLibPreviewTask getCurrentTask() {
        return null;
    }

    public int getActiveThreadCount() {
        return activeThreadCount.get();
    }

    public int getQueueSize() {
        queueLock.lock();
        try {
            return queue.size();
        } finally {
            queueLock.unlock();
        }
    }

    private void runLoop() {
        while (!shutdown) {
            StructureLibPreviewTask task = pollTask();
            if (task == null) {
                sleepQuietly();
                continue;
            }
            if (isStaleBeforeRun(task)) {
                GuideDebugLog.infoAlways(
                    "[StructureLibPreviewWorker] skip stale queued task binding={} version={}",
                    task.getBindingKey(),
                    task.getRequestVersion());
                continue;
            }
            runningTasksByBinding.put(task.getBindingKey(), task);
            activeThreadCount.incrementAndGet();
            try {
                publishIfFresh(execute(task));
            } catch (Throwable throwable) {
                GuideDebugLog.error(
                    "[StructureLibPreviewWorker] task failed binding={} version={}",
                    task.getBindingKey(),
                    task.getRequestVersion(),
                    throwable);
                publishIfFresh(
                    StructureLibPreviewResult.failed(
                        task.getType(),
                        task.getBindingKey(),
                        task.getRequestVersion(),
                        task.getSelectionKey(),
                        StructureLibSceneImportService.resolveFailureMessage(throwable),
                        throwable.getMessage()));
            } finally {
                activeThreadCount.decrementAndGet();
                runningTasksByBinding.remove(task.getBindingKey());
            }
        }
    }

    private StructureLibPreviewTask pollTask() {
        queueLock.lock();
        try {
            return queue.pollFirst();
        } finally {
            queueLock.unlock();
        }
    }

    private boolean isStaleBeforeRun(StructureLibPreviewTask task) {
        if (!task.isSelectionBuild()) {
            return false;
        }
        Integer latestVersion = latestSelectionVersionByBinding.get(task.getBindingKey());
        return latestVersion != null && task.getRequestVersion() < latestVersion;
    }

    private void publishIfFresh(StructureLibPreviewResult result) {
        if (result == null) {
            return;
        }
        if (result.getType() == StructureLibPreviewTask.Type.BUILD_SELECTION) {
            Integer latestVersion = latestSelectionVersionByBinding.get(result.getBindingKey());
            if (latestVersion != null && result.getRequestVersion() < latestVersion) {
                GuideDebugLog.infoAlways(
                    "[StructureLibPreviewWorker] discard stale result binding={} version={} latest={}",
                    result.getBindingKey(),
                    result.getRequestVersion(),
                    latestVersion);
                return;
            }
        }
        resultsLock.lock();
        try {
            completedResults.addLast(result);
        } finally {
            resultsLock.unlock();
        }
    }

    private StructureLibPreviewResult execute(StructureLibPreviewTask task) {
        return switch (task.getType()) {
            case ANALYZE_LIMITS -> executeAnalyze(task);
            case BUILD_SELECTION -> executeBuild(task);
        };
    }

    private StructureLibPreviewResult executeAnalyze(StructureLibPreviewTask task) {
        StructureLibImportRequest request = task.getRequest();
        StructureLibRuntimeFacade.ResolvedController controller = StructureLibRuntimeFacade.resolveController(request);
        StructureLibRuntimeFacade.ControlAnalysis controlAnalysis = StructureLibRuntimeFacade
            .analyzeControls(request, controller);
        return StructureLibPreviewResult
            .analyzeSuccess(task.getBindingKey(), task.getRequestVersion(), controlAnalysis);
    }

    private StructureLibPreviewResult executeBuild(StructureLibPreviewTask task) {
        StructureLibImportResult importResult = new StructureLibRuntimeFacade()
            .buildPreviewSelection(task.getRequest(), task.getControlAnalysis());
        if (importResult.isSuccess()) {
            return StructureLibPreviewResult.buildSuccess(
                task.getBindingKey(),
                task.getRequestVersion(),
                Objects.requireNonNull(task.getSelectionKey(), "selectionKey"),
                importResult,
                task.getControlAnalysis());
        }
        return StructureLibPreviewResult.failed(
            task.getType(),
            task.getBindingKey(),
            task.getRequestVersion(),
            task.getSelectionKey(),
            firstMessage(importResult),
            firstMessage(importResult));
    }

    private static String firstMessage(StructureLibImportResult importResult) {
        if (importResult == null || importResult.getErrors()
            .isEmpty()) {
            return "StructureLib preview failed";
        }
        String message = importResult.getErrors()
            .getFirst();
        return message != null && !message.trim()
            .isEmpty() ? message : "StructureLib preview failed";
    }

    private void sleepQuietly() {
        try {
            Thread.sleep(QUEUE_POLL_SLEEP_MS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();
        }
    }
}
