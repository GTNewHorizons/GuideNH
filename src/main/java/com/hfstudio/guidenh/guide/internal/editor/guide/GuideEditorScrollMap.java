package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytVisitor;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;

public class GuideEditorScrollMap {

    private final double[] editorPositions;
    private final double[] previewPositions;
    private LytDocument document;
    private long documentRevision;
    private long editorRevision;
    private int previewWidth;
    private int previewHeight;
    private int viewportHeight;

    public GuideEditorScrollMap(List<Anchor> anchors, int maxEditorScroll, int maxPreviewScroll) {
        var sorted = new ArrayList<>(anchors);
        sorted.sort(
            Comparator.comparingDouble(Anchor::editorY)
                .thenComparingDouble(Anchor::previewY));
        var points = new ArrayList<Anchor>();
        points.add(new Anchor(0, 0));
        for (var anchor : sorted) {
            var previous = points.getLast();
            if (anchor.editorY() > previous.editorY() && anchor.editorY() < maxEditorScroll
                && anchor.previewY() >= previous.previewY()
                && anchor.previewY() < maxPreviewScroll) {
                points.add(anchor);
            }
        }
        points.add(new Anchor(maxEditorScroll, maxPreviewScroll));
        editorPositions = new double[points.size()];
        previewPositions = new double[points.size()];
        for (int index = 0; index < points.size(); index++) {
            editorPositions[index] = points.get(index)
                .editorY();
            previewPositions[index] = points.get(index)
                .previewY();
        }
    }

    public static GuideEditorScrollMap create(LytDocument document, SceneEditorMultilineTextArea editor,
        int viewportHeight) {
        var anchors = new ArrayList<Anchor>();
        document.visit(new LytVisitor() {

            @Override
            public Result beforeNode(LytNode node) {
                if (node == document) return Result.CONTINUE;
                var source = node.getSourceNode();
                var bounds = node.getBounds();
                if (source != null && source.position() != null && bounds != null && bounds.height() > 0) {
                    var position = source.position();
                    anchors.add(
                        new Anchor(
                            editor.getContentYForOffset(
                                position.start()
                                    .offset()),
                            bounds.y()));
                    anchors.add(
                        new Anchor(
                            editor.getContentYForOffset(
                                position.end()
                                    .offset()),
                            bounds.bottom()));
                }
                return Result.CONTINUE;
            }
        });
        if (!anchors.isEmpty()) {
            double firstSourceY = anchors.stream()
                .mapToDouble(Anchor::editorY)
                .min()
                .orElse(0);
            anchors.add(new Anchor(firstSourceY, 0));
        }
        var map = new GuideEditorScrollMap(
            anchors,
            editor.getMaxVerticalScrollPixels(),
            Math.max(0, document.getContentHeight() - viewportHeight));
        map.document = document;
        map.documentRevision = document.getContentRevision();
        map.editorRevision = editor.getTextLayoutRevision();
        map.previewWidth = document.getAvailableWidth();
        map.previewHeight = document.getContentHeight();
        map.viewportHeight = viewportHeight;
        return map;
    }

    public boolean matches(LytDocument document, SceneEditorMultilineTextArea editor, int viewportHeight) {
        return this.document == document && documentRevision == document.getContentRevision()
            && editorRevision == editor.getTextLayoutRevision()
            && previewWidth == document.getAvailableWidth()
            && previewHeight == document.getContentHeight()
            && this.viewportHeight == viewportHeight;
    }

    public int toPreview(int editorY) {
        if (editorPositions[editorPositions.length - 1] == 0) return 0;
        return interpolate(editorPositions, previewPositions, editorY);
    }

    public int toEditor(int previewY) {
        if (previewPositions[previewPositions.length - 1] == 0) return 0;
        return interpolate(previewPositions, editorPositions, previewY);
    }

    private static int interpolate(double[] source, double[] target, int value) {
        int low = 0;
        int high = source.length;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (source[middle] <= value) low = middle;
            else high = middle;
        }
        if (low == source.length - 1) return (int) Math.round(target[low]);
        double fraction = Math.clamp((value - source[low]) / (source[low + 1] - source[low]), 0, 1);
        return (int) Math.round(target[low] + fraction * (target[low + 1] - target[low]));
    }

    public record Anchor(double editorY, double previewY) {}
}
