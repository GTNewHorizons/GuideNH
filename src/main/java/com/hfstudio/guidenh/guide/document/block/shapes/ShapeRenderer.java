package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
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
}
