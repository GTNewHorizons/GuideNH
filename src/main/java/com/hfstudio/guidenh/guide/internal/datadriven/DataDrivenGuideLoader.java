package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
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

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.Guide;
import com.hfstudio.guidenh.guide.internal.DirectoryResourcePack;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.datadriven.GuideResourcePackScanner.GuideLanguage;
import com.hfstudio.guidenh.guide.internal.datadriven.GuideResourcePackScanner.PackEntry;
import com.hfstudio.guidenh.guide.internal.datadriven.GuideResourcePackScanner.PackScan;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorAbstractResourcePack;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorFallbackResourceManager;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorSimpleReloadableResourceManager;

import cpw.mods.fml.client.FMLClientHandler;

public class DataDrivenGuideLoader {

    public static final String AUTO_GUIDE_FOLDER = "guidenh";
    private static final String DEFAULT_LANGUAGE = "en_us";

    public record PackCandidate(IResourcePack pack, ResourceLocation resourceLocation, int order) {}

    public record ScanResult(Map<ResourceLocation, MutableGuide> guides, Map<String, LinkedHashSet<String>> pagePaths,
        Map<ResourceLocation, Set<String>> discoveredLanguages) {}

    private record ZipCacheKey(String folder, ResourcePackViewKey view) {}

    private record CachedZipScan(long size, long lastModified, PackScan scan) {

        boolean matches(File root) {
            return !root.isDirectory() && size == root.length() && lastModified == root.lastModified();
        }
    }

    private static final Map<Class<?>, Field> LOOSE_ROOT_FIELDS = new IdentityHashMap<>();
    private static volatile List<IResourcePack> lastActiveResourcePacks = List.of();
    private static volatile List<IResourcePack> lastResourceManagerResourcePacks = List.of();
    private static final Map<ResourceLocation, List<PackCandidate>> pagePackIndex = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<PackCandidate>> assetPackIndex = new ConcurrentHashMap<>();
    private static volatile boolean indexReady = false;
    private static final AtomicInteger pagePackOrder = new AtomicInteger(0);
    /**
     * Language paths belong to a resource-pack view, rather than its backing file alone. A single
     * mod jar may expose both its root assets and a built-in pack rooted below
     * {@code resourcepacks/}; their available paths are not interchangeable.
     */
    private static final Map<ResourcePackViewKey, List<String>> PACK_LANG_FILE_PATHS = new HashMap<>();

    private static volatile Map<ZipCacheKey, CachedZipScan> lastZipScanCache = Map.of();

    private DataDrivenGuideLoader() {}

    public static ScanResult scanAndBuildAll(String folder) {
        return scanAndBuildAll(folder, getActiveResourcePacks());
    }

