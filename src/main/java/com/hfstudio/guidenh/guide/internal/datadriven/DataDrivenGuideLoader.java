package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private static final String DEFAULT_LANGUAGE = "en_us";

    public record PackCandidate(IResourcePack pack, int loadPriority, int order) {

        boolean shouldReplace(PackCandidate previous) {
            return loadPriority > previous.loadPriority()
                || loadPriority == previous.loadPriority() && order > previous.order();
        }
    }

    public record ScanResult(Map<ResourceLocation, MutableGuide> guides, Map<String, LinkedHashSet<String>> pagePaths,
        Map<ResourceLocation, Set<String>> discoveredLanguages) {}

    private record NamespaceRoot(String namespace, File directory, boolean allowDirectoryAsGuideRoot) {}

    private static final Map<Class<?>, Field> LOOSE_ROOT_FIELDS = new IdentityHashMap<>();
    private static volatile List<IResourcePack> lastActiveResourcePacks = List.of();
    private static volatile List<IResourcePack> lastResourceManagerResourcePacks = List.of();
    private static volatile Map<IResourcePack, Set<String>> lastResourceManagerDomainsByPack = Map.of();
    private static final Map<ResourceLocation, List<PackCandidate>> pagePackIndex = new ConcurrentHashMap<>();
    private static volatile boolean indexReady = false;
    private static final AtomicInteger pagePackOrder = new AtomicInteger(0);
    static final Map<File, List<String>> PACK_LANG_FILE_PATHS = new IdentityHashMap<>();

    private static volatile @Nullable ScanCache lastScanCache = null;

    private record ScanCache(List<File> packRoots, String folder, ScanResult result,
        Map<ResourceLocation, List<PackCandidate>> pagePackIndexSnapshot, Map<File, List<String>> langFilePathsSnapshot,
        Map<String, Map<String, String>> langKeys) {

        boolean matches(List<File> roots, String f) {
            return folder.equals(f) && packRoots.equals(roots);
        }
    }

    private static final ConcurrentHashMap<Path, List<NamespaceRoot>> nativeNamespaceRootsCache = new ConcurrentHashMap<>();

    private DataDrivenGuideLoader() {}

    public static ScanResult scanAndBuildAll(String folder) {
        return scanAndBuildAll(folder, getActiveResourcePacks());
    }

    public static ScanResult scanAndBuildAll(String folder, Iterable<? extends IResourcePack> activeResourcePacks) {
        // Cache hit: same pack roots, same folder → restore index and return cached result
        var resolvedPacks = toList(activeResourcePacks);
        var packRoots = resolvePackRoots(resolvedPacks);
        ScanCache cache = lastScanCache;
        if (cache != null && cache.matches(packRoots, folder)) {
            indexReady = true;
            pagePackIndex.putAll(cache.pagePackIndexSnapshot());
            PACK_LANG_FILE_PATHS.putAll(cache.langFilePathsSnapshot());
            GuidePageLanguageIndex.preload(cache.langKeys());
            return cache.result();
        }

        pagePackIndex.clear();
        indexReady = false;
        pagePackOrder.set(0);

        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();
        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();
        var guidePageLangKeys = new LinkedHashMap<String, LinkedHashMap<String, String>>();

        for (var pack : resolvedPacks) {
            var root = getLooseResourcePackRoot(pack);
            if (root == null || !root.exists()) continue;
            if (!root.isDirectory()) {
                scanZipBuildIndex(root, folder, pagePaths, pack, discoveredLanguages, guidePageLangKeys);
            } else {
                scanDirectoryBuildIndex(pack, root, folder, pagePaths, discoveredLanguages, guidePageLangKeys);
            }
        }

        var guides = new LinkedHashMap<ResourceLocation, MutableGuide>();
        for (var entry : discoveredLanguages.entrySet()) {
            guides.put(
                entry.getKey(),
                (MutableGuide) Guide.builder(entry.getKey())
                    .register(false)
                    .folder(folder)
                    .defaultLanguage(DEFAULT_LANGUAGE)
                    .build());
        }

        GuidePageLanguageIndex.preload(freezeLangKeys(guidePageLangKeys));

        // Save cache BEFORE indexReady — pagePackIndex must not be touched by readers
        // during the snapshot (Map.copyOf on ConcurrentHashMap can throw on concurrent read).
        if (!resolvedPacks.isEmpty() && guides.size() > 0) {
            lastScanCache = new ScanCache(
                List.copyOf(packRoots),
                folder,
                new ScanResult(guides, pagePaths, freezeDiscoveredLanguages(discoveredLanguages)),
                new HashMap<>(pagePackIndex),
                new HashMap<>(PACK_LANG_FILE_PATHS),
                freezeLangKeys(guidePageLangKeys));
        }

        indexReady = true;

        return new ScanResult(guides, pagePaths, freezeDiscoveredLanguages(discoveredLanguages));
    }

    public static @Nullable List<PackCandidate> getCandidatesFor(ResourceLocation pageLocation) {
        return pagePackIndex.get(pageLocation);
    }

    public static boolean isIndexPopulated() {
        return indexReady;
    }

    public static void clearCaches() {
        pagePackIndex.clear();
        PACK_LANG_FILE_PATHS.clear();
        nativeNamespaceRootsCache.clear();
        indexReady = false;
        pagePackOrder.set(0);
        // NOTE: lastScanCache is NOT cleared here — it persists across reloads
        // so that GuideReloadListener's second call (same packs) hits cache.
    }

    private static void scanZipBuildIndex(File resourcePackFile, String folder,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths, IResourcePack resourcePack,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        var prefix = "assets/";
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                var path = entry.getName();
                if (!path.startsWith(prefix)) continue;

                if (path.endsWith(".lang")) {
                    collectLangKeys(zip, entry, path, guidePageLangKeys);
                    PACK_LANG_FILE_PATHS.computeIfAbsent(resourcePackFile, k -> new ArrayList<>())
                        .add(path);
                    continue;
                }
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
                discoveredLanguages.computeIfAbsent(new ResourceLocation(namespace, folder), k -> new LinkedHashSet<>())
                    .add(toLanguageCode(language));

                int loadPriority = parseLoadPriorityFromZipEntry(
                    zip,
                    entry,
                    new ResourceLocation(namespace, folder + "/" + language + "/" + pagePath));
                synchronized (pagePackIndex) {
                    pagePackIndex
                        .computeIfAbsent(
                            new ResourceLocation(namespace, folder + "/" + language + "/" + pagePath),
                            k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                    pagePackIndex
                        .computeIfAbsent(
                            new ResourceLocation(namespace, folder + "/" + pagePath),
                            k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                }
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    private static void collectLangKeys(ZipFile zip, ZipEntry entry, String path,
        LinkedHashMap<String, LinkedHashMap<String, String>> guidePageLangKeys) {
        try (var input = zip.getInputStream(entry)) {
            var langFile = StringTranslate.parseLangFile(input);
            for (var langEntry : langFile.entrySet()) {
                if (langEntry.getKey()
                    .startsWith("guidenh.page.")) {
                    int langStart = path.lastIndexOf('/') + 1;
                    int langEnd = path.lastIndexOf('.');
                    if (langStart <= 0 || langEnd <= langStart) continue;
                    guidePageLangKeys
                        .computeIfAbsent(
                            LangUtil.normalizeLanguage(path.substring(langStart, langEnd)),
                            k -> new LinkedHashMap<>())
                        .put(langEntry.getKey(), langEntry.getValue());
                }
            }
        } catch (IOException ignored) {}
    }

    private static int parseLoadPriorityFromZipEntry(ZipFile zip, ZipEntry entry, ResourceLocation loc) {
        try (var stream = zip.getInputStream(entry)) {
            String content = new String(GuideResourceAccess.readFully(stream), StandardCharsets.UTF_8);
            if (content.startsWith("﻿")) content = content.substring(1);
            String yamlText = PageCompiler.extractFrontmatterText(PageCompiler.normalizeLineEndings(content));
            if (yamlText != null) {
                var nav = Frontmatter.parse(loc, yamlText)
                    .navigationEntry();
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
        // .lang files in assets/*/lang/
        File assetsDir = new File(resourcePackRoot, "assets");
        File[] nsDirs = assetsDir.listFiles(File::isDirectory);
        if (nsDirs != null) {
            for (File nsDir : nsDirs) {
                File langDir = new File(nsDir, "lang");
                if (!langDir.isDirectory()) continue;
                File[] langFiles = langDir.listFiles((dir, name) -> name.endsWith(".lang"));
                if (langFiles == null) continue;
                for (File langFile : langFiles) {
                    String fileName = langFile.getName();
                    int dot = fileName.lastIndexOf('.');
                    if (dot <= 0) continue;
                    try (var input = new FileInputStream(langFile)) {
                        var parsed = StringTranslate.parseLangFile(input);
                        for (var entry : parsed.entrySet()) {
                            if (entry.getKey()
                                .startsWith("guidenh.page.")) {
                                guidePageLangKeys
                                    .computeIfAbsent(
                                        LangUtil.normalizeLanguage(fileName.substring(0, dot)),
                                        k -> new LinkedHashMap<>())
                                    .put(entry.getKey(), entry.getValue());
                            }
                        }
                    } catch (IOException ignored) {}
                }
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
            discoveredLanguages
                .computeIfAbsent(new ResourceLocation(namespaceRoot.namespace(), folder), k -> new LinkedHashSet<>())
                .add(toLanguageCode(language));

            var collectors = new LinkedHashSet<String>();
            collectMarkdownPaths(languageDir, "", collectors);

            for (String pagePath : collectors) {
                pagePaths.computeIfAbsent(namespaceRoot.namespace(), k -> new LinkedHashSet<>())
                    .add(pagePath);
                var loc = new ResourceLocation(namespaceRoot.namespace(), folder + "/" + language + "/" + pagePath);
                int loadPriority = parseLoadPriorityFromFile(
                    languageDir.toPath()
                        .resolve(pagePath),
                    loc);
                synchronized (pagePackIndex) {
                    pagePackIndex.computeIfAbsent(loc, k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                    pagePackIndex
                        .computeIfAbsent(
                            new ResourceLocation(namespaceRoot.namespace(), folder + "/" + pagePath),
                            k -> new ArrayList<>())
                        .add(new PackCandidate(resourcePack, loadPriority, pagePackOrder.getAndIncrement()));
                }
            }
        }
    }

    private static int parseLoadPriorityFromFile(Path filePath, ResourceLocation loc) {
        try {
            String content = Files.readString(filePath);
            if (content.startsWith("﻿")) content = content.substring(1);
            String yamlText = PageCompiler.extractFrontmatterText(PageCompiler.normalizeLineEndings(content));
            if (yamlText != null) {
                var nav = Frontmatter.parse(loc, yamlText)
                    .navigationEntry();
                return nav != null ? nav.loadPriority() : 0;
            }
        } catch (IOException ignored) {}
        return 0;
    }

    public static List<String> getLangFilePaths(File resourcePackFile) {
        List<String> paths = PACK_LANG_FILE_PATHS.get(resourcePackFile);
        return paths != null ? paths : List.of();
    }

    public static Map<String, String> readLangFile(IResourcePack resourcePack, String entryPath) {
        if (!entryPath.startsWith("assets/") || !entryPath.endsWith(".lang")) return Map.of();
        var afterAssets = entryPath.substring("assets/".length());
        var firstSlash = afterAssets.indexOf('/');
        if (firstSlash <= 0) return Map.of();
        try (var input = resourcePack.getInputStream(
            new ResourceLocation(afterAssets.substring(0, firstSlash), afterAssets.substring(firstSlash + 1)))) {
            return StringTranslate.parseLangFile(input);
        } catch (IOException e) {
            return Map.of();
        }
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder) {
        return discoverPagePaths(guideId, folder, getActiveResourcePacks());
    }

    public static Set<String> discoverPagePaths(ResourceLocation guideId, String folder,
        Iterable<? extends IResourcePack> activeResourcePacks) {
        var result = new LinkedHashSet<String>();
        for (var pack : activeResourcePacks) {
            var root = getLooseResourcePackRoot(pack);
            if (root == null || !root.exists()) continue;
            if (root.isDirectory()) {
                for (File guideRoot : guideRootCandidates(root, guideId.getResourceDomain(), folder)) {
                    var langDirs = guideRoot.listFiles(File::isDirectory);
                    if (langDirs == null) continue;
                    for (var langDir : langDirs) {
                        if (isLanguageFolder(langDir.getName())) {
                            collectMarkdownPaths(langDir, "", result);
                        }
                    }
                }
            } else {
                scanZipPagePaths(root, "assets/" + guideId.getResourceDomain() + "/" + folder + "/", result);
            }
        }
        return result;
    }

    public static void scanZipPagePaths(File resourcePackFile, String prefix, Set<String> pagePaths) {
        try (var zip = new ZipFile(resourcePackFile)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                var path = entry.getName();
                if (!path.startsWith(prefix) || !path.endsWith(".md")) continue;
                var relative = path.substring(prefix.length());
                var slashIndex = relative.indexOf('/');
                if (slashIndex <= 0) continue;
                if (!isLanguageFolder(relative.substring(0, slashIndex))) continue;
                var pagePath = relative.substring(slashIndex + 1);
                if (!pagePath.isEmpty()) pagePaths.add(pagePath);
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan zip for pages: {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    public static void collectMarkdownPaths(File directory, String relativePath, Set<String> pagePaths) {
        var children = directory.listFiles();
        if (children == null) return;
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

    public static boolean isLanguageFolder(String name) {
        return name.startsWith(LANGUAGE_FOLDER_PREFIX) && LangUtil.isLanguageCode(name.substring(1));
    }

    public static String toLanguageCode(String folderName) {
        return LangUtil.normalizeLanguage(folderName.substring(LANGUAGE_FOLDER_PREFIX.length()));
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

    public static File getResourcePackFile(IResourcePack resourcePack) {
        if (resourcePack instanceof DirectoryResourcePack) {
            return ((DirectoryResourcePack) resourcePack).getRoot()
                .toFile();
        }
        if (!(resourcePack instanceof AbstractResourcePack)) return null;
        try {
            return ((AccessorAbstractResourcePack) resourcePack).guidenh$getResourcePackFile();
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to resolve backing file for pack {}",
                resourcePack.getPackName(),
                e);
            return null;
        }
    }

    public static File getLooseResourcePackRoot(IResourcePack resourcePack) {
        File resourcePackFile = getResourcePackFile(resourcePack);
        if (resourcePackFile != null) return resourcePackFile;
        Field field = findLooseRootField(resourcePack.getClass());
        if (field == null) return null;
        try {
            Object value = field.get(resourcePack);
            if (value instanceof Path path) return path.toFile();
            if (value instanceof File file) return file;
        } catch (IllegalAccessException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] Failed to resolve directory root for pack {}",
                resourcePack.getPackName(),
                e);
        }
        return null;
    }

    private static Field findLooseRootField(Class<?> resourcePackClass) {
        synchronized (LOOSE_ROOT_FIELDS) {
            if (LOOSE_ROOT_FIELDS.containsKey(resourcePackClass)) return LOOSE_ROOT_FIELDS.get(resourcePackClass);
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

    private static void addConfiguredResourcePacks(LinkedHashSet<IResourcePack> resourcePacks) {
        try {
            var accessor = (AccessorFMLClientHandler) FMLClientHandler.instance();
            var basePacks = accessor.guidenh$getResourcePackList();
            if (basePacks != null) resourcePacks.addAll(basePacks);
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways("[GuideNH] [DataDrivenGuideLoader] Failed to inspect base resource packs", e);
        }
        var repository = Minecraft.getMinecraft()
            .getResourcePackRepository();
        for (var entry : repository.getRepositoryEntries()) {
            var pack = entry.getResourcePack();
            if (pack != null) resourcePacks.add(pack);
        }
        var serverPack = repository.func_148530_e();
        if (serverPack != null) resourcePacks.add(serverPack);
    }

    private static void addResourceManagerResourcePacks(IResourceManager resourceManager,
        LinkedHashSet<IResourcePack> resourcePacks,
        IdentityHashMap<IResourcePack, LinkedHashSet<String>> domainsByPack) {
        if (!(resourceManager instanceof SimpleReloadableResourceManager)) return;
        try {
            var accessor = (AccessorSimpleReloadableResourceManager) resourceManager;
            Map<String, FallbackResourceManager> domainManagers = accessor.guidenh$getDomainResourceManagers();
            if (domainManagers == null || domainManagers.isEmpty()) return;
            for (String domain : resourceManager.getResourceDomains()) {
                FallbackResourceManager fallback = domainManagers.get(domain);
                if (fallback == null) continue;
                var packs = ((AccessorFallbackResourceManager) fallback).guidenh$getResourcePacks();
                if (packs != null) {
                    for (IResourcePack pack : packs) {
                        resourcePacks.add(pack);
                        domainsByPack.computeIfAbsent(pack, ignored -> new LinkedHashSet<>())
                            .add(domain);
                    }
                }
            }
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways("[GuideNH] [DataDrivenGuideLoader] Failed to inspect resource manager packs", e);
        }
    }

    private static Set<String> getResourceDomains(IResourcePack resourcePack) {
        Set<String> cached = lastResourceManagerDomainsByPack.get(resourcePack);
        return cached != null ? cached : resourcePack.getResourceDomains();
    }

    private static Map<IResourcePack, Set<String>> freezeDomainsByPack(
        IdentityHashMap<IResourcePack, LinkedHashSet<String>> domainsByPack) {
        if (domainsByPack.isEmpty()) return Map.of();
        var result = new IdentityHashMap<IResourcePack, Set<String>>();
        for (var entry : domainsByPack.entrySet()) {
            result.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    public static byte[] readBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        try (var input = resourcePack.getInputStream(resourceLocation)) {
            return GuideResourceAccess.readFully(input);
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            GuideDebugLog.warnAlways(
                "[GuideNH] [DataDrivenGuideLoader] readBytes failed for {} from pack {}: {}",
                resourceLocation,
                resourcePack.getPackName(),
                e.toString());
            return null;
        }
    }

    public static IResourcePack findResourcePack(ResourceLocation resourceLocation) {
        return findResourcePack(resourceLocation, getActiveResourcePacks());
    }

    public static IResourcePack findResourcePack(ResourceLocation resourceLocation,
        Iterable<? extends IResourcePack> resourcePacks) {
        var candidates = getCandidatesFor(resourceLocation);
        if (candidates != null && !candidates.isEmpty()) return candidates.get(0)
            .pack();
        if (indexReady) return null;
        for (var pack : resourcePacks) {
            try {
                pack.getInputStream(resourceLocation)
                    .close();
                return pack;
            } catch (IOException ignored) {}
        }
        return null;
    }

    private static List<File> guideRootCandidates(File resourcePackRoot, String namespace, String folder) {
        var candidates = new LinkedHashMap<Path, File>(3);
        for (NamespaceRoot nr : discoverNamespaceRoots(resourcePackRoot)) {
            if (nr.namespace()
                .equals(namespace)) {
                addGuideRootCandidates(candidates, nr, folder);
            }
        }
        addGuideRootCandidate(candidates, resourcePackRoot, "assets/" + namespace + "/" + folder + "/");
        addGuideRootCandidate(candidates, resourcePackRoot, namespace + "/" + folder + "/");
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
        var candidate = new File(resourcePackRoot, relativePath.replace('/', File.separatorChar));
        if (candidate.isDirectory()) {
            candidates.putIfAbsent(
                candidate.toPath()
                    .toAbsolutePath()
                    .normalize(),
                candidate);
        }
    }

    private static List<NamespaceRoot> discoverNamespaceRoots(File resourcePackRoot) {
        var byPath = new LinkedHashMap<Path, NamespaceRoot>();

        var assetsDir = new File(resourcePackRoot, "assets");
        var assetDirs = assetsDir.listFiles(File::isDirectory);
        if (assetDirs != null) {
            for (var dir : assetDirs) {
                addNamespaceRoot(byPath, new NamespaceRoot(namespaceFromDirectoryName(dir.getName()), dir, false));
            }
        }

        Path cacheKey = resourcePackRoot.toPath()
            .toAbsolutePath()
            .normalize();
        List<NamespaceRoot> cached = nativeNamespaceRootsCache.get(cacheKey);
        if (cached != null) {
            for (var nr : cached) {
                addNamespaceRoot(byPath, nr);
            }
            return List.copyOf(byPath.values());
        }

        var nativeRoots = new ArrayList<NamespaceRoot>();
        var nativeDirs = resourcePackRoot.listFiles(File::isDirectory);
        if (nativeDirs != null) {
            for (var dir : nativeDirs) {
                if ("assets".equals(dir.getName())) continue;
                var nr = new NamespaceRoot(namespaceFromDirectoryName(dir.getName()), dir, true);
                addNamespaceRoot(byPath, nr);
                nativeRoots.add(nr);
            }
        }
        nativeNamespaceRootsCache.put(cacheKey, List.copyOf(nativeRoots));
        return List.copyOf(byPath.values());
    }

    private static void addNamespaceRoot(LinkedHashMap<Path, NamespaceRoot> roots, NamespaceRoot nr) {
        roots.putIfAbsent(
            nr.directory()
                .toPath()
                .toAbsolutePath()
                .normalize(),
            nr);
    }

    private static String namespaceFromDirectoryName(String directoryName) {
        if (isValidNamespace(directoryName)) return directoryName;
        int openBracket = directoryName.lastIndexOf('[');
        if (openBracket < 0 || !directoryName.endsWith("]")) return directoryName;
        return directoryName.substring(openBracket + 1, directoryName.length() - 1);
    }

    private static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) return false;
        for (int i = 0; i < namespace.length(); i++) {
            char ch = namespace.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '_' || ch == '-' || ch == '.') continue;
            return false;
        }
        return true;
    }

    private static List<IResourcePack> toList(Iterable<? extends IResourcePack> resourcePacks) {
        var result = new ArrayList<IResourcePack>();
        for (IResourcePack pack : resourcePacks) result.add(pack);
        return result;
    }

    private static List<File> resolvePackRoots(List<IResourcePack> packs) {
        var roots = new ArrayList<File>(packs.size());
        for (var pack : packs) {
            File root = getLooseResourcePackRoot(pack);
            if (root != null) roots.add(root);
        }
        return roots;
    }

    private static Map<String, Map<String, String>> freezeLangKeys(
        LinkedHashMap<String, LinkedHashMap<String, String>> keys) {
        if (keys.isEmpty()) return Map.of();
        var frozen = new LinkedHashMap<String, Map<String, String>>();
        for (var entry : keys.entrySet()) frozen.put(entry.getKey(), Map.copyOf(entry.getValue()));
        return Collections.unmodifiableMap(frozen);
    }

    private static Map<ResourceLocation, Set<String>> freezeDiscoveredLanguages(
        LinkedHashMap<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {
        if (discoveredLanguages.isEmpty()) return Map.of();
        var frozen = new LinkedHashMap<ResourceLocation, Set<String>>();
        for (var entry : discoveredLanguages.entrySet()) {
            frozen.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(frozen);
    }

    @Deprecated
    public static byte[] readLooseBytes(IResourcePack resourcePack, ResourceLocation resourceLocation) {
        return null;
    }

    @Deprecated
    public static void buildPageIndex(String folder) {
        scanAndBuildAll(folder);
    }

    @Deprecated
    public static void buildPageIndex(String folder, Iterable<? extends IResourcePack> packs) {
        scanAndBuildAll(folder, packs);
    }

    @Deprecated
    public static @Nullable List<IResourcePack> getPacksFor(ResourceLocation loc) {
        var candidates = pagePackIndex.get(loc);
        return candidates != null ? candidates.stream()
            .map(PackCandidate::pack)
            .toList() : null;
    }
}
