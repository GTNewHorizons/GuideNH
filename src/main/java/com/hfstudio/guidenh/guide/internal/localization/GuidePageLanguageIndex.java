package com.hfstudio.guidenh.guide.internal.localization;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.StringTranslate;

import org.jetbrains.annotations.Nullable;

/**
 * Bounded, key-addressed page localization cache. Page translations can be very large, so indexing
 * their complete text during every resource reload is intentionally avoided.
 */
public class GuidePageLanguageIndex {

    private GuidePageLanguageIndex() {}

    public static void clear() {
        GuideLanguageIndex.clear();
    }

    /**
     * Retained for development-source updates. Normal resource reloads no longer call this method.
     */
    public static void preload(Map<String, Map<String, String>> keysByLanguage) {
        // Page bodies are intentionally never preloaded: they can be large and are owned by resident pages.
    }

    public static @Nullable String getValue(String language, String key) {
        return GuideLanguageIndex.getPageValue(language, key);
    }

    public static boolean isPageLangKey(@Nullable String key) {
        return GuideLanguageIndex.isPageLangKey(key);
    }

    public static Map<String, String> readPageKeys(InputStream input) {
        Map<String, String> source = StringTranslate.parseLangFile(input);
        Map<String, String> filtered = new LinkedHashMap<>();
        if (source.isEmpty()) return filtered;

        for (var entry : source.entrySet()) {
            if (isPageLangKey(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }
}
