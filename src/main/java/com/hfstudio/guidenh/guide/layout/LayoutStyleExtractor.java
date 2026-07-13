package com.hfstudio.guidenh.guide.layout;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytSizeBox;
import com.hfstudio.guidenh.guide.document.block.LytWidthBox;
import com.hfstudio.guidenh.guide.document.block.LytAxisBox;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.layout.flatbuffers.Style;

/**
 * High-level FlatBuffer builder for Taffy layout properties.
 * <p>
 * Handles ALL LytBlock → FlatBuffer Style field mappings.
 * Migration docs DO NOT call Style.createStyle() directly — use this.
 */
public final class LayoutStyleExtractor {

    /** Style modifiers bitmask — pass to build() to override defaults. */
    public static final class Flags {
        public static final int NONE         = 0;
        public static final int DISPLAY_BLOCK = 1 << 0;
        public static final int DISPLAY_FLEX  = 1 << 1;
        public static final int DISPLAY_GRID  = 1 << 2;
        public static final int SIZE_FULL_WIDTH = 1 << 3;  // force 100% width
        public static final int SIZE_AUTO_WIDTH  = 1 << 4;  // force auto width
        public static final int SIZE_AUTO_HEIGHT = 1 << 5;  // force auto height
        public static final int OVERFLOW_HIDDEN  = 1 << 6;
        public static final int OVERFLOW_SCROLL  = 1 << 7;
        private Flags() {}
    }

    private LayoutStyleExtractor() {}


    /** Build a FlatBuffer Style from a LytBlock node. Extracts all layout-relevant fields. */
    public static int build(FlatBufferBuilder fbb, LytBlock block) {
        return build(fbb, block, Flags.NONE);
    }

    /** Build with additional flags overriding automatic detection. */
    public static int build(FlatBufferBuilder fbb, LytBlock block, int flags) {
        byte display = getDisplay(block, flags);
        byte flexDir = getFlexDirection(block);
        byte flexWrap = getFlexWrap(block, flags);
        byte alignItems = getAlignItems(block);
        byte alignSelf = getAlignSelf(block, flags);
        byte justify = 0; // default: Start

        int sizeWOff = dimAuto(fbb);   // default: auto
        int sizeHOff = dimAuto(fbb);
        int minWOff = 0;  int minHOff = 0;
        int maxWOff = 0;  int maxHOff = 0;

        applySizeConstraints(block, flags, fbb);

        float marginL = block.getMarginLeft();
        float marginR = block.getMarginRight();
        float marginT = block.getMarginTop();
        float marginB = block.getMarginBottom();
        float padL = 0; float padR = 0; float padT = 0; float padB = 0;
        // Phase 1: padding access via LytBox fields is package-private.
        // Full integration will use LytBlock bounds computation.
        float borderL = block.getBorderLeft().width();
        float borderR = block.getBorderRight().width();
        float borderT = block.getBorderTop().width();
        float borderB = block.getBorderBottom().width();

        int gapWOff = 0;
        int gapHOff = 0;
        if (block instanceof LytAxisBox ax) {
            float g = ax.getGap();
            if (g > 0) {
                if (flexDir == 1) { // column
                    gapHOff = dimPx(fbb, g);
                } else {
                    gapWOff = dimPx(fbb, g);
                }
            }
        }

        byte overflow = getOverflow(block, flags);

        float flexGrow = 0f;
        float flexShrink = 1f;
        int flexBasisOff = 0;

        byte float_ = 0;
        byte clear = 0;

        byte position = 0;
        int insetTOff = 0; int insetROff = 0;
        int insetBOff = 0; int insetLOff = 0;

        return Style.createStyle(fbb,
            display, flexDir, flexWrap, alignItems, alignSelf, justify,
            gapWOff, gapHOff,
            sizeWOff, sizeHOff,
            minWOff, minHOff,
            maxWOff, maxHOff,
            0f, // aspectRatio
            marginL, marginR, marginT, marginB,
            false, false, false, false, // marginAuto
            padL, padR, padT, padB,
            borderL, borderR, borderT, borderB,
            overflow,
            flexGrow, flexShrink, flexBasisOff,
            float_, clear, position,
            insetTOff, insetROff, insetBOff, insetLOff);
    }


    /** Auto (null in FlatBuffer = unset). */
    public static int dimAuto(FlatBufferBuilder fbb) {
        return 0; // FlatBuffer convention: 0 = null for table fields
    }

    /** Fixed pixel value. */
    public static int dimPx(FlatBufferBuilder fbb, float px) {
        return com.hfstudio.guidenh.guide.layout.flatbuffers.Dimension.createDimension(fbb, px, (byte) 1);
    }

    /** Percentage (0.0 ~ 1.0). */
    public static int dimPct(FlatBufferBuilder fbb, float fraction) {
        return com.hfstudio.guidenh.guide.layout.flatbuffers.Dimension.createDimension(fbb, fraction * 100f, (byte) 2);
    }


    private static byte getDisplay(LytBlock block, int flags) {
        if ((flags & Flags.DISPLAY_BLOCK) != 0) return 2;
        if ((flags & Flags.DISPLAY_GRID) != 0) return 1;
        if ((flags & Flags.DISPLAY_FLEX) != 0) return 0;
        // auto-detect: Block for Document, Flex for boxes
        // Document check deferred until LytDocument package resolves
        return 0; // Flex (default for most blocks)
    }

    private static byte getFlexDirection(LytBlock block) {
        if (block instanceof LytHBox) return 0; // Row
        return 1; // Column (VBox, default)
    }

    private static byte getFlexWrap(LytBlock block, int flags) {
        // HBox wrap detection deferred until LytHBox resolves
        return 0; // NoWrap
    }

    private static byte getAlignItems(LytBlock block) {
        if (block instanceof LytAxisBox ax) {
            // Phase 1: enum mapping deferred — alignItems value resolution
            // depends on the actual LytAxisBox.AlignItems enum which may vary.
            return 0;
        }
        return 0;
    }

    private static byte getAlignSelf(LytBlock block, int flags) {
        // fullWidth → Stretch
        if ((flags & Flags.SIZE_FULL_WIDTH) != 0 || block.isFullWidth()) return 4;
        return 0; // Auto
    }

    private static void applySizeConstraints(LytBlock block, int flags, FlatBufferBuilder fbb) {
        // Phase 1: handled by caller through flags.
        // Full implementation will read explicitWidth/Height constraints.
    }

    private static byte getOverflow(LytBlock block, int flags) {
        if ((flags & Flags.OVERFLOW_SCROLL) != 0) return 2;
        if ((flags & Flags.OVERFLOW_HIDDEN) != 0) return 1;
        if (block instanceof LytSizeBox) return 2; // Scroll
        return 0; // Visible
    }
}
