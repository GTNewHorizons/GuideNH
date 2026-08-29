package com.hfstudio.guidenh.guide.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Pure-function rasterizer for wavy (kind 4) and dotted (kind 5) text
 * decorations. Converts a decoration band rectangle (document-pixel
 * coordinates) into a list of axis-aligned fragments with per-fragment
 * coverage alpha, ready to be emitted by any draw backend.
 * <p>
 * No GL / Minecraft dependencies — callable from both the batched
 * Tessellator pipeline ({@link GuideRenderEngine#drawDecorationLine}) and the
 * legacy {@code Gui.drawRect} backends ({@link GuideRenderEngine#drawTextDecorations},
 * {@link VanillaRenderContext#drawText}). This is the single source of the
 * decoration geometry — the three call sites previously each carried their own
 * byte-identical hard-edge copy (per-doc-pixel axis-aligned rects, zero
 * anti-aliasing), which produced 3-4 layers of 2x4px stepped hard blocks for
 * the wave and 3x3 hard squares for the dots at scale=2.
 * <p>
 * Coverage model: the brush is sampled at sub-pixel resolution
 * ({@value #WAVE_SAMPLE_STEP} doc-px steps for the wave, 4x4 sub-samples per
 * pixel for the dots); each output fragment's alpha (0-255) is the fraction of
 * sub-samples whose brush covers the fragment's pixel. The wave sine is no
 * longer rounded to integer rows, so adjacent columns transition through
 * intermediate alpha levels instead of discrete hard steps. Dots are
 * rasterized as circles with soft edge falloff instead of 3x3 hard squares.
 * <p>
 * Alpha composite semantics (P4R2): the fragment's {@code alpha} is pure
 * brush <em>coverage</em> — it is NOT the decoration's final opacity. Each
 * backend must combine it with the decoration tint's own alpha byte as
 * {@code finalAlpha = round(coverage * tintAlpha / 255)} and only then replace
 * the alpha byte of the tint ARGB ({@code (finalAlpha << 24) | (tintArgb & 0xFFFFFF)}).
 * This multiplies (never replaces) the tint's opacity, so a semi-transparent
 * text color keeps its transparency while the coverage still shapes the brush
 * edge. All three call sites ({@link GuideRenderEngine#drawDecorationLine},
 * {@link GuideRenderEngine#drawTextDecorations},
 * {@link VanillaRenderContext#drawText}) implement exactly this composite.
 * When the tint is opaque (alpha 255) the composite equals the coverage
 * exactly — the headless main path is pixel-identical to the pre-composite
 * output.
 * <p>
 * Geometry is conserved from the legacy copies: the wave is a ±
 * {@value #WAVE_AMPLITUDE}px, {@value #WAVE_THICKNESS}px-thick, 8-phase sine
 * (period {@value #WAVE_PERIOD}px) whose 2px line center sits at
 * {@code bandY + 1 + sin(phase) * AMPLITUDE}; dots keep the 4px cadence
 * starting at {@code bandX + DOT_PITCH / 2} with a 3px footprint centered at
 * {@code bandY + 0.5}.
 */
public final class DecorationRasterizer {

    /** Wave period in doc px (angle advance 2π/8 = π/4 per px). */
    public static final float WAVE_PERIOD = 8f;
    /** Wave amplitude in doc px (±). */
    public static final float WAVE_AMPLITUDE = 2f;
    /** Wave line thickness in doc px. */
    public static final float WAVE_THICKNESS = 2f;
    /** Sub-pixel sampling step along the band (≤ 0.5 doc px). */
    public static final float WAVE_SAMPLE_STEP = 0.25f;
    /** Wave line half-thickness in doc px. */
    private static final float WAVE_HALF = WAVE_THICKNESS / 2f;

    /** Dot pitch (center-to-center) in doc px. */
    public static final float DOT_PITCH = 4f;
    /** Dot radius in doc px (diameter = 3px, matching the legacy 3x3 dot). */
    public static final float DOT_RADIUS = 1.5f;
    /** First dot left edge offset from the band start (legacy {@code step/2}). */
    private static final float DOT_START_OFFSET = DOT_PITCH / 2f;
    /**
     * Dot center Y relative to the band top: the legacy 3x3 square spanned
     * [bandY-1, bandY+2), so its center sat at bandY+0.5.
     */
    private static final float DOT_CENTER_Y_OFFSET = -1f + DOT_RADIUS;
    /** Per-pixel sub-sampling grid for the dot circle (edge falloff). */
    private static final int DOT_SUBSAMPLES = 4;

    /** A rasterized fragment in document-pixel coordinates (alpha 0-255). */
    public record Fragment(int x, int y, int w, int h, int alpha) {}

    private DecorationRasterizer() {}

    /**
     * Rasterize a decoration band.
     *
     * @param bandX band left edge (doc px)
     * @param bandY band top edge (doc px)
     * @param bandW band width (doc px)
     * @param kind  decoration kind (4 = wavy, 5 = dotted)
     * @return fragments ordered by (y, x); adjacent equal-alpha fragments are
     *         merged along x so a long saturated run collapses to one quad
     *         instead of one quad per doc px
     */
    public static List<Fragment> rasterize(float bandX, float bandY, float bandW, int kind) {
        if (bandW <= 0f) {
            return List.of();
        }
        return switch (kind) {
            case 4 -> rasterizeWave(bandX, bandY, bandW);
            case 5 -> rasterizeDots(bandX, bandY, bandW);
            default -> List.of();
        };
    }

    // ---- wavy ---------------------------------------------------------------

    /**
     * Wavy: per-column coverage of a continuous sine band. For each output
     * column the sine is sampled at {@code 1 / WAVE_SAMPLE_STEP} sub-pixel
     * positions; each sample's 2px-thick interval contributes its vertical
     * overlap to the covered rows. A row's alpha is the average overlap across
     * the column's sub-samples — the integer rounding of the legacy copies is
     * gone, so the 3-4 discrete vertical layers become a coverage gradient.
     */
    private static List<Fragment> rasterizeWave(float bandX, float bandY, float bandW) {
        int colStart = (int) Math.floor(bandX);
        int colEnd = (int) Math.ceil(bandX + bandW);
        if (colEnd <= colStart) {
            return List.of();
        }
        int cols = colEnd - colStart;

        float angleStep = (float) (Math.PI * 2.0 / WAVE_PERIOD);
        // Interval [bandY - AMPLITUDE, bandY + THICKNESS + AMPLITUDE] bounds
        // every sine sample: center = bandY + HALF + sin*AMPLITUDE, ± HALF.
        int rowStart = (int) Math.floor(bandY - WAVE_AMPLITUDE);
        int rowEnd = (int) Math.ceil(bandY + WAVE_THICKNESS + WAVE_AMPLITUDE);
        int rows = rowEnd - rowStart;
        if (rows <= 0) {
            return List.of();
        }

        int samples = Math.max(2, Math.round(1f / WAVE_SAMPLE_STEP));
        float[] colCoverage = new float[rows];
        List<Fragment> frags = new ArrayList<>(cols * 3);
        for (int c = 0; c < cols; c++) {
            Arrays.fill(colCoverage, 0f);
            for (int k = 0; k < samples; k++) {
                float xSub = colStart + c + (k + 0.5f) / samples;
                float phase = (xSub - bandX) * angleStep;
                float center = bandY + WAVE_HALF + (float) Math.sin(phase) * WAVE_AMPLITUDE;
                float lo = center - WAVE_HALF;
                float hi = center + WAVE_HALF;
                int r0 = Math.max(rowStart, (int) Math.floor(lo));
                int r1 = Math.min(rowEnd, (int) Math.ceil(hi));
                for (int r = r0; r < r1; r++) {
                    float rowLo = r;
                    float overlap = Math.min(hi, rowLo + 1f) - Math.max(lo, rowLo);
                    if (overlap > 0f) {
                        colCoverage[r - rowStart] += overlap;
                    }
                }
            }
            for (int r = 0; r < rows; r++) {
                int alpha = Math.round(colCoverage[r] / samples * 255f);
                if (alpha <= 0) {
                    continue;
                }
                frags.add(new Fragment(colStart + c, rowStart + r, 1, 1, alpha));
            }
        }
        return mergeRuns(frags);
    }

    // ---- dotted -------------------------------------------------------------

    /**
     * Dotted: circular dots with soft edge falloff. Each dot keeps the legacy
     * 4px cadence ({@code dotLeft = bandX + DOT_PITCH/2; dotLeft + dotSize <= bandX + bandW})
     * and 3px footprint, but the 3x3 hard square is replaced by a circle of
     * radius {@value #DOT_RADIUS}px rasterized with a 4x4 sub-pixel grid — the
     * corner pixels get partial coverage, so the dot reads round with a fading
     * edge instead of a hard block.
     */
    private static List<Fragment> rasterizeDots(float bandX, float bandY, float bandW) {
        float dotSize = DOT_RADIUS * 2f;
        float dotCy = bandY + DOT_CENTER_Y_OFFSET;
        float r2 = DOT_RADIUS * DOT_RADIUS;
        List<Fragment> frags = new ArrayList<>();
        for (float dotLeft = bandX + DOT_START_OFFSET; dotLeft + dotSize <= bandX + bandW; dotLeft += DOT_PITCH) {
            float dotCx = dotLeft + DOT_RADIUS;
            int px0 = (int) Math.floor(dotCx - DOT_RADIUS);
            int px1 = (int) Math.ceil(dotCx + DOT_RADIUS);
            int py0 = (int) Math.floor(dotCy - DOT_RADIUS);
            int py1 = (int) Math.ceil(dotCy + DOT_RADIUS);
            for (int py = py0; py < py1; py++) {
                for (int px = px0; px < px1; px++) {
                    float cov = 0f;
                    for (int i = 0; i < DOT_SUBSAMPLES; i++) {
                        float sx = px + (i + 0.5f) / DOT_SUBSAMPLES;
                        for (int j = 0; j < DOT_SUBSAMPLES; j++) {
                            float sy = py + (j + 0.5f) / DOT_SUBSAMPLES;
                            float dx = sx - dotCx;
                            float dy = sy - dotCy;
                            if (dx * dx + dy * dy <= r2) {
                                cov += 1f;
                            }
                        }
                    }
                    int alpha = Math.round(cov / (DOT_SUBSAMPLES * DOT_SUBSAMPLES) * 255f);
                    if (alpha <= 0) {
                        continue;
                    }
                    frags.add(new Fragment(px, py, 1, 1, alpha));
                }
            }
        }
        return mergeRuns(frags);
    }

    // ---- merging ------------------------------------------------------------

    /**
     * Merge adjacent (x-contiguous) fragments that share the same row and
     * alpha into a single wider fragment, bounding the emitted vertex count.
     * Without this, a saturated middle row of a long wave band would emit one
     * quad per doc px; with it, an N-px same-alpha run collapses to one quad.
     */
    private static List<Fragment> mergeRuns(List<Fragment> frags) {
        if (frags.size() < 2) {
            return frags;
        }
        frags.sort(
            Comparator.comparingInt((Fragment a) -> a.y)
                .thenComparingInt(a -> a.x));
        List<Fragment> out = new ArrayList<>(frags.size());
        Fragment cur = null;
        for (Fragment f : frags) {
            if (cur != null && cur.y == f.y && cur.alpha == f.alpha && cur.x + cur.w == f.x) {
                cur = new Fragment(cur.x, cur.y, cur.w + f.w, cur.h, cur.alpha);
            } else {
                if (cur != null) {
                    out.add(cur);
                }
                cur = f;
            }
        }
        if (cur != null) {
            out.add(cur);
        }
        return out;
    }
}
