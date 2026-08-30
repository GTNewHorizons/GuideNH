package com.hfstudio.guidenh.guide.internal.localization;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.resources.IResourcePack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

/** Bounded lookup cache for runtime language values. */
public class GuideResourceLanguageIndex {

    private static final int MAX_CACHED_VALUES = 4096;
    private static final String MISSING = "\u0000";
    private static final Map<String, String> VALUES = new LinkedHashMap<>(MAX_CACHED_VALUES, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_CACHED_VALUES;
        }
    };

    private GuideResourceLanguageIndex() {}

    public static void clear() {
        synchronized (VALUES) {
            VALUES.clear();
        }
    }

    /** Retained as an API compatibility hook; values are intentionally loaded by key. */
    public static void warm(@Nullable String language) {}

    public static @Nullable String getValue(String language, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String normalized = LangUtil.normalizeLanguage(language);
        String cacheKey = normalized + '\u0001' + key;
        synchronized (VALUES) {
            String cached = VALUES.get(cacheKey);
            if (cached != null) {
                return MISSING.equals(cached) ? null : cached;
            }
        }
        String value = loadValue(normalized, key);
        synchronized (VALUES) {
            VALUES.put(cacheKey, value != null ? value : MISSING);
        }
        return value;
    }

    private static @Nullable String loadValue(String language, String key) {
        String result = null;
        for (IResourcePack pack : DataDrivenGuideLoader.getLastActiveResourcePacks()) {
            File root = DataDrivenGuideLoader.getLooseResourcePackRoot(pack);
            if (root == null || !root.exists()) {
                continue;
            }
            for (String path : DataDrivenGuideLoader.getLangFilePaths(root)) {
                int fileNameStart = path.lastIndexOf('/') + 1;
                if (fileNameStart <= 0) {
                    continue;
                }
                String fileName = path.substring(fileNameStart);
                if (!fileName.endsWith(".lang") || !LangUtil.normalizeLanguage(fileName.substring(0, fileName.length() - 5))
                    .equals(language)) {
                    continue;
                }
                String candidate = DataDrivenGuideLoader.readLangValue(pack, path, key);
                if (candidate != null) {
                    result = candidate;
                }
            }
        }
        return result;
    }
}