    public static ScanResult scanAndBuildAll(String folder, Iterable<? extends IResourcePack> activeResourcePacks) {
        var resolvedPacks = toList(activeResourcePacks);

        pagePackIndex.clear();
        assetPackIndex.clear();
        PACK_LANG_FILE_PATHS.clear();
        GuideResourcePackScanner.clearCaches();
        indexReady = false;
        pagePackOrder.set(0);

        var pagePaths = new LinkedHashMap<String, LinkedHashSet<String>>();
        var discoveredLanguages = new LinkedHashMap<ResourceLocation, LinkedHashSet<String>>();
        var previousZipCache = lastZipScanCache;
        var nextZipCache = new HashMap<ZipCacheKey, CachedZipScan>();

        for (var pack : resolvedPacks) {
            var root = getLooseResourcePackRoot(pack);
            if (root == null || !root.exists()) continue;

            final PackScan scan;
            if (root.isDirectory()) {
                scan = GuideResourcePackScanner.scanDirectoryPack(root, folder);
            } else {
                var cacheKey = new ZipCacheKey(folder, resourcePackViewKey(pack));
                var cached = previousZipCache.get(cacheKey);

                if (cached != null && cached.matches(root)) {
                    scan = cached.scan();
                } else {
                    scan = GuideResourcePackScanner.scanZipPack(root, folder, pack);
                    if (scan == null) continue;
                }

                nextZipCache.put(cacheKey, new CachedZipScan(root.length(), root.lastModified(), scan));
            }

            applyPackScan(pack, folder, scan, pagePaths, discoveredLanguages);
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

        lastZipScanCache = nextZipCache;
        indexReady = true;

        return new ScanResult(guides, pagePaths, freezeDiscoveredLanguages(discoveredLanguages));
    }

    public static @Nullable List<PackCandidate> getCandidatesFor(ResourceLocation pageLocation) {
        return pagePackIndex.get(pageLocation);
    }

    public static @Nullable List<PackCandidate> getAssetCandidatesFor(ResourceLocation assetId) {
        return assetPackIndex.get(assetId);
    }

    public static boolean isIndexPopulated() {
        return indexReady;
    }

    public static void ensureIndexReady(IResourceManager resourceManager) {
        if (isIndexPopulated()) {
            return;
        }
        synchronized (DataDrivenGuideLoader.class) {
            if (isIndexPopulated()) {
                return;
            }
            scanAndBuildAll(AUTO_GUIDE_FOLDER, getActiveResourcePacks(resourceManager));
        }
    }

    public static void clearCaches() {
        pagePackIndex.clear();
        assetPackIndex.clear();
        PACK_LANG_FILE_PATHS.clear();
        GuideResourcePackScanner.clearCaches();
        indexReady = false;
        pagePackOrder.set(0);
        // Keep immutable ZIP scans; archive size + timestamp cheaply validates them next time.
    }

    private static void applyPackScan(IResourcePack resourcePack, String folder, PackScan scan,
        LinkedHashMap<String, LinkedHashSet<String>> pagePaths,
        Map<ResourceLocation, LinkedHashSet<String>> discoveredLanguages) {

        if (!scan.langPaths()
            .isEmpty()) {
            PACK_LANG_FILE_PATHS.put(resourcePackViewKey(resourcePack), scan.langPaths());
        }

        for (GuideLanguage language : scan.languages()) {
            discoveredLanguages
                .computeIfAbsent(new ResourceLocation(language.namespace(), folder), k -> new LinkedHashSet<>())
                .add(language.language());
        }

        for (PackEntry entry : scan.entries()) {
            if (entry.relativePath()
                .endsWith(".md")) {
                pagePaths.computeIfAbsent(entry.namespace(), k -> new LinkedHashSet<>())
                    .add(entry.relativePath());
                addPackCandidates(pagePackIndex, resourcePack, folder, entry);
            } else {
                addPackCandidates(assetPackIndex, resourcePack, folder, entry);
            }
        }
    }

    private static void addPackCandidates(Map<ResourceLocation, List<PackCandidate>> index, IResourcePack resourcePack,
        String folder, PackEntry entry) {

        var resourceLocation = new ResourceLocation(
            entry.namespace(),
            folder + "/" + entry.language() + "/" + entry.relativePath());

        index.computeIfAbsent(resourceLocation, k -> new ArrayList<>())
            .add(new PackCandidate(resourcePack, resourceLocation, pagePackOrder.getAndIncrement()));

        index
            .computeIfAbsent(
                new ResourceLocation(entry.namespace(), folder + "/" + entry.relativePath()),
                k -> new ArrayList<>())
            .add(new PackCandidate(resourcePack, resourceLocation, pagePackOrder.getAndIncrement()));
    }

    public static List<String> getLangFilePaths(IResourcePack resourcePack) {
        List<String> paths = PACK_LANG_FILE_PATHS.get(resourcePackViewKey(resourcePack));
        return paths != null ? paths : List.of();
    }

    private record ResourcePackViewKey(@Nullable File root, Class<?> packType, String packName) {}

    private static ResourcePackViewKey resourcePackViewKey(IResourcePack resourcePack) {
        File root = getLooseResourcePackRoot(resourcePack);
        return new ResourcePackViewKey(
            root != null ? normalizePackRoot(root) : null,
            resourcePack.getClass(),
            resourcePack.getPackName());
    }

    private static File normalizePackRoot(File resourcePackFile) {
        return resourcePackFile.toPath()
            .toAbsolutePath()
            .normalize()
            .toFile();
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
                for (File guideRoot : GuideResourcePackScanner
                    .guideRootCandidates(root, guideId.getResourceDomain(), folder)) {
                    var langDirs = guideRoot.listFiles(File::isDirectory);
                    if (langDirs == null) continue;
                    for (var langDir : langDirs) {
                        if (GuideResourcePackScanner.isLanguageFolder(langDir.getName())) {
                            GuideResourcePackScanner.collectMarkdownPaths(langDir, "", result);
                        }
                    }
                }
            } else {
                GuideResourcePackScanner
                    .scanZipPagePaths(root, "assets/" + guideId.getResourceDomain() + "/" + folder + "/", result);
            }
        }
        return result;
    }

    public static void scanZipPagePaths(File resourcePackFile, String prefix, Set<String> pagePaths) {
        GuideResourcePackScanner.scanZipPagePaths(resourcePackFile, prefix, pagePaths);
    }

    public static void collectMarkdownPaths(File directory, String relativePath, Set<String> pagePaths) {
        GuideResourcePackScanner.collectMarkdownPaths(directory, relativePath, pagePaths);
    }

    public static boolean isLanguageFolder(String name) {
        return GuideResourcePackScanner.isLanguageFolder(name);
    }

    public static String toLanguageCode(String folderName) {
        return GuideResourcePackScanner.toLanguageCode(folderName);
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
            GuideDebugLog.warn(
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
            GuideDebugLog.warn(
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
            GuideDebugLog.warn("[GuideNH] [DataDrivenGuideLoader] Failed to inspect base resource packs", e);
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
            GuideDebugLog.warn("[GuideNH] [DataDrivenGuideLoader] Failed to inspect resource manager packs", e);
        }
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
            GuideDebugLog.warn(
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

    private static List<IResourcePack> toList(Iterable<? extends IResourcePack> resourcePacks) {
        var result = new ArrayList<IResourcePack>();
        for (IResourcePack pack : resourcePacks) result.add(pack);
        return result;
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
