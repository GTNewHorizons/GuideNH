package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class EllipseShape implements ShapeRenderer {

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        return FlowchartShapes.intersectEllipse(nodeRect, ex, ey);
    }

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        float rx = rect.width() / 2f;
        float ry = rect.height() / 2f;
        context.fillEllipse(cx, cy, rx, ry, borderColor);
        context.fillEllipse(cx, cy, Math.max(rx - 1, 0.5f), Math.max(ry - 1, 0.5f), backgroundColor);
    }

    @Override
    public String renderSvg(int x, int y, int w, int h, String fill, String stroke) {
        int cx = x + w / 2, cy = y + h / 2;
        return String.format(
            "<ellipse cx=\"%d\" cy=\"%d\" rx=\"%d\" ry=\"%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\"/>",
            cx,
            cy,
            w / 2,
            h / 2,
            fill,
            stroke);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int cx = nodeRect.x() + nodeRect.width() / 2;
        int cy = nodeRect.y() + nodeRect.height() / 2;
        int insW = (int) (nodeRect.width() / Math.sqrt(2));
        int insH = (int) (nodeRect.height() / Math.sqrt(2));
        int availW = Math.max(insW - 2 * padX, 1);
        int availH = Math.max(insH - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        int w = (int) Math.ceil(pw * Math.sqrt(2));
        int h = (int) Math.ceil(ph * Math.sqrt(2));
        return new LytRect(0, 0, w, h);
    }
}
