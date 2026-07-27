package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

public class CircleShape implements ShapeRenderer {

    @Override
    public void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = Math.min(rect.width(), rect.height()) / 2;
        if (r > 0) {
            c.emit(new GuideRenderPrimitive.DrawCircle(cx, cy, r, borderColor, true));
            c.emit(new GuideRenderPrimitive.DrawCircle(cx, cy, Math.max(r - 1, 1), backgroundColor, true));
        }
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int cx = nodeRect.x() + nodeRect.width() / 2;
        int cy = nodeRect.y() + nodeRect.height() / 2;
        int r = Math.min(nodeRect.width(), nodeRect.height()) / 2;
        int insSide = (int) (r * Math.sqrt(2));
        return new LytRect(cx - insSide / 2, cy - insSide / 2, insSide, insSide);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int maxContent = Math.max(cw + 2 * padX, ch + 2 * padY);
        int side = (int) Math.ceil(maxContent * Math.sqrt(2));
        return new LytRect(0, 0, side, side);
    }
}
