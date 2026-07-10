package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringTranslate;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.compiler.Frontmatter;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.internal.DirectoryResourcePack;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.localization.GuidePageLanguageIndex;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorAbstractResourcePack;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorFallbackResourceManager;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorSimpleReloadableResourceManager;

import cpw.mods.fml.client.FMLClientHandler;

public class DataDrivenGuideLoader {

    public static final String AUTO_GUIDE_FOLDER = "guidenh";
    public static final String LANGUAGE_FOLDER_PREFIX = "_";
    private static final Map<Class<?>, Field> LOOSE_ROOT_FIELDS = new IdentityHashMap<>();
    private static volatile List<IResourcePack> lastActiveResourcePacks = List.of();
    private static volatile List<IResourcePack> lastResourceManagerResourcePacks = List.of();
    private static volatile Map<IResourcePack, Set<String>> lastResourceManagerDomainsByPack = Map.of();
    private static volatile GuideLanguageDiscoverySnapshot lastGuideLanguageDiscovery = GuideLanguageDiscoverySnapshot
        .empty();

    /**
     * A candidate resource pack that contains a page, with its loadPriority pre-parsed
     * during index building so that select() never needs to read bytes.
     */
    public record PackCandidate(IResourcePack pack, int loadPriority, int order) {

        boolean shouldReplace(PackCandidate previous) {
            return loadPriority > previous.loadPriority()
                || loadPriority == previous.loadPriority() && order > previous.order();
        }
    }

    private static final Map<ResourceLocation, List<PackCandidate>> pagePackIndex = new ConcurrentHashMap<>();

    /** Set true after buildPageIndex() completes. Guards select() fast-null returns. */
    private static volatile boolean indexReady = false;

    /** Global build-order counter for deterministic PackCandidate ordering across all packs. */
    private static final AtomicInteger pagePackOrder = new AtomicInteger(0);

    /**
     * Cache of discovered .lang file entry paths per resource pack file.
     * Populated during scanAndBuildAll() so that GuidePageLanguageIndex and
     * GuideResourceLanguageIndex can read lang files via IResourcePack API
     * instead of re-opening the zip and scanning entries again.
     */
    private static final Map<File, List<String>> PACK_LANG_FILE_PATHS = new IdentityHashMap<>();

    /**
     * Returns the cached list of .lang entry paths for a resource pack file,
     * or empty list if the pack was not scanned (never has .lang files).
     */
    public static List<String> getLangFilePaths(File resourcePackFile) {
        List<String> paths = PACK_LANG_FILE_PATHS.get(resourcePackFile);
        return paths != null ? paths : List.of();
    }

    /**
     * Reads a .lang file from a resource pack using IResourcePack API,
     * parses it, and returns all key-value pairs.
     * The entryPath must be in the form "assets/&lt;domain&gt;/lang/&lt;language&gt;.lang".
     */
    public static Map<String, String> readLangFile(IResourcePack resourcePack, String entryPath) {
        if (!entryPath.startsWith("assets/") || !entryPath.endsWith(".lang")) {
            return Map.of();
        }
        var afterAssets = entryPath.substring("assets/".length());
        var firstSlash = afterAssets.indexOf('/');
        if (firstSlash <= 0) return Map.of();
        var domain = afterAssets.substring(0, firstSlash);
        var resourcePath = afterAssets.substring(firstSlash + 1);
        try (var input = resourcePack.getInputStream(new ResourceLocation(domain, resourcePath))) {
            return StringTranslate.parseLangFile(input);
        } catch (IOException e) {
            return Map.of();
        }
    }

    // readBytes I/O performance counters (reset per reload)
    private static long totalReadBytesNs = 0;
    private static long totalReadBytesCalls = 0;
    private static long totalReadBytesSuccess = 0;

    private DataDrivenGuideLoader() {}

    public static Map<ResourceLocation, MutableGuide> load() {
        return load(getActiveResourcePacks());
    }

    public static Map<ResourceLocation, MutableGuide> load(IResourceManager resourceManager) {
        return load(getActiveResourcePacks(resourceManager));
    }

    public static Map<ResourceLocation, MutableGuide> load(Iterable<? extends IResourcePack> activeResourcePacks) {
        long startedAt = System.nanoTime();
        long stageStartedAt = startedAt;
        var resolvedResourcePacks = toList(activeResourcePacks);
        long resourcePackResolveNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        var discoveredLanguages = discoverGuideLanguages(resolvedResourcePacks);
        long scanNs = System.nanoTime() - stageStartedAt;

        stageStartedAt = System.nanoTime();
        var guides = new LinkedHashMap<ResourceLocation, MutableGuide>();
        for (var entry : discoveredLanguages.entrySet()) {
            ResourceLocation guideId = entry.getKey();
            var builder = Guide.builder(guideId)
                .register(false)
                .folder(AUTO_GUIDE_FOLDER)
                .defaultLanguage(autoDiscoveredDefaultLanguage());
            guides.put(guideId, (MutableGuide) builder.build());
        }
        long buildNs = System.nanoTime() - stageStartedAt;
        int discoveredLanguageCount = countDiscoveredLanguages(discoveredLanguages);
        long totalNs = System.nanoTime() - startedAt;
        GuideDebugLog.warnAlways(
            "[GuideNH] [DataDrivenGuideLoader] Loaded {} guides across {} languages from {} resource packs in {} ms (resourcePackResolveMs={}, scanMs={}, buildMs={})",
            guides.size(),
            discoveredLanguageCount,
            resolvedResourcePacks.size(),
            totalNs / 1_000_000L,
            resourcePackResolveNs / 1_000_000L,
            scanNs / 1_000_000L,
            buildNs / 1_000_000L);
        return guides;
    }

    public static Map<ResourceLocation, Set<String>> discoverGuideLanguages() {
        return discoverGuideLanguages(getActiveResourcePacks());
    }

