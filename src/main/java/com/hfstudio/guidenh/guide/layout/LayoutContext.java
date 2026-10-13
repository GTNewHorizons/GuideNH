package com.hfstudio.guidenh.guide.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

import lombok.Getter;

public class LayoutContext implements FontMetrics {

    private final FontMetrics fontMetrics;
    @Getter
    private float visualScale = 1.0f;

    private final List<LytRect> leftFloats = new ArrayList<>();
    private final List<LytRect> rightFloats = new ArrayList<>();

    private int cachedLeftFloatRightEdge = Integer.MIN_VALUE;
    private int cachedRightFloatLeftEdge = Integer.MAX_VALUE;

    public LayoutContext(FontMetrics fontMetrics) {
        this.fontMetrics = fontMetrics;
    }

    public LayoutContext withVisualScale(float visualScale) {
        this.visualScale = Math.clamp(visualScale, 0.1f, 1.0f);
        return this;
    }

    public LayoutContext resetTransientState() {
        visualScale = 1.0f;
        leftFloats.clear();
        rightFloats.clear();
        cachedLeftFloatRightEdge = Integer.MIN_VALUE;
        cachedRightFloatLeftEdge = Integer.MAX_VALUE;
        return this;
    }

    public void addLeftFloat(LytRect bounds) {
        leftFloats.add(bounds);
        cachedLeftFloatRightEdge = Integer.MIN_VALUE;
    }

    public void addRightFloat(LytRect bounds) {
        rightFloats.add(bounds);
        cachedRightFloatLeftEdge = Integer.MAX_VALUE;
    }

    public OptionalInt getLeftFloatRightEdge() {
        if (leftFloats.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getLeftFloatRightEdgeOr(0));
    }

    public int getLeftFloatRightEdgeOr(int fallback) {
        if (leftFloats.isEmpty()) {
            return fallback;
        }
        if (cachedLeftFloatRightEdge == Integer.MIN_VALUE) {
            int maxRight = Integer.MIN_VALUE + 1;
            for (var f : leftFloats) {
                int r = f.right();
                if (r > maxRight) maxRight = r;
            }
            cachedLeftFloatRightEdge = maxRight;
        }
        return cachedLeftFloatRightEdge;
    }

    public int getLeftFloatRightEdgeOr(int fallback, int regionLeft, int regionRight) {
        if (regionRight <= regionLeft || leftFloats.isEmpty()) {
            return fallback;
        }
        int edge = fallback;
        for (var floatBounds : leftFloats) {
            if (floatBounds.right() > regionLeft && floatBounds.x() < regionRight) {
                edge = Math.max(edge, floatBounds.right());
            }
        }
        return edge;
    }

    public OptionalInt getRightFloatLeftEdge() {
        if (rightFloats.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getRightFloatLeftEdgeOr(0));
    }

    public int getRightFloatLeftEdgeOr(int fallback) {
        if (rightFloats.isEmpty()) {
            return fallback;
        }
        if (cachedRightFloatLeftEdge == Integer.MAX_VALUE) {
            int minLeft = Integer.MAX_VALUE - 1;
            for (var f : rightFloats) {
                int x = f.x();
                if (x < minLeft) minLeft = x;
            }
            cachedRightFloatLeftEdge = minLeft;
        }
        return cachedRightFloatLeftEdge;
    }

    public int getRightFloatLeftEdgeOr(int fallback, int regionLeft, int regionRight) {
        if (regionRight <= regionLeft || rightFloats.isEmpty()) {
            return fallback;
        }
        int edge = fallback;
        for (var floatBounds : rightFloats) {
            if (floatBounds.right() > regionLeft && floatBounds.x() < regionRight) {
                edge = Math.min(edge, floatBounds.x());
            }
        }
        return edge;
    }

