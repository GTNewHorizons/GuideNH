package com.hfstudio.guidenh.guide.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.ClientProxy;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.ParsedGuidePage;
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
import com.hfstudio.guidenh.guide.scene.preview.StructureLibDefinitionCache;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

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
        var activeResourcePacks = DataDrivenGuideLoader.getActiveResourcePacks(resourceManager);
        LazyParsedGuidePage.clearResidentPages();
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
        StructureLibDefinitionCache.getInstance()
            .refresh();
        ClientProxy.getLytHost()
            .clearPageCaches();

        DataDrivenGuideLoader.ScanResult scan = DataDrivenGuideLoader
            .scanAndBuildAll(DataDrivenGuideLoader.AUTO_GUIDE_FOLDER, activeResourcePacks);
        GuideRegistry.setDataDriven(scan.guides());
        MediaWikiTranslationStats.invalidateCache();

        var guidePages = new HashMap<ResourceLocation, Map<ResourceLocation, ParsedGuidePage>>();

        String language = LangUtil.getCurrentLanguage();

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

        for (var entry : guidePages.entrySet()) {
            GuideRegistry.updatePages(entry.getKey(), entry.getValue(), false);
        }
        GuideRegistry.invalidateMergedNavigationTree();
        // Page ASTs are lazy. Clearing the old compiled results is enough here; queuing every
        // page would immediately defeat lazy parsing and make every resource reload pay the full
        // Micromark/compile cost. GuideScreen prioritizes the page the player actually opens.
        ClientProxy.getWorker()
            .clearCompiledPages();

        try {
            GuideME.getSearch()
                .indexAll();
        } catch (Throwable t) {
            GuideDebugLog.warn("[GuideNH] [GuideLightweightReloadService] Failed to reindex search after reload", t);
        }
    }

    public static Map<ResourceLocation, ParsedGuidePage> loadPages(IResourceManager resourceManager,
        ResourceLocation guideId, String folder, String defaultLanguage, @Nullable String currentLanguage) {
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
        var pages = new HashMap<ResourceLocation, ParsedGuidePage>();
        LinkedHashSet<String> pagePaths = allPagePaths != null ? allPagePaths.get(guideId.getResourceDomain()) : null;
        if (pagePaths == null || pagePaths.isEmpty()) {
            pagePaths = new LinkedHashSet<>();
        }
        String lang = currentLanguage != null ? currentLanguage : defaultLanguage;
        String sourceNamespace = guideId.getResourceDomain();

        for (var pagePath : pagePaths) {
            ResourceLocation pageId = new ResourceLocation(sourceNamespace, pagePath);
            PageLoadResult result = loadPage(
                resourceManager,
                sourceNamespace,
                folder,
                defaultLanguage,
                lang,
                pagePath,
                pageId,
                activeResourcePacks);
            if (result != null) {
                pages.put(pageId, result.page());
            }
        }
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

    public static @Nullable ParsedGuidePage loadPageForLanguage(ResourceLocation guideId, String folder,
        String requestedLanguage, String sourceLanguage, ResourceLocation pageId) {
        String sourcePack = "resources:" + guideId.getResourceDomain();
        PageLoadResult result = tryLoadPage(
            sourcePack,
            LangUtil.normalizeLanguage(requestedLanguage),
            LangUtil.normalizeLanguage(sourceLanguage),
            guideId.getResourceDomain(),
            folder,
            pageId.getResourcePath(),
            pageId,
            LoadKind.LOCALIZED,
            DataDrivenGuideLoader.getActiveResourcePacks());
        return result != null ? result.page() : null;
    }

    @Nullable
    private static ParsedGuidePage tryParsePageCandidate(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, ResourceLocation sourceId, Iterable<? extends IResourcePack> activeResourcePacks) {
        GuidePageResourceSelector.SelectedPack selected = GuidePageResourceSelector
            .select(sourceId, activeResourcePacks);
        if (selected == null) return null;
        byte[] bytes = DataDrivenGuideLoader.readBytes(selected.pack(), sourceId);
        if (bytes == null) return null;
        return parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, bytes, selected);
    }

    @Nullable
    private static ParsedGuidePage parsePageBytes(String sourcePack, String language, String contentRootFolder,
        ResourceLocation pageId, ResourceLocation sourceId, byte[] bytes,
        GuidePageResourceSelector.SelectedPack selected) {
        try {
            GuideLocalizedPageSourceResolver.ResolvedGuidePageSource resolved = GuideLocalizedPageSourceResolver
                .resolveFrontmatterOnly(language, contentRootFolder, pageId, bytes);
            ParsedGuidePage frontmatter = PageCompiler
                .parseFrontmatterOnly(sourcePack, language, pageId, resolved.source());
            Supplier<String> sourceLoader = () -> {
                byte[] currentBytes = DataDrivenGuideLoader.readBytes(selected.pack(), sourceId);
                if (currentBytes == null) {
                    return "";
                }
                return GuideLocalizedPageSourceResolver.resolve(language, contentRootFolder, pageId, currentBytes)
                    .source();
            };
            return new LazyParsedGuidePage(
                sourcePack,
                pageId,
                frontmatter.getFrontmatter(),
                frontmatter.getLanguage(),
                frontmatter.getParseFailureMessage(),
                frontmatter.getParseFailureFrom(),
                frontmatter.getParseFailureTo(),
                sourceLoader,
                resolved.contentFingerprint());
        } catch (Exception ex) {
            GuideDebugLog
                .warn("[GuideNH] [GuideLightweightReloadService] Error parsing page {} from {}", pageId, sourceId, ex);
            return null;
        }
    }

    @Nullable
    private static PageLoadResult loadPage(IResourceManager resourceManager, String namespace, String folder,
        String defaultLanguage, String requestedLanguage, String pagePath, ResourceLocation pageId,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        String sourcePack = "resources:" + namespace;
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
        if (localized != null) return localized;
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
            if (fallback != null) return fallback;
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

    @Nullable
    private static ParsedGuidePage tryParsePage(IResourceManager resourceManager, String sourcePack, String language,
        String contentRootFolder, ResourceLocation pageId, ResourceLocation sourceId,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        GuidePageResourceSelector.SelectedPack selected = GuidePageResourceSelector
            .select(sourceId, activeResourcePacks);
        if (selected == null) return null;
        byte[] bytes = DataDrivenGuideLoader.readBytes(selected.pack(), sourceId);
        if (bytes == null) return null;
        return parsePageBytes(sourcePack, language, contentRootFolder, pageId, sourceId, bytes, selected);
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

    private enum LoadKind {
        LOCALIZED,
        DEFAULT_LANGUAGE,
        RAW_SOURCE
    }

    @Desugar
    private record PageLoadResult(ParsedGuidePage page, LoadKind kind) {}
}
