package com.hfstudio.guidenh.guide.internal.datadriven;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

final class GuideResourcePackScanner {

    private static final String LANGUAGE_FOLDER_PREFIX = "_";

    record GuideLanguage(String namespace, String language) {}

    record PackEntry(String namespace, String language, String relativePath) {}

    record PackScan(List<PackEntry> entries, List<String> langPaths, List<GuideLanguage> languages) {}

    private record NamespaceRoot(String namespace, File directory, boolean allowDirectoryAsGuideRoot) {}

    private static final ConcurrentHashMap<Path, List<NamespaceRoot>> nativeNamespaceRootsCache = new ConcurrentHashMap<>();

    private GuideResourcePackScanner() {}

    static void clearCaches() {
        nativeNamespaceRootsCache.clear();
    }

    static @Nullable PackScan scanZipPack(File resourcePackFile, String folder, IResourcePack resourcePack) {
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

                entries.add(new PackEntry(namespace, language, relativePath));
                if (path.endsWith(".md")) {
                    languages.add(new GuideLanguage(namespace, toLanguageCode(language)));
                }
            }
        } catch (IOException e) {
            GuideDebugLog.warn(
                "[GuideNH] [GuideResourcePackScanner] Failed to scan guide pages from resource pack {}",
                resourcePackFile.getAbsolutePath(),
                e);
            return null;
        }

        return new PackScan(entries, langPaths, List.copyOf(languages));
    }

    static PackScan scanDirectoryPack(File resourcePackRoot, String folder) {
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

            Path languageRoot = languageDir.toPath();
            try (var paths = Files.walk(languageRoot)) {
                paths.forEach(path -> {
                    if (!Files.isRegularFile(path)) return;

                    String relativePath = languageRoot.relativize(path)
                        .toString();
                    if (relativePath.isEmpty() || relativePath.endsWith(".lang")) return;
                    if (File.separatorChar != '/') relativePath = relativePath.replace(File.separatorChar, '/');

                    entries.add(new PackEntry(namespaceRoot.namespace(), language, relativePath));
                    if (relativePath.endsWith(".md")) {
                        languages.add(new GuideLanguage(namespaceRoot.namespace(), toLanguageCode(language)));
                    }
                });
            } catch (IOException e) {
                GuideDebugLog
                    .warn("[GuideNH] [GuideResourcePackScanner] Failed to scan guide files in {}", languageDir, e);
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
            GuideDebugLog
                .warn("[GuideNH] [GuideResourcePackScanner] Failed to index language files in {}", langRoot, e);
        }
    }

    static List<File> guideRootCandidates(File resourcePackRoot, String namespace, String folder) {
        var candidates = new LinkedHashMap<Path, File>(3);
        for (NamespaceRoot namespaceRoot : discoverNamespaceRoots(resourcePackRoot)) {
            if (namespaceRoot.namespace()
                .equals(namespace)) {
                addGuideRootCandidates(candidates, namespaceRoot, folder);
            }
        }
        addGuideRootCandidate(candidates, resourcePackRoot, "assets/" + namespace + "/" + folder + "/");
        addGuideRootCandidate(candidates, resourcePackRoot, namespace + "/" + folder + "/");
        if (folder.equals(namespace)) {
            addGuideRootCandidate(candidates, resourcePackRoot, folder + "/");
        }
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
            for (var namespaceRoot : cached) {
                addNamespaceRoot(byPath, namespaceRoot);
            }
            return List.copyOf(byPath.values());
        }

        var nativeRoots = new ArrayList<NamespaceRoot>();
        var nativeDirs = resourcePackRoot.listFiles(File::isDirectory);
        if (nativeDirs != null) {
            for (var dir : nativeDirs) {
                if ("assets".equals(dir.getName())) continue;
                var namespaceRoot = new NamespaceRoot(namespaceFromDirectoryName(dir.getName()), dir, true);
                addNamespaceRoot(byPath, namespaceRoot);
                nativeRoots.add(namespaceRoot);
            }
        }
        nativeNamespaceRootsCache.put(cacheKey, List.copyOf(nativeRoots));
        return List.copyOf(byPath.values());
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

    static void scanZipPagePaths(File resourcePackFile, String prefix, Set<String> pagePaths) {
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
                "[GuideNH] [GuideResourcePackScanner] Failed to scan zip for pages: {}",
                resourcePackFile.getAbsolutePath(),
                e);
        }
    }

    static void collectMarkdownPaths(File directory, String relativePath, Set<String> pagePaths) {
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

    static boolean isLanguageFolder(String name) {
        return name.startsWith(LANGUAGE_FOLDER_PREFIX) && LangUtil.isLanguageCode(name.substring(1));
    }

    static String toLanguageCode(String folderName) {
        return LangUtil.normalizeLanguage(folderName.substring(LANGUAGE_FOLDER_PREFIX.length()));
    }

    private static boolean resourceExists(IResourcePack resourcePack, ResourceLocation location) {
        try {
            return resourcePack.resourceExists(location);
        } catch (RuntimeException ignored) {
            // Third-party resource packs occasionally implement a missing resource as a runtime failure.
            return false;
        }
    }
}
