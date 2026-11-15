package com.hfstudio.guidenh.guide.document.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.internal.markdown.FileTreeParser.SlotKind;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

class LytFileTreeLayoutTest {

    private static LayoutContext context() {
        return new LayoutContext(new FontMetrics() {

            @Override
            public float getAdvance(int codePoint, ResolvedTextStyle style) {
                return 6;
            }

            @Override
            public int getLineHeight(ResolvedTextStyle style) {
                return 10;
            }
        });
    }

    @Test
    void exportsStretchableConnectorSlots() {
        var html = com.hfstudio.guidenh.guide.siteexport.site.GuideSiteGraphRenderer
            .renderFileTree("root/\n│   ├── file.json\n│   └── next.json");
        assertTrue(html.contains("--tree-max-depth:2"), html);
        assertTrue(html.contains("guide-file-tree-slot-vertical"), html);
        assertTrue(html.contains("guide-file-tree-slot-last-branch"), html);
    }

    @Test
    void deepRowsKeepReadableTextWidth() {
        var tree = new LytFileTree();
        var payload = LytParagraph.of("configuration/processing/very_long_machine_recipe.json");
        tree.appendRow(Collections.nCopies(12, SlotKind.BRANCH), null, payload);
        tree.layout(context(), 20, 30, 120);
        assertTrue(
            payload.getBounds()
                .x() <= 80,
            payload.getBounds()
                .toString());
        assertTrue(
            payload.getBounds()
                .right() <= 140,
            payload.getBounds()
                .toString());
        assertTrue(
            payload.getFirstLineBounds()
                .width() >= 54);
    }

    @Test
    void iconsStayAlignedWithTheFirstLineWhenTextWraps() {
        var tree = new LytFileTree();
        var icon = LytParagraph.of("+");
        var payload = LytParagraph.of("configuration/processing/very_long_machine_recipe.json");
        tree.appendRow(List.of(SlotKind.LAST_BRANCH), icon, payload);
        tree.layout(context(), 0, 0, 120);
        assertTrue(
            payload.getBounds()
                .height()
                > icon.getBounds()
                    .height());
        assertEquals(
            payload.getFirstLineBounds()
                .y(),
            icon.getBounds()
                .y());
        tree.moveLayoutPos(7, 9);
        assertEquals(
            payload.getFirstLineBounds()
                .y(),
            icon.getBounds()
                .y());
    }
}
