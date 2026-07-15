package com.hfstudio.guidenh.guide.document.block.shapes;

import java.util.EnumMap;
import java.util.Map;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.mermaid.flowchart.FlowchartLayoutResult.Point;
import com.hfstudio.guidenh.guide.render.RenderContext;

public final class FlowchartShapes {

    private static final Map<MermaidNodeShape, ShapeRenderer> RENDERERS = new EnumMap<>(MermaidNodeShape.class);

    static {
        RENDERERS.put(MermaidNodeShape.DEFAULT, new RectShape());
        RENDERERS.put(MermaidNodeShape.SQUARE, new RectShape());
        RENDERERS.put(MermaidNodeShape.ROUNDED, new RoundedRectShape());
        RENDERERS.put(MermaidNodeShape.STADIUM, new StadiumShape());
        RENDERERS.put(MermaidNodeShape.SUBPROCESS, new SubprocessShape());
        RENDERERS.put(MermaidNodeShape.DIAMOND, new DiamondShape());
        RENDERERS.put(MermaidNodeShape.CYLINDER, new CylinderShape());
        RENDERERS.put(MermaidNodeShape.HEXAGON, new HexagonShape());
        RENDERERS.put(MermaidNodeShape.CIRCLE, new CircleShape());
        RENDERERS.put(MermaidNodeShape.DOUBLE_CIRCLE, new DoubleCircleShape());
        RENDERERS.put(MermaidNodeShape.CLOUD, new CloudShape());
        RENDERERS.put(MermaidNodeShape.BANG, new BangShape());
        RENDERERS.put(MermaidNodeShape.ASYMMETRIC, new AsymmetricShape());
        RENDERERS.put(MermaidNodeShape.TRAPEZOID, new TrapezoidShape());
        RENDERERS.put(MermaidNodeShape.ELLIPSE, new EllipseShape());
    }

    private FlowchartShapes() {}

