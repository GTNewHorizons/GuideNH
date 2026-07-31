package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class HexagonShape implements ShapeRenderer {

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        int x = nodeRect.x(), y = nodeRect.y(), w = nodeRect.width(), h = nodeRect.height();
        int r = x + w, b = y + h, cy = y + h / 2;
        int inset = Math.max(1, h / 4);
        return FlowchartShapes.intersectPolygon(
            nodeRect,
            new int[][] { { x + inset, y }, { r - inset, y }, { r, cy }, { r - inset, b }, { x + inset, b },
                { x, cy } },
            ex,
            ey);
    }

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom(), cy = y + h / 2;
        int inset = Math.max(1, h / 4);

        float[] xs = { x + inset, r - inset, r, r - inset, x + inset, x };
        float[] ys = { y, y, cy, b, b, cy };
        float cx = x + w / 2f;
        float cy2 = y + h / 2f;
        float[] shrunkXs = new float[6];
        float[] shrunkYs = new float[6];
        for (int i = 0; i < 6; i++) {
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
        int inset = Math.max(1, h / 4);
        return String.format(
            "<polygon points=\"%d,%d %d,%d %d,%d %d,%d %d,%d %d,%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\" stroke-linejoin=\"round\"/>",
            x + inset,
            y,
            r - inset,
            y,
            r,
            cy,
            r - inset,
            b,
            x + inset,
            b,
            x,
            cy,
            fill,
            stroke);
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int w = nodeRect.width();
        int h = nodeRect.height();
        int cx = nodeRect.x() + w / 2;
        int cy = nodeRect.y() + h / 2;
        int inset = Math.max(1, h / 4);
        int hh = Math.min(ch / 2 + padY, h / 2);
        int availW = Math.max(w - 4 * hh * inset / h, 1);
        int availH = Math.max(2 * hh, 1);
        int contentW = Math.min(availW, cw + 2 * padX);
        int contentH = Math.min(availH, ch + 2 * padY);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        // Inscribed rectangle width = w - 4*hh*inset/h where hh = ph/2, inset = h/4.
        // width = w - 4*(ph/2)*(h/4)/h = w - ph/2
        // Need width >= pw, so w >= pw + ph/2
        int w = pw + Math.max(ph / 2, 1);
        return new LytRect(0, 0, w, ph);
    }
}
