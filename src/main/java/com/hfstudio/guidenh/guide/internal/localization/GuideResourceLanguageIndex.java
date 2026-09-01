package com.hfstudio.guidenh.guide.internal.localization;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.resources.IResourcePack;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.datadriven.DataDrivenGuideLoader;
import com.hfstudio.guidenh.guide.internal.util.LangUtil;

/** Snapshot index for runtime language values, rebuilt when resources are reloaded. */
public class GuideResourceLanguageIndex {

    private static final Object LOCK = new Object();
    /** Immutable language snapshots; each .lang file is read at most once per reload. */
    private static volatile Map<String, Map<String, String>> VALUES = Map.of();

    private GuideResourceLanguageIndex() {}

    public static void clear() {
        synchronized (LOCK) {
            VALUES = Map.of();
        }
    }

    /** Builds the runtime key index for a language. Safe to call during reload or on demand. */
    public static void warm(@Nullable String language) {
        if (language == null) return;
        ensureIndexed(LangUtil.normalizeLanguage(language));
    }

    public static @Nullable String getValue(String language, String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String normalized = LangUtil.normalizeLanguage(language);
        ensureIndexed(normalized);
        return VALUES.getOrDefault(normalized, Map.of())
            .get(key);
    }

    private static void ensureIndexed(String language) {
        if (VALUES.containsKey(language)) return;
        synchronized (LOCK) {
            if (VALUES.containsKey(language)) return;
            Map<String, String> values = new LinkedHashMap<>();
            for (IResourcePack pack : DataDrivenGuideLoader.getLastActiveResourcePacks()) {
                for (String path : DataDrivenGuideLoader.getLangFilePaths(pack)) {
                    int fileNameStart = path.lastIndexOf('/') + 1;
                    if (fileNameStart <= 0) continue;
                    String fileName = path.substring(fileNameStart);
                    if (!fileName.endsWith(".lang")
                        || !LangUtil.normalizeLanguage(fileName.substring(0, fileName.length() - 5))
                            .equals(language)) {
                        continue;
                    }
                    // Iterate in effective pack order; later packs override earlier values.
                    values.putAll(DataDrivenGuideLoader.readRuntimeLangValues(pack, path));
                }
            }
            Map<String, Map<String, String>> updated = new HashMap<>(VALUES);
            updated.put(language, values.isEmpty() ? Map.of() : Map.copyOf(values));
            VALUES = Map.copyOf(updated);
        }
    }
}
