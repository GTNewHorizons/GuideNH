package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

public interface ShapeRenderer {

    /** Emit primitives for this shape into the collector. */
    void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor);

    /**
     * Compute the content rect (text/badge area) inside a rendered node rect.
     * {@code nodeRect} is in the scaled render coordinate space (already
     * multiplied by the active zoom); {@code zoom} lets shapes that consume
     * fixed logical insets (subprocess frame, circular insets) scale those
     * insets consistently, so the content rect stays self-consistent with
     * the scaled text width at any zoom (otherwise the content area shrinks
     * faster than the text and spurious word-wrap / overflow appears).
     */
    LytRect contentBounds(LytRect nodeRect, int contentW, int contentH, int padX, int padY, float zoom);

    LytRect minNodeRect(int contentW, int contentH, int padX, int padY);

    default Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        return FlowchartShapes.intersectRect(nodeRect, ex, ey);
    }

    default boolean isClipped() {
        return false;
    }

    /** Returns SVG markup for this shape's outline + fill, fitted to (x,y,w,h). */
    default String renderSvg(int x, int y, int w, int h, String fill, String stroke) {
        return String.format(
            "<rect x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\"/>",
            x,
            y,
            w,
            h,
            fill,
            stroke);
    }
}
