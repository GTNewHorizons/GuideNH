package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

public interface ShapeRenderer {

    /** Emit primitives for this shape into the collector. */
    void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor);

    LytRect contentBounds(LytRect nodeRect, int contentW, int contentH, int padX, int padY);

    LytRect minNodeRect(int contentW, int contentH, int padX, int padY);
}
