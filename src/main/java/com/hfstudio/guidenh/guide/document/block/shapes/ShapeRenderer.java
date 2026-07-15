package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public interface ShapeRenderer {

    void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor);

    LytRect contentBounds(LytRect nodeRect, int contentW, int contentH, int padX, int padY);

    LytRect minNodeRect(int contentW, int contentH, int padX, int padY);

    default Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        return FlowchartShapes.intersectRect(nodeRect, ex, ey);
    }

    default boolean isClipped() {
        return false;
    }
}
