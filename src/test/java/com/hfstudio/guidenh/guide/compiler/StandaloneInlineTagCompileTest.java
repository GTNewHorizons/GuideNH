package com.hfstudio.guidenh.guide.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.mediawiki.template.MediaWikiTemplateTestHarness;

class StandaloneInlineTagCompileTest {

    private final MediaWikiTemplateTestHarness harness = new MediaWikiTemplateTestHarness();

    @Test
    void tagAloneOnItsLineInATableCellIsCompiled() {
        assertEquals(1, inlineBlocks("<table><tr><td>\n<ItemImage id=\"minecraft:stone\" />\n</td></tr></table>\n"));
    }

    @Test
    void tagAloneInADetailsSummaryIsCompiled() {
        String source = "<details>\n<summary><ItemImage id=\"minecraft:stone\" /></summary>\n\nbody\n\n</details>\n";
        assertEquals(1, inlineBlocks(source));
        assertFalse(
            MediaWikiTemplateTestHarness.text(harness.compile(source))
                .contains("Details"));
    }

    private int inlineBlocks(String source) {
        int[] count = new int[1];
        harness.compile(source)
            .document()
            .visit(new LytVisitor() {

                @Override
                public Result beforeFlowContent(LytFlowContent content) {
                    if (content instanceof LytFlowInlineBlock) {
                        count[0]++;
                    }
                    return Result.CONTINUE;
                }
            });
        return count[0];
    }
}
