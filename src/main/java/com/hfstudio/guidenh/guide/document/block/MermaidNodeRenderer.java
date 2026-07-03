package com.hfstudio.guidenh.guide.document.block;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public final class MermaidNodeRenderer {

    private MermaidNodeRenderer() {}

    public static ResolvedTextStyle scaleTextStyle(ResolvedTextStyle base, float zoom) {
        return new ResolvedTextStyle(
            base.fontScale() * zoom,
            base.bold(),
            base.italic(),
            base.underlined(),
            base.wavyUnderline(),
            base.dottedUnderline(),
            base.strikethrough(),
            base.obfuscated(),
            base.font(),
            base.color(),
            base.whiteSpace(),
            base.alignment(),
            base.dropShadow(),
            base.backgroundColor(),
            base.inlineCode());
    }

    public static ResolvedTextStyle getOrScaleStyle(Map<ResolvedTextStyle, ResolvedTextStyle> cache,
        ResolvedTextStyle base, float zoom) {
        return cache.computeIfAbsent(base, key -> scaleTextStyle(key, zoom));
    }

    public static void renderNode(RenderContext context, LytRect rect, MermaidNodeShape shape,
        int backgroundColor, int borderColor) {
        switch (shape) {
            case ROUNDED:
                renderRoundedRect(context, rect, backgroundColor, borderColor);
                break;
            case STADIUM:
                renderStadium(context, rect, backgroundColor, borderColor);
                break;
            case DIAMOND:
                renderDiamond(context, rect, backgroundColor, borderColor);
                break;
            case CYLINDER:
                renderCylinder(context, rect, backgroundColor, borderColor);
                break;
            case HEXAGON:
                renderHexagon(context, rect, backgroundColor, borderColor);
                break;
            case CIRCLE:
            case DOUBLE_CIRCLE:
                renderCircle(context, rect, backgroundColor, borderColor);
                break;
            case CLOUD:
                renderCloud(context, rect, backgroundColor, borderColor);
                break;
            case BANG:
                renderBang(context, rect, backgroundColor, borderColor);
                break;
            default:
                renderRect(context, rect, backgroundColor, borderColor);
                break;
        }
    }

    public static void renderRect(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        context.fillRect(rect, backgroundColor);
        context.drawBorder(rect, borderColor, 1);
    }

    public static void renderRoundedRect(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int r = Math.clamp(rect.width() / 6, 1, 8);
        context.fillRect(rect, backgroundColor);
        context.drawBorder(rect, borderColor, 1);
        if (r > 1 && rect.width() > r * 2 && rect.height() > r * 2) {
            context.fillCircle(rect.x() + r, rect.y() + r, r, backgroundColor);
            context.fillCircle(rect.right() - r, rect.y() + r, r, backgroundColor);
            context.fillCircle(rect.x() + r, rect.bottom() - r, r, backgroundColor);
            context.fillCircle(rect.right() - r, rect.bottom() - r, r, backgroundColor);
        }
    }

    public static void renderStadium(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        context.fillRect(rect, backgroundColor);
        context.drawBorder(rect, borderColor, 1);
        int r = Math.min(rect.width(), rect.height()) / 2;
        if (r > 1) {
            context.fillCircle(rect.x() + r, rect.y() + (float) rect.height() / 2, r, backgroundColor);
            context.fillCircle(rect.right() - r, rect.y() + (float) rect.height() / 2, r, backgroundColor);
        }
    }

    public static void renderDiamond(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        context.fillPolygon(
            new float[]{cx, rect.right(), cx, rect.x()},
            new float[]{rect.y(), cy, rect.bottom(), cy},
            backgroundColor);
        context.drawLine(cx, rect.y(), rect.right(), cy, 1, borderColor);
        context.drawLine(rect.right(), cy, cx, rect.bottom(), 1, borderColor);
        context.drawLine(cx, rect.bottom(), rect.x(), cy, 1, borderColor);
        context.drawLine(rect.x(), cy, cx, rect.y(), 1, borderColor);
    }

    public static void renderCircle(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = Math.min(rect.width(), rect.height()) / 2;
        if (r > 0) {
            context.fillCircle(cx, cy, r, backgroundColor);
            context.drawCircleOutline(cx, cy, r, 1, borderColor);
        }
    }

    public static void renderCylinder(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int ellipseR = Math.min(rect.width(), rect.height()) / 4;
        context.fillRect(new LytRect(rect.x(), rect.y() + ellipseR, rect.width(), rect.height() - ellipseR * 2),
            backgroundColor);
        context.fillCircle(cx, rect.y() + ellipseR, ellipseR, backgroundColor);
        context.fillCircle(cx, rect.bottom() - ellipseR, ellipseR, backgroundColor);
        context.drawCircleOutline(cx, rect.y() + ellipseR, ellipseR, 1, borderColor);
        context.drawLine(rect.x(), rect.y() + ellipseR, rect.x(), rect.bottom() - ellipseR, 1, borderColor);
        context.drawLine(rect.right(), rect.y() + ellipseR, rect.right(), rect.bottom() - ellipseR, 1, borderColor);
        context.drawCircleOutline(cx, rect.bottom() - ellipseR, ellipseR, 1, borderColor);
    }

    public static void renderHexagon(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int w2 = rect.width() / 2;
        float inset = rect.width() * 0.25f;
        context.fillPolygon(
            new float[]{rect.x() + inset, rect.right() - inset, rect.right(), rect.right() - inset, rect.x() + inset, rect.x()},
            new float[]{rect.y(), rect.y(), cy, rect.bottom(), rect.bottom(), cy},
            backgroundColor);
        context.drawLine((int)(rect.x() + inset), rect.y(), (int)(rect.right() - inset), rect.y(), 1, borderColor);
        context.drawLine((int)(rect.right() - inset), rect.y(), rect.right(), cy, 1, borderColor);
        context.drawLine(rect.right(), cy, (int)(rect.right() - inset), rect.bottom(), 1, borderColor);
        context.drawLine((int)(rect.right() - inset), rect.bottom(), (int)(rect.x() + inset), rect.bottom(), 1, borderColor);
        context.drawLine((int)(rect.x() + inset), rect.bottom(), rect.x(), cy, 1, borderColor);
        context.drawLine(rect.x(), cy, (int)(rect.x() + inset), rect.y(), 1, borderColor);
    }

    public static void renderCloud(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + rect.height() / 2;
        int r = Math.min(rect.width(), rect.height()) / 3;
        context.fillCircle(cx, cy, r, backgroundColor);
        context.drawCircleOutline(cx, cy, r, 1, borderColor);
    }

    public static void renderBang(RenderContext context, LytRect rect, int backgroundColor, int borderColor) {
        context.fillRect(rect, backgroundColor);
        context.drawBorder(rect, borderColor, 2);
    }


}
