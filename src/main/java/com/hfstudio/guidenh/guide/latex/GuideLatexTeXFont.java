package com.hfstudio.guidenh.guide.latex;

import org.scilab.forge.jlatexmath.Char;
import org.scilab.forge.jlatexmath.CharFont;
import org.scilab.forge.jlatexmath.DefaultTeXFont;
import org.scilab.forge.jlatexmath.Extension;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFont;

public class GuideLatexTeXFont implements TeXFont {

    private static final String CONTOUR_INTEGRAL_SYMBOL = "oint";
    private static final int CONTOUR_INTEGRAL_DISPLAY_EXTRA_STEPS = 0;

    private final DefaultTeXFont delegate;

    public GuideLatexTeXFont(float pointSize) {
        this(new DefaultTeXFont(pointSize));
    }

    public GuideLatexTeXFont(DefaultTeXFont delegate) {
        this.delegate = delegate;
    }

    @Override
    public TeXFont deriveFont(float pointSize) {
        return new GuideLatexTeXFont((DefaultTeXFont) delegate.deriveFont(pointSize));
    }

    @Override
    public TeXFont scaleFont(float factor) {
        return new GuideLatexTeXFont((DefaultTeXFont) delegate.scaleFont(factor));
    }

    @Override
    public float getScaleFactor() {
        return delegate.getScaleFactor();
    }

    @Override
    public float getAxisHeight(int style) {
        return delegate.getAxisHeight(style);
    }

    @Override
    public float getBigOpSpacing1(int style) {
        return delegate.getBigOpSpacing1(style);
    }

    @Override
    public float getBigOpSpacing2(int style) {
        return delegate.getBigOpSpacing2(style);
    }

    @Override
    public float getBigOpSpacing3(int style) {
        return delegate.getBigOpSpacing3(style);
    }

    @Override
    public float getBigOpSpacing4(int style) {
        return delegate.getBigOpSpacing4(style);
    }

    @Override
    public float getBigOpSpacing5(int style) {
        return delegate.getBigOpSpacing5(style);
    }

    @Override
    public Char getChar(char c, String textStyle, int style) {
        if (GuideLatexFontBootstrap.isMinecraftTexActive()) {
            return delegate.getChar(c, resolveGuideTextStyle(textStyle), style);
        }
        return delegate.getChar(c, textStyle, style);
    }

    @Override
    public Char getChar(String symbolName, int style) {
        Char c = delegate.getChar(symbolName, style);
        if (GuideLatexFontBootstrap.isMinecraftTexActive() && CONTOUR_INTEGRAL_SYMBOL.equals(symbolName)
            && style < TeXConstants.STYLE_TEXT) {
            return promoteNextLarger(c, style, CONTOUR_INTEGRAL_DISPLAY_EXTRA_STEPS);
        }
        return c;
    }

    @Override
    public Char getChar(CharFont cf, int style) {
        return delegate.getChar(cf, style);
    }

    @Override
    public Char getDefaultChar(char c, int style) {
        if (GuideLatexFontBootstrap.isMinecraftTexActive()) {
            return delegate.getChar(c, GuideLatexFontBootstrap.TEXT_STYLE_NAME, style);
        }
        return delegate.getDefaultChar(c, style);
    }

    @Override
    public float getDefaultRuleThickness(int style) {
        return delegate.getDefaultRuleThickness(style);
    }

    @Override
    public float getDenom1(int style) {
        return delegate.getDenom1(style);
    }

    @Override
    public float getDenom2(int style) {
        return delegate.getDenom2(style);
    }

    @Override
    public Extension getExtension(Char c, int style) {
        return delegate.getExtension(c, style);
    }

    @Override
    public float getKern(CharFont left, CharFont right, int style) {
        return delegate.getKern(left, right, style);
    }

    @Override
    public CharFont getLigature(CharFont left, CharFont right) {
        return delegate.getLigature(left, right);
    }

    @Override
    public int getMuFontId() {
        return delegate.getMuFontId();
    }

    @Override
    public Char getNextLarger(Char c, int style) {
        return delegate.getNextLarger(c, style);
    }

    @Override
    public float getNum1(int style) {
        return delegate.getNum1(style);
    }

    @Override
    public float getNum2(int style) {
        return delegate.getNum2(style);
    }

    @Override
    public float getNum3(int style) {
        return delegate.getNum3(style);
    }

    @Override
    public float getQuad(int style, int fontCode) {
        return delegate.getQuad(style, fontCode);
    }

    @Override
    public float getSize() {
        return delegate.getSize();
    }

    @Override
    public float getSkew(CharFont cf, int style) {
        return delegate.getSkew(cf, style);
    }

    @Override
    public float getSpace(int style) {
        return delegate.getSpace(style);
    }

    @Override
    public float getSub1(int style) {
        return delegate.getSub1(style);
    }

    @Override
    public float getSub2(int style) {
        return delegate.getSub2(style);
    }

    @Override
    public float getSubDrop(int style) {
        return delegate.getSubDrop(style);
    }

    @Override
    public float getSup1(int style) {
        return delegate.getSup1(style);
    }

    @Override
    public float getSup2(int style) {
        return delegate.getSup2(style);
    }

    @Override
    public float getSup3(int style) {
        return delegate.getSup3(style);
    }

    @Override
    public float getSupDrop(int style) {
        return delegate.getSupDrop(style);
    }

    @Override
    public float getXHeight(int style, int fontCode) {
        return delegate.getXHeight(style, fontCode);
    }

    @Override
    public float getEM(int style) {
        return delegate.getEM(style);
    }

    @Override
    public boolean hasNextLarger(Char c) {
        return delegate.hasNextLarger(c);
    }

    @Override
    public void setBold(boolean bold) {
        delegate.setBold(bold);
    }

    @Override
    public boolean getBold() {
        return delegate.getBold();
    }

    @Override
    public void setRoman(boolean rm) {
        delegate.setRoman(rm);
    }

    @Override
    public boolean getRoman() {
        return delegate.getRoman();
    }

    @Override
    public void setTt(boolean tt) {
        delegate.setTt(tt);
    }

    @Override
    public boolean getTt() {
        return delegate.getTt();
    }

    @Override
    public void setIt(boolean it) {
        delegate.setIt(it);
    }

    @Override
    public boolean getIt() {
        return delegate.getIt();
    }

    @Override
    public void setSs(boolean ss) {
        delegate.setSs(ss);
    }

    @Override
    public boolean getSs() {
        return delegate.getSs();
    }

    @Override
    public TeXFont copy() {
        return new GuideLatexTeXFont((DefaultTeXFont) delegate.copy());
    }

    @Override
    public boolean hasSpace(int font) {
        return delegate.hasSpace(font);
    }

    @Override
    public boolean isExtensionChar(Char c) {
        return delegate.isExtensionChar(c);
    }

    private String resolveGuideTextStyle(String textStyle) {
        return switch (textStyle) {
            case "mathnormal" -> GuideLatexFontBootstrap.TEXT_STYLE_NAME;
            case "mathrm" -> GuideLatexFontBootstrap.ROMAN_TEXT_STYLE_NAME;
            case "mathit" -> GuideLatexFontBootstrap.ITALIC_TEXT_STYLE_NAME;
            case "mathbf" -> GuideLatexFontBootstrap.BOLD_TEXT_STYLE_NAME;
            default -> textStyle;
        };
    }

    private Char promoteNextLarger(Char c, int style, int steps) {
        Char current = c;
        for (int i = 0; i < steps && delegate.hasNextLarger(current); i++) {
            current = delegate.getNextLarger(current, style);
        }
        return current;
    }
}
