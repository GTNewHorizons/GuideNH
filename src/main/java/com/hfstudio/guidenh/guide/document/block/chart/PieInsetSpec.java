package com.hfstudio.guidenh.guide.document.block.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hfstudio.guidenh.guide.color.ColorUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for a small pie chart rendered inside another chart's plot area.
 * Used by {@link LytColumnChart} / {@link LytBarChart} to overlay summary pies in a corner.
 */
@Getter
public class PieInsetSpec {

    public enum Position {
        TOP_RIGHT,
        TOP_LEFT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        /**
         * Special placement: the host chart reserves an extra column on its right side and the pie
         * occupies that whole column instead of overlapping the plot area.
         */
        RIGHT_OUTSIDE
    }

    private final List<PieSlice> slices = new ArrayList<>();
    private int size = 60;
    private Position position = Position.TOP_RIGHT;
    @Setter
    private float startAngleDeg = -90f;
    @Setter
    private boolean clockwise = true;
    private String title = "";
    @Setter
    private int titleColor = ColorUtils.CHART_TITLE.getColor();

    public void setSlices(List<PieSlice> newSlices) {
        slices.clear();
        if (newSlices != null) slices.addAll(newSlices);
    }

    public void setSize(int size) {
        if (size > 16) this.size = size;
    }

    public void setPosition(Position position) {
        if (position != null) this.position = position;
    }

    public void setTitle(String title) {
        this.title = title != null ? title : "";
    }

    public static Position parsePosition(String raw, Position fallback) {
        if (raw == null) return fallback;
        return switch (raw.trim()
            .toLowerCase(Locale.ROOT)) {
            case "topright", "top-right", "tr" -> Position.TOP_RIGHT;
            case "topleft", "top-left", "tl" -> Position.TOP_LEFT;
            case "bottomright", "bottom-right", "br" -> Position.BOTTOM_RIGHT;
            case "bottomleft", "bottom-left", "bl" -> Position.BOTTOM_LEFT;
            case "right", "rightoutside", "right-outside", "outside", "side" -> Position.RIGHT_OUTSIDE;
            default -> fallback;
        };
    }
}
