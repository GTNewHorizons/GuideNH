package com.hfstudio.guidenh.guide.layout.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;

class LineBuilderExplicitBreakTest {

    private static final FontMetrics MONOSPACE = new FontMetrics() {

        @Override
        public float getAdvance(int codePoint, ResolvedTextStyle style) {
            return 6;
        }

        @Override
        public int getLineHeight(ResolvedTextStyle style) {
            return 9;
        }
    };

    @Test
    void eachLineIsCenteredUsingItsOwnWidth() {
        var lines = layout("first\nsecond", 120);
        assertEquals(2, lines.size());
        assertEquals(30, lines.get(0).firstElement.bounds.width());
        assertEquals(45, lines.get(0).firstElement.bounds.x());
        assertEquals(36, lines.get(1).firstElement.bounds.width());
        assertEquals(42, lines.get(1).firstElement.bounds.x());
    }

    @Test
    void aFullFirstLineDoesNotConsumeTheNextLinesWidth() {
        var lines = layout("abcdefghij\nklmnopqrst", 60);
        assertEquals(2, lines.size());
        assertEquals("klmnopqrst", ((LineTextRun) lines.get(1).firstElement).text.toString());
        assertEquals(60, lines.get(1).firstElement.bounds.width());
    }

    private static List<Line> layout(String text, int width) {
        var paragraph = new LytParagraph();
        paragraph.setStyle(
            TextStyle.builder()
                .whiteSpace(WhiteSpaceMode.PRE_LINE)
                .build());
        var content = LytFlowText.of(text);
        paragraph.append(content);
        var lines = new ArrayList<Line>();
        var builder = new LineBuilder(
            new LayoutContext(MONOSPACE),
            0,
            0,
            width,
            lines,
            new ArrayList<>(),
            TextAlignment.CENTER);
        builder.accept(content);
        builder.end();
        return lines;
    }
}
