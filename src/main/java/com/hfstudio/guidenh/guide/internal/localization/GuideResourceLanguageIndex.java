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

public class GuideResourceLanguageIndex {

    private static final Map<String, Map<String, String>> VALUES_BY_LANGUAGE = new ConcurrentHashMap<>();

    /**
     * Parsed {@code .lang} content keyed by a stable file identity (absolute path for loose
     * packs, {@code <packFile>!<entry>} for zip packs). Lets repeated builds across languages
     * and guide reloads reuse the same parsed entries instead of re-reading/parsing every file.
     */
    private static final Map<String, Map<String, String>> PARSED_LANG_BY_FILE = new ConcurrentHashMap<>();

    private GuideResourceLanguageIndex() {}

    public static void clear() {
        VALUES_BY_LANGUAGE.clear();
    }

    /**
     * Pre-builds the language index for the given language (off the caller thread). Once built,
     * {@link #getValue} is a plain map hit. No-op if the language is already cached.
     */
    public static void warm(@Nullable String language) {
        String normalized = LangUtil.normalizeLanguage(language != null ? language : LangUtil.getCurrentLanguage());
        VALUES_BY_LANGUAGE.computeIfAbsent(normalized, GuideResourceLanguageIndex::load);
    }

    public static @Nullable String getValue(String language, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        return VALUES_BY_LANGUAGE
            .computeIfAbsent(LangUtil.normalizeLanguage(language), GuideResourceLanguageIndex::load)
            .get(key);
    }

    private static Map<String, String> load(String normalizedLanguage) {
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
                    "[GuideNH] [GuideResourceLanguageIndex] Slow resource pack [#{}/{}] {} took {} ms",
                    packIndex,
                    activeResourcePacks.size(),
                    resourcePack.getPackName(),
                    packNs / 1_000_000L);
            }
            packIndex++;
        }
        long totalNs = System.nanoTime() - startedAt;
        GuideDebugLog.warn(
            "[GuideNH] [GuideResourceLanguageIndex] Loaded {} lang entries for language {} from {} resource packs in {} ms",
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
            Map<String, String> parsed = PARSED_LANG_BY_FILE.computeIfAbsent(child.getAbsolutePath(), p -> {
                try (InputStream input = new FileInputStream(child)) {
                    return StringTranslate.parseLangFile(input);
                } catch (IOException e) {
                    GuideDebugLog.warn(
                        "[GuideNH] [GuideResourceLanguageIndex] Failed to read lang file {}",
                        child.getAbsolutePath(),
                        e);
                    return Map.of();
                }
            });
            target.putAll(parsed);
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
            target.putAll(
                PARSED_LANG_BY_FILE.computeIfAbsent(
                    resourcePackFile.getAbsolutePath() + "!" + path,
                    p -> DataDrivenGuideLoader.readLangFile(resourcePack, path)));
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
}
