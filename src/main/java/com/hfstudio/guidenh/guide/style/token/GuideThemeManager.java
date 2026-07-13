package com.hfstudio.guidenh.guide.style.token;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global singleton. Holds the active Theme.
 * <p>
 * Usage:
 * <pre>
 *   TokenKey&lt;ColorValue&gt; BREAK_COLOR = TokenKey.define("--thematic-break-color", TokenType.COLOR, new ColorValue(0xFF373737));
 *   // in collectPrimitives:
 *   int argb = GuideThemeManager.instance().active().color(BREAK_COLOR).argb();
 * </pre>
 * <p>
 * Theme files live under {@code config/GuideNH/themes/}.
 * Default theme: {@code config/GuideNH/themes/default.cfg}.
 * Reload via {@code /guidenh theme reload <name>}.
 */
public final class GuideThemeManager {

    private static final GuideThemeManager INSTANCE = new GuideThemeManager();

    private final AtomicReference<Theme> active;

    private GuideThemeManager() {
        // Build a bootstrap theme from all registered TokenKey defaults.
        List<TokenKey<?>> keys = ThemeRegistry.snapshot();
        Theme bootstrap = Theme.build("bootstrap", keys, Collections.emptyMap());
        this.active = new AtomicReference<>(bootstrap);
    }

    public static GuideThemeManager instance() { return INSTANCE; }

    public Theme active() { return active.get(); }




    /**
     * Load a theme from the config directory.
     * @param themeName theme file name without extension (e.g. "default")
     */
    public void reload(String themeName) {
        Path themesDir = Minecraft.getMinecraft().mcDataDir.toPath()
            .resolve("config").resolve("GuideNH").resolve("themes");
        Path file = themesDir.resolve(themeName + ".cfg");

        if (!file.toFile().exists()) {
            GuideDebugLog.warnAlways("Theme file not found: " + file + ", using defaults");
            return;
        }

        Map<String, String> overrides = parseCfg(file);
        List<TokenKey<?>> keys = ThemeRegistry.snapshot();
        Theme newTheme = Theme.build(themeName, keys, overrides);
        active.set(newTheme);
        GuideDebugLog.warnAlways("Theme loaded: " + themeName + " (" + overrides.size() + " overrides)");
    }

    /**
     * Load the default theme on startup.
     * Called from Guide.init() or GuideScreen first open.
     */
    public void ensureLoaded() {
        if (active().name().equals("bootstrap")) {
            reload("default");
        }
    }




    /** Parse a simple .cfg file into key=value pairs. */
    private static Map<String, String> parseCfg(Path file) {
        Map<String, String> map = new LinkedHashMap<>();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(new FileInputStream(file.toFile()), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = r.readLine()) != null) {
                lineNo++;
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                int eq = line.indexOf('=');
                if (eq < 0) {
                    GuideDebugLog.warnAlways("Theme " + file.getFileName() + ":" + lineNo +
                        " — no '=' separator, skipping: " + line);
                    continue;
                }
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (key.isEmpty()) continue;

                // Strip inline comments after value (unless value starts with #)
                int comment = value.indexOf(" #");
                if (comment > 0) value = value.substring(0, comment).strip();
                comment = value.indexOf(" //");
                if (comment > 0) value = value.substring(0, comment).strip();

                map.put(key, value);
            }
        } catch (IOException e) {
            GuideDebugLog.warnAlways("Failed to read theme file: " + file + " — " + e.getMessage());
        }
        return map;
    }
}
