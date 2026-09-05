package com.hfstudio.guidenh.guide.internal.localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

/**
 * Shared language index for normal runtime strings and large localized guide pages.
 * <p/>
 * Each matching .lang file is scanned once per language. Normal strings are retained in the
 * runtime snapshot, while page translations retain only their source locations and are read on
 * demand to keep large page bodies out of the index.
 */
public class GuideLanguageIndex {

    private static final String PAGE_LANG_KEY_PREFIX = "guidenh.page.";
    // Matches the numeric format placeholders normalized by StringTranslate.parseLangFile.
    private static final Pattern NUMERIC_LANG_VARIABLE = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    private static final Object LOCK = new Object();

    private static volatile Map<String, IndexedLanguage> indexedLanguages = Map.of();

    private GuideLanguageIndex() {}

    private record PageSource(IResourcePack pack, String path) {}

    private record IndexedLanguage(Map<String, String> plainTranslations, Map<String, List<PageSource>> pageSources) {}

    public static void clear() {
        synchronized (LOCK) {
            indexedLanguages = Map.of();
        }
    }

    public static void indexLanguage(@Nullable String language) {
        if (language == null) return;
        getOrBuild(LangUtil.normalizeLanguage(language));
    }

    public static @Nullable String getValue(String language, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String normalized = LangUtil.normalizeLanguage(language);
        return getOrBuild(normalized).plainTranslations()
            .get(key);
    }

    public static @Nullable String getPageValue(String language, String key) {
        if (!isPageLangKey(key)) {
            return null;
        }

        String normalized = LangUtil.normalizeLanguage(language);
        return loadPageValue(getOrBuild(normalized), key);
    }

    public static boolean isPageLangKey(@Nullable String key) {
        return key != null && key.startsWith(PAGE_LANG_KEY_PREFIX);
    }

    private static IndexedLanguage getOrBuild(String language) {
        IndexedLanguage data = indexedLanguages.get(language);
        if (data != null) return data;

        synchronized (LOCK) {
            data = indexedLanguages.get(language);
            if (data != null) return data;

            data = buildIndex(language);
            Map<String, IndexedLanguage> updated = new HashMap<>(indexedLanguages);
            updated.put(language, data);
            indexedLanguages = updated;
            return data;
        }
    }

    private static IndexedLanguage buildIndex(String language) {
        Map<String, String> plainTranslations = new LinkedHashMap<>();
        Map<String, List<PageSource>> pageSources = new LinkedHashMap<>();

        for (IResourcePack pack : DataDrivenGuideLoader.getLastActiveResourcePacks()) {
            for (String path : DataDrivenGuideLoader.getLangFilePaths(pack)) {
                int fileNameStart = path.lastIndexOf('/') + 1;
                if (fileNameStart <= 0 || !isMatchingLangFile(path.substring(fileNameStart), language)) {
                    continue;
                }

                indexLangFile(pack, path, plainTranslations, pageSources);
            }
        }

        return new IndexedLanguage(plainTranslations, pageSources);
    }

    private static @Nullable String loadPageValue(IndexedLanguage data, String key) {
        List<PageSource> matchingSources = data.pageSources()
            .get(key);
        if (matchingSources == null) {
            return null;
        }

        String result = null;
        for (PageSource source : matchingSources) {
            String value = DataDrivenGuideLoader.readLangValue(source.pack(), source.path(), key);
            if (value != null) {
                // Sources are collected in effective pack order; later values override earlier ones.
                result = value;
            }
        }
        return result;
    }

    private static void indexLangFile(IResourcePack resourcePack, String entryPath,
        Map<String, String> plainTranslations, Map<String, List<PageSource>> pageSources) {

        ResourceLocation location = getLangResourceLocation(entryPath);
        if (location == null || !resourceExists(resourcePack, location)) {
            return;
        }

        PageSource pageSource = null;
        try (var input = resourcePack.getInputStream(location);
            var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) line = line.substring(1);
                if (line.isEmpty() || line.startsWith("#")) continue;

                int separator = line.indexOf('=');
                if (separator <= 0) continue;

                String key = line.substring(0, separator);
                if (isPageLangKey(key)) {
                    if (pageSource == null) {
                        pageSource = new PageSource(resourcePack, entryPath);
                    }

                    List<PageSource> sources = pageSources.computeIfAbsent(key, k -> new ArrayList<>());
                    // A key can theoretically occur more than once in one file
                    if (sources.isEmpty() || sources.getLast() != pageSource) {
                        sources.add(pageSource);
                    }
                    continue;
                }

                String value = line.substring(separator + 1);
                if (value.indexOf('%') >= 0) {
                    value = NUMERIC_LANG_VARIABLE.matcher(value)
                        .replaceAll("%$1s");
                }

                plainTranslations.put(key, value);
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    private static @Nullable ResourceLocation getLangResourceLocation(@Nullable String entryPath) {
        if (entryPath == null || !entryPath.endsWith(".lang")) return null;
        if (entryPath.startsWith("assets/")) entryPath = entryPath.substring("assets/".length());

        int firstSlash = entryPath.indexOf('/');
        if (firstSlash <= 0) return null;

        return new ResourceLocation(entryPath.substring(0, firstSlash), entryPath.substring(firstSlash + 1));
    }

    private static boolean resourceExists(IResourcePack resourcePack, ResourceLocation location) {
        try {
            return resourcePack.resourceExists(location);
        } catch (RuntimeException ignored) {
            // Third-party resource packs occasionally implement a missing resource as a runtime failure.
            return false;
        }
    }

    private static boolean isMatchingLangFile(String fileName, String language) {
        if (!fileName.endsWith(".lang")) {
            return false;
        }
        return LangUtil.normalizeLanguage(fileName.substring(0, fileName.length() - 5))
            .equals(language);
    }
}
