package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.regex.Pattern;
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
import com.hfstudio.guidenh.guide.internal.DirectoryResourcePack;
import com.hfstudio.guidenh.guide.internal.GuideDevelopmentResourcePacks;
import com.hfstudio.guidenh.guide.internal.MutableGuide;
import com.hfstudio.guidenh.guide.internal.resource.GuideResourceAccess;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.hfstudio.guidenh.mixins.early.fml.AccessorFMLClientHandler;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorAbstractResourcePack;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorFallbackResourceManager;
import com.hfstudio.guidenh.mixins.early.minecraft.AccessorSimpleReloadableResourceManager;

import cpw.mods.fml.client.FMLClientHandler;

public class DataDrivenGuideLoader {

    // Matches the numeric format placeholders normalized by StringTranslate.parseLangFile.
    private static final Pattern NUMERIC_LANG_VARIABLE = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");

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

    private record GuideLanguage(String namespace, String language) {}

    private record PackEntry(String namespace, String language, String relativePath, int loadPriority) {}

    private record PackScan(List<PackEntry> entries, List<String> langPaths, List<GuideLanguage> languages) {}

    private record ZipCacheKey(String folder, ResourcePackViewKey view) {}

    private record CachedZipScan(long size, long lastModified, PackScan scan) {

        boolean matches(File root) {
            return !root.isDirectory() && size == root.length() && lastModified == root.lastModified();
        }
    }

    private static final Map<Class<?>, Field> LOOSE_ROOT_FIELDS = new IdentityHashMap<>();
    private static volatile List<IResourcePack> lastActiveResourcePacks = List.of();
    private static volatile List<IResourcePack> lastResourceManagerResourcePacks = List.of();
    private static volatile Map<IResourcePack, Set<String>> lastResourceManagerDomainsByPack = Map.of();
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

    private static final ConcurrentHashMap<Path, List<NamespaceRoot>> nativeNamespaceRootsCache = new ConcurrentHashMap<>();

    private DataDrivenGuideLoader() {}

    public static ScanResult scanAndBuildAll(String folder) {
        return scanAndBuildAll(folder, getActiveResourcePacks());
    }

