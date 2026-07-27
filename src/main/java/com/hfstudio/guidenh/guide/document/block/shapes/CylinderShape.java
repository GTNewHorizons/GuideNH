package com.hfstudio.guidenh.guide.document.block.shapes;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;

public class CylinderShape implements ShapeRenderer {

    @Override
    public void emitPrimitives(PrimitiveCollector c, LytRect rect, int backgroundColor, int borderColor) {
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

        ShapeUtils.emitPolygonCentered(c, oxs, oys, borderColor);
        ShapeUtils.emitPolygonCentered(c, ixs, iys, backgroundColor);

        // Emit the 20 line segments for the ellipse front arc
        int arcSegments = 20;
        for (int i = 0; i < arcSegments; i++) {
            double a1 = Math.PI * i / arcSegments;
            double a2 = Math.PI * (i + 1) / arcSegments;
            float x1 = cx + (float) (Math.cos(a1) * rx);
            float y1 = bodyTop + (float) (Math.sin(a1) * ry);
            float x2 = cx + (float) (Math.cos(a2) * rx);
            float y2 = bodyTop + (float) (Math.sin(a2) * ry);
            c.emit(new GuideRenderPrimitive.DrawLine(x1, y1, x2, y2, 1, borderColor));
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
    public LytRect minNodeRect(int cw, int ch, int padX, int padY) {
        int pw = cw + 2 * padX;
        int ph = ch + 2 * padY;
        int estRy = Math.max(1, pw / 6);
        int estExtra = Math.max(2, estRy / 3);
        int minH = 3 * estRy + ch + 2 * padY + 2 * estExtra;
        return new LytRect(0, 0, pw, Math.max(ph * 2, minH));
    }
}
