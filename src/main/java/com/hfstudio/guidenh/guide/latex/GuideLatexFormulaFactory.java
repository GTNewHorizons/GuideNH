package com.hfstudio.guidenh.guide.latex;

import org.scilab.forge.jlatexmath.MacroInfo;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXFormula;

public class GuideLatexFormulaFactory {

    static {
        // TeXFormula initializes the built-in command registry on its first use.
        new TeXFormula();
        MacroInfo.Commands.remove("includegraphics");
    }

    private GuideLatexFormulaFactory() {}

    public static TeXFormula parse(String formula) throws ParseException {
        return new TeXFormula(formula);
    }
}
