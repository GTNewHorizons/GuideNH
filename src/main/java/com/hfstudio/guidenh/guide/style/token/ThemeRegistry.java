package com.hfstudio.guidenh.guide.style.token;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal registry — the growing list of all TokenKeys ever defined.
 * Package-private; GuideThemeManager is the only consumer.
 */
final class ThemeRegistry {

    private static final List<TokenKey<?>> keys = new ArrayList<>();

    static void register(TokenKey<?> key) {
        keys.add(key);
    }

    static List<TokenKey<?>> snapshot() {
        return new ArrayList<>(keys);
    }

    static int count() {
        return keys.size();
    }

    private ThemeRegistry() {}
}
