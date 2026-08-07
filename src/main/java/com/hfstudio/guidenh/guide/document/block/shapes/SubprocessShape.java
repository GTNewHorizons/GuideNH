package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

public class SubprocessShape implements ShapeRenderer {

    private static final int FRAME_WIDTH = 8;

    @Override
    public void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int innerX = x + FRAME_WIDTH;
        int innerW = w - FRAME_WIDTH * 2;

        c.emit(new GuideRenderPrimitive.FillRect(x, y, w, h, borderColor));
        c.emit(new GuideRenderPrimitive.FillRect(innerX, y, innerW, h, backgroundColor));
        c.emit(new GuideRenderPrimitive.DrawLine(innerX, y, innerX, y + h, 1, borderColor));
        c.emit(new GuideRenderPrimitive.DrawLine(innerX + innerW, y, innerX + innerW, y + h, 1, borderColor));
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY, float zoom) {
        // The frame inset must scale with zoom: the node rect is already in
        // scaled render space, so an unscaled FRAME_WIDTH would carve a fixed
        // logical-8px frame out of a scaled rect and shrink the content area
        // faster than the (scaled) text, forcing spurious word-wrap/overflow
        // on the zoomed path.
        int frame = Math.max(1, Math.round(FRAME_WIDTH * zoom));
        int innerX = nodeRect.x() + frame;
        int innerW = nodeRect.width() - frame * 2;
        return new LytRect(innerX + padX, nodeRect.y() + padY, innerW - 2 * padX, nodeRect.height() - 2 * padY);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int w = cw + 2 * padX + FRAME_WIDTH * 2;
        int h = ch + 2 * padY;
        return new LytRect(0, 0, w, h);
    }
}
