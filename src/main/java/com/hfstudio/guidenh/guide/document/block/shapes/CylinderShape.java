package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
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

        int segments = 24;
        int arcPts = segments + 1;
        int n = arcPts * 2;
        float[] oxs = new float[n];
        float[] oys = new float[n];
        int idx = 0;
        // Upper arc (back arc, top rim): angle π → 2π (left to right, upper half)
        for (int i = 0; i < arcPts; i++) {
            double a = Math.PI + Math.PI * i / segments;
            oxs[idx] = cx + (float) (Math.cos(a) * rx);
            oys[idx] = bodyTop + (float) (Math.sin(a) * ry);
            idx++;
        }
        // Lower arc (front arc, bottom rim): angle 0 → π (right to left)
        for (int i = 0; i < arcPts; i++) {
            double a = Math.PI * i / segments;
            oxs[idx] = cx + (float) (Math.cos(a) * rx);
            oys[idx] = bodyBottom + (float) (Math.sin(a) * ry);
            idx++;
        }

        float centerX = cx;
        float centerY = (bodyTop + bodyBottom) / 2f;
        float[] ixs = new float[n];
        float[] iys = new float[n];
        for (int i = 0; i < n; i++) {
            float dx = oxs[i] - centerX;
            float dy = oys[i] - centerY;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1) {
                float s = (d - 1) / d;
                ixs[i] = centerX + dx * s;
                iys[i] = centerY + dy * s;
            } else {
                ixs[i] = oxs[i];
                iys[i] = oys[i];
            }
        }

        ShapeUtils.fillPolygonCentered(context, oxs, oys, borderColor);
        ShapeUtils.fillPolygonCentered(context, ixs, iys, backgroundColor);

        drawEllipseFrontArc(context, cx, bodyTop, rx, ry, borderColor);
    }

    private static void drawEllipseFrontArc(RenderContext context, float cx, float cy, float rx, float ry, int color) {
        int segments = 20;
        for (int i = 0; i < segments; i++) {
            double a1 = Math.PI * i / segments;
            double a2 = Math.PI * (i + 1) / segments;
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
        int extraV = Math.max(2, ry / 3);
        int top = nodeRect.y() + 2 * ry + padY + extraV;
        int bodyH = nodeRect.height() - 3 * ry - 2 * padY - 2 * extraV;
        return new LytRect(nodeRect.x() + padX, top, nodeRect.width() - 2 * padX, bodyH);
    }

    @Override
    public String renderSvg(int x, int y, int w, int h, String fill, String stroke) {
        int cx = x + w / 2, rx = w / 2, r = Math.max(3, h / 4);
        int top = y + r, bot = y + h - r;
        return String.format(
            """
                <path d="M %d,%d A %d,%d 0 0,1 %d,%d L %d,%d A %d,%d 0 0,1 %d,%d Z" fill="%s"/>
                <ellipse cx="%d" cy="%d" rx="%d" ry="%d" fill="%s" stroke="%s" stroke-width="1.5"/>
                <path d="M %d,%d A %d,%d 0 0,1 %d,%d" fill="none" stroke="%s" stroke-width="1.5"/>
                <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s" stroke-width="1.5"/>
                <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s" stroke-width="1.5"/>""",
            x,
            top,
            rx,
            r,
            x + w,
            top,
            x + w,
            bot,
            rx,
            r,
            x,
            bot,
            fill,
            cx,
            top,
            rx,
            r,
            fill,
            stroke,
            x + w,
            bot,
            rx,
            r,
            x,
            bot,
            stroke,
            x,
            top,
            x,
            bot,
            stroke,
            x + w,
            top,
            x + w,
            bot,
            stroke);
    }

    @Override
    public boolean isClipped() {
        return true;
    }

    @Override
    public Point edgeIntersect(LytRect nodeRect, int ex, int ey) {
        int x = nodeRect.x(), y = nodeRect.y(), w = nodeRect.width(), h = nodeRect.height();
        int cx = x + w / 2;
        int rx = w / 2;
        int ry = Math.max(1, rx / 3);
        int bodyTop = y + ry;
        int bodyBottom = y + h - ry;
        int midY = (bodyTop + bodyBottom) / 2;

        double dx = ex - cx;
        double dy = ey - midY;
        if (dx == 0 && dy == 0) return new Point(cx, midY);

        double rx2 = (double) rx * rx;
        double ry2 = (double) ry * ry;

        // Precompute shared terms for ellipse intersection
        double A = dx * dx / rx2 + dy * dy / ry2;

        double bestT = Double.MAX_VALUE;
        Point best = null;

        if (A > 1e-12) {
            // Top half-ellipse: center at (cx, bodyTop), y <= bodyTop
            double hy = midY - bodyTop;
            double B = 2.0 * hy * dy / ry2;
            double C = hy * hy / ry2 - 1.0;
            double disc = B * B - 4.0 * A * C;
            if (disc >= 0) {
                double sqrtDisc = Math.sqrt(disc);
                for (double t : new double[] { (-B - sqrtDisc) / (2.0 * A), (-B + sqrtDisc) / (2.0 * A) }) {
                    if (t > 0 && t < bestT && midY + t * dy <= bodyTop) {
                        bestT = t;
                        best = new Point((int) Math.round(cx + t * dx), (int) Math.round(midY + t * dy));
                    }
                }
            }

            // Bottom half-ellipse: center at (cx, bodyBottom), y >= bodyBottom
            hy = midY - bodyBottom;
            B = 2.0 * hy * dy / ry2;
            C = hy * hy / ry2 - 1.0;
            disc = B * B - 4.0 * A * C;
            if (disc >= 0) {
                double sqrtDisc = Math.sqrt(disc);
                for (double t : new double[] { (-B - sqrtDisc) / (2.0 * A), (-B + sqrtDisc) / (2.0 * A) }) {
                    if (t > 0 && t < bestT && midY + t * dy >= bodyBottom) {
                        bestT = t;
                        best = new Point((int) Math.round(cx + t * dx), (int) Math.round(midY + t * dy));
                    }
                }
            }
        }

        // Check left and right vertical lines
        if (Math.abs(dx) > 1e-12) {
            double invDx = 1.0 / dx;
            for (int lx : new int[] { x, x + w }) {
                double t = (lx - cx) * invDx;
                if (t > 0 && t < bestT) {
                    double py = midY + t * dy;
                    if (py >= bodyTop && py <= bodyBottom) {
                        bestT = t;
                        best = new Point(lx, (int) Math.round(py));
                    }
                }
            }
        }

        return best != null ? best : FlowchartShapes.intersectRect(nodeRect, ex, ey);
    }

    @Override
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        int estRy = Math.max(1, pw / 6);
        int estExtra = Math.max(2, estRy / 3);
        int minH = 3 * estRy + ch + 2 * padY + 2 * estExtra;
        return new LytRect(0, 0, pw, Math.max(ph * 2, minH));
    }
}
