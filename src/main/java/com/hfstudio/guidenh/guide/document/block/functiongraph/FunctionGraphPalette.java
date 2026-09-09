package com.hfstudio.guidenh.guide.document.block.functiongraph;

import com.hfstudio.guidenh.guide.color.ColorUtils;

/**
 * Deterministic palette used when {@link FunctionPlot} authors omit an explicit colour. Colours are
 * picked by plot index so re-rendering the same page does not flicker.
 */
public class FunctionGraphPalette {

    protected FunctionGraphPalette() {}

    public static int color(int index) {
        int n = ColorUtils.FUNCTION_GRAPH_PALETTE.length;
        int i = ((index % n) + n) % n;
        return ColorUtils.getColor(ColorUtils.FUNCTION_GRAPH_PALETTE[i]);
    }
}
