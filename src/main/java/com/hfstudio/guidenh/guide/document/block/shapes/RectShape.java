package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class RectShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        context.fillRect(rect, backgroundColor);
        context.drawBorder(rect, borderColor, 1);
    }

    @Override
    public void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor) {
        c.emit(new GuideRenderPrimitive.FillRect(rect.x(), rect.y(), rect.width(), rect.height(), backgroundColor));
        c.emit(
            new GuideRenderPrimitive.DrawBorder(
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height(),
                1,
                1,
                1,
                1,
                borderColor));
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        return nodeRect.shrink(padX, padY, padX, padY);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        return new LytRect(0, 0, cw + 2 * padX, ch + 2 * padY);
    }
}
