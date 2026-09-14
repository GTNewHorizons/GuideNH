package com.hfstudio.guidenh.guide.document.block.chart;

public record CornerLegendEntry(String name, int color, boolean lineMarker) {

    public boolean isVisible() {
        return name != null && !name.isEmpty();
    }
}
