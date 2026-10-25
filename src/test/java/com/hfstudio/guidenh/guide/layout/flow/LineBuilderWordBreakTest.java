package com.hfstudio.guidenh.guide.layout.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateTestHarness;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;

class LineBuilderWordBreakTest {

    private static final String WBR = String.valueOf(LineBuilder.WORD_BREAK_OPPORTUNITY);

    /** Every glyph is 6 wide, so a 60 wide line holds ten characters. */
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
    void wbrTextNodeLetsTheNextWordWrap() {
        assertEquals(List.of("Cobble", "stone/s"), layout(60, "Cobble", WBR, "stone/s"));
    }

    @Test
    void wbrInsideOneTextLetsTheWordWrap() {
        assertEquals(List.of("Cobble", "stone/s"), layout(60, "Cobble" + WBR + "stone/s"));
    }

    @Test
    void laterWbrWinsOverAnEarlierOne() {
        assertEquals(List.of("ab" + "cdefgh", "ij"), layout(48, "ab" + WBR + "cdefgh" + WBR + "ij"));
    }

    @Test
    void wbrIsInvisibleWhenTheWordFits() {
        assertEquals(List.of("Cobblestone/s"), layout(200, "Cobble", WBR, "stone/s"));
        assertEquals(List.of("Cobblestone/s"), layout(200, "Cobble" + WBR + "stone/s"));
    }

    @Test
    void wbrTagCompilesToABreakOpportunity() {
        String text = MediaWikiTemplateTestHarness.textOf("Cobble<wbr>stone and 11,<wbr />520\n");
        assertTrue(text.contains("Cobble" + WBR + "stone"), text);
        assertTrue(text.contains("11," + WBR + "520"), text);
    }

    private static List<String> layout(int width, String... texts) {
        var lines = new ArrayList<Line>();
        var builder = new LineBuilder(
            new LayoutContext(MONOSPACE),
            0,
            0,
            width,
            lines,
            new ArrayList<>(),
            TextAlignment.LEFT);
        for (String text : texts) {
            builder.accept(LytFlowText.of(text));
        }
        builder.end();

        var result = new ArrayList<String>();
        for (var line : lines) {
            var text = new StringBuilder();
            for (var el = line.firstElement; el != null; el = el.next) {
                if (el instanceof LineTextRun run) {
                    text.append(run.text);
                }
            }
            result.add(text.toString());
        }
        return result;
    }
}