    public static ScanResult scanAndBuildAll(String folder, Iterable<? extends IResourcePack> activeResourcePacks) {
        var resolvedPacks = toList(activeResourcePacks);

        pagePackIndex.clear();
        assetPackIndex.clear();
        PACK_LANG_FILE_PATHS.clear();
        nativeNamespaceRootsCache.clear();
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
                // Validating a directory cache requires touching the same files anyway, so scan
                // once and rebuild the small indexes directly instead of hashing and rescanning.
                scan = scanDirectoryPack(root, folder);
            } else {
                var cacheKey = new ZipCacheKey(folder, resourcePackViewKey(pack));
                var cached = previousZipCache.get(cacheKey);

                if (cached != null && cached.matches(root)) {
                    scan = cached.scan();
                } else {
                    scan = scanZipPack(root, folder, pack);
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
        nativeNamespaceRootsCache.clear();
        indexReady = false;
        pagePackOrder.set(0);
        // Keep immutable ZIP scans; archive size + timestamp cheaply validates them next time.
    }

    private static PackScan scanZipPack(File resourcePackFile, String folder, IResourcePack resourcePack) {
        var entries = new ArrayList<PackEntry>();
        var langPaths = new ArrayList<String>();
        var languages = new LinkedHashSet<GuideLanguage>();
        var prefix = "assets/";

        try (var zip = new ZipFile(resourcePackFile)) {
            var zipEntries = zip.entries();
            while (zipEntries.hasMoreElements()) {
                var entry = zipEntries.nextElement();
                if (entry.isDirectory()) continue;

                var path = entry.getName();
                if (!path.startsWith(prefix)) continue;

                var afterAssets = path.substring(prefix.length());
                var firstSlash = afterAssets.indexOf('/');
                if (firstSlash <= 0) continue;

                var namespace = afterAssets.substring(0, firstSlash);
                var afterNamespace = afterAssets.substring(firstSlash + 1);

                if (path.endsWith(".lang")) {
                    var resourceLocation = new ResourceLocation(namespace, afterNamespace);
                    if (resourceExists(resourcePack, resourceLocation)) {
                        langPaths.add(path);
                    }
                    continue;
                }
                if (!afterNamespace.startsWith(folder + "/")) continue;

                var resourceLocation = new ResourceLocation(namespace, afterNamespace);
                if (!resourceExists(resourcePack, resourceLocation)) continue;

                var afterFolder = afterNamespace.substring(folder.length() + 1);
                var slashIndex = afterFolder.indexOf('/');
                if (slashIndex <= 0) continue;

                var language = afterFolder.substring(0, slashIndex);
                if (!isLanguageFolder(language)) continue;

                var relativePath = afterFolder.substring(slashIndex + 1);
                if (relativePath.isEmpty()) continue;

                if (path.endsWith(".md")) {
                    int loadPriority;
                    try {
                        loadPriority = parseLoadPriority(zip.getInputStream(entry), resourceLocation);
                    } catch (IOException e) {
                        loadPriority = 0;
                    }

                    entries.add(new PackEntry(namespace, language, relativePath, loadPriority));
                    languages.add(new GuideLanguage(namespace, toLanguageCode(language)));
                } else {
                    entries.add(new PackEntry(namespace, language, relativePath, 0));
                }
            }
        } catch (IOException e) {
            GuideDebugLog.warn(
                "[GuideNH] [DataDrivenGuideLoader] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
            return null;
        }

        return new PackScan(entries, langPaths, List.copyOf(languages));
    }

    private static PackScan scanDirectoryPack(File resourcePackRoot, String folder) {
        var entries = new ArrayList<PackEntry>();
        var langPaths = new ArrayList<String>();
        var languages = new LinkedHashSet<GuideLanguage>();

        for (NamespaceRoot namespaceRoot : discoverNamespaceRoots(resourcePackRoot)) {
            scanGuideDirectory(namespaceRoot, folder, entries, languages);
            collectNamespaceLangPaths(resourcePackRoot, namespaceRoot.directory(), langPaths);
        }

        return new PackScan(entries, langPaths, List.copyOf(languages));
    }

    private static void scanGuideDirectory(NamespaceRoot namespaceRoot, String folder, List<PackEntry> entries,
        Set<GuideLanguage> languages) {

        File guideRoot = new File(namespaceRoot.directory(), folder);
        if (!guideRoot.isDirectory()) return;

        File[] languageDirs = guideRoot.listFiles(File::isDirectory);
        if (languageDirs == null) return;

        for (File languageDir : languageDirs) {
            String language = languageDir.getName();
            if (!isLanguageFolder(language)) continue;

            languages.add(new GuideLanguage(namespaceRoot.namespace(), toLanguageCode(language)));

            Path languageRoot = languageDir.toPath();
            try (var paths = Files.walk(languageRoot)) {
                paths.forEach(path -> {
                    if (!Files.isRegularFile(path)) return;

                    String relativePath = languageRoot.relativize(path)
                        .toString();
                    if (relativePath.isEmpty() || relativePath.endsWith(".lang")) return;
                    if (File.separatorChar != '/') relativePath = relativePath.replace(File.separatorChar, '/');

                    int loadPriority = 0;
                    if (relativePath.endsWith(".md")) {
                        var location = new ResourceLocation(
                            namespaceRoot.namespace(),
                            folder + "/" + language + "/" + relativePath);
                        loadPriority = parseLoadPriority(path, location);
                    }
                    entries.add(new PackEntry(namespaceRoot.namespace(), language, relativePath, loadPriority));
                });
            } catch (IOException e) {
                GuideDebugLog
                    .warn("[GuideNH] [DataDrivenGuideLoader] Failed to scan guide files in {}", languageDir, e);
            }
        }
    }

    private static void collectNamespaceLangPaths(File resourcePackRoot, File namespaceRoot, List<String> langPaths) {
        Path langRoot = namespaceRoot.toPath()
            .resolve("lang");
        if (!Files.isDirectory(langRoot)) return;

        try (var paths = Files.walk(langRoot)) {
            paths.filter(Files::isRegularFile)
                .filter(
                    path -> path.getFileName()
                        .toString()
                        .endsWith(".lang"))
                .map(resourcePackRoot.toPath()::relativize)
                .map(Path::toString)
                .map(path -> path.replace(File.separatorChar, '/'))
                .forEach(langPaths::add);
        } catch (IOException e) {
            GuideDebugLog.warn("[GuideNH] [DataDrivenGuideLoader] Failed to index language files in {}", langRoot, e);
        }
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
            if (entry.relativePath().endsWith(".md")) {
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

        index
            .computeIfAbsent(
                new ResourceLocation(entry.namespace(), folder + "/" + entry.language() + "/" + entry.relativePath()),
                k -> new ArrayList<>())
            .add(new PackCandidate(resourcePack, entry.loadPriority(), pagePackOrder.getAndIncrement()));

        index
            .computeIfAbsent(
                new ResourceLocation(entry.namespace(), folder + "/" + entry.relativePath()),
                k -> new ArrayList<>())
            .add(new PackCandidate(resourcePack, entry.loadPriority(), pagePackOrder.getAndIncrement()));
    }

    private static int parseLoadPriority(Path path, ResourceLocation location) {
        try {
            return parseLoadPriority(Files.newInputStream(path), location);
        } catch (IOException ignored) {
            return 0;
        }
    }

    /**
     * Reads only the leading YAML frontmatter needed to resolve navigation load priority
     */
    private static int parseLoadPriority(InputStream input, ResourceLocation location) {
        try (input; var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine != null && firstLine.startsWith("\uFEFF")) {
                firstLine = firstLine.substring(1);
            }
            if (!"---".equals(firstLine)) {
                return 0;
            }

            var frontmatter = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if ("---".equals(line)) {
                    var navigation = Frontmatter.parse(location, frontmatter.toString())
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

    public static Map<String, String> readLangFile(IResourcePack resourcePack, String entryPath) {
        ResourceLocation location = getLangResourceLocation(entryPath);
        if (location == null || !resourceExists(resourcePack, location)) return Map.of();
        try (var input = resourcePack.getInputStream(location)) {
            return StringTranslate.parseLangFile(input);
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
    }

    /**
     * Reads runtime language entries once for the localization index. Large page-body entries are
     * intentionally skipped; they are resolved by {@code GuidePageLanguageIndex} on demand.
     */
    public static Map<String, String> readRuntimeLangValues(IResourcePack resourcePack, String entryPath) {
        ResourceLocation location = getLangResourceLocation(entryPath);
        if (location == null || !resourceExists(resourcePack, location)) return Map.of();
        var values = new LinkedHashMap<String, String>();
        try (var input = resourcePack.getInputStream(location);
            var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // StringTranslate.parseLangFile accepts the first '=' separator. Keep the same
                // format while avoiding a parser/stream allocation for every entry in large files.
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.isEmpty() || line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator <= 0) continue;
                String key = line.substring(0, separator);
                if (key.startsWith("guidenh.page.")) continue;
                String value = line.substring(separator + 1);
                values.put(
                    key,
                    value.indexOf('%') >= 0 ? NUMERIC_LANG_VARIABLE.matcher(value)
                        .replaceAll("%$1s") : value);
            }
        } catch (IOException | RuntimeException ignored) {}
        return values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    public static @Nullable String readLangValue(IResourcePack resourcePack, String entryPath, String key) {
        ResourceLocation location = getLangResourceLocation(entryPath);
        if (location == null || !resourceExists(resourcePack, location)) return null;
        try (var input = resourcePack.getInputStream(location);
            var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator > 0 && key.equals(line.substring(0, separator))) {
                    // The line has already been split using the same first '=' rule as
                    // StringTranslate.parseLangFile; avoid reparsing a one-line stream.
                    String value = line.substring(separator + 1);
                    return value.indexOf('%') >= 0 ? NUMERIC_LANG_VARIABLE.matcher(value)
                        .replaceAll("%$1s") : value;
                }
            }
        } catch (IOException | RuntimeException ignored) {}
        return null;
    }

    /** Reads only keys from a language file, avoiding retention of large translated page bodies. */
    public static Set<String> readLangKeys(IResourcePack resourcePack, String entryPath) {
        ResourceLocation location = getLangResourceLocation(entryPath);
        if (location == null || !resourceExists(resourcePack, location)) return Set.of();
        var keys = new LinkedHashSet<String>();
        try (var input = resourcePack.getInputStream(location);
            var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.startsWith("#")) continue;
                int separator = line.indexOf('=');
                if (separator > 0) keys.add(line.substring(0, separator));
            }
        } catch (IOException | RuntimeException ignored) {}
        return keys.isEmpty() ? Set.of() : Set.copyOf(keys);
    }

    private static @Nullable ResourceLocation getLangResourceLocation(@Nullable String entryPath) {
        if (entryPath == null || !entryPath.endsWith(".lang")) return null;
        var afterAssets = entryPath.startsWith("assets/") ? entryPath.substring("assets/".length()) : entryPath;
        var firstSlash = afterAssets.indexOf('/');
        return firstSlash > 0
            ? new ResourceLocation(afterAssets.substring(0, firstSlash), afterAssets.substring(firstSlash + 1))
            : null;
    }

    private static boolean resourceExists(IResourcePack resourcePack, ResourceLocation location) {
        try {
            return resourcePack.resourceExists(location);
        } catch (RuntimeException ignored) {
            // Third-party resource packs occasionally implement a missing resource as a runtime failure.
            return false;
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
            GuideDebugLog.warn(
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
