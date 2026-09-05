package com.hfstudio.guidenh.guide.internal.localization;

import org.jetbrains.annotations.Nullable;

/** Snapshot index for runtime language values, rebuilt when resources are reloaded. */
public class GuideResourceLanguageIndex {

    private GuideResourceLanguageIndex() {}

    public static void clear() {
        GuideLanguageIndex.clear();
    }

    public static void warm(@Nullable String language) {
        GuideLanguageIndex.indexLanguage(language);
    }

    public static @Nullable String getValue(String language, String key) {
        return GuideLanguageIndex.getValue(language, key);
    }
}
