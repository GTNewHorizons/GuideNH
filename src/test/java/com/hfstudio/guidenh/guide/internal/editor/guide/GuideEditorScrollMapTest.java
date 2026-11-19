package com.hfstudio.guidenh.guide.internal.editor.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.compiler.GuideMarkdownOptions;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytFloatAwareBlock;
import com.hfstudio.guidenh.guide.internal.editor.guide.GuideEditorScrollMap.Anchor;
import com.hfstudio.guidenh.guide.internal.markdown.MdAstToMdxConverter;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.libs.mdast.MdAst;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;

public class GuideEditorScrollMapTest {

    @Test
    public void reparsedContainerChildrenDoNotMapLocalOffsetsIntoThePageFrontmatter() {
        String source = "---\ntitle: Example\n" + "# Metadata\n".repeat(30)
            + "---\n\nFirst paragraph.\n\nContainer body.\n\nLast paragraph.\n";
        var root = MdAst.fromMarkdown(source, GuideMarkdownOptions.runtime());
        MdAstToMdxConverter.convert(root, Map.of());
        var firstSource = (MdAstNode) root.children()
            .get(0);
        var containerSource = (MdAstNode) root.children()
            .get(1);
        var lastSource = (MdAstNode) root.children()
            .get(2);
        var localRoot = MdAst.fromMarkdown("Reparsed child.", GuideMarkdownOptions.runtime());
        var localSource = (MdAstNode) localRoot.children()
            .get(0);
        var document = new LytDocument() {

            @Override
            public int getContentHeight() {
                return 1000;
            }
        };
        document.setSourceNode(root);
        document.append(block(firstSource, 0, 40));
        var container = new LytFloatAwareBlock(block(localSource, 300, 40)) {

            @Override
            public LytRect getBounds() {
                return new LytRect(0, 300, 100, 40);
            }
        };
        container.setSourceNode(containerSource);
        document.append(container);
        document.append(block(lastSource, 700, 40));

        var map = GuideEditorScrollMap.create(document, offset -> offset, 1000, 100);

        assertEquals(0, map.toPreview(20));
        assertEquals(
            0,
            map.toPreview(
                firstSource.position()
                    .start()
                    .offset()));
        assertEquals(
            300,
            map.toPreview(
                containerSource.position()
                    .start()
                    .offset()));
        assertEquals(
            700,
            map.toPreview(
                lastSource.position()
                    .start()
                    .offset()));
    }

    private static LytBlock block(MdAstNode source, int y, int height) {
        var block = new LytBlock() {

            @Override
            public LytRect getBounds() {
                return new LytRect(0, y, 100, height);
            }

            @Override
            protected LytRect computeLayout(LayoutContext context, int x, int top, int availableWidth) {
                return getBounds();
            }

            @Override
            public void render(RenderContext context) {}

            @Override
            protected void onLayoutMoved(int deltaX, int deltaY) {}
        };
        block.setSourceNode(source);
        return block;
    }

    @Test
    public void frontmatterDoesNotAdvanceThePreviewAndBlocksUseTheirOwnHeight() {
        var map = new GuideEditorScrollMap(
            List.of(new Anchor(300, 0), new Anchor(400, 800), new Anchor(900, 900)),
            1000,
            1200);

        assertEquals(0, map.toPreview(150));
        assertEquals(400, map.toPreview(350));
        assertEquals(350, map.toEditor(400));
        assertEquals(850, map.toPreview(650));
        assertEquals(650, map.toEditor(850));
        assertEquals(1200, map.toPreview(1000));
        assertEquals(1000, map.toEditor(1200));
    }

    @Test
    public void collapsedSourceRegionsAndOverlappingColumnsKeepBothDirectionsMonotone() {
        var map = new GuideEditorScrollMap(
            List.of(
                new Anchor(100, 100),
                new Anchor(200, 100),
                new Anchor(200, 120),
                new Anchor(250, 90),
                new Anchor(300, 300)),
            400,
            400);

        assertEquals(200, map.toEditor(100));
        for (int offset = 0; offset < 400; offset++) {
            assertTrue(map.toPreview(offset) <= map.toPreview(offset + 1));
            assertTrue(map.toEditor(offset) <= map.toEditor(offset + 1));
        }
    }

    @Test
    public void unscrollablePanesAndOutOfRangeOffsetsStayAtValidEndpoints() {
        var sourceFits = new GuideEditorScrollMap(List.of(), 0, 500);
        var previewFits = new GuideEditorScrollMap(List.of(), 500, 0);
        var scrollable = new GuideEditorScrollMap(List.of(new Anchor(100, 200)), 500, 800);

        assertEquals(0, sourceFits.toPreview(0));
        assertEquals(0, previewFits.toEditor(0));
        assertEquals(0, scrollable.toPreview(-10));
        assertEquals(800, scrollable.toPreview(999));
        assertEquals(500, scrollable.toEditor(999));
    }
}
