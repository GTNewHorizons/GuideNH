package com.hfstudio.guidenh.guide.internal.localization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.StringTranslate;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuidePageLanguageIndex {

    private static final String PAGE_LANG_KEY_PREFIX = "guidenh.page.";

    private static final Map<String, Map<String, String>> PAGE_KEYS_BY_LANGUAGE = new ConcurrentHashMap<>();
    private static volatile boolean preloaded = false;

    private GuidePageLanguageIndex() {}

    public static void clear() {
        PAGE_KEYS_BY_LANGUAGE.clear();
        preloaded = false;
    }

    /**
     * Pre-populates the language index with keys collected during scanAll().
     * After this call, getValue() returns instantly without scanning packs.
     */
    public static void preload(Map<String, Map<String, String>> keysByLanguage) {
        PAGE_KEYS_BY_LANGUAGE.putAll(keysByLanguage);
        preloaded = true;
    }

    public static @Nullable String getValue(String language, String key) {
        if (key == null || !key.startsWith(PAGE_LANG_KEY_PREFIX)) {
            return null;
        }
        String normalizedLanguage = LangUtil.normalizeLanguage(language);
        if (preloaded) {
            Map<String, String> keys = PAGE_KEYS_BY_LANGUAGE.get(normalizedLanguage);
            return keys != null ? keys.get(key) : null;
        }
        return PAGE_KEYS_BY_LANGUAGE.computeIfAbsent(normalizedLanguage, GuidePageLanguageIndex::loadLanguage)
            .get(key);
    }

    public static boolean isPageLangKey(@Nullable String key) {
        return key != null && key.startsWith(PAGE_LANG_KEY_PREFIX);
    }

    public static Map<String, String> readPageKeys(InputStream input) {
        Map<String, String> source = StringTranslate.parseLangFile(input);
        if (source.isEmpty()) {
            return Map.of();
        }

        Map<String, String> filtered = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            String key = entry.getKey();
            if (isPageLangKey(key)) {
                filtered.put(key, entry.getValue());
            }
        }
        return filtered.isEmpty() ? Map.of() : filtered;
    }

    private static Map<String, String> loadLanguage(String normalizedLanguage) {
        long startedAt = System.nanoTime();
        Map<String, String> merged = new LinkedHashMap<>();
        var activeResourcePacks = DataDrivenGuideLoader.getLastActiveResourcePacks();
        int packIndex = 0;
        for (IResourcePack resourcePack : activeResourcePacks) {
            long packStartedAt = System.nanoTime();
            loadResourcePackLanguage(resourcePack, normalizedLanguage, merged);
            long packNs = System.nanoTime() - packStartedAt;
            if (packNs > 100_000_000) {
                GuideDebugLog.warn(
                    "[GuideNH] [GuidePageLanguageIndex] Slow resource pack [#{}/{}] {} took {} ms",
                    packIndex,
                    activeResourcePacks.size(),
                    resourcePack.getPackName(),
                    packNs / 1_000_000L);
            }
            packIndex++;
        }
        long totalNs = System.nanoTime() - startedAt;
        GuideDebugLog.warn(
            "[GuideNH] [GuidePageLanguageIndex] Loaded {} page language keys for language {} from {} resource packs in {} ms",
            merged.size(),
            normalizedLanguage,
            activeResourcePacks.size(),
            totalNs / 1_000_000L);
        return merged.isEmpty() ? Map.of() : Map.copyOf(merged);
    }

    private static void loadResourcePackLanguage(IResourcePack resourcePack, String normalizedLanguage,
        Map<String, String> target) {
        File resourcePackFile = DataDrivenGuideLoader.getLooseResourcePackRoot(resourcePack);
        if (resourcePackFile == null || !resourcePackFile.exists()) {
            return;
        }
        if (resourcePackFile.isDirectory()) {
            loadDirectoryLanguage(resourcePackFile, normalizedLanguage, target);
            return;
        }
        loadZipLanguage(resourcePack, resourcePackFile, normalizedLanguage, target);
    }

    private static void loadDirectoryLanguage(File resourcePackRoot, String normalizedLanguage,
        Map<String, String> target) {
        File assetsDir = new File(resourcePackRoot, "assets");
        File[] namespaceDirs = assetsDir.listFiles(File::isDirectory);
        if (namespaceDirs != null) {
            for (File namespaceDir : namespaceDirs) {
                loadDirectoryLanguageNamespace(namespaceDir, normalizedLanguage, target);
            }
        }

        File[] looseNamespaceDirs = resourcePackRoot.listFiles(File::isDirectory);
        if (looseNamespaceDirs == null) {
            return;
        }
        for (File namespaceDir : looseNamespaceDirs) {
            if ("assets".equals(namespaceDir.getName())) {
                continue;
            }
            loadDirectoryLanguageNamespace(namespaceDir, normalizedLanguage, target);
        }
    }

    private static void loadDirectoryLanguageNamespace(File namespaceDir, String normalizedLanguage,
        Map<String, String> target) {
        File langDir = new File(namespaceDir, "lang");
        if (!langDir.isDirectory()) {
            return;
        }
        loadDirectoryLanguageEntries(langDir, normalizedLanguage, target);
    }

    private static void loadDirectoryLanguageEntries(File directory, String normalizedLanguage,
        Map<String, String> target) {
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                loadDirectoryLanguageEntries(child, normalizedLanguage, target);
                continue;
            }
            if (!isMatchingLangFile(child.getName(), normalizedLanguage)) {
                continue;
            }
            try (InputStream input = new FileInputStream(child)) {
                mergePageKeys(input, target);
            } catch (IOException e) {
                GuideDebugLog
                    .warn("[GuideNH] [GuidePageLanguageIndex] Failed to read lang file {}", child.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Reads .lang files for the requested language using the cached entry list
     * from DataDrivenGuideLoader's single scan, avoiding a redundant zip entry iteration.
     */
    private static void loadZipLanguage(IResourcePack resourcePack, File resourcePackFile, String normalizedLanguage,
        Map<String, String> target) {
        List<String> langEntryPaths = DataDrivenGuideLoader.getLangFilePaths(resourcePackFile);
        for (String path : langEntryPaths) {
            int fileNameStart = path.lastIndexOf('/') + 1;
            if (fileNameStart <= 0 || fileNameStart >= path.length()) {
                continue;
            }
            if (!isMatchingLangFile(path.substring(fileNameStart), normalizedLanguage)) {
                continue;
            }
            Map<String, String> entries = DataDrivenGuideLoader.readLangFile(resourcePack, path);
            if (!entries.isEmpty()) {
                for (var entry : entries.entrySet()) {
                    if (isPageLangKey(entry.getKey())) {
                        target.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    private static boolean isMatchingLangFile(String fileName, String normalizedLanguage) {
        if (!fileName.endsWith(".lang")) {
            return false;
        }
        String baseName = fileName.substring(0, fileName.length() - 5);
        return LangUtil.normalizeLanguage(baseName)
            .equals(normalizedLanguage);
    }

    private static void mergePageKeys(InputStream input, Map<String, String> target) throws IOException {
        target.putAll(readPageKeys(input));
    }
}
