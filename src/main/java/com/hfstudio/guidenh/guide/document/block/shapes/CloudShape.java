package com.hfstudio.guidenh.guide.document.block.shapes;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.RenderContext;

public class CloudShape implements ShapeRenderer {

    @Override
    public void render(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int x = rect.x(), y = rect.y(), w = rect.width(), h = rect.height();
        float[] raw = buildCloudPolygon(w, h);
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

        context.fillPolygon(xs, ys, borderColor);
        context.fillPolygon(ixs, iys, backgroundColor);
    }

    private static float[] buildCloudPolygon(float w, float h) {
        float r1 = 0.15f * w;
        float r2 = 0.25f * w;
        float r3 = 0.35f * w;
        float r4 = 0.20f * w;

        float[][] arcs = { { 0.25f * w, -0.10f * w, r1, r1, 0, 1 }, { 0.40f * w, -0.10f * w, r3, r3, 1, 1 },
            { 0.35f * w, 0.20f * w, r2, r2, 1, 1 }, { 0.15f * w, 0.35f * h, r1, r1, 1, 1 },
            { -0.15f * w, 0.65f * h, r4, r4, 1, 1 }, { -0.25f * w, 0.15f * w, r2, r1, 1, 1 },
            { -0.50f * w, 0, r3, r3, 1, 1 }, { -0.25f * w, -0.15f * w, r1, r1, 1, 1 },
            { -0.10f * w, -0.35f * h, r1, r1, 1, 1 }, { 0.10f * w, -0.65f * h, r4, r4, 1, 1 }, };

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
        int cx = nodeRect.x() + nodeRect.width() / 2;
        int cy = nodeRect.y() + nodeRect.height() / 2;
        int r = Math.min(nodeRect.width(), nodeRect.height()) / 3;
        int insSide = (int) (r * Math.sqrt(2));
        int availW = Math.max(insSide - 2 * padX, 1);
        int availH = Math.max(insSide - 2 * padY, 1);
        int contentW = Math.min(availW, cw);
        int contentH = Math.min(availH, ch);
        return new LytRect(cx - contentW / 2, cy - contentH / 2, contentW, contentH);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int maxPadded = Math.max(cw + 2 * padX, ch + 2 * padY);
        int dim = (int) Math.ceil(3 * maxPadded / Math.sqrt(2));
        return new LytRect(0, 0, dim, dim);
    }
}
