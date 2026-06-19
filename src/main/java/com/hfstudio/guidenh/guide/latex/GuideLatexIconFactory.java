package com.hfstudio.guidenh.guide.latex;

import java.awt.Color;

import org.scilab.forge.jlatexmath.Box;
import org.scilab.forge.jlatexmath.DefaultTeXFont;
import org.scilab.forge.jlatexmath.GuideNhTeXIconBridge;
import org.scilab.forge.jlatexmath.HorizontalBox;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXEnvironment;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

public class GuideLatexIconFactory {

    protected GuideLatexIconFactory() {}

    public static TeXIcon createIcon(TeXFormula formula, int style, float size, Color foregroundColor) {
        GuideLatexFontBootstrap.ensureInstalled();

        var baseFont = new DefaultTeXFont(size);
        var font = GuideLatexFontBootstrap.isMinecraftTexActive() ? new GuideLatexTeXFont(baseFont) : baseFont;
        var environment = new TeXEnvironment(style, font);
        Box box = formula.root == null ? new HorizontalBox() : formula.root.createBox(environment);
        TeXIcon icon = new GuideNhTeXIconBridge(box, size);
        if (foregroundColor != null) {
            icon.setForeground(foregroundColor);
        }
        return icon;
    }

    public static TeXIcon createDisplayIcon(TeXFormula formula, float size, Color foregroundColor) {
        return createIcon(formula, TeXConstants.STYLE_DISPLAY, size, foregroundColor);
    }
}
