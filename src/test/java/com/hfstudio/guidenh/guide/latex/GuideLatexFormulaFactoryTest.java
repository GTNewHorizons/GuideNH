package com.hfstudio.guidenh.guide.latex;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;

class GuideLatexFormulaFactoryTest {

    @Test
    void rendersMathWithoutAllowingImages() throws ParseException {
        var formula = GuideLatexFormulaFactory.parse("\\frac{a}{b}");
        var icon = formula.new TeXIconBuilder().setStyle(TeXConstants.STYLE_DISPLAY)
            .setSize(16)
            .build();
        assertTrue(icon.getIconWidth() > 0);
        assertThrows(ParseException.class, () -> GuideLatexFormulaFactory.parse("\\includegraphics{missing.png}"));
    }
}
