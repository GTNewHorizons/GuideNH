package com.hfstudio.guidenh.guide.layout.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowText;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;

class LineBuilderInlineImageWrapTest {

    private static final String ICON = "[i]";

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
    void iconWrapsWithTheLabelAfterIt() {
        assertEquals(List.of("process ", ICON + "Cleanroom"), layout(60, "process ", ICON, "Cleanroom"));
    }

    @Test
    void iconWrapsWithTheLabelBeforeIt() {
        assertEquals(List.of("ab ", "Cleanroom" + ICON), layout(72, "ab ", "Cleanroom", ICON));
        assertEquals(List.of("ab ", "Cleanroom" + ICON), layout(72, "ab Cleanroom", ICON));
    }

    @Test
    void punctuationAfterAnIconPullsTheLabelAlong() {
        assertEquals(List.of("a ", "Clean" + ICON + ","), layout(48, "a ", "Clean", ICON, ","));
    }

    @Test
    void labelStillBreaksBetweenItsWords() {
        assertEquals(List.of("a " + ICON + "Circuit", "Assembler"), layout(60, "a ", ICON, "Circuit Assembler"));
    }

    @Test
    void whitespaceBeforeTheIconStillAllowsAWrap() {
        assertEquals(List.of("process ", ICON + "x"), layout(48, "process ", ICON, "x"));
    }

    @Test
    void tooLongLabelIsSplitAfterTheIconOnItsOwnLine() {
        assertEquals(List.of("ab ", ICON + "Cleanroo", "mCl"), layout(54, "ab ", ICON, "CleanroomCl"));
    }

    private static List<String> layout(int width, String... parts) {
        var lines = new ArrayList<Line>();
        var builder = new LineBuilder(
            new LayoutContext(MONOSPACE),
            0,
            0,
            width,
            lines,
            new ArrayList<>(),
            TextAlignment.LEFT);
        for (String part : parts) {
            builder.accept(ICON.equals(part) ? icon() : LytFlowText.of(part));
        }
        builder.end();

        var result = new ArrayList<String>();
        for (var line : lines) {
            var text = new StringBuilder();
            int x = line.firstElement != null ? line.firstElement.bounds.x() : 0;
            for (var el = line.firstElement; el != null; el = el.next) {
                assertEquals(x, el.bounds.x(), "elements on a line must be laid out back to back");
                x = el.bounds.right();
                if (el instanceof LineTextRun run) {
                    text.append(run.text);
                } else {
                    text.append(ICON);
                }
            }
            result.add(text.toString());
        }
        return result;
    }

    private static LytFlowContent icon() {
        var image = new LytImage();
        image.setDisplaySize(6, 6);
        return LytFlowInlineBlock.of(image);
    }
}
