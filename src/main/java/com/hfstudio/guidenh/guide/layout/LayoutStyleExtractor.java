package com.hfstudio.guidenh.guide.layout;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytAxisBox;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlockToolbar;
import com.hfstudio.guidenh.guide.document.block.LytHBox;
import com.hfstudio.guidenh.guide.document.block.LytItemGrid;
import com.hfstudio.guidenh.guide.document.block.LytSizeBox;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.document.block.LytViewportBox;
import com.hfstudio.guidenh.guide.document.block.recipes.LytStandardRecipeBox;
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

        public static final int NONE = 0;
        public static final int DISPLAY_BLOCK = 1 << 0;
        public static final int DISPLAY_FLEX = 1 << 1;
        public static final int DISPLAY_GRID = 1 << 2;
        public static final int SIZE_FULL_WIDTH = 1 << 3; // force 100% width
        public static final int SIZE_AUTO_WIDTH = 1 << 4; // force auto width
        public static final int SIZE_AUTO_HEIGHT = 1 << 5; // force auto height
        public static final int OVERFLOW_HIDDEN = 1 << 6;
        public static final int OVERFLOW_SCROLL = 1 << 7;
        /** Fall back to the block's current Java-computed bounds when no explicit size is set. */
        public static final int SIZE_FROM_JAVA_BOUNDS = 1 << 8;

        private Flags() {}
    }

    private LayoutStyleExtractor() {}

    /** Absolute-position lowering for floated blocks: inset + size in px, relative to the flattened parent. */
    public record FloatAbs(int insetLeft, int insetTop, int width, int height) {}

    /** Lane pinning for float-adjacent blocks: extra margin-left and explicit width in px. */
    public record LanePin(int marginLeft, int width) {}

    /**
     * Per-node style adjustments computed by {@code LayoutTreeSerializer} while
     * lowering the tree. {@code minHeight} bridges the Java flow height of a
     * paragraph whose height was extended by a float-clearing break (the break
     * has no text, so Rust cannot measure the cleared space).
     */
    public record NodeAdjustments(int marginT, int marginR, int marginB, int marginL, FloatAbs abs, LanePin lane,
        float flexGrow, int minHeight) {

        public static final NodeAdjustments ZERO = new NodeAdjustments(0, 0, 0, 0, null, null, 0f, 0);
    }

    /** Build a FlatBuffer Style from a LytBlock node. Extracts all layout-relevant fields. */
    public static int build(FlatBufferBuilder fbb, LytBlock block) {
        return build(fbb, block, Flags.NONE, NodeAdjustments.ZERO);
    }

    /** Build with additional flags overriding automatic detection. */
    public static int build(FlatBufferBuilder fbb, LytBlock block, int flags) {
        return build(fbb, block, flags, NodeAdjustments.ZERO);
    }

    /**
     * Build with margin offsets from eliminated ancestor nodes.
     * The offset values are <b>added</b> to the block's own margins.
     */
    public static int build(FlatBufferBuilder fbb, LytBlock block, int marginOffT, int marginOffR, int marginOffB,
        int marginOffL) {
        return build(
            fbb,
            block,
            Flags.NONE,
            new NodeAdjustments(marginOffT, marginOffR, marginOffB, marginOffL, null, null, 0f, 0));
    }

    /**
     * Build with flags and per-node adjustments from the serializer's lowering pass.
     */
    public static int build(FlatBufferBuilder fbb, LytBlock block, int flags, NodeAdjustments adj) {
        byte display = getDisplay(block, flags);
        byte flexDir = getFlexDirection(block);
        byte flexWrap = getFlexWrap(block, flags);
        byte alignItems = getAlignItems(block);
        byte alignSelf = getAlignSelf(block, flags);
        byte justify = 0; // default: Start

        int sizeWOff = dimAuto(fbb); // default: auto
        int sizeHOff = dimAuto(fbb);
        int minWOff = 0;
        int minHOff = 0;
        int maxWOff = 0;
        int maxHOff = 0;

        // Apply explicit size constraints from the block
        int explicitW = block.getExplicitWidth();
        int explicitH = block.getExplicitHeight();

        // ---- compiler lowering: per-class size rules ------------------------
        // Fixed grids: explicit width so the row-wrap forms exactly N columns.
        if (block instanceof LytSlotGrid sg && explicitW <= 0) {
            explicitW = sg.getWidth() * LytSlot.OUTER_SIZE;
        }
        // Size boxes with a preferred width reserve exactly that width.
        if (block instanceof LytSizeBox sb && sb.getPreferredWidth() > 0 && explicitW <= 0) {
            explicitW = sb.getPreferredWidth();
        }
        // Scroll containers clip their content through an inner LytViewportBox
        // (which declares the viewport height itself), so no container-level
        // size/overflow rule is needed here.

        if ((flags & Flags.SIZE_FROM_JAVA_BOUNDS) != 0) {
            // Leaf-serialized blocks have no Rust-measured content; their size
            // must match the Java-computed flow bounds (available because
            // serialization runs after the Java layout pass) so Rust reserves
            // the same box. Flow bounds — not visual getBounds(): floats report
            // a zero-height flow rect while their content visually overflows.
            LytRect b = block.getFlowBounds();
            if (explicitW <= 0 && b != null) explicitW = b.width();
            if (explicitH <= 0 && b != null) explicitH = b.height();
        }

        // ---- compiler lowering: float de-sugar ------------------------------
        // Float-adjacent blocks are pinned to their Java lane: margin-left
        // displacement + explicit lane width.
        if (adj.lane() != null && explicitW <= 0) {
            explicitW = adj.lane()
                .width();
        }
        // Floated blocks become position:absolute with their Java rect as
        // inset + size (out of flow, zero flow height — exactly CSS float).
        if (adj.abs() != null) {
            explicitW = adj.abs()
                .width();
            explicitH = adj.abs()
                .height();
        }

        if (explicitW > 0) {
            sizeWOff = dimPx(fbb, explicitW);
        } else if ((flags & Flags.SIZE_FULL_WIDTH) != 0) {
            sizeWOff = dimPct(fbb, 1.0f);
        } else if ((flags & Flags.SIZE_AUTO_WIDTH) != 0) {
            sizeWOff = dimAuto(fbb);
        }

        if (explicitH > 0) {
            sizeHOff = dimPx(fbb, explicitH);
        } else if ((flags & Flags.SIZE_AUTO_HEIGHT) != 0) {
            sizeHOff = dimAuto(fbb);
        }

        // Clear-break bridge: reserve the Java flow height of a paragraph whose
        // height was extended by <br clear="..."/> so following blocks stack
        // below the float (Rust measures only the text — the break has none).
        if (adj.minHeight() > 0) {
            minHOff = dimPx(fbb, adj.minHeight());
        }

        float marginL = block.getMarginLeft() + adj.marginL();
        float marginR = block.getMarginRight() + adj.marginR();
        float marginT = block.getMarginTop() + adj.marginT();
        float marginB = block.getMarginBottom() + adj.marginB();
        if (adj.lane() != null) {
            marginL += adj.lane()
                .marginLeft();
        }
        if (adj.abs() != null) {
            // Absolutely-positioned (floated) blocks: margins are meaningless.
            marginL = 0;
            marginR = 0;
            marginT = 0;
            marginB = 0;
        }
        float padL = 0;
        float padR = 0;
        float padT = 0;
        float padB = 0;
        // Read actual padding from LytBox subclasses (VBox, HBox, etc.)
        if (block instanceof LytBox lb) {
            padL = readLytBoxPadding(lb, "paddingLeft");
            padR = readLytBoxPadding(lb, "paddingRight");
            padT = readLytBoxPadding(lb, "paddingTop");
            padB = readLytBoxPadding(lb, "paddingBottom");
        }
        float borderL = block.getBorderLeft()
            .width();
        float borderR = block.getBorderRight()
            .width();
        float borderT = block.getBorderTop()
            .width();
        float borderB = block.getBorderBottom()
            .width();

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

        float flexGrow = adj.flexGrow();
        // Content inside a scroll viewport keeps its natural height — Taffy
        // must not shrink it to fit the (shorter) viewport.
        float flexShrink = block.getParent() instanceof LytViewportBox ? 0f : 1f;
        int flexBasisOff = 0;

        byte float_ = 0; // floats are lowered to absolute positioning, never emitted
        byte clear = 0;

        byte position = 0;
        int insetTOff = 0;
        int insetROff = 0;
        int insetBOff = 0;
        int insetLOff = 0;
        if (adj.abs() != null) {
            position = 1; // Absolute
            insetLOff = dimPx(
                fbb,
                adj.abs()
                    .insetLeft());
            insetTOff = dimPx(
                fbb,
                adj.abs()
                    .insetTop());
        }

        return Style.createStyle(
            fbb,
            display,
            flexDir,
            flexWrap,
            alignItems,
            alignSelf,
            justify,
            gapWOff,
            gapHOff,
            sizeWOff,
            sizeHOff,
            minWOff,
            minHOff,
            maxWOff,
            maxHOff,
            0f, // aspectRatio
            marginL,
            marginR,
            marginT,
            marginB,
            false,
            false,
            false,
            false, // marginAuto
            padL,
            padR,
            padT,
            padB,
            borderL,
            borderR,
            borderT,
            borderB,
            overflow,
            flexGrow,
            flexShrink,
            flexBasisOff,
            float_,
            clear,
            position,
            insetTOff,
            insetROff,
            insetBOff,
            insetLOff);
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

    /**
     * Read an int padding field from LytBox via reflection.
     * {@code LytBox.paddingLeft/Top/Right/Bottom} are {@code protected}, not
     * accessible from this package, so we use reflection as the least-invasive
     * bridge. Revisit if a public getter is added to LytBox in the future.
     */
    static int readLytBoxPadding(LytBox box, String fieldName) {
        try {
            var field = LytBox.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getInt(box);
        } catch (Exception e) {
            return 0;
        }
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
        // Compiler lowering: grids and horizontal composites become row containers.
        if (block instanceof LytSlotGrid || block instanceof LytItemGrid) return 0;
        if (block instanceof LytStandardRecipeBox) return 0;
        if (block instanceof LytCodeBlockToolbar) return 0;
        // Table rows are real Row containers (cells keep pinned column widths).
        if (block instanceof com.hfstudio.guidenh.guide.document.block.table.LytTableRow) return 0;
        return 1; // Column (VBox, default)
    }

    private static byte getFlexWrap(LytBlock block, int flags) {
        if (block instanceof LytHBox hb && hb.isWrap()) return 1; // Wrap
        // Grids lower to wrapping rows: N per line, then wrap.
        if (block instanceof LytSlotGrid || block instanceof LytItemGrid) return 1;
        return 0; // NoWrap
    }

    private static byte getAlignItems(LytBlock block) {
        if (block instanceof LytCodeBlockToolbar) return 1; // Center (label + icon buttons)
        // CSS default is stretch: block-level children fill the cross axis.
        return 3; // Stretch
    }

    private static byte getAlignSelf(LytBlock block, int flags) {
        // fullWidth → Stretch
        if ((flags & Flags.SIZE_FULL_WIDTH) != 0 || block.isFullWidth()) return 4;
        // Item grids wrap by available width — stretch so the wrap engages.
        if (block instanceof LytItemGrid) return 4;
        return 0; // Auto
    }

    private static byte getOverflow(LytBlock block, int flags) {
        if ((flags & Flags.OVERFLOW_SCROLL) != 0) return 2;
        if ((flags & Flags.OVERFLOW_HIDDEN) != 0) return 1;
        if (block instanceof LytSizeBox) return 2; // Scroll
        return 0; // Visible
    }
}
