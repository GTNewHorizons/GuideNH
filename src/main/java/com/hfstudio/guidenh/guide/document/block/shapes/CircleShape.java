package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CircleShape implements ShapeRenderer {

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        return FlowchartShapes.intersectCircle(nodeRect, ex, ey);
    }

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = Math.min(rect.width(), rect.height()) / 2;
        if (r > 0) {
            context.fillCircle(cx, cy, r, borderColor);
            context.fillCircle(cx, cy, Math.max(r - 1, 1), backgroundColor);
        }
    }

    @Override
    public String renderSvg(int x, int y, int w, int h, String fill, String stroke) {
        int cx = x + w / 2, cy = y + h / 2;
        int r = Math.min(w, h) / 2;
        return String.format(
            "<circle cx=\"%d\" cy=\"%d\" r=\"%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\"/>",
            cx,
            cy,
            r,
            fill,
            stroke);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int r = Math.min(nodeRect.width(), nodeRect.height()) / 2;
        int insSide = 2 * (int) (r / Math.sqrt(2));
        return ShapeUtils.centeredContent(
            nodeRect.x() + (nodeRect.width() - insSide) / 2 + padX,
            nodeRect.y() + (nodeRect.height() - insSide) / 2 + padY,
            insSide - 2 * padX,
            insSide - 2 * padY,
            cw,
            ch);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int maxContent = Math.max(cw + 2 * padX, ch + 2 * padY);
        int radius = (int) Math.ceil(((maxContent + 1) / 2.0) * Math.sqrt(2));
        int side = radius * 2;
        return new LytRect(0, 0, side, side);
    }
}
