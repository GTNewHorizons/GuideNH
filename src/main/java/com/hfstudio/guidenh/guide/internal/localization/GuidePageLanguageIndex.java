package com.hfstudio.guidenh.guide.internal.localization;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.StringTranslate;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

/**
 * Bounded, key-addressed page localization cache. Page translations can be very large, so indexing
 * their complete text during every resource reload is intentionally avoided.
 */
public class GuidePageLanguageIndex {

    private static final String PAGE_LANG_KEY_PREFIX = "guidenh.page.";
    private static final int MAX_CACHED_VALUES = 4096;
    private static final int MAX_CACHED_LANG_FILES = 8;
    private static final String MISSING = "\u0000";
    /**
     * Language files are indexed by key, but their translated page bodies are not retained here.
     * This prevents a cache miss from reopening every .lang file while keeping the resident
     * memory bounded to file locations and the existing value LRU.
     */
    private static final Map<String, Map<String, List<LangSource>>> KEY_SOURCES = new LinkedHashMap<>();
    private static final Set<String> INDEXED_LANGUAGES = new LinkedHashSet<>();
    /** Small LRU for parsed lang files; avoids reopening the same file for every page key. */
    private static final Map<LangSource, Map<String, String>> PARSED_LANG_FILES = new LinkedHashMap<>(
        MAX_CACHED_LANG_FILES,
        0.75f,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<LangSource, Map<String, String>> eldest) {
            return size() > MAX_CACHED_LANG_FILES;
        }
    };
    private static final Map<String, String> CACHED_VALUES = new LinkedHashMap<>(MAX_CACHED_VALUES, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHED_VALUES;
        }
    };

    private GuidePageLanguageIndex() {}

    private record LangSource(IResourcePack pack, String path) {}

    public static void clear() {
        synchronized (CACHED_VALUES) {
            CACHED_VALUES.clear();
        }
        synchronized (KEY_SOURCES) {
            KEY_SOURCES.clear();
            INDEXED_LANGUAGES.clear();
            PARSED_LANG_FILES.clear();
        }
    }

    /**
     * Retained for development-source updates. Normal resource reloads no longer call this method.
     */
    public static void preload(Map<String, Map<String, String>> keysByLanguage) {
        synchronized (CACHED_VALUES) {
            for (var languageEntry : keysByLanguage.entrySet()) {
                for (var entry : languageEntry.getValue()
                    .entrySet()) {
                    CACHED_VALUES.put(cacheKey(languageEntry.getKey(), entry.getKey()), entry.getValue());
                }
            }
        }
    }

    public static @Nullable String getValue(String language, String key) {
        if (!isPageLangKey(key)) {
            return null;
        }
        String normalizedLanguage = LangUtil.normalizeLanguage(language);
        String cacheKey = cacheKey(normalizedLanguage, key);
        synchronized (CACHED_VALUES) {
            String cached = CACHED_VALUES.get(cacheKey);
            if (cached != null) {
                return MISSING.equals(cached) ? null : cached;
            }
        }

        ensureIndexed(normalizedLanguage);
        String loaded = loadValue(normalizedLanguage, key);
        synchronized (CACHED_VALUES) {
            CACHED_VALUES.put(cacheKey, loaded != null ? loaded : MISSING);
        }
        return loaded;
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
            if (isPageLangKey(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered.isEmpty() ? Map.of() : filtered;
    }

    private static @Nullable String loadValue(String language, String key) {
        String result = null;
        Map<String, List<LangSource>> sources;
        synchronized (KEY_SOURCES) {
            sources = KEY_SOURCES.get(language);
        }
        if (sources == null) {
            return null;
        }
        List<LangSource> matchingSources = sources.get(key);
        if (matchingSources == null) {
            return null;
        }
        for (LangSource source : matchingSources) {
            String value;
            synchronized (KEY_SOURCES) {
                Map<String, String> parsed = PARSED_LANG_FILES.get(source);
                value = parsed != null ? parsed.get(key) : null;
            }
            if (value == null) {
                Map<String, String> parsed = DataDrivenGuideLoader.readLangFile(source.pack(), source.path());
                synchronized (KEY_SOURCES) {
                    PARSED_LANG_FILES.put(source, parsed);
                }
                value = parsed.get(key);
            }
            if (value != null) {
                // Sources are collected in effective pack order; later values override earlier ones.
                result = value;
            }
        }
        return result;
    }

    /** Builds the key-to-file index once per language; only one language file is parsed at a time. */
    private static void ensureIndexed(String language) {
        synchronized (KEY_SOURCES) {
            if (INDEXED_LANGUAGES.contains(language)) {
                return;
            }
            Map<String, List<LangSource>> sources = new LinkedHashMap<>();
            for (IResourcePack resourcePack : DataDrivenGuideLoader.getLastActiveResourcePacks()) {
                File resourcePackFile = DataDrivenGuideLoader.getLooseResourcePackRoot(resourcePack);
                if (resourcePackFile == null || !resourcePackFile.exists()) {
                    continue;
                }
                for (String path : DataDrivenGuideLoader.getLangFilePaths(resourcePackFile)) {
                    int fileNameStart = path.lastIndexOf('/') + 1;
                    if (fileNameStart <= 0 || !isMatchingLangFile(path.substring(fileNameStart), language)) {
                        continue;
                    }
                    Map<String, String> values = DataDrivenGuideLoader.readLangFile(resourcePack, path);
                    LangSource source = new LangSource(resourcePack, path);
                    PARSED_LANG_FILES.put(source, values);
                    for (String key : values.keySet()) {
                        if (isPageLangKey(key)) {
                            sources.computeIfAbsent(key, ignored -> new ArrayList<>())
                                .add(source);
                        }
                    }
                }
            }
            KEY_SOURCES.put(language, sources);
            INDEXED_LANGUAGES.add(language);
        }
    }

    private static boolean isMatchingLangFile(String fileName, String language) {
        if (!fileName.endsWith(".lang")) {
            return false;
        }
        return LangUtil.normalizeLanguage(fileName.substring(0, fileName.length() - 5))
            .equals(language);
    }

    private static String cacheKey(String language, String key) {
        return language + '\u0001' + key;
    }
}