    public static Map<ResourceLocation, Set<String>> discoverGuideLanguages(
        Iterable<? extends IResourcePack> activeResourcePacks) {
        var resolvedResourcePacks = toList(activeResourcePacks);
        GuideLanguageDiscoverySnapshot cached = lastGuideLanguageDiscovery;
        if (cached.matches(resolvedResourcePacks)) {
            return cached.discoveredLanguages();
        }

        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();
        for (var resourcePack : resolvedResourcePacks) {
            scanResourcePack(resourcePack, discoveredLanguages);
        }

        var frozen = freezeDiscoveredLanguages(discoveredLanguages);
        lastGuideLanguageDiscovery = new GuideLanguageDiscoverySnapshot(List.copyOf(resolvedResourcePacks), frozen);
        return frozen;
    }

    /**
     * Discovers page paths by scanning only filenames — does NOT rebuild the pagePackIndex.
     * Index building is done once upfront by {@link #buildPageIndex(String)}.
     */
    public static LinkedHashMap<String, LinkedHashSet<String>> discoverPagePaths(String folder,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        long startedAt = System.nanoTime();
        var resolvedResourcePacks = toList(activeResourcePacks);
        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();

        for (var resourcePack : resolvedResourcePacks) {
            scanPagePathsForDiscovery(resourcePack, folder, pagePaths);
        }

        long totalNs = System.nanoTime() - startedAt;
        GuideDebugLog.warnAlways(
            "[GuideNH] [DataDrivenGuideLoader] Discovered {} page paths across {} namespaces for folder {} from {} resource packs in {} ms",
            countDiscoveredPagePaths(pagePaths),
            pagePaths.size(),
            folder,
            resolvedResourcePacks.size(),
            totalNs / 1_000_000L);
        return pagePaths;
    }

    /**
     * Same as discoverPagePaths but ONLY from path listing (no frontmatter parsing).
     * Used during page reload where index was already built by buildPageIndex().
     */
    public static LinkedHashMap<String, LinkedHashSet<String>> discoverPagePaths(String folder) {
        return discoverPagePaths(folder, getActiveResourcePacks());
    }

    private static int countDiscoveredPagePaths(LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        int total = 0;
        for (var namespacePaths : pagePaths.values()) {
            total += namespacePaths.size();
        }
        return total;
    }

    private static int countDiscoveredLanguages(Map<ResourceLocation, ? extends Set<String>> discoveredLanguages) {
        int total = 0;
        for (var languages : discoveredLanguages.values()) {
            total += languages.size();
        }
        return total;
    }

    public static void scanPagePathsAllNamespaces(File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        if (!resourcePackRoot.isDirectory()) {
            scanZipPagePathsOnly(resourcePackRoot, folder, pagePaths);
            return;
        }

        for (NamespaceRoot namespaceRoot : discoverNamespaceRoots(resourcePackRoot)) {
            scanPagePathsForNamespaceRoot(namespaceRoot, folder, pagePaths);
        }
    }

    private static void scanPagePathsAllNamespaces(IResourcePack resourcePack, File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var discoveredRoots = discoverNamespaceRoots(resourcePackRoot);
        if (!discoveredRoots.isEmpty()) {
            for (NamespaceRoot namespaceRoot : discoveredRoots) {
                scanPagePathsForNamespaceRoot(namespaceRoot, folder, pagePaths);
            }
            return;
        }

        for (String domain : getResourceDomains(resourcePack)) {
            scanPagePathsForNamespaceRoot(resourcePackRoot, namespaceFromDirectoryName(domain), folder, pagePaths);
        }
    }

    private static void scanPagePathsForNamespaceRoot(File resourcePackRoot, String namespace, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        if (!isValidNamespace(namespace)) {
            return;
        }

        var discovered = new LinkedHashSet<String>();
        for (File guideRoot : guideRootCandidates(resourcePackRoot, namespace, folder)) {
            scanFolderPagePaths(guideRoot, discovered);
        }
        if (!discovered.isEmpty()) {
            pagePaths.computeIfAbsent(namespace, k -> new LinkedHashSet<>())
                .addAll(discovered);
        }
    }

    private static void scanPagePathsForNamespaceRoot(NamespaceRoot namespaceRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var discovered = new LinkedHashSet<String>();
        for (File guideRoot : guideRootCandidates(namespaceRoot, folder)) {
            scanFolderPagePaths(guideRoot, discovered);
        }
        if (!discovered.isEmpty()) {
            pagePaths.computeIfAbsent(namespaceRoot.namespace(), k -> new LinkedHashSet<>())
                .addAll(discovered);
        }
    }

    /**
     * Lightweight scan for path discovery — no frontmatter parsing, no index building.
     * Used during reload where the index has already been built by buildPageIndex().
     */
    private static void scanPagePathsForDiscovery(IResourcePack resourcePack, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }

