package com.hfstudio.guidenh.guide.latex;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.scilab.forge.jlatexmath.DefaultTeXFont;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideLatexFontBootstrap {

    public static final String FONT_RESOURCE = "/assets/guidenh/jlatexmath/minecrafttex/GuideNhTeXFont.xml";
    public static final String TEXT_STYLE_NAME = "guidenh_minecrafttex_mathnormal";
    public static final String ROMAN_TEXT_STYLE_NAME = "guidenh_minecrafttex_mathrm";
    public static final String ITALIC_TEXT_STYLE_NAME = "guidenh_minecrafttex_mathit";
    public static final String BOLD_TEXT_STYLE_NAME = "guidenh_minecrafttex_mathbf";

    private static final GuideLatexFontBootstrap RESOURCE_BASE = new GuideLatexFontBootstrap();
    private static final Field[] STATE_FIELDS = findStateFields();

    private static TeXFontState vanillaState;
    private static TeXFontState minecraftTexState;
    private static GuideLatexFontProfile activeProfile;
    private static boolean minecraftTexFailed;

    protected GuideLatexFontBootstrap() {}

    public static void ensureInstalled() {
        if (activeProfile != null) {
            return;
        }
        synchronized (GuideLatexFontBootstrap.class) {
            if (activeProfile == null) {
                reloadFromConfig();
            }
        }
    }

    public static void reloadFromConfig() {
        synchronized (GuideLatexFontBootstrap.class) {
            GuideLatexFontProfile profile = GuideLatexFontProfile.fromConfig();
            applyProfile(profile);
            activeProfile = profile;
            GuideDebugLog.infoAlways(
                "[GuideNH/LaTeX] Active LaTeX font mode: {}{}",
                profile.mode(),
                profile.usesExternalFont() ? " (" + profile.externalFontName() + ")" : "");
        }
    }

    public static boolean isInstalled() {
        return isMinecraftTexActive();
    }

    public static boolean isMinecraftTexActive() {
        return activeProfile != null && activeProfile.usesMinecraftTex()
            && minecraftTexState != null
            && !minecraftTexFailed;
    }

    public static GuideLatexFontProfile activeProfile() {
        ensureInstalled();
        return activeProfile;
    }

    private static void applyProfile(GuideLatexFontProfile profile) {
        try {
            if (profile.usesMinecraftTex()) {
                if (ensureMinecraftTexState()) {
                    minecraftTexState.apply();
                }
            } else if (vanillaState != null) {
                vanillaState.apply();
            }
            GuideLatexExternalFonts.restoreDefault();
        } catch (Exception exception) {
            GuideDebugLog
                .warnAlways("[GuideNH/LaTeX] Failed to apply LaTeX font profile, fallback will be used", exception);
            restoreFallback();
        }
    }

    private static boolean ensureMinecraftTexState() {
        if (minecraftTexState != null) {
            return true;
        }
        if (minecraftTexFailed) {
            return false;
        }
        try {
            vanillaState = TeXFontState.capture();
            vanillaState.apply();
            try (InputStream in = GuideLatexFontBootstrap.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing bundled jlatexmath font resource: " + FONT_RESOURCE);
                }
                DefaultTeXFont.addTeXFontDescription(RESOURCE_BASE, in, FONT_RESOURCE);
                minecraftTexState = TeXFontState.capture();
                GuideDebugLog.infoAlways("[GuideNH/LaTeX] Installed GuideNH MinecraftTeX font resources");
                return true;
            }
        } catch (Exception exception) {
            minecraftTexFailed = true;
            GuideDebugLog.warnAlways(
                "[GuideNH/LaTeX] Failed to install GuideNH MinecraftTeX font resources, fallback will be used",
                exception);
            restoreFallback();
            return false;
        }
    }

    private static void restoreFallback() {
        try {
            if (vanillaState != null) {
                vanillaState.apply();
            }
        } catch (Exception ignored) {}
        GuideLatexExternalFonts.restoreDefault();
    }

    private static class TeXFontState {

        private final Map<String, Object> values;

        protected TeXFontState(Map<String, Object> values) {
            this.values = values;
        }

        public static TeXFontState capture() throws ReflectiveOperationException {
            var captured = new HashMap<String, Object>();
            for (Field field : STATE_FIELDS) {
                captured.put(field.getName(), copyStateValue(field.get(null)));
            }
            return new TeXFontState(captured);
        }

        public void apply() throws ReflectiveOperationException {
            for (Field field : STATE_FIELDS) {
                field.set(null, copyStateValue(values.get(field.getName())));
            }
        }

        private static Object copyStateValue(Object value) {
            if (value instanceof Map<?, ?>map) {
                return new HashMap<>(map);
            }
            if (value instanceof Object[]array) {
                return array.clone();
            }
            return value;
        }
    }

    private static Field[] findStateFields() {
        String[] names = { "fontInfo", "textStyleMappings", "symbolMappings" };
        Field[] fields = new Field[names.length];
        try {
            for (int i = 0; i < names.length; i++) {
                Field field = DefaultTeXFont.class.getDeclaredField(names[i]);
                field.setAccessible(true);
                fields[i] = field;
            }
            return fields;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Missing JLaTeXMath font state fields", exception);
        }
    }
}
