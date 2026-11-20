package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class AsymmetricShape implements ShapeRenderer {

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        int x = nodeRect.x(), y = nodeRect.y(), w = nodeRect.width(), h = nodeRect.height();
        int r = x + w, b = y + h, cy = y + h / 2;
        int inset = Math.max(2, h / 4);
        return FlowchartShapes.intersectPolygon(
            nodeRect,
            new int[][] { { r, y }, { r, b }, { x + inset, b }, { x, cy }, { x + inset, y } },
            ex,
            ey);
    }

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cy = y + h / 2;
        int inset = Math.max(2, h / 4);

        float cx = x + w / 2f;
        float cy2 = y + h / 2f;
        float[] xs = { r, r, x + inset, x, x + inset };
        float[] ys = { y, b, b, cy, y };
        float[] shrunkXs = new float[5];
        float[] shrunkYs = new float[5];
        for (int i = 0; i < 5; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy2;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                shrunkXs[i] = cx + dx * s;
                shrunkYs[i] = cy2 + dy * s;
            } else {
                shrunkXs[i] = xs[i];
                shrunkYs[i] = ys[i];
            }
        }
        context.fillPolygon(xs, ys, borderColor);
        context.fillPolygon(shrunkXs, shrunkYs, backgroundColor);
    }

    @Override
    public String renderSvg(int x, int y, int w, int h, String fill, String stroke) {
        int r = x + w, b = y + h, cy = y + h / 2;
        int inset = Math.max(2, h / 4);
        return String.format(
            "<polygon points=\"%d,%d %d,%d %d,%d %d,%d %d,%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\" stroke-linejoin=\"round\"/>",
            r,
            y,
            r,
            b,
            x + inset,
            b,
            x,
            cy,
            x + inset,
            y,
            fill,
            stroke);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int w = nodeRect.width();
        int h = nodeRect.height();
        int inset = Math.max(2, h / 4);
        return ShapeUtils.centeredContent(
            nodeRect.x() + inset + padX,
            nodeRect.y() + padY,
            w - inset - 2 * padX,
            h - 2 * padY,
            cw,
            ch);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        int inset = Math.max(2, ph / 4);
        int w = pw + inset;
        return new LytRect(0, 0, w, ph);
    }
}
