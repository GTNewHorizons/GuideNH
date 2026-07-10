package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.ClientProxy;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
import com.hfstudio.guidenh.guide.internal.compile.CompileWorker;
import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.datadriven.GuidePageResourceSelector;
import com.hfstudio.guidenh.guide.internal.localization.GuideLocalizedPageSourceResolver;
import com.hfstudio.guidenh.guide.internal.localization.GuidePageLanguageIndex;
import com.hfstudio.guidenh.guide.internal.localization.GuideResourceLanguageIndex;
import com.hfstudio.guidenh.guide.internal.recipe.NeiAnimationTicker;
import com.hfstudio.guidenh.guide.internal.recipe.RecipeCache;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.latex.GuideLatexTextureCache;
import com.hfstudio.guidenh.guide.mediawiki.MediaWikiTranslationStats;
import com.hfstudio.guidenh.guide.render.GuidePageTexture;
import com.hfstudio.guidenh.guide.scene.cache.GuideSceneStructureCache;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.integration.structurelib.StructureLibElementTooltipResolver;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;

public class GuideLightweightReloadService {

    private GuideLightweightReloadService() {}

    public static void reloadDevelopmentGuides() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return;
        }
        reloadGuides(minecraft.getResourceManager());
    }

    public static void reloadGuides(IResourceManager resourceManager) {
        GuideDebugLog.info("[GuideNH] [GuideLightweightReloadService] Reloading guide data...");
        long startedAt = System.nanoTime();
        var activeResourcePacks = DataDrivenGuideLoader.getActiveResourcePacks(resourceManager);
        DataDrivenGuideLoader.clearCaches();
        RecipeCache.clear();
        NeiAnimationTicker.clear();
        GuidePageTexture.clear();
        GuideResourceAccess.clearCache();
        GuidePageLanguageIndex.clear();
        GuideResourceLanguageIndex.clear();
        GuideLatexTextureCache.INSTANCE.clearAll();
        GuideSceneStructureCache.global()
            .clear();
        StructureLibRuntimeFacade.CONTROL_ANALYSIS_CACHE.clear();
        StructureLibRuntimeFacade.ANALYSIS_SNAPSHOT_CACHE.clear();
        StructureLibRuntimeFacade.IMPORT_RESULT_CACHE.clear();
        StructureLibElementTooltipResolver.BLOCK_CANDIDATE_CACHE.clear();
        StructureLibElementTooltipResolver.HATCH_CANDIDATE_CACHE.clear();
        ClientProxy.getLytHost()
            .clearPageCaches();
        ClientProxy.getStructureLibPreviewWorker()
            .reset();

        // Single-pass scan: builds page index, discovers guide definitions,
        // collects page paths and language keys — all in one IO pass.
        long stageStartedAt = System.nanoTime();
        DataDrivenGuideLoader.ScanResult scan = DataDrivenGuideLoader
            .scanAndBuildAll(DataDrivenGuideLoader.AUTO_GUIDE_FOLDER, activeResourcePacks);
        GuideRegistry.setDataDriven(scan.guides());
        MediaWikiTranslationStats.invalidateCache();
        long dataDrivenLoadNs = System.nanoTime() - stageStartedAt;

        var guidePages = new HashMap<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>>();

        String language = LangUtil.getCurrentLanguage();
        GuideDebugLog.warnAlways(
            "[GuideNH] [GuideLightweightReloadService] reloadGuides currentLanguage='{}' (raw gameSettings.language='{}')",
            language,
            Minecraft.getMinecraft() != null && Minecraft.getMinecraft().gameSettings != null
                ? Minecraft.getMinecraft().gameSettings.language
                : "null");

        stageStartedAt = System.nanoTime();
        for (var guide : GuideRegistry.getAll()) {
            var pages = loadPages(
                resourceManager,
                guide.getId(),
                guide.getContentRootFolder(),
                guide.getDefaultLanguage(),
                language,
                scan.pagePaths(),
                activeResourcePacks);
            guidePages.put(guide.getId(), pages);
        }
        long pageLoadNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        for (var entry : guidePages.entrySet()) {
            GuideRegistry.updatePages(entry.getKey(), entry.getValue(), false);
        }
        GuideRegistry.invalidateMergedNavigationTree();
        // Trigger background compilation of all loaded pages
        CompileWorker worker = ClientProxy.getWorker();
        var allPageIds = new ArrayList<ResourceLocation>();
        for (var pages : guidePages.values()) {
            allPageIds.addAll(pages.keySet());
        }
        if (!allPageIds.isEmpty()) {
            worker.reset(allPageIds);
        }
        ClientProxy.getStructureLibPreviewBootstrap()
            .scheduleReloadPrewarm();
        long registryUpdateNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        try {
            GuideME.getSearch()
                .indexAll();
        } catch (Throwable t) {
            GuideDebugLog
                .warnAlways("[GuideNH] [GuideLightweightReloadService] Failed to reindex search after reload", t);
        }
        long searchIndexNs = System.nanoTime() - stageStartedAt;

        int loadedPageCount = countLoadedPages(guidePages);
        int loadedLanguageCount = countLoadedLanguages(guidePages);
        long totalNs = System.nanoTime() - startedAt;

        GuideDebugLog.warnAlways(
            "[GuideNH] [GuideLightweightReloadService] Guide reload complete, loaded {} guides, {} pages, {} languages in {} ms (dataDrivenLoadMs={}, pageLoadMs={}, registryUpdateMs={}, searchIndexMs={})",
            guidePages.size(),
            loadedPageCount,
            loadedLanguageCount,
            totalNs / 1_000_000L,
            dataDrivenLoadNs / 1_000_000L,
            pageLoadNs / 1_000_000L,
            registryUpdateNs / 1_000_000L,
            searchIndexNs / 1_000_000L);
        GuideDebugLog.warnAlways(
            "[GuideNH] [GuideLightweightReloadService] select() stats during this reload: {}",
            GuidePageResourceSelector.formatSelectStats());
        GuidePageResourceSelector.resetSelectStats();
    }

    /**
     * Scans the guide folder tree and loads all markdown files under {@code assets/<namespace>/<folder>/_<lang>/...}.
     */
    public static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager,
        ResourceLocation guideId, String folder, String defaultLanguage, @Nullable String currentLanguage) {
        // Runtime fallback: scan paths for this specific guide only (not all packs)
        var activePacks = DataDrivenGuideLoader.getActiveResourcePacks(resourceManager);
        var paths = DataDrivenGuideLoader.discoverPagePaths(guideId, folder, activePacks);
        var singleGuidePaths = new LinkedHashMap<String, LinkedHashSet<String>>();
        if (!paths.isEmpty()) {
            singleGuidePaths.put(guideId.getResourceDomain(), new LinkedHashSet<>(paths));
        }
        return loadPages(
            resourceManager,
            guideId,
            folder,
            defaultLanguage,
            currentLanguage,
            singleGuidePaths,
            activePacks);
    }

    public static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager,
        ResourceLocation guideId, String folder, String defaultLanguage, @Nullable String currentLanguage,
        Map<String, LinkedHashSet<String>> allPagePaths, Iterable<? extends IResourcePack> activeResourcePacks) {
        long startedAt = System.nanoTime();
        var pages = new HashMap<ResourceLocation, ParsedGuidePage>();
        LinkedHashSet<String> pagePaths = allPagePaths != null ? allPagePaths.get(guideId.getResourceDomain()) : null;
        if (pagePaths == null || pagePaths.isEmpty()) {
            pagePaths = new LinkedHashSet<>();
        }
        String lang = currentLanguage != null ? currentLanguage : defaultLanguage;
        String sourceNamespace = guideId.getResourceDomain();
        String sourcePack = "resources:" + sourceNamespace;
        int localizedHits = 0;
        int defaultLanguageHits = 0;
        int rawSourceHits = 0;
        int failedLoads = 0;

        for (var pagePath : pagePaths) {
            long pageStartedAt = System.nanoTime();
            ResourceLocation pageId = new ResourceLocation(sourceNamespace, pagePath);
            PageLoadResult loadResult = loadPage(
                resourceManager,
                sourcePack,
                sourceNamespace,
                folder,
                defaultLanguage,
                lang,
                pagePath,
                pageId,
                activeResourcePacks);
            long pageNs = System.nanoTime() - pageStartedAt;
            if (pageNs > 50_000_000) {
                GuideDebugLog.warnAlways(
                    "[GuideNH] [GuideLightweightReloadService] Slow page {} took {} ms",
                    pageId,
                    pageNs / 1_000_000L);
            }
            ParsedGuidePage parsed = loadResult != null ? loadResult.page() : null;
            if (parsed == null) {
                failedLoads++;
                GuideDebugLog.warn("[GuideNH] [GuideLightweightReloadService] Failed to load guide page {}", pageId);
                continue;
            }
            switch (loadResult.kind()) {
                case LOCALIZED:
                    localizedHits++;
                    break;
                case DEFAULT_LANGUAGE:
                    defaultLanguageHits++;
                    break;
                case RAW_SOURCE:
                    rawSourceHits++;
                    break;
                default:
                    break;
            }
            pages.put(pageId, parsed);
        }

        long totalNs = System.nanoTime() - startedAt;
        GuideDebugLog.warnAlways(
            "[GuideNH] [GuideLightweightReloadService] Loaded {} pages for guide {} folder {} requestedLanguage={} defaultLanguage={} discoveredPaths={} localizedHits={} defaultLanguageHits={} rawSourceHits={} failedLoads={} durationMs={}",
            pages.size(),
            guideId,
            folder,
            lang,
            defaultLanguage,
            pagePaths.size(),
            localizedHits,
            defaultLanguageHits,
            rawSourceHits,
            failedLoads,
            totalNs / 1_000_000L);
        return pages;
    }

    static LinkedHashSet<String> pagePathsForGuide(ResourceLocation guideId, String folder,
        Map<String, LinkedHashMap<String, LinkedHashSet<String>>> pagePathCache,
        Function<String, LinkedHashMap<String, LinkedHashSet<String>>> discoverPagePaths) {
        var pathsByNamespace = pagePathCache.computeIfAbsent(folder, discoverPagePaths);
        var pagePaths = pathsByNamespace.get(guideId.getResourceDomain());
        return pagePaths != null ? pagePaths : new LinkedHashSet<>();
    }

    @Nullable
    private static PageLoadResult tryLoadPage(String sourcePack, String requestedLanguage, String sourceLanguage,
        String namespace, String folder, String pagePath, ResourceLocation pageId, LoadKind kind,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        ParsedGuidePage page = tryParsePageCandidate(
            sourcePack,
            requestedLanguage,
            folder,
            pageId,
            new ResourceLocation(namespace, folder + "/_" + sourceLanguage + "/" + pagePath),
            activeResourcePacks);
        return page != null ? new PageLoadResult(page, kind) : null;
    }

    public static @Nullable ParsedGuidePage loadPageForLanguage(ResourceLocation guideId, String folder,
        String requestedLanguage, String sourceLanguage, ResourceLocation pageId) {
        String normalizedRequestedLanguage = LangUtil.normalizeLanguage(requestedLanguage);
        String normalizedSourceLanguage = LangUtil.normalizeLanguage(sourceLanguage);
        String sourcePack = "resources:" + guideId.getResourceDomain();
        PageLoadResult result = tryLoadPage(
            sourcePack,
            normalizedRequestedLanguage,
            normalizedSourceLanguage,
            guideId.getResourceDomain(),
            folder,
            pageId.getResourcePath(),
            pageId,
            LoadKind.LOCALIZED,
            DataDrivenGuideLoader.getActiveResourcePacks());
        return result != null ? result.page() : null;
    }

    public static @Nullable ParsedGuidePage tryLoadNeutralPageForExport(IResourceManager resourceManager,
        String sourcePack, String requestedLanguage, String contentRootFolder, ResourceLocation pageId,
        ResourceLocation sourceId) {
        return tryParsePage(
            resourceManager,
            sourcePack,
            LangUtil.normalizeLanguage(requestedLanguage),
            contentRootFolder,
            pageId,
            sourceId,
            DataDrivenGuideLoader.getActiveResourcePacks());
    }

    @Nullable
    private static ParsedGuidePage tryParsePage(IResourceManager resourceManager, String sourcePack, String language,
        String contentRootFolder, ResourceLocation pageId, ResourceLocation sourceId,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        GuidePageResourceSelector.SelectedPack selected = GuidePageResourceSelector
            .select(sourceId, activeResourcePacks);
        if (selected == null) {
            return null;
        }
        byte[] bytes = DataDrivenGuideLoader.readBytes(selected.pack(), sourceId);
        if (bytes == null) {
            return null;
        }
        return parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, bytes);
    }

    @Nullable
    private static ParsedGuidePage tryParsePageCandidate(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, ResourceLocation sourceId, Iterable<? extends IResourcePack> activeResourcePacks) {
        long t0 = System.nanoTime();
        GuidePageResourceSelector.SelectedPack selected = GuidePageResourceSelector
            .select(sourceId, activeResourcePacks);
        long t1 = System.nanoTime();
        if (selected == null) {
            return null;
        }
        byte[] bytes = DataDrivenGuideLoader.readBytes(selected.pack(), sourceId);
        long t2 = System.nanoTime();
        if (bytes == null) {
            return null;
        }
        ParsedGuidePage result = parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, bytes);
        long t3 = System.nanoTime();
        long totalUs = (t3 - t0) / 1000;
        if (totalUs > 10_000) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [GuideLightweightReloadService] Page load detail {} select={}us readBytes={}us parse={}us total={}us",
                sourceId,
                (t1 - t0) / 1000,
                (t2 - t1) / 1000,
                (t3 - t2) / 1000,
                totalUs);
        }
        return result;
    }

    @Nullable
    private static ParsedGuidePage parsePageBytes(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, ResourceLocation sourceId, byte[] bytes) {
        long t0 = System.nanoTime();
        try {
            ParsedGuidePage result = GuideLocalizedPageSourceResolver
                .parseFrontmatterOnly(sourcePack, language, contentRootFolder, pageId, bytes);
            long t1 = System.nanoTime();
            long parseUs = (t1 - t0) / 1000;
            if (parseUs > 5_000) {
                GuideDebugLog
                    .warnAlways("[GuideNH] [GuideLightweightReloadService] Slow parse {} took {}us", sourceId, parseUs);
            }
            return result;
        } catch (Exception ex) {
            long t1 = System.nanoTime();
            GuideDebugLog.warnAlways(
                "[GuideNH] [GuideLightweightReloadService] Error parsing page {} from {} after {}us",
                pageId,
                sourceId,
                (t1 - t0) / 1000,
                ex);
            return null;
        }
    }

    static byte @Nullable [] selectPageCandidate(ResourceLocation sourceId) {
        return selectPageCandidate(sourceId, DataDrivenGuideLoader.getActiveResourcePacks());
    }

    static byte @Nullable [] selectPageCandidate(ResourceLocation sourceId,
        Iterable<? extends IResourcePack> resourcePacks) {
        GuidePageResourceSelector.SelectedPack winner = GuidePageResourceSelector.select(sourceId, resourcePacks);
        if (winner == null) return null;
        return DataDrivenGuideLoader.readBytes(winner.pack(), sourceId);
    }

    static int readLoadPriority(ResourceLocation sourceId, byte[] bytes) {
        return GuidePageResourceSelector.readLoadPriority(sourceId, bytes);
    }

    @Nullable
    private static PageLoadResult loadPage(IResourceManager resourceManager, String sourcePack, String namespace,
        String folder, String defaultLanguage, String requestedLanguage, String pagePath, ResourceLocation pageId,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        PageLoadResult localized = tryLoadPage(
            sourcePack,
            requestedLanguage,
            requestedLanguage,
            namespace,
            folder,
            pagePath,
            pageId,
            LoadKind.LOCALIZED,
            activeResourcePacks);
        if (localized != null) {
            return localized;
        }
        if (!requestedLanguage.equals(defaultLanguage)) {
            PageLoadResult fallback = tryLoadPage(
                sourcePack,
                requestedLanguage,
                defaultLanguage,
                namespace,
                folder,
                pagePath,
                pageId,
                LoadKind.DEFAULT_LANGUAGE,
                activeResourcePacks);
            if (fallback != null) {
                return fallback;
            }
        }
        ParsedGuidePage rawPage = tryParsePage(
            resourceManager,
            sourcePack,
            requestedLanguage,
            folder,
            pageId,
            new ResourceLocation(namespace, folder + "/" + pagePath),
            activeResourcePacks);
        return rawPage != null ? new PageLoadResult(rawPage, LoadKind.RAW_SOURCE) : null;
    }

    private static int countLoadedPages(Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> guidePages) {
        int total = 0;
        for (var pages : guidePages.values()) {
            total += pages.size();
        }
        return total;
    }

    private static int countLoadedLanguages(Map<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>> guidePages) {
        var languages = new LinkedHashSet<String>();
        for (var pages : guidePages.values()) {
            for (var parsedPage : pages.values()) {
                languages.add(parsedPage.getLanguage());
            }
        }
        return languages.size();
    }

    private enum LoadKind {
        LOCALIZED,
        DEFAULT_LANGUAGE,
        RAW_SOURCE
    }

    @Desugar
    private record PageLoadResult(ParsedGuidePage page, LoadKind kind) {}
}
