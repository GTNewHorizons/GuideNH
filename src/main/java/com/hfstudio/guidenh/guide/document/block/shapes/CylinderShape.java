package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CylinderShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int cx = x + w / 2;
        int rx = w / 2;
        int ry = Math.max(1, rx / 3);
        int bodyTop = y + ry;
        int bodyBottom = y + h - ry;

        context.fillRect(new LytRect(x, bodyTop, w, bodyBottom - bodyTop), borderColor);
        context.fillEllipse(cx, bodyTop, rx, ry, borderColor);
        context.fillEllipse(cx, bodyBottom, rx, ry, borderColor);

        int irx = Math.max(rx - 1, 0);
        int iry = Math.max(ry - 1, 0);
        context.fillRect(new LytRect(x + 1, bodyTop + 1, w - 2, bodyBottom - bodyTop - 2), backgroundColor);
        context.fillEllipse(cx, bodyTop, irx, iry, backgroundColor);
        context.fillEllipse(cx, bodyBottom, irx, iry, backgroundColor);

        drawEllipseFrontArc(context, cx, bodyTop, rx, ry, borderColor);
        drawEllipseFrontArc(context, cx, bodyBottom, rx, ry, borderColor);
    }

    private static void drawEllipseFrontArc(RenderContext context, float cx, float cy, float rx, float ry, int color) {
        int segments = 20;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI + (Math.PI * i / segments);
            double a2 = Math.PI + (Math.PI * (i + 1) / segments);
            float x1 = cx + (float) (Math.cos(a1) * rx);
            float y1 = cy + (float) (Math.sin(a1) * ry);
            float x2 = cx + (float) (Math.cos(a2) * rx);
            float y2 = cy + (float) (Math.sin(a2) * ry);
            context.drawLine(x1, y1, x2, y2, 1, color);
        }
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int rx = nodeRect.width() / 2;
        int ry = Math.max(1, rx / 3);
        int top = nodeRect.y() + ry;
        int bodyH = nodeRect.height() - 2 * ry;
        return new LytRect(nodeRect.x() + padX, top + padY, nodeRect.width() - 2 * padX, bodyH - 2 * padY);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        return new LytRect(0, 0, pw, ph * 2);
    }
}
