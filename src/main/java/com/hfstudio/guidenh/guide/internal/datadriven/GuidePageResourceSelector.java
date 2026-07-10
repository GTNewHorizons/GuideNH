package com.hfstudio.guidenh.guide.internal.datadriven;

import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.Frontmatter;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;

public class GuidePageResourceSelector {

    private static long totalSelectNs = 0;
    private static long totalSelectCalls = 0;
    private static long totalPacksChecked = 0;
    private static long totalHits = 0;

    private GuidePageResourceSelector() {}

    public static void resetSelectStats() {
        totalSelectNs = 0;
        totalSelectCalls = 0;
        totalPacksChecked = 0;
        totalHits = 0;
    }

    public static String formatSelectStats() {
        if (totalSelectCalls == 0) return "no select() calls";
        return String.format(
            "select() called %d times, total %.0f ms, avg %.1f ms/call, packs checked %d (avg %.0f/call), hits %d",
            totalSelectCalls,
            totalSelectNs / 1_000_000.0,
            totalSelectNs / 1_000_000.0 / totalSelectCalls,
            totalPacksChecked,
            (double) totalPacksChecked / totalSelectCalls,
            totalHits);
    }

    /**
     * Selects the best resource pack that contains the given resource location.
     * <p>
     * Uses the pre-built reverse index for O(1) lookup. If the index is ready
     * but the key is absent, returns null immediately (the resource is guaranteed
     * not to exist in any indexed pack).
     * <p>
     * Only falls back to a full scan if the index has not been built yet
     * (e.g. during very early init).
     */
    public static @Nullable SelectedPack select(ResourceLocation sourceId) {
        return select(sourceId, DataDrivenGuideLoader.getActiveResourcePacks());
    }

    public static @Nullable SelectedPack select(ResourceLocation sourceId,
        Iterable<? extends IResourcePack> resourcePacks) {
        long startedAt = System.nanoTime();

        // O(1) index lookup
        List<DataDrivenGuideLoader.PackCandidate> candidates = DataDrivenGuideLoader.getCandidatesFor(sourceId);
        if (candidates != null && !candidates.isEmpty()) {
            DataDrivenGuideLoader.PackCandidate best = candidates.get(0);
            for (int i = 1; i < candidates.size(); i++) {
                if (candidates.get(i)
                    .shouldReplace(best)) {
                    best = candidates.get(i);
                }
            }
            totalHits++;
            int packsChecked = candidates.size();
            long selectNs = System.nanoTime() - startedAt;
            totalSelectNs += selectNs;
            totalSelectCalls++;
            totalPacksChecked += packsChecked;
            return new SelectedPack(sourceId, best.pack(), best.loadPriority());
        }

        // Index says it doesn't exist — fast null
        if (DataDrivenGuideLoader.isIndexPopulated()) {
            long selectNs = System.nanoTime() - startedAt;
            totalSelectNs += selectNs;
            totalSelectCalls++;
            totalPacksChecked += 0;
            return null;
        }

        // Index not built yet — emergency full scan
        return selectFullScan(sourceId, resourcePacks, startedAt);
    }

    /**
     * Full-scan fallback used only when the index hasn't been built yet.
     * Reads bytes for comparison (loadPriority requires frontmatter parsing).
     */
    private static @Nullable SelectedPack selectFullScan(ResourceLocation sourceId,
        Iterable<? extends IResourcePack> resourcePacks, long startedAt) {
        SelectedPack winner = null;
        byte[] winnerBytes = null;
        int order = 0;
        int packsChecked = 0;
        for (IResourcePack resourcePack : resourcePacks) {
            packsChecked++;
            byte[] bytes = DataDrivenGuideLoader.readBytes(resourcePack, sourceId);
            if (bytes == null) {
                continue;
            }
            totalHits++;
            int candidateOrder = order++;
            int candidatePriority = readLoadPriority(sourceId, bytes);
            if (winner == null) {
                winner = new SelectedPack(sourceId, resourcePack, candidatePriority);
                winnerBytes = bytes;
                continue;
            }
            DataDrivenGuideLoader.PackCandidate candidate = new DataDrivenGuideLoader.PackCandidate(
                resourcePack,
                candidatePriority,
                candidateOrder);
            DataDrivenGuideLoader.PackCandidate current = new DataDrivenGuideLoader.PackCandidate(
                winner.pack(),
                winner.loadPriority(),
                order - 2);
            if (candidate.shouldReplace(current)) {
                winner = new SelectedPack(sourceId, resourcePack, candidatePriority);
                winnerBytes = bytes;
            }
        }
        long selectNs = System.nanoTime() - startedAt;
        totalSelectNs += selectNs;
        totalSelectCalls++;
        totalPacksChecked += packsChecked;
        return winner;
    }

