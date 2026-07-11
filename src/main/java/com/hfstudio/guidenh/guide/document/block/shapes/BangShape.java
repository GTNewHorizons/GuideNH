package com.hfstudio.guidenh.guide.document.block.shapes;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class BangShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        float[] raw = buildBangPolygon(w, h);
        int n = raw.length / 2;

        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        for (int i = 0; i < raw.length; i += 2) {
            if (raw[i] < minX) minX = raw[i];
            if (raw[i] > maxX) maxX = raw[i];
            if (raw[i + 1] < minY) minY = raw[i + 1];
            if (raw[i + 1] > maxY) maxY = raw[i + 1];
        }

        float sx = (maxX > minX) ? w / (maxX - minX) : 1;
        float sy = (maxY > minY) ? h / (maxY - minY) : 1;
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = x + (raw[i * 2] - minX) * sx;
            ys[i] = y + (raw[i * 2 + 1] - minY) * sy;
        }

        float cx = x + w / 2f, cy = y + h / 2f;
        float[] ixs = new float[n];
        float[] iys = new float[n];
        for (int i = 0; i < n; i++) {
            float dx = xs[i] - cx;
            float dy = ys[i] - cy;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                ixs[i] = cx + dx * s;
                iys[i] = cy + dy * s;
            } else {
                ixs[i] = xs[i];
                iys[i] = ys[i];
            }
        }

        ShapeUtils.fillPolygonCentered(context, xs, ys, borderColor);
        ShapeUtils.fillPolygonCentered(context, ixs, iys, backgroundColor);
    }

    private static float[] buildBangPolygon(float w, float h) {
        float r = 0.15f * w;

        // SVG arc: a rx ry xAxisRot largeArc sweep dx dy
        float[][] arcs = { { 0.25f * w, -0.10f * h, r, r, 0, 0 }, { 0.25f * w, 0, r, r, 0, 0 },
            { 0.25f * w, 0, r, r, 0, 0 }, { 0.25f * w, 0.10f * h, r, r, 0, 0 }, { 0.15f * w, 0.33f * h, r, r, 0, 0 },
            { 0, 0.34f * h, r * 0.8f, r * 0.8f, 0, 0 }, { -0.15f * w, 0.33f * h, r, r, 0, 0 },
            { -0.25f * w, 0.15f * h, r, r, 0, 0 }, { -0.25f * w, 0, r, r, 0, 0 }, { -0.25f * w, 0, r, r, 0, 0 },
            { -0.25f * w, -0.15f * h, r, r, 0, 0 }, { -0.10f * w, -0.33f * h, r, r, 0, 0 },
            { 0, -0.34f * h, r * 0.8f, r * 0.8f, 0, 0 }, { 0.10f * w, -0.33f * h, r, r, 0, 0 }, };

        List<Float> pts = new ArrayList<>();
        float cx = 0, cy = 0;
        pts.add(cx);
        pts.add(cy);

        int segments = 20;
        for (float[] a : arcs) {
            float dx = a[0], dy = a[1], rx = a[2], ry = a[3];
            boolean large = a[4] > 0;
            boolean sweep = a[5] > 0;
            float ex = cx + dx;
            float ey = cy + dy;
            List<float[]> arcPts = ShapeUtils.svgArc(cx, cy, ex, ey, rx, ry, 0, large, sweep, segments);
            for (int i = 1; i < arcPts.size(); i++) {
                pts.add(arcPts.get(i)[0]);
                pts.add(arcPts.get(i)[1]);
            }
            cx = ex;
            cy = ey;
        }

        float[] result = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            result[i] = pts.get(i);
        }
        return result;
    }

    @Override
    public LytRect contentBounds(LytRect nodeRect, int cw, int ch, int padX, int padY) {
        int w = nodeRect.width();
        int h = nodeRect.height();
        int cx = nodeRect.x() + w / 2;
        int cy = nodeRect.y() + h / 2;
        int n = Math.max(2, Math.min(w, h) / 6);
        int availW = Math.max(w - 2 * n - 2 * padX, 1);
        int availH = Math.max(h - 2 * n - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int maxPadded = Math.max(cw + 2 * padX, ch + 2 * padY);
        int dim = (int) Math.ceil(maxPadded * 1.5);
        return new LytRect(0, 0, dim, dim);
    }
}
