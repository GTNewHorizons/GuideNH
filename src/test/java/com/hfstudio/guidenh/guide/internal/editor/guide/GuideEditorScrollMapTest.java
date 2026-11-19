package com.hfstudio.guidenh.guide.internal.editor.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.hfstudio.guidenh.guide.internal.editor.guide.GuideEditorScrollMap.Anchor;

public class GuideEditorScrollMapTest {

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
