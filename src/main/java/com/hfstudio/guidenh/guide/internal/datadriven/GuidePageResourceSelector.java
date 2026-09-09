package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.compiler.Frontmatter;

public class GuidePageResourceSelector {

    private GuidePageResourceSelector() {}

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
        // O(1) index lookup
        List<DataDrivenGuideLoader.PackCandidate> candidates = DataDrivenGuideLoader.getCandidatesFor(sourceId);

        if (candidates != null && !candidates.isEmpty()) {
            if (candidates.size() == 1) {
                return new SelectedPack(
                    sourceId,
                    candidates.getFirst()
                        .pack());
            }

            DataDrivenGuideLoader.PackCandidate best = candidates.getFirst();
            int bestPriority = readLoadPriority(best.pack(), best.resourceLocation());

            for (int i = 1; i < candidates.size(); i++) {
                DataDrivenGuideLoader.PackCandidate candidate = candidates.get(i);
                int priority = readLoadPriority(candidate.pack(), candidate.resourceLocation());

                if (priority > bestPriority || priority == bestPriority && candidate.order() > best.order()) {
                    best = candidate;
                    bestPriority = priority;
                }
            }

            return new SelectedPack(sourceId, best.pack());
        }

        // Index says it doesn't exist — fast null
        if (DataDrivenGuideLoader.isIndexPopulated()) {
            return null;
        }

        // Index not built yet — emergency full scan
        return selectFullScan(sourceId, resourcePacks);
    }

    /**
     * Full-scan fallback used only when the index hasn't been built yet.
     * Reads bytes for comparison (loadPriority requires frontmatter parsing).
     */
    private static @Nullable SelectedPack selectFullScan(ResourceLocation sourceId,
        Iterable<? extends IResourcePack> resourcePacks) {

        SelectedPack winner = null;
        int winnerPriority = 0;
        int winnerOrder = -1;
        int order = 0;

        for (IResourcePack resourcePack : resourcePacks) {
            byte[] bytes = DataDrivenGuideLoader.readBytes(resourcePack, sourceId);
            if (bytes == null) {
                continue;
            }

            int candidateOrder = order++;
            int candidatePriority = readLoadPriority(sourceId, bytes);

            if (winner == null || candidatePriority > winnerPriority
                || candidatePriority == winnerPriority && candidateOrder > winnerOrder) {

                winner = new SelectedPack(sourceId, resourcePack);
                winnerPriority = candidatePriority;
                winnerOrder = candidateOrder;
            }
        }

        return winner;
    }

    /**
     * Selects the first resource location found from the given candidates.
     * Used by the editor and runtime navigation where the caller has a
     * localized → default → raw fallback chain.
     * <p>
     * Uses the resource-pack index when available and falls back to a targeted
     * scan before the index has been built.
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
                        candidates.getFirst()
                            .pack());
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
            for (ResourceLocation sourceId : sourceIds) {
                if (sourceId == null) continue;
                if (DataDrivenGuideLoader.readBytes(resourcePack, sourceId) != null) {
                    return new SelectedPack(sourceId, resourcePack);
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
        return readLoadPriority(sourceId, new ByteArrayInputStream(bytes));
    }

    private static int readLoadPriority(IResourcePack resourcePack, ResourceLocation sourceId) {
        try {
            return readLoadPriority(sourceId, resourcePack.getInputStream(sourceId));
        } catch (IOException | RuntimeException ignored) {
            return 0;
        }
    }

    private static int readLoadPriority(ResourceLocation sourceId, InputStream input) {
        try (input; var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (!"---".equals(firstLine) && !"\uFEFF---".equals(firstLine)) {
                return 0;
            }

            var frontmatter = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                if ("---".equals(line)) {
                    var navigation = Frontmatter.parse(sourceId, frontmatter.toString())
                        .navigationEntry();

                    return navigation != null ? navigation.loadPriority() : 0;
                }

                if (!frontmatter.isEmpty()) {
                    frontmatter.append('\n');
                }
                frontmatter.append(line);
            }
        } catch (Exception ignored) {}

        return 0;
    }

    /**
     * A resource location found in a specific resource pack.
     */
    @Desugar
    public record SelectedPack(ResourceLocation sourceId, IResourcePack pack) {}
}
