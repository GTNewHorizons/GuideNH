package com.hfstudio.guidenh.guide.latex;

public final class GuideLatexRasterMath {

    static final float MAX_PIXEL_SCALE = 16f;

    private GuideLatexRasterMath() {}

    /**
     * Rounds the physical-pixels-per-GUI-unit factor <em>up</em> to a coarse step so that zoom animations do not
     * re-rasterize every frame. Rounding up means the texture is never magnified; the slight minification that
     * remains is handled by the mip chain.
     */
    public static float quantizePixelScale(float scale) {
        if (!(scale > 1f)) {
            // Never rasterize below 1 texel per GUI unit; smaller draws are minified through the mip chain.
            return 1f;
        }
        float step = scale <= 4f ? 0.5f : 1f;
        return Math.min(MAX_PIXEL_SCALE, (float) Math.ceil(scale / step - 1.0e-4f) * step);
    }

    /**
     * Area-averages premultiplied ARGB pixels from {@code srcW x srcH} down to {@code dstW x dstH} (about half
     * of each source dimension). Odd trailing rows/columns are folded into the last destination texel so no
     * source coverage is lost.
     */
    public static int[] downsample(int[] src, int srcW, int srcH, int dstW, int dstH) {
        int[] dst = new int[dstW * dstH];
        for (int dy = 0; dy < dstH; dy++) {
            int sy0 = dy * srcH / dstH;
            int sy1 = Math.max(sy0 + 1, (dy + 1) * srcH / dstH);
            for (int dx = 0; dx < dstW; dx++) {
                int sx0 = dx * srcW / dstW;
                int sx1 = Math.max(sx0 + 1, (dx + 1) * srcW / dstW);
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;
                int n = 0;
                for (int sy = sy0; sy < sy1; sy++) {
                    int row = sy * srcW;
                    for (int sx = sx0; sx < sx1; sx++) {
                        int p = src[row + sx];
                        a += (p >>> 24) & 0xFF;
                        r += (p >> 16) & 0xFF;
                        g += (p >> 8) & 0xFF;
                        b += p & 0xFF;
                        n++;
                    }
                }
                int half = n >> 1;
                dst[dy * dstW + dx] = ((a + half) / n) << 24 | ((r + half) / n) << 16
                    | ((g + half) / n) << 8
                    | ((b + half) / n);
            }
        }
        return dst;
    }
}
