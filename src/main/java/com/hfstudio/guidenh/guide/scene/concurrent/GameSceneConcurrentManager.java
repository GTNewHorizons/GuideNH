package com.hfstudio.guidenh.guide.scene.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

import lombok.Getter;

/**
 * Central manager for all GameScene concurrent operations.
 * <p>
 * IMPORTANT: OpenGL rendering MUST happen on the main thread.
 * This manager handles:
 * - Data preparation and computation in parallel
 * - Structure analysis and cache operations
 * - Block/entity metadata collection
 * - Scene loading and initialization
 * <p>
 * Actual OpenGL calls remain on the main thread.
 */
public class GameSceneConcurrentManager {

    private static final GameSceneConcurrentManager INSTANCE = new GameSceneConcurrentManager();
    private static final int WORKER_THREADS = 4;
    private static final int DATA_PREP_BATCH_SIZE = 512;
    private static final int ANALYSIS_BATCH_SIZE = 256;

    private final ExecutorService dataPrepExecutor;
    /**
     * -- GETTER --
     * Get the analysis executor for direct access (used by prewarm system).
     */
    @Getter
    private final ExecutorService analysisExecutor;
    private final AtomicInteger activeDataPrepTasks = new AtomicInteger(0);
    private final AtomicInteger activeAnalysisTasks = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Object> preparedDataCache = new ConcurrentHashMap<>();

    private GameSceneConcurrentManager() {
        ThreadFactory dataPrepThreadFactory = r -> {
            Thread thread = new Thread(r, "guidenh-scene-dataprep-" + System.nanoTime());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        };

        ThreadFactory analysisThreadFactory = r -> {
            Thread thread = new Thread(r, "guidenh-scene-analysis-" + System.nanoTime());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        };

        dataPrepExecutor = Executors.newFixedThreadPool(WORKER_THREADS, dataPrepThreadFactory);
        analysisExecutor = Executors.newFixedThreadPool(WORKER_THREADS, analysisThreadFactory);
    }

    public static GameSceneConcurrentManager getInstance() {
        return INSTANCE;
    }

