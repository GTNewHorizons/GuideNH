package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class TrapezoidShape implements ShapeRenderer {

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        int x = nodeRect.x(), y = nodeRect.y(), w = nodeRect.width(), h = nodeRect.height();
        int r = x + w, b = y + h;
        int inset = Math.max(1, h / 4);
        return FlowchartShapes
            .intersectPolygon(nodeRect, new int[][] { { x + inset, y }, { r - inset, y }, { r, b }, { x, b } }, ex, ey);
    }

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        int r = rect.right(), b = rect.bottom();
        int inset = Math.max(1, h / 4);

        float cx = x + w / 2f;
        float cy = y + h / 2f;
        float[] xs = { x + inset, r - inset, r, x };
        float[] ys = { y, y, b, b };
        float[] shrunkXs = new float[4];
        float[] shrunkYs = new float[4];
        for (int i = 0; i < 4; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                shrunkXs[i] = cx + dx * s;
                shrunkYs[i] = cy + dy * s;
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
        int r = x + w, b = y + h;
        int inset = Math.max(1, h / 4);
        return String.format(
            "<polygon points=\"%d,%d %d,%d %d,%d %d,%d\" fill=\"%s\" stroke=\"%s\" stroke-width=\"1.5\" stroke-linejoin=\"round\"/>",
            x + inset,
            y,
            r - inset,
            y,
            r,
            b,
            x,
            b,
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
        // Trapezoid is wider at bottom, narrower at top.
        // Top width = w - 2*inset, bottom width = w.
        // The inscribed rectangle has full height h, width constrained by the narrower top.
        int hh = Math.min(ch / 2 + padY, h / 2);
        // Width at this half-height from center:
        float t = (float) hh / (h / 2f);
        int availW = Math.max(w - 2 * (int) (inset * t) - 2 * padX, 1);
        int availH = Math.max(h - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        // Top width = w - 2*inset, bottom = w. Content is centered.
        // At the vertical midpoint of content (hh = ph/2 from center),
        // width = w - 2*inset*(hh/(h/2)) = w - 4*hh*inset/h.
        // With inset = h/4: width = w - hh = w - ph/2.
        // Need width >= pw, so w >= pw + ph/2.
        int w = pw + Math.max(ph / 2, 1);
        return new LytRect(0, 0, w, ph);
    }
}