        if (!resourcePackRoot.isDirectory()) {
            scanZipPagePathsOnly(resourcePackRoot, folder, pagePaths);
            return;
        }
        scanPagePathsAllNamespaces(resourcePack, resourcePackRoot, folder, pagePaths);
    }

    /**
     * Scan a ZIP for .md files — adds to pagePaths but does NOT build the index.
     * The index is built once upfront by buildPageIndex().
     */
    private static void scanZipPagePathsOnly(File resourcePackFile, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths) {
        var prefix = "assets/";
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                var path = entry.getName();
                if (!path.startsWith(prefix) || !path.endsWith(".md")) continue;

                var afterAssets = path.substring(prefix.length());
                var firstSlash = afterAssets.indexOf('/');
                if (firstSlash <= 0) continue;

                var namespace = afterAssets.substring(0, firstSlash);
                var afterNamespace = afterAssets.substring(firstSlash + 1);
                if (!afterNamespace.startsWith(folder + "/")) continue;

                var afterFolder = afterNamespace.substring(folder.length() + 1);
                var slashIndex = afterFolder.indexOf('/');
                if (slashIndex <= 0) continue;

                var language = afterFolder.substring(0, slashIndex);
                if (!isLanguageFolder(language)) continue;

                var pagePath = afterFolder.substring(slashIndex + 1);
                if (!pagePath.isEmpty()) {
                    pagePaths.computeIfAbsent(namespace, k -> new LinkedHashSet<>())
                        .add(pagePath);
                }
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    private static void scanZipBuildIndex(File resourcePackFile, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths, IResourcePack resourcePack,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        long t0 = System.nanoTime();
        var prefix = "assets/";
        int mdCount = 0;
        int langCount = 0;
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                var path = entry.getName();
                if (!path.startsWith(prefix)) continue;

                // Handle .lang files: collect guidenh.page.* keys + cache path
                if (path.endsWith(".lang")) {
                    collectLangKeys(zip, entry, path, guidePageLangKeys);
                    PACK_LANG_FILE_PATHS.computeIfAbsent(resourcePackFile, k -> new ArrayList<>())
                        .add(path);
                    langCount++;
                    continue;
                }

                // Handle .md files
                if (!path.endsWith(".md")) continue;

                var afterAssets = path.substring(prefix.length());
                var firstSlash = afterAssets.indexOf('/');
                if (firstSlash <= 0) continue;

                var namespace = afterAssets.substring(0, firstSlash);
                var afterNamespace = afterAssets.substring(firstSlash + 1);
                if (!afterNamespace.startsWith(folder + "/")) continue;

                var afterFolder = afterNamespace.substring(folder.length() + 1);
                var slashIndex = afterFolder.indexOf('/');
                if (slashIndex <= 0) continue;

                var language = afterFolder.substring(0, slashIndex);
                if (!isLanguageFolder(language)) continue;

                var pagePath = afterFolder.substring(slashIndex + 1);
                if (pagePath.isEmpty()) continue;

                pagePaths.computeIfAbsent(namespace, k -> new LinkedHashSet<>())
                    .add(pagePath);

                // Record that this namespace has this language (for guide discovery)
                discoveredLanguages.computeIfAbsent(new ResourceLocation(namespace, folder), k -> new LinkedHashSet<>())
                    .add(toLanguageCode(language));

                // Build reverse index with pre-parsed loadPriority
                ResourceLocation loc = new ResourceLocation(namespace, folder + "/" + language + "/" + pagePath);
                int loadPriority = parseLoadPriorityFromZipEntry(zip, entry, loc);
                synchronized (pagePackIndex) {
                    pagePackIndex.computeIfAbsent(loc, k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                }

                // Also index the raw source key (strip the _language/ prefix)
                ResourceLocation rawLoc = new ResourceLocation(namespace, folder + "/" + pagePath);
                synchronized (pagePackIndex) {
                    pagePackIndex.computeIfAbsent(rawLoc, k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                }
                mdCount++;
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
        long elapsedNs = System.nanoTime() - t0;
        if (elapsedNs > 200_000_000) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Slow pack {}: {} ms ({} .md, {} .lang)",
                resourcePack.getPackName(),
                elapsedNs / 1_000_000L,
                mdCount,
                langCount);
        }
    }

    private static void collectLangKeys(ZipFile zip, ZipEntry entry, String path,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        try (var input = zip.getInputStream(entry)) {
            var langFile = StringTranslate.parseLangFile(input);
            for (var langEntry : langFile.entrySet()) {
                String key = langEntry.getKey();
                if (key.startsWith("guidenh.page.")) {
                    // key format: guidenh.page.<ns>.<folder>.<pagePath> e.g. guiden.page.gregtech.guidenh.index
                    // Extract language from path: assets/<ns>/lang/<language>.lang
                    int langStart = path.lastIndexOf('/') + 1;
                    int langEnd = path.lastIndexOf('.');
                    if (langStart <= 0 || langEnd <= langStart) continue;
                    String language = path.substring(langStart, langEnd);
                    guidePageLangKeys.computeIfAbsent(LangUtil.normalizeLanguage(language), k -> new LinkedHashMap<>())
                        .put(key, langEntry.getValue());
                }
            }
        } catch (IOException ignored) {}
    }

    private static int parseLoadPriorityFromZipEntry(ZipFile zip, java.util.zip.ZipEntry entry, ResourceLocation loc) {
        try (var entryStream = zip.getInputStream(entry)) {
            byte[] entryBytes = GuideResourceAccess.readFully(entryStream);
            String content = new String(entryBytes, StandardCharsets.UTF_8);
            if (content.startsWith("﻿")) content = content.substring(1);
            String yamlText = PageCompiler.extractFrontmatterText(PageCompiler.normalizeLineEndings(content));
            if (yamlText != null) {
                var frontmatter = Frontmatter.parse(loc, yamlText);
                var nav = frontmatter.navigationEntry();
                return nav != null ? nav.loadPriority() : 0;
            }
        } catch (IOException ignored) {}
        return 0;
    }

    private static void scanDirectoryBuildIndex(IResourcePack resourcePack, File resourcePackRoot, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        for (NamespaceRoot namespaceRoot : discoverNamespaceRoots(resourcePackRoot)) {
            scanDirectoryBuildIndexForNamespace(
                resourcePack,
                namespaceRoot,
                folder,
                pagePaths,
                discoveredLanguages,
                guidePageLangKeys);
        }
        // Also scan for .lang files in the lang/ directory
        scanDirectoryLangFiles(resourcePackRoot, guidePageLangKeys);
    }

    private static void scanDirectoryLangFiles(File resourcePackRoot,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        File assetsDir = new File(resourcePackRoot, "assets");
        File[] nsDirs = assetsDir.listFiles(File::isDirectory);
        if (nsDirs == null) return;
        for (File nsDir : nsDirs) {
            File langDir = new File(nsDir, "lang");
            if (!langDir.isDirectory()) continue;
            File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".lang"));
            if (langFiles == null) continue;
            for (File langFile : langFiles) {
                String fileName = langFile.getName();
                int dot = fileName.lastIndexOf('.');
                if (dot <= 0) continue;
                String language = fileName.substring(0, dot);
                try (var input = new java.io.FileInputStream(langFile)) {
                    var parsed = StringTranslate.parseLangFile(input);
                    for (var entry : parsed.entrySet()) {
                        if (entry.getKey()
                            .startsWith("guidenh.page.")) {
                            guidePageLangKeys
                                .computeIfAbsent(LangUtil.normalizeLanguage(language), k -> new LinkedHashMap<>())
                                .put(entry.getKey(), entry.getValue());
                        }
                    }
                } catch (IOException ignored) {}
            }
        }
    }

    private static void scanDirectoryBuildIndexForNamespace(IResourcePack resourcePack, NamespaceRoot namespaceRoot,
        String folder, LinkedHashMap<String, LinkedHashSet<String>> pagePaths,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        File guideRoot = new File(namespaceRoot.directory(), folder);
        if (!guideRoot.isDirectory()) return;

        File[] languageDirs = guideRoot.listFiles(File::isDirectory);
        if (languageDirs == null) return;

        for (File languageDir : languageDirs) {
            String language = languageDir.getName();
            if (!isLanguageFolder(language)) continue;

            // Record discovered language for this namespace
            discoveredLanguages
                .computeIfAbsent(new ResourceLocation(namespaceRoot.namespace(), folder), k -> new LinkedHashSet<>())
                .add(toLanguageCode(language));

            var pathCollector = new LinkedHashSet<String>();
            collectMarkdownPaths(languageDir, "", pathCollector);

            for (String pagePath : pathCollector) {
                pagePaths.computeIfAbsent(namespaceRoot.namespace(), k -> new LinkedHashSet<>())
                    .add(pagePath);

                ResourceLocation loc = new ResourceLocation(
                    namespaceRoot.namespace(),
                    folder + "/" + language + "/" + pagePath);

                // For directory packs, parse the actual file to get loadPriority
                int loadPriority = parseLoadPriorityFromFile(
                    languageDir.toPath()
                        .resolve(pagePath),
                    loc);

                synchronized (pagePackIndex) {
                    pagePackIndex.computeIfAbsent(loc, k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                }

                // Also raw source key
                ResourceLocation rawLoc = new ResourceLocation(namespaceRoot.namespace(), folder + "/" + pagePath);
                synchronized (pagePackIndex) {
                    pagePackIndex.computeIfAbsent(rawLoc, k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                }
            }
        }
    }

    private static int parseLoadPriorityFromFile(java.nio.file.Path filePath, ResourceLocation loc) {
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(filePath);
            String content = new String(bytes, StandardCharsets.UTF_8);
            if (content.startsWith("﻿")) content = content.substring(1);
            String yamlText = PageCompiler.extractFrontmatterText(PageCompiler.normalizeLineEndings(content));
            if (yamlText != null) {
                var frontmatter = Frontmatter.parse(loc, yamlText);
                var nav = frontmatter.navigationEntry();
                return nav != null ? nav.loadPriority() : 0;
            }
        } catch (IOException ignored) {}
        return 0;
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder) {
        return discoverPagePaths(guideId, folder, getActiveResourcePacks());
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        var result = new LinkedHashSet<String>();
        for (var resourcePack : activeResourcePacks) {
            scanPagePathsForNamespace(resourcePack, guideId.getResourceDomain(), folder, result);
        }
        return result;
    }

    public static void scanPagePathsForNamespace(IResourcePack resourcePack, String namespace, String folder,
        Set<String> pagePaths) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }
        scanPagePathsForNamespace(resourcePackRoot, namespace, folder, pagePaths);
    }

    public static void scanPagePathsForNamespace(File resourcePackRoot, String namespace, String folder,
        Set<String> pagePaths) {
        if (resourcePackRoot.isDirectory()) {
            for (File guideRoot : guideRootCandidates(resourcePackRoot, namespace, folder)) {
                scanFolderPagePaths(guideRoot, pagePaths);
            }
        } else {
            scanZipPagePaths(resourcePackRoot, toFolderPrefix(namespace, folder), pagePaths);
        }
    }

    public static List<IResourcePack> getActiveResourcePacks() {
        var resourcePacks = new LinkedHashSet<IResourcePack>(GuideDevelopmentResourcePacks.getConfiguredPacks());
        resourcePacks.addAll(lastResourceManagerResourcePacks);
        addConfiguredResourcePacks(resourcePacks);
        var resolved = new ArrayList<>(resourcePacks);
        lastActiveResourcePacks = List.copyOf(resolved);
        return resolved;
    }

    public static List<IResourcePack> getActiveResourcePacks(IResourceManager resourceManager) {
        var resourcePacks = new LinkedHashSet<IResourcePack>(GuideDevelopmentResourcePacks.getConfiguredPacks());
        var resourceManagerResourcePacks = new LinkedHashSet<IResourcePack>();
        var domainsByPack = new IdentityHashMap<IResourcePack, LinkedHashSet<String>>();
        addResourceManagerResourcePacks(resourceManager, resourceManagerResourcePacks, domainsByPack);
        lastResourceManagerResourcePacks = List.copyOf(resourceManagerResourcePacks);
        lastResourceManagerDomainsByPack = freezeDomainsByPack(domainsByPack);
        resourcePacks.addAll(resourceManagerResourcePacks);
        addConfiguredResourcePacks(resourcePacks);
        var resolved = new ArrayList<>(resourcePacks);
        lastActiveResourcePacks = List.copyOf(resolved);
        return resolved;
    }

    public static List<IResourcePack> getLastActiveResourcePacks() {
        List<IResourcePack> snapshot = lastActiveResourcePacks;
        return snapshot.isEmpty() ? getActiveResourcePacks() : snapshot;
    }

    private static void addConfiguredResourcePacks(LinkedHashSet<IResourcePack> resourcePacks) {
        try {
            var accessor = (AccessorFMLClientHandler) FMLClientHandler.instance();
            var basePacks = accessor.guidenh$getResourcePackList();
            if (basePacks != null) {
                resourcePacks.addAll(basePacks);
            }
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to inspect the currently loaded base resource packs",
                e);
        }

        var repository = Minecraft.getMinecraft()
            .getResourcePackRepository();
        for (var entry : repository.getRepositoryEntries()) {
            var resourcePack = entry.getResourcePack();
            if (resourcePack != null) {
                resourcePacks.add(resourcePack);
            }
        }

        var serverPack = repository.func_148530_e();
        if (serverPack != null) {
            resourcePacks.add(serverPack);
        }
    }

    private static void addResourceManagerResourcePacks(IResourceManager resourceManager,
        LinkedHashSet<IResourcePack> resourcePacks,
        IdentityHashMap<IResourcePack, LinkedHashSet<String>> domainsByPack) {
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) {
            return;
        }

        try {
            var accessor = (AccessorSimpleReloadableResourceManager) resourceManager;
            Map<String, FallbackResourceManager> domainManagers = accessor.guidenh$getDomainResourceManagers();
            if (domainManagers == null || domainManagers.isEmpty()) {
                return;
            }

            for (String domain : resourceManager.getResourceDomains()) {
                FallbackResourceManager fallbackResourceManager = domainManagers.get(domain);
                if (fallbackResourceManager == null) {
                    continue;
                }
                List<IResourcePack> packs = ((AccessorFallbackResourceManager) fallbackResourceManager)
                    .guidenh$getResourcePacks();
                if (packs != null) {
                    for (IResourcePack pack : packs) {
                        resourcePacks.add(pack);
                        domainsByPack.computeIfAbsent(pack, ignored -> new LinkedHashSet<>())
                            .add(domain);
                    }
                }
            }
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to inspect the currently loaded resource manager packs",
                e);
        }
    }

    private static Map<IResourcePack, Set<String>> freezeDomainsByPack(
        IdentityHashMap<IResourcePack, LinkedHashSet<String>> domainsByPack) {
        if (domainsByPack.isEmpty()) {
            return Map.of();
        }

        var result = new IdentityHashMap<IResourcePack, Set<String>>();
        for (var entry : domainsByPack.entrySet()) {
            result.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Set<String> getResourceDomains(IResourcePack resourcePack) {
        Set<String> cachedDomains = lastResourceManagerDomainsByPack.get(resourcePack);
        return cachedDomains != null ? cachedDomains : resourcePack.getResourceDomains();
    }

    public static void scanResourcePack(IResourcePack resourcePack,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }

        if (resourcePackRoot.isDirectory()) {
            scanResourcePackFolder(resourcePack, resourcePackRoot, discoveredLanguages);
        } else {
            scanResourcePackZip(resourcePackRoot, discoveredLanguages);
        }
    }

    public static void scanPagePaths(IResourcePack resourcePack, String prefix, Set<String> pagePaths) {
        var resourcePackRoot = getLooseResourcePackRoot(resourcePack);
        if (resourcePackRoot == null || !resourcePackRoot.exists()) {
            return;
        }

        if (resourcePackRoot.isDirectory()) {
            scanFolderPagePaths(new File(resourcePackRoot, prefix.replace('/', File.separatorChar)), pagePaths);
        } else {
            scanZipPagePaths(resourcePackRoot, prefix, pagePaths);
        }
    }

    public static File getResourcePackFile(IResourcePack resourcePack) {
        if (resourcePack instanceof DirectoryResourcePack) {
            return ((DirectoryResourcePack) resourcePack).getRoot()
                .toFile();
        }

        if (!(resourcePack instanceof AbstractResourcePack)) {
            return null;
        }

        try {
            return ((AccessorAbstractResourcePack) resourcePack).guidenh$getResourcePackFile();
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to resolve the backing file for resource pack {}",
                resourcePack.getPackName(),
                e);
            return null;
        }
    }

    public static File getLooseResourcePackRoot(IResourcePack resourcePack) {
        File resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile != null) {
            return resourcePackFile;
        }

        Field field = findLooseRootField(resourcePack.getClass());
        if (field == null) {
            return null;
        }

        try {
            Object value = field.get(resourcePack);
            if (value instanceof Path path) {
                return path.toFile();
            }
            if (value instanceof File file) {
                return file;
            }
        } catch (IllegalAccessException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to resolve the directory root for resource pack {}",
                resourcePack.getPackName(),
                e);
        }
        return null;
    }

    private static Field findLooseRootField(Class<?> resourcePackClass) {
        synchronized (LOOSE_ROOT_FIELDS) {
            if (LOOSE_ROOT_FIELDS.containsKey(resourcePackClass)) {
                return LOOSE_ROOT_FIELDS.get(resourcePackClass);
            }

            Field field = discoverLooseRootField(resourcePackClass);
            LOOSE_ROOT_FIELDS.put(resourcePackClass, field);
            return field;
        }
    }

    private static Field discoverLooseRootField(Class<?> resourcePackClass) {
        Class<?> current = resourcePackClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                Class<?> type = field.getType();
                if (type == Path.class || type == File.class) {
                    field.setAccessible(true);
                    return field;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * Reads bytes from the given resource pack — pure I/O, no existence check, no path guessing.
     * Returns null if the resource does not exist or cannot be read.
     */
    public static byte[] readBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        long startedAt = System.nanoTime();
        try (var input = resourcePack.getInputStream(resourceLocation)) {
            byte[] result = GuideResourceAccess.readFully(input);
            totalReadBytesCalls++;
            totalReadBytesSuccess++;
            totalReadBytesNs += System.nanoTime() - startedAt;
            return result;
        } catch (IOException e) {
            totalReadBytesCalls++;
            totalReadBytesNs += System.nanoTime() - startedAt;
            return null;
        }
    }

    /** @deprecated No longer needed — readBytes() does not do path guessing. */
    @Deprecated
    public static byte[] readLooseBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        return null;
    }

    public static IResourcePack findResourcePack(ResourceLocation resourceLocation) {
        return findResourcePack(resourceLocation, getActiveResourcePacks());
    }

    public static IResourcePack findResourcePack(ResourceLocation resourceLocation,
        Iterable<? extends IResourcePack> resourcePacks) {
        var candidates = getCandidatesFor(resourceLocation);
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0)
                .pack();
        }
        if (indexReady) return null;
        // Fallback full scan if index not built yet
        for (var pack : resourcePacks) {
            try {
                pack.getInputStream(resourceLocation)
                    .close();
                return pack;
            } catch (IOException ignored) {}
        }
        return null;
    }

    public static void scanResourcePackFolder(File resourcePackRoot,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        for (NamespaceRoot namespaceRoot : discoverNamespaceRoots(resourcePackRoot)) {
            scanResourcePackFolderNamespaceRoot(namespaceRoot, AUTO_GUIDE_FOLDER, discoveredLanguages);
        }
    }

    private static void scanResourcePackFolder(IResourcePack resourcePack, File resourcePackRoot,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        var discoveredRoots = discoverNamespaceRoots(resourcePackRoot);
        if (!discoveredRoots.isEmpty()) {
            for (NamespaceRoot namespaceRoot : discoveredRoots) {
                scanResourcePackFolderNamespaceRoot(namespaceRoot, AUTO_GUIDE_FOLDER, discoveredLanguages);
            }
            return;
        }

        for (String domain : getResourceDomains(resourcePack)) {
            scanResourcePackFolderNamespace(resourcePackRoot, namespaceFromDirectoryName(domain), discoveredLanguages);
        }
    }

    private static void scanResourcePackFolderNamespace(File resourcePackRoot, String namespace,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (!isValidNamespace(namespace)) {
            return;
        }
        for (File guideRoot : guideRootCandidates(resourcePackRoot, namespace, AUTO_GUIDE_FOLDER)) {
            scanResourcePackFolderNamespaceRoot(namespace, guideRoot, discoveredLanguages);
        }
    }

    private static void scanResourcePackFolderNamespaceRoot(NamespaceRoot namespaceRoot, String folder,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        for (File guideRoot : guideRootCandidates(namespaceRoot, folder)) {
            scanResourcePackFolderNamespaceRoot(namespaceRoot.namespace(), guideRoot, discoveredLanguages);
        }
    }

    private static void scanResourcePackFolderNamespaceRoot(String namespace, File guideRootDir,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (!guideRootDir.isDirectory()) {
            return;
        }
        var languageDirs = guideRootDir.listFiles(File::isDirectory);
        if (languageDirs == null) {
            return;
        }
        for (var languageDir : languageDirs) {
            var languageFolder = languageDir.getName();
            if (!isLanguageFolder(languageFolder)) {
                continue;
            }

            if (!containsMarkdownFiles(languageDir)) {
                continue;
            }

            var guideId = new ResourceLocation(namespace, AUTO_GUIDE_FOLDER);
            discoveredLanguages.computeIfAbsent(guideId, ignored -> new LinkedHashSet<>())
                .add(toLanguageCode(languageFolder));
        }
    }

    public static void scanResourcePackZip(File resourcePackFile,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        String assetsPrefix = "assets/";
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var path = entry.getName();
                if (!path.startsWith(assetsPrefix) || !path.endsWith(".md")) {
                    continue;
                }

                var afterAssets = path.substring(assetsPrefix.length());
                var namespaceEnd = afterAssets.indexOf('/');
                if (namespaceEnd <= 0) {
                    continue;
                }

                var namespace = afterAssets.substring(0, namespaceEnd);
                var afterNamespace = afterAssets.substring(namespaceEnd + 1);
                if (!afterNamespace.startsWith(AUTO_GUIDE_FOLDER + "/")) {
                    continue;
                }

                var afterGuideFolder = afterNamespace.substring(AUTO_GUIDE_FOLDER.length() + 1);
                var languageEnd = afterGuideFolder.indexOf('/');
                if (languageEnd <= 0) {
                    continue;
                }

                var languageFolder = afterGuideFolder.substring(0, languageEnd);
                if (!isLanguageFolder(languageFolder)) {
                    continue;
                }

                discoveredLanguages
                    .computeIfAbsent(
                        new ResourceLocation(namespace, AUTO_GUIDE_FOLDER),
                        ignored -> new LinkedHashSet<>())
                    .add(toLanguageCode(languageFolder));
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide languages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    public static void scanFolderPagePaths(File guideRoot, Set<String> pagePaths) {
        var languageDirs = guideRoot.listFiles(File::isDirectory);
        if (languageDirs == null) {
            return;
        }

        for (var languageDir : languageDirs) {
            if (!isLanguageFolder(languageDir.getName())) {
                continue;
            }
            collectMarkdownPaths(languageDir, "", pagePaths);
        }
    }

    public static void scanZipPagePaths(File resourcePackFile, String prefix, Set<String> pagePaths) {
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                var path = entry.getName();
                if (!path.startsWith(prefix) || !path.endsWith(".md")) {
                    continue;
                }

                var relative = path.substring(prefix.length());
                var slashIndex = relative.indexOf('/');
                if (slashIndex <= 0) {
                    continue;
                }

                var language = relative.substring(0, slashIndex);
                if (!isLanguageFolder(language)) {
                    continue;
                }

                var pagePath = relative.substring(slashIndex + 1);
                if (!pagePath.isEmpty()) {
                    pagePaths.add(pagePath);
                }
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    public static void collectMarkdownPaths(File directory, String relativePath, Set<String> pagePaths) {
        var children = directory.listFiles();
        if (children == null) {
            return;
        }

        for (var child : children) {
            String childPath = relativePath.isEmpty() ? child.getName() : relativePath + "/" + child.getName();
            if (child.isDirectory()) {
                collectMarkdownPaths(child, childPath, pagePaths);
            } else if (child.isFile() && child.getName()
                .endsWith(".md")) {
                    pagePaths.add(childPath);
                }
        }
    }

    public static boolean containsMarkdownFiles(File directory) {
        var children = directory.listFiles();
        if (children == null) {
            return false;
        }

        for (var child : children) {
            if (child.isFile() && child.getName()
                .endsWith(".md")) {
                return true;
            }
            if (child.isDirectory() && containsMarkdownFiles(child)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isLanguageFolder(String name) {
        return name.startsWith(LANGUAGE_FOLDER_PREFIX) && LangUtil.isLanguageCode(name.substring(1));
    }

    public static String toLanguageCode(String folderName) {
        return LangUtil.normalizeLanguage(folderName.substring(LANGUAGE_FOLDER_PREFIX.length()));
    }

    public static String autoDiscoveredDefaultLanguage() {
        return LangUtil.ENGLISH_LANGUAGE;
    }

    public static String toFolderPrefix(String namespace, String folder) {
        return "assets/" + namespace + "/" + folder + "/";
    }

    public static String toLooseFolderPrefix(String namespace, String folder) {
        return namespace + "/" + folder + "/";
    }

    private static List<File> guideRootCandidates(File resourcePackRoot, String namespace, String folder) {
        var candidates = new LinkedHashMap<Path, File>(3);
        for (NamespaceRoot namespaceRoot : discoverNamespaceRoots(resourcePackRoot)) {
            if (namespaceRoot.namespace()
                .equals(namespace)) {
                addGuideRootCandidates(candidates, namespaceRoot, folder);
            }
        }
        addGuideRootCandidate(candidates, resourcePackRoot, toFolderPrefix(namespace, folder));
        addGuideRootCandidate(candidates, resourcePackRoot, toLooseFolderPrefix(namespace, folder));
        if (folder.equals(namespace)) {
            addGuideRootCandidate(candidates, resourcePackRoot, folder + "/");
        }
        return List.copyOf(candidates.values());
    }

    private static List<File> guideRootCandidates(NamespaceRoot namespaceRoot, String folder) {
        var candidates = new LinkedHashMap<Path, File>(2);
        addGuideRootCandidates(candidates, namespaceRoot, folder);
        return List.copyOf(candidates.values());
    }

    private static void addGuideRootCandidates(LinkedHashMap<Path, File> candidates, NamespaceRoot namespaceRoot,
        String folder) {
        addGuideRootCandidate(candidates, namespaceRoot.directory(), folder + "/");
        if (folder.equals(namespaceRoot.namespace()) && namespaceRoot.allowDirectoryAsGuideRoot()) {
            addGuideRootCandidate(candidates, namespaceRoot.directory(), "");
        }
    }

    private static void addGuideRootCandidate(LinkedHashMap<Path, File> candidates, File resourcePackRoot,
        String relativePath) {
        var candidate = new File(resourcePackRoot, toNativePath(relativePath));
        if (!candidate.isDirectory()) {
            return;
        }
        candidates.putIfAbsent(
            candidate.toPath()
                .toAbsolutePath()
                .normalize(),
            candidate);
    }

    private static String toNativePath(String resourcePath) {
        return resourcePath.replace('/', File.separatorChar);
    }

    private static List<NamespaceRoot> discoverNamespaceRoots(File resourcePackRoot) {
        var roots = new LinkedHashMap<Path, NamespaceRoot>();
        var assetsDir = new File(resourcePackRoot, "assets");
        var assetNamespaceDirs = assetsDir.listFiles(File::isDirectory);
        if (assetNamespaceDirs != null) {
            for (var namespaceDir : assetNamespaceDirs) {
                String namespace = namespaceFromDirectoryName(namespaceDir.getName());
                if (isValidNamespace(namespace)) {
                    addNamespaceRoot(roots, new NamespaceRoot(namespace, namespaceDir, false));
                }
            }
        }

        for (NamespaceRoot namespaceRoot : discoverNativeNamespaceRoots(resourcePackRoot)) {
            addNamespaceRoot(roots, namespaceRoot);
        }
        return List.copyOf(roots.values());
    }

    private static List<NamespaceRoot> discoverNativeNamespaceRoots(File resourcePackRoot) {
        var roots = new LinkedHashMap<Path, NamespaceRoot>();
        var nativeNamespaceDirs = resourcePackRoot.listFiles(File::isDirectory);
        if (nativeNamespaceDirs != null) {
            for (var namespaceDir : nativeNamespaceDirs) {
                if ("assets".equals(namespaceDir.getName())) {
                    continue;
                }
                String namespace = namespaceFromDirectoryName(namespaceDir.getName());
                if (isValidNamespace(namespace)) {
                    addNamespaceRoot(roots, new NamespaceRoot(namespace, namespaceDir, true));
                }
            }
        }
        return List.copyOf(roots.values());
    }

    private static void addNamespaceRoot(LinkedHashMap<Path, NamespaceRoot> roots, NamespaceRoot namespaceRoot) {
        roots.putIfAbsent(
            namespaceRoot.directory()
                .toPath()
                .toAbsolutePath()
                .normalize(),
            namespaceRoot);
    }

    private static String namespaceFromDirectoryName(String directoryName) {
        if (isValidNamespace(directoryName)) {
            return directoryName;
        }
        int openBracket = directoryName.lastIndexOf('[');
        if (openBracket < 0 || !directoryName.endsWith("]")) {
            return directoryName;
        }
        return directoryName.substring(openBracket + 1, directoryName.length() - 1);
    }

    private static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            return false;
        }
        for (int i = 0; i < namespace.length(); i++) {
            char ch = namespace.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '_' || ch == '-' || ch == '.') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static List<IResourcePack> toList(Iterable<? extends IResourcePack> resourcePacks) {
        var result = new ArrayList<IResourcePack>();
        for (IResourcePack resourcePack : resourcePacks) {
            result.add(resourcePack);
        }
        return result;
    }

    public static void clearCaches() {
        lastGuideLanguageDiscovery = GuideLanguageDiscoverySnapshot.empty();
        pagePackIndex.clear();
        PACK_LANG_FILE_PATHS.clear();
        indexReady = false;
        pagePackOrder.set(0);
        totalReadBytesNs = 0;
        totalReadBytesCalls = 0;
        totalReadBytesSuccess = 0;
    }

    /**
     * Result of a single-pass scan of all resource packs: guides, page paths,
     * and pack index. Language index is pre-populated as a side effect.
     */
    public record ScanResult(Map<ResourceLocation, MutableGuide> guides,
        Map<String, LinkedHashSet<String>> pagePaths) {}

    /**
     * One-pass scan of all active resource packs. Builds everything needed for a reload:
     * pagePackIndex, page paths, guide definitions, and GuidePageLanguageIndex.
     * Replaces separate calls to load(), buildPageIndex(), discoverPagePaths(),
     * and GuidePageLanguageIndex's lazy scan.
     */
    public static ScanResult scanAndBuildAll(String folder) {
        return scanAndBuildAll(folder, getActiveResourcePacks());
    }

    public static ScanResult scanAndBuildAll(String folder, Iterable<? extends IResourcePack> activeResourcePacks) {
        long t0 = System.nanoTime();
        var resolvedPacks = toList(activeResourcePacks);

        pagePackIndex.clear();
        indexReady = false;
        pagePackOrder.set(0);

        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();
        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();
        var guidePageLangKeys = new LinkedHashMap<String, LinkedHashMap<String, String>>();

        int zipPackCount = 0;
        int dirPackCount = 0;
        long zipScanNs = 0;
        long dirScanNs = 0;

        for (var pack : resolvedPacks) {
            var root = getLooseResourcePackRoot(pack);
            if (root == null || !root.exists()) continue;
            long packT0 = System.nanoTime();
            if (!root.isDirectory()) {
                zipPackCount++;
                scanZipBuildIndex(root, folder, pagePaths, pack, discoveredLanguages, guidePageLangKeys);
                zipScanNs += System.nanoTime() - packT0;
            } else {
                dirPackCount++;
                scanDirectoryBuildIndex(pack, root, folder, pagePaths, discoveredLanguages, guidePageLangKeys);
                dirScanNs += System.nanoTime() - packT0;
            }
        }

        long guideBuildT0 = System.nanoTime();
        var guides = new LinkedHashMap<ResourceLocation, MutableGuide>();
        for (var entry : discoveredLanguages.entrySet()) {
            var guideId = entry.getKey();
            var builder = Guide.builder(guideId)
                .register(false)
                .folder(folder)
                .defaultLanguage(autoDiscoveredDefaultLanguage());
            guides.put(guideId, (MutableGuide) builder.build());
        }
        long guideBuildNs = System.nanoTime() - guideBuildT0;

        long preloadT0 = System.nanoTime();
        GuidePageLanguageIndex.preload(freezeLangKeys(guidePageLangKeys));
        long preloadNs = System.nanoTime() - preloadT0;

        indexReady = true;

        long totalNs = System.nanoTime() - t0;
        int mdCount = countDiscoveredPagePaths(pagePaths);
        int langKeyCount = guidePageLangKeys.values()
            .stream()
            .mapToInt(Map::size)
            .sum();
        GuideDebugLog.warnAlways(
            "[GuideNH] [DataDrivenGuideLoader] scanAndBuildAll: {} guides, {} page paths, {} lang keys from {} packs ({} zip, {} dir) in {} ms — zipScan={}ms dirScan={}ms guideBuild={}ms preload={}ms ({} pack refs)",
            guides.size(),
            mdCount,
            langKeyCount,
            resolvedPacks.size(),
            zipPackCount,
            dirPackCount,
            totalNs / 1_000_000L,
            zipScanNs / 1_000_000L,
            dirScanNs / 1_000_000L,
            guideBuildNs / 1_000_000L,
            preloadNs / 1_000_000L,
            countIndexPackRefs());
        return new ScanResult(guides, pagePaths);
    }

    private static Map<String, Map<String, String>> freezeLangKeys(
        LinkedHashMap<String, LinkedHashMap<String, String>> keys) {
        if (keys.isEmpty()) return Map.of();
        var frozen = new LinkedHashMap<String, Map<String, String>>();
        for (var entry : keys.entrySet()) {
            frozen.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    /** @deprecated Use {@link #scanAndBuildAll(String, Iterable)} — single pass. */
    @Deprecated
    public static void buildPageIndex(String folder) {
        scanAndBuildAll(folder);
    }

    /** @deprecated Use {@link #scanAndBuildAll(String, Iterable)} — single pass. */
    @Deprecated
    public static void buildPageIndex(String folder, Iterable<? extends IResourcePack> activeResourcePacks) {
        scanAndBuildAll(folder, activeResourcePacks);
    }

    private static int countIndexPackRefs() {
        int total = 0;
        for (var candidates : pagePackIndex.values()) {
            total += candidates.size();
        }
        return total;
    }

    /**
     * @deprecated Use {@link #getCandidatesFor(ResourceLocation)} instead.
     */
    @Deprecated
    public static @Nullable List<IResourcePack> getPacksFor(ResourceLocation pageLocation) {
        List<PackCandidate> candidates = pagePackIndex.get(pageLocation);
        if (candidates == null) return null;
        return candidates.stream()
            .map(PackCandidate::pack)
            .toList();
    }

    /**
     * Returns the list of pack candidates for a given resource location from the index,
     * or null if the location is not indexed (definitely does not exist when indexReady).
     */
    public static @Nullable List<PackCandidate> getCandidatesFor(ResourceLocation pageLocation) {
        return pagePackIndex.get(pageLocation);
    }

    /**
     * Returns true after buildPageIndex() has completed, meaning the index covers all
     * known packs. When true, any key not found in the index is guaranteed absent.
     */
    public static boolean isIndexPopulated() {
        return indexReady;
    }

    public static String formatIndexStats() {
        if (!indexReady) return "pagePackIndex not ready (buildPageIndex() not called)";
        int totalKeys = pagePackIndex.size();
        int totalPackRefs = 0;
        int minCandidates = Integer.MAX_VALUE;
        int maxCandidates = 0;
        for (List<PackCandidate> candidates : pagePackIndex.values()) {
            int size = candidates.size();
            totalPackRefs += size;
            if (size < minCandidates) minCandidates = size;
            if (size > maxCandidates) maxCandidates = size;
        }
        double avg = (double) totalPackRefs / totalKeys;
        return String.format(
            "pagePackIndex: %d keys, %d total pack refs, candidates per key min=%d max=%d avg=%.1f",
            totalKeys,
            totalPackRefs,
            minCandidates,
            maxCandidates,
            avg);
    }

    public static String formatReadBytesStats() {
        if (totalReadBytesCalls == 0) return "no readBytes() calls";
        long totalMs = totalReadBytesNs / 1_000_000L;
        long avgUs = totalReadBytesCalls > 0 ? totalReadBytesNs / totalReadBytesCalls / 1000 : 0;
        return String.format(
            "readBytes: %d calls, %d ms total, %d us avg/call, %d success (hits)",
            totalReadBytesCalls,
            totalMs,
            avgUs,
            totalReadBytesSuccess);
    }

    private static Map<ResourceLocation, Set<String>> freezeDiscoveredLanguages(
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (discoveredLanguages.isEmpty()) {
            return Map.of();
        }

        var frozen = new LinkedHashMap<ResourceLocation, Set<String>>(discoveredLanguages.size());
        for (var entry : discoveredLanguages.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private record GuideLanguageDiscoverySnapshot(List<IResourcePack> resourcePacks,
        Map<ResourceLocation, Set<String>> discoveredLanguages) {

        private static GuideLanguageDiscoverySnapshot empty() {
            return new GuideLanguageDiscoverySnapshot(List.of(), Map.of());
        }

        private boolean matches(List<IResourcePack> otherResourcePacks) {
            return !resourcePacks.isEmpty() && resourcePacks.equals(otherResourcePacks);
        }
    }

    private record NamespaceRoot(String namespace, File directory, boolean allowDirectoryAsGuideRoot) {}
}