    /**
     * Process a large collection in parallel batches for data preparation.
     * This is used for non-rendering operations like metadata collection.
     *
     * @param items     Items to process
     * @param batchSize Size of each batch
     * @param processor Function to process each item
     * @param <T>       Item type
     * @return CompletableFuture that completes when all batches are processed
     */
    public <T> CompletableFuture<Void> processDataBatched(List<T> items, int batchSize, Consumer<T> processor) {

        if (items.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int totalBatches = (items.size() + batchSize - 1) / batchSize;

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int startIndex = batchIndex * batchSize;
            int endIndex = Math.min(startIndex + batchSize, items.size());
            List<T> batch = items.subList(startIndex, endIndex);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                activeDataPrepTasks.incrementAndGet();
                try {
                    for (T item : batch) {
                        processor.accept(item);
                    }
                } catch (Throwable t) {
                    GuideDebugLog.error("[GameSceneConcurrentManager] Data prep batch processing failed", t);
                } finally {
                    activeDataPrepTasks.decrementAndGet();
                }
            }, dataPrepExecutor);

            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Prepare block metadata in parallel (NOT actual rendering).
     * Collects information needed for rendering, which happens on main thread.
     *
     * @param blocks        List of block positions [x, y, z]
     * @param dataExtractor Function to extract data from each block position
     * @param <R>           Result type
     * @return CompletableFuture with list of extracted data
     */
    public <R> CompletableFuture<List<R>> prepareBlockDataParallel(List<int[]> blocks,
        Function<int[], R> dataExtractor) {

        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<R>> futures = new ArrayList<>(blocks.size());

            for (int[] block : blocks) {
                CompletableFuture<R> future = CompletableFuture.supplyAsync(() -> {
                    activeDataPrepTasks.incrementAndGet();
                    try {
                        return dataExtractor.apply(block);
                    } catch (Throwable t) {
                        GuideDebugLog.error("[GameSceneConcurrentManager] Block data extraction failed", t);
                        return null;
                    } finally {
                        activeDataPrepTasks.decrementAndGet();
                    }
                }, dataPrepExecutor);
                futures.add(future);
            }

            return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
        });
    }

    /**
     * Analyze blocks in parallel for statistics and metadata.
     *
     * @param blocks   List of block positions
     * @param analyzer Function to analyze each block
     * @param <T>      Analysis result type
     * @return CompletableFuture with list of analysis results
     */
    public <T> CompletableFuture<List<T>> analyzeBlocksParallel(List<int[]> blocks, Function<int[], T> analyzer) {

        if (blocks.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<List<T>>> futures = new ArrayList<>();
            int batchSize = ANALYSIS_BATCH_SIZE;
            int totalBatches = (blocks.size() + batchSize - 1) / batchSize;

            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int startIndex = batchIndex * batchSize;
                int endIndex = Math.min(startIndex + batchSize, blocks.size());
                List<int[]> batch = blocks.subList(startIndex, endIndex);

                CompletableFuture<List<T>> batchFuture = CompletableFuture.supplyAsync(() -> {
                    activeAnalysisTasks.incrementAndGet();
                    try {
                        List<T> results = new ArrayList<>(batch.size());
                        for (int[] block : batch) {
                            T result = analyzer.apply(block);
                            if (result != null) {
                                results.add(result);
                            }
                        }
                        return results;
                    } catch (Throwable t) {
                        GuideDebugLog.error("[GameSceneConcurrentManager] Block analysis batch failed", t);
                        return List.of();
                    } finally {
                        activeAnalysisTasks.decrementAndGet();
                    }
                }, analysisExecutor);

                futures.add(batchFuture);
            }

            return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
        });
    }

    /**
     * Execute a scene loading task asynchronously.
     *
     * @param task Task to execute
     * @return CompletableFuture that completes when task finishes
     */
    public CompletableFuture<Void> executeSceneLoad(Runnable task) {
        return CompletableFuture.runAsync(() -> {
            activeAnalysisTasks.incrementAndGet();
            try {
                task.run();
            } catch (Throwable t) {
                GuideDebugLog.error("[GameSceneConcurrentManager] Scene load task failed", t);
            } finally {
                activeAnalysisTasks.decrementAndGet();
            }
        }, analysisExecutor);
    }

    /**
     * Execute a scene loading task and return result.
     *
     * @param task Task to execute
     * @param <T>  Result type
     * @return CompletableFuture with result
     */
    public <T> CompletableFuture<T> executeSceneLoadWithResult(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            activeAnalysisTasks.incrementAndGet();
            try {
                return task.call();
            } catch (Throwable t) {
                GuideDebugLog.error("[GameSceneConcurrentManager] Scene load task failed", t);
                return null;
            } finally {
                activeAnalysisTasks.decrementAndGet();
            }
        }, analysisExecutor);
    }

    /**
     * Execute multiple scene loading tasks in parallel.
     *
     * @param tasks List of tasks to execute
     * @return CompletableFuture that completes when all tasks finish
     */
    public CompletableFuture<Void> executeSceneLoadsParallel(List<Runnable> tasks) {
        if (tasks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>(tasks.size());
        for (Runnable task : tasks) {
            futures.add(executeSceneLoad(task));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Cache prepared data for later use.
     *
     * @param key  Cache key
     * @param data Data to cache
     */
    public void cachePreparedData(String key, Object data) {
        preparedDataCache.put(key, data);
    }

    /**
     * Get cached prepared data.
     *
     * @param key Cache key
     * @param <T> Data type
     * @return Cached data or null
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedData(String key) {
        return (T) preparedDataCache.get(key);
    }

    /**
     * Clear prepared data cache.
     */
    public void clearCache() {
        preparedDataCache.clear();
    }

    /**
     * Get number of active data preparation tasks.
     */
    public int getActiveDataPrepTaskCount() {
        return activeDataPrepTasks.get();
    }

    /**
     * Get number of active analysis tasks.
     */
    public int getActiveAnalysisTaskCount() {
        return activeAnalysisTasks.get();
    }

    /**
     * Wait for all active data preparation tasks to complete.
     *
     * @param timeoutMs Maximum time to wait in milliseconds
     * @return true if all tasks completed, false if timeout
     */
    public boolean awaitDataPrepCompletion(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (activeDataPrepTasks.get() > 0 || activeAnalysisTasks.get() > 0) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                return false;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * Shutdown all executors gracefully.
     */
    public void shutdown() {
        GuideDebugLog.infoAlways("[GameSceneConcurrentManager] Shutting down executors");
        dataPrepExecutor.shutdown();
        analysisExecutor.shutdown();

        try {
            if (!dataPrepExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                dataPrepExecutor.shutdownNow();
            }
            if (!analysisExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                analysisExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            dataPrepExecutor.shutdownNow();
            analysisExecutor.shutdownNow();
            Thread.currentThread()
                .interrupt();
        }
    }

    /**
     * Get statistics about concurrent operations.
     */
    public ConcurrentStats getStats() {
        return new ConcurrentStats(
            activeDataPrepTasks.get(),
            activeAnalysisTasks.get(),
            preparedDataCache.size(),
            WORKER_THREADS,
            DATA_PREP_BATCH_SIZE,
            ANALYSIS_BATCH_SIZE);
    }

    @Getter
    public static class ConcurrentStats {

        private final int activeDataPrepTasks;
        private final int activeAnalysisTasks;
        private final int cachedDataCount;
        private final int workerThreads;
        private final int dataPrepBatchSize;
        private final int analysisBatchSize;

        public ConcurrentStats(int activeDataPrepTasks, int activeAnalysisTasks, int cachedDataCount, int workerThreads,
            int dataPrepBatchSize, int analysisBatchSize) {
            this.activeDataPrepTasks = activeDataPrepTasks;
            this.activeAnalysisTasks = activeAnalysisTasks;
            this.cachedDataCount = cachedDataCount;
            this.workerThreads = workerThreads;
            this.dataPrepBatchSize = dataPrepBatchSize;
            this.analysisBatchSize = analysisBatchSize;
        }

    }
}
