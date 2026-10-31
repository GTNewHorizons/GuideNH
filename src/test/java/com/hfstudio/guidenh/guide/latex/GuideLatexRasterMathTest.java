package com.hfstudio.guidenh.guide.latex;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuideLatexRasterMathTest {

    @Test
    void pixelScaleIsExactForIntegerGuiScales() {
        assertEquals(1f, GuideLatexRasterMath.quantizePixelScale(1f));
        assertEquals(2f, GuideLatexRasterMath.quantizePixelScale(2f));
        assertEquals(3f, GuideLatexRasterMath.quantizePixelScale(3f));
        assertEquals(4f, GuideLatexRasterMath.quantizePixelScale(4f));
    }

    @Test
    void pixelScaleRoundsUpSoTexturesAreNeverMagnified() {
        assertEquals(1f, GuideLatexRasterMath.quantizePixelScale(0.5f));
        assertEquals(2.5f, GuideLatexRasterMath.quantizePixelScale(2.1f));
        assertEquals(6f, GuideLatexRasterMath.quantizePixelScale(5.2f));
        assertEquals(16f, GuideLatexRasterMath.quantizePixelScale(100f));
    }

    @Test
    void downsampleKeepsCoverageOfOnePixelHorizontalLine() {
        // 4x4 transparent image with a single opaque white row (a thin fraction bar).
        int[] src = new int[16];
        for (int x = 0; x < 4; x++) {
            src[4 + x] = 0xFFFFFFFF;
        }
        int[] dst = GuideLatexRasterMath.downsample(src, 4, 4, 2, 2);
        // The bar must survive as half coverage instead of disappearing.
        assertArrayEquals(new int[] { 0x80808080, 0x80808080, 0, 0 }, dst);
    }

    @Test
    void downsampleFoldsOddTrailingRowIntoLastTexel() {
        int[] src = new int[3 * 3];
        src[8] = 0xFFFFFFFF; // bottom-right corner only
        int[] dst = GuideLatexRasterMath.downsample(src, 3, 3, 1, 1);
        assertTrue((dst[0] >>> 24) > 0, "odd trailing texel must contribute to the mip level");
    }
}
