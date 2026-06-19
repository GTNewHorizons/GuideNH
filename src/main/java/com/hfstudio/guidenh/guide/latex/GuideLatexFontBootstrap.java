package com.hfstudio.guidenh.guide.latex;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.scilab.forge.jlatexmath.DefaultTeXFont;

import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

public class GuideLatexFontBootstrap {

    public static final String FONT_RESOURCE = "/assets/guidenh/jlatexmath/minecrafttex/GuideNhTeXFont.xml";
    public static final String TEXT_STYLE_NAME = "guidenh_minecrafttex_mathnormal";
    public static final String ROMAN_TEXT_STYLE_NAME = "guidenh_minecrafttex_mathrm";
    public static final String ITALIC_TEXT_STYLE_NAME = "guidenh_minecrafttex_mathit";
    public static final String BOLD_TEXT_STYLE_NAME = "guidenh_minecrafttex_mathbf";

    private static final GuideLatexFontBootstrap RESOURCE_BASE = new GuideLatexFontBootstrap();
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean FAILED = new AtomicBoolean(false);

    protected GuideLatexFontBootstrap() {}

    public static void ensureInstalled() {
        if (INSTALLED.get() || FAILED.get()) {
            return;
        }

        synchronized (GuideLatexFontBootstrap.class) {
            if (INSTALLED.get() || FAILED.get()) {
                return;
            }

            try (InputStream in = GuideLatexFontBootstrap.class.getResourceAsStream(FONT_RESOURCE)) {
                if (in == null) {
                    throw new IllegalStateException("Missing bundled jlatexmath font resource: " + FONT_RESOURCE);
                }

                DefaultTeXFont.addTeXFontDescription(RESOURCE_BASE, in, FONT_RESOURCE);
                INSTALLED.set(true);
                GuideDebugLog.infoAlways("[GuideNH/LaTeX] Installed GuideNH MinecraftTeX font resources");
            } catch (Exception exception) {
                FAILED.set(true);
                GuideDebugLog.warnAlways(
                    "[GuideNH/LaTeX] Failed to install GuideNH MinecraftTeX font resources, fallback will be used",
                    exception);
            }
        }
    }

    public static boolean isInstalled() {
        return INSTALLED.get();
    }
}
