package com.hfstudio.guidenh.guide.document.block.shapes;

import java.util.ArrayList;
import java.util.List;

import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

final class ShapeUtils {

    private ShapeUtils() {}

    /** Same as fillPolygonCentered but emits a DrawPolygon primitive. */
    static void emitPolygonCentered(PrimitiveCollector c, float[] xs, float[] ys, int color) {
        int n = xs.length;
        if (n < 3) return;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            cx += xs[i];
            cy += ys[i];
        }
        cx /= n;
        cy /= n;
        float[] fanXs = new float[n + 2];
        float[] fanYs = new float[n + 2];
        fanXs[0] = cx;
        fanYs[0] = cy;
        System.arraycopy(xs, 0, fanXs, 1, n);
        System.arraycopy(ys, 0, fanYs, 1, n);
        fanXs[n + 1] = xs[0];
        fanYs[n + 1] = ys[0];
        c.emit(new GuideRenderPrimitive.DrawPolygon(fanXs, fanYs, color));
    }

    static List<float[]> arcToPoints(float cx, float cy, float rx, float ry, float startAngle, float endAngle,
        int segments) {
        List<float[]> points = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            float t = startAngle + (endAngle - startAngle) * i / segments;
            points.add(new float[] { cx + (float) Math.cos(t) * rx, cy + (float) Math.sin(t) * ry });
        }
        return points;
    }

    static List<float[]> svgArc(float x1, float y1, float x2, float y2, float rx, float ry, float xAxisRot,
        boolean largeArc, boolean sweep, int segments) {
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        if (rx == 0 || ry == 0) {
            List<float[]> pts = new ArrayList<>();
            pts.add(new float[] { x1, y1 });
            pts.add(new float[] { x2, y2 });
            return pts;
        }

        double phi = Math.toRadians(xAxisRot);
        double cosP = Math.cos(phi);
        double sinP = Math.sin(phi);

        double dx = (x1 - x2) / 2;
        double dy = (y1 - y2) / 2;
        double x1p = cosP * dx + sinP * dy;
        double y1p = -sinP * dx + cosP * dy;

        double rxSq = rx * rx;
        double rySq = ry * ry;
        double x1pSq = x1p * x1p;
        double y1pSq = y1p * y1p;

        double cr = x1pSq / rxSq + y1pSq / rySq;
        if (cr > 1) {
            double s = Math.sqrt(cr);
            rx *= (float) s;
            ry *= (float) s;
            rxSq = rx * rx;
            rySq = ry * ry;
        }

        double dq = rxSq * y1pSq + rySq * x1pSq;
        if (dq == 0) {
            List<float[]> pts = new ArrayList<>();
            pts.add(new float[] { x1, y1 });
            pts.add(new float[] { x2, y2 });
            return pts;
        }

        double sq = Math.sqrt(Math.max(0, (rxSq * rySq - dq) / dq));
        if (largeArc == sweep) sq = -sq;

        double cxp = sq * rx * y1p / ry;
        double cyp = -sq * ry * x1p / rx;

        double cx = cosP * cxp - sinP * cyp + (x1 + x2) / 2;
        double cy = sinP * cxp + cosP * cyp + (y1 + y2) / 2;

        double startAngle = Math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx);
        double endAngle = Math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);

        double delta = endAngle - startAngle;
        if (sweep && delta < 0) delta += 2 * Math.PI;
        if (!sweep && delta > 0) delta -= 2 * Math.PI;

        List<float[]> points = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double t = startAngle + delta * i / segments;
            double ex = cx + rx * Math.cos(t) * cosP - ry * Math.sin(t) * sinP;
            double ey = cy + rx * Math.cos(t) * sinP + ry * Math.sin(t) * cosP;
            points.add(new float[] { (float) ex, (float) ey });
        }
        return points;
    }
}
