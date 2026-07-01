package com.hfstudio.guidenh.guide.scene.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCache;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheEntry;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCacheKey;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Thread-safe wrapper for GuideSceneStructureCache with concurrent access optimization.
 * Provides parallel scene caching and retrieval without blocking.
 */
public class ConcurrentSceneCacheManager {

    private static final ConcurrentSceneCacheManager INSTANCE = new ConcurrentSceneCacheManager();

    private final ConcurrentHashMap<GuideSceneStructureCacheKey, CompletableFuture<GuideSceneStructureCacheEntry>> pendingLoads = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<GuideSceneStructureCacheKey, CompletableFuture<Void>> pendingStores = new ConcurrentHashMap<>();

    private ConcurrentSceneCacheManager() {}

    public static ConcurrentSceneCacheManager getInstance() {
        return INSTANCE;
    }

    /**
     * Restore scene from cache asynchronously.
     *
     * @param key Cache key
     * @return CompletableFuture with cached entry or null if not found
     */
    public CompletableFuture<GuideSceneStructureCacheEntry> restoreAsync(GuideSceneStructureCacheKey key) {
        CompletableFuture<GuideSceneStructureCacheEntry> existing = pendingLoads.get(key);
        if (existing != null) {
            return existing;
        }

        CompletableFuture<GuideSceneStructureCacheEntry> future = GameSceneConcurrentManager.getInstance()
            .executeSceneLoadWithResult(() -> {
                GuideSceneStructureCacheEntry entry = GuideSceneStructureCache.global()
                    .restore(key);
                GuideDebugLog.info(
                    "[ConcurrentSceneCacheManager] Cache restore {} for key {}",
                    entry != null ? "HIT" : "MISS",
                    key);
                return entry;
            });

        pendingLoads.put(key, future);
        future.whenComplete((result, throwable) -> pendingLoads.remove(key));

        return future;
    }

    /**
     * Store scene to cache asynchronously.
     *
     * @param key   Cache key
     * @param entry Cache entry
     * @return CompletableFuture that completes when storage finishes
     */
    public CompletableFuture<Void> storeAsync(GuideSceneStructureCacheKey key, GuideSceneStructureCacheEntry entry) {
        CompletableFuture<Void> existing = pendingStores.get(key);
        if (existing != null) {
            return existing;
        }

        CompletableFuture<Void> future = GameSceneConcurrentManager.getInstance()
            .executeSceneLoad(() -> {
                GuideSceneStructureCache.global()
                    .put(key, entry);
                GuideDebugLog.info("[ConcurrentSceneCacheManager] Cached entry for key {}", key);
            });

        pendingStores.put(key, future);
        future.whenComplete((result, throwable) -> pendingStores.remove(key));

        return future;
    }

    /**
     * Restore multiple scenes from cache in parallel.
     *
     * @param keys List of cache keys
     * @return CompletableFuture with list of entries (nulls for cache misses)
     */
    public CompletableFuture<List<GuideSceneStructureCacheEntry>> restoreBatchAsync(
        List<GuideSceneStructureCacheKey> keys) {

        List<CompletableFuture<GuideSceneStructureCacheEntry>> futures = new ArrayList<>(keys.size());
        for (GuideSceneStructureCacheKey key : keys) {
            futures.add(restoreAsync(key));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(
                v -> futures.stream()
                    .map(CompletableFuture::join)
                    .toList());
    }

    /**
     * Store multiple scenes to cache in parallel.
     *
     * @param entries List of key-entry pairs
     * @return CompletableFuture that completes when all stores finish
     */
    public CompletableFuture<Void> storeBatchAsync(List<CacheStoreEntry> entries) {
        List<CompletableFuture<Void>> futures = new ArrayList<>(entries.size());
        for (CacheStoreEntry entry : entries) {
            futures.add(storeAsync(entry.key, entry.entry));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Clear all pending operations and cache.
     */
    public void clear() {
        pendingLoads.clear();
        pendingStores.clear();
        GuideSceneStructureCache.global()
            .clear();
        GuideDebugLog.info("[ConcurrentSceneCacheManager] Cleared all cache and pending operations");
    }

    /**
     * Get number of pending load operations.
     */
    public int getPendingLoadCount() {
        return pendingLoads.size();
    }

    /**
     * Get number of pending store operations.
     */
    public int getPendingStoreCount() {
        return pendingStores.size();
    }

    public static class CacheStoreEntry {

        public final GuideSceneStructureCacheKey key;
        public final GuideSceneStructureCacheEntry entry;

        public CacheStoreEntry(GuideSceneStructureCacheKey key, GuideSceneStructureCacheEntry entry) {
            this.key = key;
            this.entry = entry;
        }
    }
}
