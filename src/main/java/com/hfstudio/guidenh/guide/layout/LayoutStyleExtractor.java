package com.hfstudio.guidenh.guide.layout;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytAxisBox;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlockToolbar;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
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
        /**
         * Historical: SIZE_FROM_JAVA_BOUNDS = 1 &lt;&lt; 0 was used by the Java layout
         * pre-pass to reserve Java-computed bounds for opaque containers. The
         * pre-pass has been removed; opaque containers now declare size through
         * explicit dimensions or Rust-side measure functions. The constant is
         * kept as a comment-only symbol — any reference outside this comment
         * should be treated as a bug.
         */
        // public static final int SIZE_FROM_JAVA_BOUNDS = 1 << 0;

        private Flags() {}
    }

    private LayoutStyleExtractor() {}

    /** Out-of-flow marker for inline blocks: visual box size in px (position comes from Rust's inline post-pass). */
    public record FloatAbs(int width, int height) {}

    /**
     * Per-node style adjustments computed by {@code LayoutTreeSerializer} while
     * lowering the tree. {@code floatSide} (0 none, 1 left, 2 right) is set on a
     * floated block's inner so the Rust pusher treats it as a real CSS float
     * (the float gap rides the inner's margin); {@code abs} lowers inline blocks
     * to position:absolute.
     */
    public record NodeAdjustments(int marginT, int marginR, int marginB, int marginL, FloatAbs abs, int floatSide,
        int columnWidth) {

        public static final NodeAdjustments ZERO = new NodeAdjustments(0, 0, 0, 0, null, 0, 0);
    }

    /**
     * Build with flags and per-node adjustments from the serializer's lowering pass.
     */
    public static int build(FlatBufferBuilder fbb, LytBlock block, int flags, NodeAdjustments adj) {
        byte display = getDisplay(block);
        byte flexDir = getFlexDirection(block);
        byte flexWrap = getFlexWrap(block);
        byte alignItems = getAlignItems(block);
        byte alignSelf = getAlignSelf(block);
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

        // SIZE_FROM_JAVA_BOUNDS consumption branch removed (the Java layout
        // pre-pass is gone). Opaque containers declare size through explicit
        // dimensions or Rust-side measure functions. Flow-bounds fallback no
        // longer applies — getFlowBounds() is retained for other callers.

        // Table cells keep their Java-resolved column width so cell content
        // wraps at the column width (column model not yet on taffy Grid).
        if (adj.columnWidth() > 0 && explicitW <= 0) {
            explicitW = adj.columnWidth();
        }

        // ---- compiler lowering: inline-block absolute sizing -----------------
        // Inline blocks become position:absolute with their visual box as inset
        // + size (out of flow). Document-level floats are NOT lowered here: the
        // inner carries a real `float` (floatSide) and the Rust pusher lays it
        // out, with the float gap expressed as the inner's margin.
        if (adj.abs() != null) {
            explicitW = adj.abs()
                .width();
            explicitH = adj.abs()
                .height();
        }

        if (explicitW > 0) {
            sizeWOff = dimPx(fbb, explicitW);
        }

        if (explicitH > 0) {
            sizeHOff = dimPx(fbb, explicitH);
        }

        float marginL = block.getMarginLeft() + adj.marginL();
        float marginR = block.getMarginRight() + adj.marginR();
        float marginT = block.getMarginTop() + adj.marginT();
        float marginB = block.getMarginBottom() + adj.marginB();
        if (adj.abs() != null) {
            // Absolutely-positioned (floated) blocks: margins are meaningless.
            marginL = 0;
            marginR = 0;
            marginT = 0;
            marginB = 0;
        }
        // Float gap rides the inner's margin on the text-facing side + bottom,
        // so the pusher's registered (margin-box) rectangle keeps FLOAT_GAP
        // clear of the float content while the drawn box stays the content box.
        if (adj.floatSide() == 2) {
            marginL += LytDocumentFloat.FLOAT_GAP;
            marginB += LytDocumentFloat.FLOAT_GAP;
        } else if (adj.floatSide() == 1) {
            marginR += LytDocumentFloat.FLOAT_GAP;
            marginB += LytDocumentFloat.FLOAT_GAP;
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

        byte overflow = getOverflow(block);

        float flexGrow = block.getFlexGrow();
        // Content inside a scroll viewport keeps its natural height — Taffy
        // must not shrink it to fit the (shorter) viewport.
        float flexShrink = block.getParent() instanceof LytViewportBox ? 0f : 1f;
        int flexBasisOff = 0;

        byte float_ = (byte) adj.floatSide(); // 0 none, 1 left, 2 right — real CSS float for the pusher
        byte clear = 0;

        byte position = 0;
        int insetTOff = 0;
        int insetROff = 0;
        int insetBOff = 0;
        int insetLOff = 0;
        if (adj.abs() != null) {
            position = 1; // Absolute — Rust's inline post-pass assigns the real position
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

    private static byte getDisplay(LytBlock block) {
        return 0; // Flex (default for all blocks)
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

    private static byte getFlexWrap(LytBlock block) {
        if (block instanceof LytHBox hb && hb.isWrap()) return 1; // Wrap
        // Grids lower to wrapping rows: N per line, then wrap.
        if (block instanceof LytSlotGrid || block instanceof LytItemGrid) return 1;
        return 0; // NoWrap
    }

    private static byte getAlignItems(LytBlock block) {
        if (block instanceof LytCodeBlockToolbar) return 1; // Center (label + icon buttons)
        if (block instanceof LytAxisBox ax) {
            return switch (ax.getAlignItems()) {
                case CENTER -> 1;
                case END    -> 2;
                // START -> Stretch: preserve existing behavior for the many blocks
                // that never call setAlignItems() and keep the LytAxisBox default.
                default -> 3;
            };
        }
        // CSS default is stretch: block-level children fill the cross axis.
        return 3; // Stretch
    }

    private static byte getAlignSelf(LytBlock block) {
        if (block.isFullWidth()) return 4; // Stretch
        // Item grids wrap by available width — stretch so the wrap engages.
        if (block instanceof LytItemGrid) return 4;
        return 0; // Auto
    }

    private static byte getOverflow(LytBlock block) {
        if (block instanceof LytSizeBox) return 2; // Scroll
        return 0; // Visible
    }
}
