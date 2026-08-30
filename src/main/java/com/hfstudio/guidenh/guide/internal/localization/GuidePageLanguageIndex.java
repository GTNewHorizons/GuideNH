package com.hfstudio.guidenh.guide.internal.localization;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

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
    private static final String MISSING = "\u0000";
    private static final Map<String, String> CACHED_VALUES = new LinkedHashMap<>(MAX_CACHED_VALUES, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHED_VALUES;
        }
    };

    private GuidePageLanguageIndex() {}

    public static void clear() {
        synchronized (CACHED_VALUES) {
            CACHED_VALUES.clear();
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
                String value = DataDrivenGuideLoader.readLangValue(resourcePack, path, key);
                if (value != null) {
                    // Resource packs are visited in effective priority order; a later value overrides.
                    result = value;
                }
            }
        }
        return result;
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