    /**
     * Selects the first resource location found from the given candidates.
     * Used by the editor and runtime navigation where the caller has a
     * localized → default → raw fallback chain.
     * <p>
     * This does NOT use the index — it does a targeted scan of only the given
     * candidate IDs. For bulk page loading during reload, use {@link #select}
     * instead.
     */
    public static @Nullable SelectedPack selectFirstPresent(Iterable<? extends IResourcePack> resourcePacks,
        ResourceLocation... sourceIds) {
        if (sourceIds == null || sourceIds.length == 0) {
            return null;
        }

        // Fast path: try index first
        if (DataDrivenGuideLoader.isIndexPopulated()) {
            for (ResourceLocation sourceId : sourceIds) {
                if (sourceId == null) continue;
                var candidates = DataDrivenGuideLoader.getCandidatesFor(sourceId);
                if (candidates != null && !candidates.isEmpty()) {
                    return new SelectedPack(
                        sourceId,
                        candidates.get(0)
                            .pack(),
                        candidates.get(0)
                            .loadPriority());
                }
            }
            return null;
        }

        // Slow path: full scan
        return selectFirstPresentFullScan(resourcePacks, sourceIds);
    }

    private static @Nullable SelectedPack selectFirstPresentFullScan(Iterable<? extends IResourcePack> resourcePacks,
        ResourceLocation... sourceIds) {
        for (IResourcePack resourcePack : resourcePacks) {
            for (int i = 0; i < sourceIds.length; i++) {
                ResourceLocation sourceId = sourceIds[i];
                if (sourceId == null) continue;
                if (DataDrivenGuideLoader.readBytes(resourcePack, sourceId) != null) {
                    return new SelectedPack(sourceId, resourcePack, 0);
                }
            }
        }
        return null;
    }

    public static @Nullable SelectedPack selectFirstPresent(List<IResourcePack> resourcePacks,
        ResourceLocation... sourceIds) {
        return selectFirstPresent((Iterable<? extends IResourcePack>) resourcePacks, sourceIds);
    }

    /**
     * Parses loadPriority from frontmatter for a given page's bytes.
     * Used during full-scan fallback and by MediaWikiSpecialDataIndexer.
     */
    public static int readLoadPriority(ResourceLocation sourceId, byte[] bytes) {
        String source = new String(bytes, StandardCharsets.UTF_8);
        String yamlText = PageCompiler.extractFrontmatterText(PageCompiler.normalizeLineEndings(stripBom(source)));
        if (yamlText == null) {
            return 0;
        }
        try {
            var frontmatter = Frontmatter.parse(sourceId, yamlText);
            var navigation = frontmatter.navigationEntry();
            return navigation != null ? navigation.loadPriority() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String stripBom(String source) {
        return source.startsWith("﻿") ? source.substring(1) : source;
    }

    /**
     * A resource location found in a specific resource pack.
     * <p>
     * Unlike the old {@code SelectedPageResource}, this does NOT carry the page bytes —
     * the caller is expected to call {@link DataDrivenGuideLoader#readBytes} separately.
     */
    @Desugar
    public record SelectedPack(ResourceLocation sourceId, IResourcePack pack, int loadPriority) {}

    /**
     * @deprecated Use {@link SelectedPack} instead. Bytes are no longer included;
     *             read them separately via {@link DataDrivenGuideLoader#readBytes}.
     */
    @Deprecated
    @Desugar
    public record SelectedPageResource(ResourceLocation sourceId, IResourcePack resourcePack, byte[] bytes,
        int loadPriority, int order) {

        public boolean shouldReplace(SelectedPageResource previous) {
            return loadPriority > previous.loadPriority()
                || loadPriority == previous.loadPriority() && order > previous.order();
        }

        public SelectedPageResource withLoadPriority(int resolvedLoadPriority) {
            return loadPriority == resolvedLoadPriority ? this
                : new SelectedPageResource(sourceId, resourcePack, bytes, resolvedLoadPriority, order);
        }
    }
}
