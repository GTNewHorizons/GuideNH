package com.hfstudio.guidenh.guide.document.block;

import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.render.GuideRenderPrimitive;
import com.hfstudio.guidenh.guide.render.PrimitiveCollector;
import com.hfstudio.guidenh.guide.render.RenderContext;

import lombok.Getter;

public class LytHeading extends LytParagraph {

    @Getter
    private int depth = 1;

    public LytHeading() {
        setMarginTop(5);
        setMarginBottom(5);
    }

    /**
     * Per-depth vertical margins: the space before a heading grows with its
     * level (H1 20 / H2 18 / H3 14 / H4 12 / H5-H6 10) while the space after
     * stays small (H1-H2 8 / H3 6 / H4+ 5). The strong top/bottom ratio makes
     * a heading "breathe" above while binding it to its own content below
     * (taffy adds margins, no collapsing), so parent-child and sibling heading
     * gaps stay distinguishable. Index 0 is the depth-agnostic fallback used
     * when no valid depth is assigned.
     */
    private static final int[] HEADING_MARGIN_TOP = { 5, 20, 18, 14, 12, 10, 10 };
    private static final int[] HEADING_MARGIN_BOTTOM = { 5, 8, 8, 6, 5, 5, 5 };

    public void setDepth(int depth) {
        this.depth = depth;
        var style = switch (depth) {
            case 1 -> DefaultStyles.HEADING1;
            case 2 -> DefaultStyles.HEADING2;
            case 3 -> DefaultStyles.HEADING3;
            case 4 -> DefaultStyles.HEADING4;
            case 5 -> DefaultStyles.HEADING5;
            case 6 -> DefaultStyles.HEADING6;
            default -> DefaultStyles.BODY_TEXT;
        };
        setStyle(style);
        int idx = (depth >= 1 && depth <= 6) ? depth : 0;
        setMarginTop(HEADING_MARGIN_TOP[idx]);
        setMarginBottom(HEADING_MARGIN_BOTTOM[idx]);
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);

        // Separators stay reserved for the top two levels: the monotonic size
        // ladder + bold white now distinguish H3-H6 from body text without a
        // rule line (judgment per P2 typography pass — H3 gets no fainter line).
        if (depth == 1) {
            emitSeparator(c, SymbolicColor.HEADER1_SEPARATOR.resolve(LightDarkMode.current()));
        } else if (depth == 2) {
            emitSeparator(c, SymbolicColor.HEADER2_SEPARATOR.resolve(LightDarkMode.current()));
        }
    }

    /**
     * Fixed gap between the lowest glyph bottom and the separator line, so the
     * rule never grazes descender tails (g/p/y) regardless of how far they
     * hang below the baseline.
     */
    private static final int HEADING_SEPARATOR_GAP = 5;

    private void emitSeparator(PrimitiveCollector c, int argb) {
        int sepY = separatorY();
        int[] ext = separatorExtent();
        c.emit(new GuideRenderPrimitive.FillRect(ext[0], sepY, ext[1], 1, argb));
    }

    @Override
    public void render(RenderContext context) {
        super.render(context);

        if (depth == 1) {
            emitSeparatorLegacy(context, SymbolicColor.HEADER1_SEPARATOR);
        } else if (depth == 2) {
            emitSeparatorLegacy(context, SymbolicColor.HEADER2_SEPARATOR);
        }
    }

    private void emitSeparatorLegacy(RenderContext context, SymbolicColor color) {
        int sepY = separatorY();
        int[] ext = separatorExtent();
        context.fillRect(ext[0], sepY, ext[1], 1, color);
    }

    /**
     * The separator's vertical position: a fixed gap below the actual lowest
     * glyph bottom (baseline + real descender extent), decoupled from the
     * block bounds bottom whose distance to the text depends on the font's
     * ascent/descent allocation. Falls back to the block bottom when no glyph
     * run is available (legacy/no-Rust path).
     */
    private int separatorY() {
        var data = getGlyphData();
        if (data != null) {
            float maxBottom = Float.NEGATIVE_INFINITY;
            boolean found = false;
            for (var run : data.runs()) {
                for (var g : run.glyphs()) {
                    maxBottom = Math.max(maxBottom, g.y() + g.h());
                    found = true;
                }
            }
            if (found) {
                return Math.round(maxBottom) + HEADING_SEPARATOR_GAP;
            }
        }
        return getBounds().bottom() - 1;
    }

    /**
     * Returns {@code [x, width]} for the separator, computed from the Rust-
     * emitted kind=3 DecorationRect (the full float-compressed line window).
     * Falls back to the block bounds when no separator rect is available
     * (legacy/no-Rust path).
     */
    private int[] separatorExtent() {
        var bounds = getBounds();
        var data = getGlyphData();
        if (data != null && !data.separators()
            .isEmpty()) {
            var r = data.separators()
                .get(0);
            return new int[] { r.x(), Math.max(0, r.w()) };
        }
        return new int[] { bounds.x(), bounds.width() };
    }
}