    public OptionalInt clearFloats(boolean left, boolean right) {
        if (left && right) {
            var result = getMaxBottom(leftFloats, rightFloats);
            leftFloats.clear();
            rightFloats.clear();
            cachedLeftFloatRightEdge = Integer.MIN_VALUE;
            cachedRightFloatLeftEdge = Integer.MAX_VALUE;
            return result;
        } else if (left) {
            var result = getMaxBottom(leftFloats);
            leftFloats.clear();
            cachedLeftFloatRightEdge = Integer.MIN_VALUE;
            return result;
        } else if (right) {
            var result = getMaxBottom(rightFloats);
            rightFloats.clear();
            cachedRightFloatLeftEdge = Integer.MAX_VALUE;
            return result;
        } else {
            return OptionalInt.empty();
        }
    }

    public void clearFloatsAbove(int y) {
        boolean leftChanged = leftFloats.removeIf(f -> f.bottom() <= y);
        boolean rightChanged = rightFloats.removeIf(f -> f.bottom() <= y);
        if (leftChanged) cachedLeftFloatRightEdge = Integer.MIN_VALUE;
        if (rightChanged) cachedRightFloatLeftEdge = Integer.MAX_VALUE;
    }

    @Override
    public float getAdvance(int codePoint, ResolvedTextStyle style) {
        return fontMetrics.getAdvance(codePoint, style);
    }

    @Override
    public float getRenderedAdvance(int codePoint, ResolvedTextStyle style, boolean hasVisibleGlyphBefore) {
        return fontMetrics.getRenderedAdvance(codePoint, style, hasVisibleGlyphBefore);
    }

    @Override
    public int getLineHeight(ResolvedTextStyle style) {
        return fontMetrics.getLineHeight(style);
    }

    @Override
    public int getAscent(ResolvedTextStyle style) {
        return fontMetrics.getAscent(style);
    }

    @Override
    public int getDescent(ResolvedTextStyle style) {
        return fontMetrics.getDescent(style);
    }

    public OptionalInt getNextFloatBottomEdge(int y) {
        int nextBottom = Integer.MAX_VALUE;
        boolean found = false;

        for (var bounds : leftFloats) {
            var bottom = bounds.bottom();
            if (bottom > y && bottom < nextBottom) {
                nextBottom = bottom;
                found = true;
            }
        }

        for (var bounds : rightFloats) {
            var bottom = bounds.bottom();
            if (bottom > y && bottom < nextBottom) {
                nextBottom = bottom;
                found = true;
            }
        }

        return found ? OptionalInt.of(nextBottom) : OptionalInt.empty();
    }

    public OptionalInt getNextFloatBottomEdge(int y, int regionLeft, int regionRight) {
        if (regionRight <= regionLeft) {
            return OptionalInt.empty();
        }

        int nextBottom = Integer.MAX_VALUE;
        boolean found = false;
        for (var bounds : leftFloats) {
            if (bounds.right() <= regionLeft || bounds.x() >= regionRight) {
                continue;
            }
            int bottom = bounds.bottom();
            if (bottom > y && bottom < nextBottom) {
                nextBottom = bottom;
                found = true;
            }
        }
        for (var bounds : rightFloats) {
            if (bounds.right() <= regionLeft || bounds.x() >= regionRight) {
                continue;
            }
            int bottom = bounds.bottom();
            if (bottom > y && bottom < nextBottom) {
                nextBottom = bottom;
                found = true;
            }
        }
        return found ? OptionalInt.of(nextBottom) : OptionalInt.empty();
    }

    public static OptionalInt getMaxBottom(List<LytRect> boundsList) {
        int maxBottom = Integer.MIN_VALUE;
        boolean found = false;

        for (var bounds : boundsList) {
            maxBottom = Math.max(maxBottom, bounds.bottom());
            found = true;
        }

        return found ? OptionalInt.of(maxBottom) : OptionalInt.empty();
    }

    public static OptionalInt getMaxBottom(List<LytRect> leftBounds, List<LytRect> rightBounds) {
        int maxBottom = Integer.MIN_VALUE;
        boolean found = false;

        for (var bounds : leftBounds) {
            maxBottom = Math.max(maxBottom, bounds.bottom());
            found = true;
        }

        for (var bounds : rightBounds) {
            maxBottom = Math.max(maxBottom, bounds.bottom());
            found = true;
        }

        return found ? OptionalInt.of(maxBottom) : OptionalInt.empty();
    }
}
