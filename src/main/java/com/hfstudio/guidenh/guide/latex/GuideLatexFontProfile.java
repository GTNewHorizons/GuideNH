package com.hfstudio.guidenh.guide.latex;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.hfstudio.guidenh.config.LatexFontMode;
import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideLatexFontProfile {

    private static final String ANGELICA_FONT_CONFIG = "com.gtnewhorizons.angelica.config.FontConfig";
    private static final String SMOOTH_FONT_COMMON_CONFIG = "bre.smoothfont.config.CommonConfig";
    private static final String NONE_FONT_NAME = "(none)";

    private static volatile Set<String> availableFontNames;
    private static volatile AngelicaFontFields angelicaFontFields;
    private static volatile SmoothFontFields smoothFontFields;

    private final LatexFontMode mode;
    private final String externalFontName;

    protected GuideLatexFontProfile(LatexFontMode mode, String externalFontName) {
        this.mode = mode != null ? mode : LatexFontMode.MinecraftTeX;
        this.externalFontName = cleanFontName(externalFontName);
    }

    public static GuideLatexFontProfile fromConfig() {
        LatexFontMode mode = ModConfig.ui.latexFontMode != null ? ModConfig.ui.latexFontMode
            : LatexFontMode.MinecraftTeX;
        return switch (mode) {
            case MinecraftTeX, JLaTeX -> new GuideLatexFontProfile(mode, "");
            case Custom -> new GuideLatexFontProfile(mode, resolveAvailableFont(ModConfig.ui.latexCustomFontName));
            case ModFont, ModFontMinecraftTeX -> new GuideLatexFontProfile(mode, resolveModFont());
        };
    }

    public LatexFontMode mode() {
        return mode;
    }

    public String externalFontName() {
        return externalFontName;
    }

    public boolean usesMinecraftTex() {
        return mode == LatexFontMode.MinecraftTeX || mode == LatexFontMode.ModFontMinecraftTeX;
    }

    public boolean usesExternalFont() {
        return !externalFontName.isEmpty();
    }

    public String signature() {
        return mode.name() + ':' + externalFontName;
    }

    private static String resolveModFont() {
        var candidates = new ArrayList<String>();
        addAngelicaFonts(candidates);
        addSmoothFontFonts(candidates);
        for (String candidate : candidates) {
            String resolved = resolveAvailableFont(candidate);
            if (!resolved.isEmpty()) {
                return resolved;
            }
        }
        return "";
    }

    private static void addAngelicaFonts(ArrayList<String> candidates) {
        try {
            AngelicaFontFields fields = angelicaFontFields();
            if (fields == null || !fields.isEnabled()) {
                return;
            }
            addCandidate(candidates, fields.primaryFontName());
            addCandidate(candidates, fields.fallbackFontName());
        } catch (Throwable ignored) {}
    }

    private static void addSmoothFontFonts(ArrayList<String> candidates) {
        try {
            SmoothFontFields fields = smoothFontFields();
            Object currentConfig = fields != null ? fields.currentConfig() : null;
            if (fields == null || currentConfig == null || !fields.useOsFont(currentConfig)) {
                return;
            }
            addCandidate(candidates, fields.primaryFontName(currentConfig));
            addCandidate(candidates, fields.secondaryFontName(currentConfig));
        } catch (Throwable ignored) {}
    }

    private static void addCandidate(ArrayList<String> candidates, String fontName) {
        String cleaned = cleanFontName(fontName);
        if (!cleaned.isEmpty() && !NONE_FONT_NAME.equalsIgnoreCase(cleaned)) {
            candidates.add(cleaned);
        }
    }

    private static AngelicaFontFields angelicaFontFields() {
        AngelicaFontFields cached = angelicaFontFields;
        if (cached != null) {
            return cached;
        }
        synchronized (GuideLatexFontProfile.class) {
            if (angelicaFontFields == null) {
                AngelicaFontFields found = AngelicaFontFields.find();
                if (found != null) {
                    angelicaFontFields = found;
                }
            }
            return angelicaFontFields;
        }
    }

    private static SmoothFontFields smoothFontFields() {
        SmoothFontFields cached = smoothFontFields;
        if (cached != null) {
            return cached;
        }
        synchronized (GuideLatexFontProfile.class) {
            if (smoothFontFields == null) {
                SmoothFontFields found = SmoothFontFields.find();
                if (found != null) {
                    smoothFontFields = found;
                }
            }
            return smoothFontFields;
        }
    }

    private static String resolveAvailableFont(String requestedFontName) {
        String cleaned = cleanFontName(requestedFontName);
        if (cleaned.isEmpty()) {
            return "";
        }
        if (getAvailableFontNames().contains(normalizeFontName(cleaned))) {
            return cleaned;
        }
        GuideDebugLog
            .warnAlways("[GuideNH/LaTeX] Requested LaTeX font '{}' was not found; using JLaTeX fallback", cleaned);
        return "";
    }

    private static Set<String> getAvailableFontNames() {
        Set<String> cached = availableFontNames;
        if (cached != null) {
            return cached;
        }
        synchronized (GuideLatexFontProfile.class) {
            if (availableFontNames == null) {
                availableFontNames = loadAvailableFontNames();
            }
            return availableFontNames;
        }
    }

    private static Set<String> loadAvailableFontNames() {
        var names = new LinkedHashSet<String>();
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (String family : environment.getAvailableFontFamilyNames()) {
            names.add(normalizeFontName(family));
        }
        for (Font font : environment.getAllFonts()) {
            names.add(normalizeFontName(font.getFontName()));
            names.add(normalizeFontName(font.getFamily()));
            names.add(normalizeFontName(font.getPSName()));
        }
        return Set.copyOf(names);
    }

    private static String cleanFontName(String fontName) {
        return fontName == null ? "" : fontName.trim();
    }

    private static String normalizeFontName(String fontName) {
        return cleanFontName(fontName).toLowerCase(Locale.ROOT);
    }

    private static String getString(Field field, Object target) throws ReflectiveOperationException {
        Object value = field.get(target);
        return value instanceof String text ? text : "";
    }

    private record AngelicaFontFields(Field enabled, Field primary, Field fallback) {

        public static AngelicaFontFields find() {
            try {
                Class<?> config = Class
                    .forName(ANGELICA_FONT_CONFIG, false, GuideLatexFontProfile.class.getClassLoader());
                return new AngelicaFontFields(
                    config.getField("enableCustomFont"),
                    config.getField("customFontNamePrimary"),
                    config.getField("customFontNameFallback"));
            } catch (Throwable ignored) {
                return null;
            }
        }

        public boolean isEnabled() throws ReflectiveOperationException {
            return enabled.getBoolean(null);
        }

        public String primaryFontName() throws ReflectiveOperationException {
            return getString(primary, null);
        }

        public String fallbackFontName() throws ReflectiveOperationException {
            return getString(fallback, null);
        }
    }

    private record SmoothFontFields(Field currentConfigField, Field useOsFont, Field primary, Field secondary) {

        public static SmoothFontFields find() {
            try {
                Class<?> config = Class
                    .forName(SMOOTH_FONT_COMMON_CONFIG, false, GuideLatexFontProfile.class.getClassLoader());
                return new SmoothFontFields(
                    config.getField("currentConfig"),
                    config.getField("useOSFont"),
                    config.getField("fontName"),
                    config.getField("secondaryFontName"));
            } catch (Throwable ignored) {
                return null;
            }
        }

        public Object currentConfig() throws ReflectiveOperationException {
            return currentConfigField.get(null);
        }

        public boolean useOsFont(Object target) throws ReflectiveOperationException {
            return useOsFont.getBoolean(target);
        }

        public String primaryFontName(Object target) throws ReflectiveOperationException {
            return getString(primary, target);
        }

        public String secondaryFontName(Object target) throws ReflectiveOperationException {
            return getString(secondary, target);
        }
    }
}
