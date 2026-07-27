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
    }

    @Override
    public void computePrimitives(PrimitiveCollector c) {
        super.computePrimitives(c);

        if (depth == 1) {
            emitSeparator(c, SymbolicColor.HEADER1_SEPARATOR.resolve(LightDarkMode.current()));
        } else if (depth == 2) {
            emitSeparator(c, SymbolicColor.HEADER2_SEPARATOR.resolve(LightDarkMode.current()));
        }
    }

    private void emitSeparator(PrimitiveCollector c, int argb) {
        var bounds = getBounds();
        int sepY = bounds.bottom() - 1;
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
        var bounds = getBounds();
        int sepY = bounds.bottom() - 1;
        int[] ext = separatorExtent();
        context.fillRect(ext[0], sepY, ext[1], 1, color);
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
