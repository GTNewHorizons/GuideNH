package com.hfstudio.guidenh.guide.internal.localization;

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
    /**
     * Language files are indexed by key, but their translated page bodies are not retained here.
     * This prevents a cache miss from reopening every .lang file while keeping the resident
     * memory bounded to file locations and the existing value LRU.
     */
    private static final Map<String, Map<String, List<LangSource>>> KEY_SOURCES = new LinkedHashMap<>();
    private static final Set<String> INDEXED_LANGUAGES = new LinkedHashSet<>();

    private GuidePageLanguageIndex() {}

    private record LangSource(IResourcePack pack, String path) {}

    public static void clear() {
        synchronized (KEY_SOURCES) {
            KEY_SOURCES.clear();
            INDEXED_LANGUAGES.clear();
        }
    }

    /**
     * Retained for development-source updates. Normal resource reloads no longer call this method.
     */
    public static void preload(Map<String, Map<String, String>> keysByLanguage) {
        // Page bodies are intentionally never preloaded: they can be large and are owned by resident pages.
    }

    public static @Nullable String getValue(String language, String key) {
        if (!isPageLangKey(key)) {
            return null;
        }
        String normalizedLanguage = LangUtil.normalizeLanguage(language);
        ensureIndexed(normalizedLanguage);
        return loadValue(normalizedLanguage, key);
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
            String value = DataDrivenGuideLoader.readLangValue(source.pack(), source.path(), key);
            if (value != null) {
                // Sources are collected in effective pack order; later values override earlier ones.
                result = value;
            }
        }
        return result;
    }

    /** Builds the key-to-file index once per language without retaining translated page bodies. */
    private static void ensureIndexed(String language) {
        synchronized (KEY_SOURCES) {
            if (INDEXED_LANGUAGES.contains(language)) {
                return;
            }
            Map<String, List<LangSource>> sources = new LinkedHashMap<>();
            for (IResourcePack resourcePack : DataDrivenGuideLoader.getLastActiveResourcePacks()) {
                for (String path : DataDrivenGuideLoader.getLangFilePaths(resourcePack)) {
                    int fileNameStart = path.lastIndexOf('/') + 1;
                    if (fileNameStart <= 0 || !isMatchingLangFile(path.substring(fileNameStart), language)) {
                        continue;
                    }
                    LangSource source = new LangSource(resourcePack, path);
                    for (String key : DataDrivenGuideLoader.readLangKeys(resourcePack, path)) {
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

}
