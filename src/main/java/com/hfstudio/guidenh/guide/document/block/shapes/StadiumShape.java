package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

public class StadiumShape implements ShapeRenderer {

    @Override
    public void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = h / 2;
        float cy = y + h / 2f;

        c.emit(new GuideRenderPrimitive.FillRect(x + r, y, w - r * 2, h, borderColor));
        c.emit(new GuideRenderPrimitive.DrawCircle(x + r, cy, r, borderColor, true));
        c.emit(new GuideRenderPrimitive.DrawCircle(x + w - r, cy, r, borderColor, true));

        int ir = Math.max(r - 1, 0);
        c.emit(new GuideRenderPrimitive.FillRect(x + r, y + 1, w - r * 2, h - 2, backgroundColor));
        c.emit(new GuideRenderPrimitive.DrawCircle(x + r, cy, ir, backgroundColor, true));
        c.emit(new GuideRenderPrimitive.DrawCircle(x + w - r, cy, ir, backgroundColor, true));
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