    public static void render(RenderContext context, LytRect rect, MermaidNodeShape shape, int backgroundColor,
        int borderColor) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        if (renderer != null) {
            renderer.render(context, rect, backgroundColor, borderColor);
        }
    }

    public static LytRect contentBounds(LytRect nodeRect, MermaidNodeShape shape, int cw, int ch, int padX, int padY) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        return renderer != null ? renderer.contentBounds(nodeRect, cw, ch, padX, padY) : nodeRect;
    }

    public static LytRect minNodeRect(MermaidNodeShape shape, int cw, int ch, int padX, int padY) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        return renderer != null ? renderer.minNodeRect(cw, ch, padX, padY)
            : new LytRect(0, 0, cw + 2 * padX, ch + 2 * padY);
    }

    public static boolean hasAccentBar(MermaidNodeShape shape) {
        return switch (shape) {
            case DEFAULT, SQUARE, ROUNDED, STADIUM, SUBPROCESS -> true;
            default -> false;
        };
    }

    public static boolean isShapeClipped(MermaidNodeShape shape) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        return renderer != null && renderer.isClipped();
    }

    public static Point edgeIntersect(LytRect nodeRect, MermaidNodeShape shape, int ex, int ey) {
        ShapeRenderer renderer = RENDERERS.get(shape);
        return renderer != null ? renderer.edgeIntersect(nodeRect, ex, ey) : intersectRect(nodeRect, ex, ey);
    }

    public static Point intersectRect(LytRect rect, int ex, int ey) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int dx = ex - cx;
        int dy = ey - cy;

        if (dx == 0 && dy == 0) return new Point(cx, cy);

        int hw = rect.width() / 2;
        int hh = rect.height() / 2;

        double sx, sy;
        if (Math.abs(dy) * (double) hw > Math.abs(dx) * (double) hh) {
            sy = dy > 0 ? rect.y() + rect.height() : rect.y();
            sx = cx + dx * (sy - cy) / (double) dy;
        } else if (dx != 0) {
            sx = dx > 0 ? rect.x() + rect.width() : rect.x();
            sy = cy + dy * (sx - cx) / (double) dx;
        } else {
            sy = dy > 0 ? rect.y() + rect.height() : rect.y();
            sx = cx;
        }

        return new Point((int) Math.round(sx), (int) Math.round(sy));
    }

    /**
     * Intersects a ray from the rect center through (ex, ey) with a circle
     * of radius min(width, height)/2 centered in the rect.
     * This matches the visual boundary of Circle and DoubleCircle shapes.
     */
    public static Point intersectCircle(LytRect rect, int ex, int ey) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = Math.min(rect.width(), rect.height()) / 2;

        int dx = ex - cx;
        int dy = ey - cy;
        if (dx == 0 && dy == 0) return new Point(cx, cy);
        if (r <= 0) return new Point(cx, cy);

        double dist = Math.sqrt((double) dx * dx + (double) dy * dy);
        double scale = r / dist;
        return new Point(cx + (int) Math.round(dx * scale), cy + (int) Math.round(dy * scale));
    }

    public static Point intersectEllipse(LytRect rect, int ex, int ey) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        double rx = rect.width() / 2.0;
        double ry = rect.height() / 2.0;
        double px = ex - cx;
        double py = ey - cy;

        if (px == 0 && py == 0) return new Point(cx, cy);

        double det = Math.sqrt(rx * rx * py * py + ry * ry * px * px);
        if (det == 0) return new Point(cx, cy);

        double ix = Math.abs(rx * ry * px / det);
        double iy = Math.abs(rx * ry * py / det);

        return new Point(cx + (int) Math.round(px > 0 ? ix : -ix), cy + (int) Math.round(py > 0 ? iy : -iy));
    }

    public static Point intersectPolygon(LytRect rect, int[][] vertices, int ex, int ey) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;

        double bestDist = Double.MAX_VALUE;
        Point best = null;

        int n = vertices.length;
        for (int i = 0; i < n; i++) {
            int[] v1 = vertices[i];
            int[] v2 = vertices[(i + 1) % n];

            Point p = lineSegmentIntersect(cx, cy, ex, ey, v1[0], v1[1], v2[0], v2[1]);
            if (p != null) {
                double pdx = p.getX() - cx;
                double pdy = p.getY() - cy;
                double dist = pdx * pdx + pdy * pdy;
                double dot = pdx * (ex - cx) + pdy * (ey - cy);
                if (dist < bestDist && dot >= 0) {
                    bestDist = dist;
                    best = p;
                }
            }
        }

        if (best != null) return best;
        return intersectRect(rect, ex, ey);
    }

    private static Point lineSegmentIntersect(int p1x, int p1y, int p2x, int p2y, int q1x, int q1y, int q2x, int q2y) {
        double denom = (double) (q2y - q1y) * (p2x - p1x) - (double) (q2x - q1x) * (p2y - p1y);
        if (Math.abs(denom) < 1e-10) return null;

        double ua = ((double) (q2x - q1x) * (p1y - q1y) - (double) (q2y - q1y) * (p1x - q1x)) / denom;
        double ub = ((double) (p2x - p1x) * (p1y - q1y) - (double) (p2y - p1y) * (p1x - q1x)) / denom;

        if (ub < 0 || ub > 1) return null;

        return new Point((int) Math.round(p1x + ua * (p2x - p1x)), (int) Math.round(p1y + ua * (p2y - p1y)));
    }

    /**
     * Intersects a ray from the center of nodeRect through (ex, ey) with a connected
     * path of SVG arcs. Each arc in arcData is {dx, dy, rx, ry, largeArc, sweep}.
     * rawBounds is {minX, maxX, minY, maxY} of the arc path in raw coordinates.
     * Returns the closest intersection in node-rect space, or rect-based fallback.
     */
    public static Point intersectArcs(LytRect nodeRect, float[] rawBounds, float[][] arcData, int ex, int ey) {
        int x = nodeRect.x(), y = nodeRect.y(), w = nodeRect.width(), h = nodeRect.height();
        float minX = rawBounds[0], maxX = rawBounds[1], minY = rawBounds[2], maxY = rawBounds[3];
        float sx = (maxX > minX) ? w / (maxX - minX) : 1;
        float sy = (maxY > minY) ? h / (maxY - minY) : 1;

        // Ray origin in raw space (center of node rect mapped to raw)
        float rawCx = (minX + maxX) / 2;
        float rawCy = (minY + maxY) / 2;

        // Ray target in raw space
        float rawEx = (ex - x) / sx + minX;
        float rawEy = (ey - y) / sy + minY;

        float rdx = rawEx - rawCx;
        float rdy = rawEy - rawCy;
        if (rdx == 0 && rdy == 0) return intersectRect(nodeRect, ex, ey);

        double bestDist = Double.MAX_VALUE;
        Point best = null;

        float curX = 0, curY = 0;
        for (float[] a : arcData) {
            float adx = a[0], ady = a[1], rx = a[2], ry = a[3];
            boolean large = a[4] > 0;
            boolean sweep = a[5] > 0;
            float endX = curX + adx;
            float endY = curY + ady;

            double[] center = svgArcCenter(curX, curY, endX, endY, rx, ry, 0, large, sweep);
            if (center == null) {
                curX = endX;
                curY = endY;
                continue;
            }
            double ecx = center[0], ecy = center[1];
            double startAngle = center[2], endAngle = center[3];

            double delta = endAngle - startAngle;
            if (sweep && delta < 0) delta += 2 * Math.PI;
            if (!sweep && delta > 0) delta -= 2 * Math.PI;

            // Ray-ellipse intersection in raw space (axis-aligned, xAxisRot=0)
            double fx = rawCx - ecx;
            double fy = rawCy - ecy;
            double rx2 = rx * rx;
            double ry2 = ry * ry;

            double A = ((double) rdx) * rdx / rx2 + ((double) rdy) * rdy / ry2;
            double B = 2.0 * (fx * rdx / rx2 + fy * rdy / ry2);
            double C = fx * fx / rx2 + fy * fy / ry2 - 1.0;

            double disc = B * B - 4.0 * A * C;
            if (disc >= 0 && A > 0) {
                double sqrtDisc = Math.sqrt(disc);
                double t1 = (-B - sqrtDisc) / (2.0 * A);
                double t2 = (-B + sqrtDisc) / (2.0 * A);
                for (double t : new double[] { t1, t2 }) {
                    if (t > 0) {
                        double px = rawCx + t * rdx;
                        double py = rawCy + t * rdy;

                        double theta = Math.atan2((py - ecy) / ry, (px - ecx) / rx);

                        if (isAngleOnArc(theta, startAngle, delta)) {
                            double dist = (px - rawCx) * (px - rawCx) + (py - rawCy) * (py - rawCy);
                            if (dist < bestDist) {
                                bestDist = dist;
                                int nx = Math.round(x + ((float) px - minX) * sx);
                                int ny = Math.round(y + ((float) py - minY) * sy);
                                best = new Point(nx, ny);
                            }
                        }
                    }
                }
            }

            curX = endX;
            curY = endY;
        }

        return best != null ? best : intersectRect(nodeRect, ex, ey);
    }

    /**
     * Computes SVG arc center and start/end angles (axis-aligned, xAxisRot=0).
     * Returns [cx, cy, startAngle, endAngle] or null for degenerate arcs.
     */
    private static double[] svgArcCenter(double x1, double y1, double x2, double y2, double rx, double ry,
        double xAxisRot, boolean largeArc, boolean sweep) {
        rx = Math.abs(rx);
        ry = Math.abs(ry);
        if (rx == 0 || ry == 0) return null;

        double phi = Math.toRadians(xAxisRot);
        double cosP = Math.cos(phi);
        double sinP = Math.sin(phi);

        double dx = (x1 - x2) / 2.0;
        double dy = (y1 - y2) / 2.0;
        double x1p = cosP * dx + sinP * dy;
        double y1p = -sinP * dx + cosP * dy;

        double rxSq = rx * rx;
        double rySq = ry * ry;
        double x1pSq = x1p * x1p;
        double y1pSq = y1p * y1p;

        double cr = x1pSq / rxSq + y1pSq / rySq;
        if (cr > 1) {
            double s = Math.sqrt(cr);
            rx *= s;
            ry *= s;
            rxSq = rx * rx;
            rySq = ry * ry;
        }

        double dq = rxSq * y1pSq + rySq * x1pSq;
        if (dq == 0) return null;

        double sq = Math.sqrt(Math.max(0, (rxSq * rySq - dq) / dq));
        if (largeArc == sweep) sq = -sq;

        double cxp = sq * rx * y1p / ry;
        double cyp = -sq * ry * x1p / rx;

        double cx = cosP * cxp - sinP * cyp + (x1 + x2) / 2.0;
        double cy = sinP * cxp + cosP * cyp + (y1 + y2) / 2.0;

        double startAngle = Math.atan2((y1p - cyp) / ry, (x1p - cxp) / rx);
        double endAngle = Math.atan2((-y1p - cyp) / ry, (-x1p - cxp) / rx);

        return new double[] { cx, cy, startAngle, endAngle };
    }

    /**
     * Checks if angle theta lies on the arc defined by startAngle and delta.
     * delta is signed: positive for sweep=true, negative for sweep=false.
     */
    private static boolean isAngleOnArc(double theta, double startAngle, double delta) {
        double diff = theta - startAngle;
        if (delta >= 0) {
            if (diff < 0) diff += 2 * Math.PI;
            return diff >= 0 && diff <= delta;
        } else {
            if (diff > 0) diff -= 2 * Math.PI;
            return diff <= 0 && diff >= delta;
        }
    }
}
