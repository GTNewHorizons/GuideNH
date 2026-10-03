package com.hfstudio.guidenh.guide.compiler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.style.TextStyle;

class SectionFormattingTest {

    // Color code 'f' selects white (0xFFFFFFFF).

    @Test
    void sectionFProducesWhiteSpan() {
        List<LytFlowContent> result = PageCompiler.parseSectionFormatting("§fWhite");
        assertEquals(1, result.size());

        LytFlowSpan span = assertInstanceOf(LytFlowSpan.class, result.get(0));
        TextStyle style = span.getStyle();
        assertNotNull(style.color());
        assertEquals(new ConstantColor(0xFFFFFFFF), style.color());

        assertEquals(
            1,
            span.getChildren()
                .size());
        LytFlowText text = assertInstanceOf(
            LytFlowText.class,
            span.getChildren()
                .get(0));
        assertEquals("White", text.getText());
    }

    // Color code 'c' selects red (0xFFFF5555).

    @Test
    void sectionCRegression() {
        List<LytFlowContent> result = PageCompiler.parseSectionFormatting("§cRed");
        assertEquals(1, result.size());

        LytFlowSpan span = assertInstanceOf(LytFlowSpan.class, result.get(0));
        TextStyle style = span.getStyle();
        assertNotNull(style.color());
        assertEquals(new ConstantColor(0xFFFF5555), style.color());

        assertEquals(
            1,
            span.getChildren()
                .size());
        LytFlowText text = assertInstanceOf(
            LytFlowText.class,
            span.getChildren()
                .get(0));
        assertEquals("Red", text.getText());
    }

    // Color code 'l' sets bold and carries no color.

    @Test
    void sectionLProducesBoldSpan() {
        List<LytFlowContent> result = PageCompiler.parseSectionFormatting("§lBold");
        assertEquals(1, result.size());

        LytFlowSpan span = assertInstanceOf(LytFlowSpan.class, result.get(0));
        TextStyle style = span.getStyle();
        assertNull(style.color());
        assertTrue(style.bold());

        assertEquals(
            1,
            span.getChildren()
                .size());
        LytFlowText text = assertInstanceOf(
            LytFlowText.class,
            span.getChildren()
                .get(0));
        assertEquals("Bold", text.getText());
    }

    @Test
    void plainTextReturnsLytFlowText() {
        List<LytFlowContent> result = PageCompiler.parseSectionFormatting("Hello");
        assertEquals(1, result.size());

        LytFlowText text = assertInstanceOf(LytFlowText.class, result.get(0));
        assertEquals("Hello", text.getText());
    }

    // An unknown code after the section sign stays literal text.

    @Test
    void illegalCodeKeptLiteral() {
        List<LytFlowContent> result = PageCompiler.parseSectionFormatting("§zFoo");
        assertEquals(1, result.size());

        LytFlowText text = assertInstanceOf(LytFlowText.class, result.get(0));
        assertEquals("§zFoo", text.getText());
    }

    @Test
    void mapSectionColorFWhite() {
        assertEquals(0xFFFFFFFF, PageCompiler.mapSectionColor('f'));
    }

    @Test
    void mapSectionColorFUpperCase() {
        assertEquals(0xFFFFFFFF, PageCompiler.mapSectionColor('F'));
    }

    @Test
    void mapSectionColorZSentinel() {
        assertEquals(0, PageCompiler.mapSectionColor('z'));
    }

    // A color code also resets formatting state set by earlier codes.

    @Test
    void colorCodeResetsBold() {
        // §l sets bold, then §f resets all formatting including bold
        List<LytFlowContent> result = PageCompiler.parseSectionFormatting("§l§fRest");
        assertEquals(1, result.size());

        LytFlowSpan span = assertInstanceOf(LytFlowSpan.class, result.get(0));
        TextStyle style = span.getStyle();
        assertNotNull(style.color());
        assertEquals(new ConstantColor(0xFFFFFFFF), style.color());
        // bold is explicitly set to false by the color code
        assertFalse(style.bold());
    }
}
