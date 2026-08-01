package com.hfstudio.guidenh.guide.layout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.ContentAlign;
import com.hfstudio.guidenh.guide.document.block.LytAlignedBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytGuiSprite;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytItemImage;
import com.hfstudio.guidenh.guide.document.block.LytLatexBlock;
import com.hfstudio.guidenh.guide.document.block.LytLatexDisplayBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytSlot;
import com.hfstudio.guidenh.guide.document.block.LytThematicBreak;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.document.flow.InlineBlockAlignment;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.flow.LytFlowInlineBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.layout.flatbuffers.LayoutInput;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Serializes a Lyt document tree into a FlatBuffer LayoutInput byte array.
 * <p>
 * Handles:
 * - Tree traversal and flat_index assignment
 * - Node elimination (spans, anchors, aligned blocks, floats)
 * - Paragraph merging (multiple LytFlowContent → one text FlatNode)
 * - Delegates Style build to {@link LayoutStyleExtractor}
 * - Delegates FlatNode build to {@link LayoutNodeSerializer}
 * <p>
 * This replaces RFLayoutTreeBuilder.
 */
public class LayoutTreeSerializer {

    /** Theme token: text justification (0=off, 1=auto — stretch spaces on Latin lines). */
    private static final com.hfstudio.guidenh.guide.style.token.TokenKey<com.hfstudio.guidenh.guide.style.token.IntValue> TEXT_JUSTIFY = com.hfstudio.guidenh.guide.style.token.TokenKey
        .define(
            "--lyt-text-justify",
            com.hfstudio.guidenh.guide.style.token.TokenType.INT,
                new com.hfstudio.guidenh.guide.style.token.IntValue(1));

    /** Padding subtracted from available width for table column layout (matches Rust layout.rs:17). */
    private static final int CONTENT_PAD = 14;

    /**
     * Minimum available width (px) for justified text. Parley's Justify has no
     * inter-word spacing cap: below ~60 chars per line it stretches the leftover
     * space into wide word caves ("rivers"), so narrow columns fall back to left
     * alignment (ragged right) instead. Wide columns keep the
     * {@link #TEXT_JUSTIFY} theme token's decision, and per-paragraph
     * center/right alignment is unaffected — it outranks justify in Rust's
     * resolve_alignment (parley_text.rs).
     */
    private static final float JUSTIFY_MIN_WIDTH = 550f;

    private final List<LytBlock> flatNodes = new ArrayList<>();
    private final Map<LytNode, Integer> nodeToIndex = new IdentityHashMap<>();
    /** Margins accumulated from eliminated ancestors, applied during style extraction. */
    private final Map<LytBlock, MarginAccum> marginOffsets = new IdentityHashMap<>();
    /**
     * Absolute-position lowering for paragraph-inline blocks only: inset + size
     * in px relative to the Rust parent's content box. (Document-level floats no
     * longer lower to absolute — the inner carries a real `float` instead.)
     */
    private final Map<LytBlock, LayoutStyleExtractor.FloatAbs> absoluteFloats = new IdentityHashMap<>();
    /**
     * Float side (1 left, 2 right) for a floated block's inner, set when the
     * {@link LytDocumentFloat} wrapper is eliminated so the inner is emitted as a
     * real CSS float for the Rust pusher.
     */
    private final Map<LytBlock, Integer> floatIntents = new IdentityHashMap<>();
    /** Table-cell column widths (px), kept until the column model moves to taffy Grid. */
    private final Map<LytBlock, Integer> columnWidths = new IdentityHashMap<>();
    /**
     * Float rects registered during serialize, in document coordinates. NOT fed
     * to Rust (the pusher computes its own float geometry); retained solely as
     * the test-only oracle for the harness glyph-vs-float overlap invariant.
     * Filled from the Java-laid-out inner bounds at wrapper-elimination time.
     */
    private final List<FloatRect> floatRects = new ArrayList<>();
    /**
     * Alignment intents inherited from eliminated {@link LytAlignedBlock} wrappers.
     * Keys are flat nodes (nearest non-eliminated ancestors of a non-LEFT AlignedBlock);
     * values are Taffy align_items bytes: 1=Center, 2=End.
     * Applied in {@link LayoutStyleExtractor#build} to override the default Stretch
     * so the wrapper centres/right-aligns its children with natural width.
     */
    private final Map<LytBlock, Byte> alignIntents = new IdentityHashMap<>();
    /** Available width (px) from the last serialize() — needed by flattenTree for table column layout. */
    private float serializeAvailWidth;
    /** Visual scale from the last serialize() — needed by flattenTree for inline-block dimension computation. */
    private float serializeVisualScale;

    record FloatRect(LytRect rect, boolean right) {}

    public byte[] serialize(LytNode root, float availWidth, float visualScale, float renderScale) {
        flatNodes.clear();
        nodeToIndex.clear();
        marginOffsets.clear();
        absoluteFloats.clear();
        floatIntents.clear();
        columnWidths.clear();
        floatRects.clear();
        alignIntents.clear();
        this.serializeAvailWidth = availWidth;
        this.serializeVisualScale = visualScale;

        flattenTree(root);

        FlatBufferBuilder fbb = new FlatBufferBuilder(4096);
        int[] nodeOffsets = new int[flatNodes.size()];

        // DEBUG: log flat node count and types
        long paraCount = flatNodes.stream()
            .filter(b -> b instanceof LytParagraph)
            .count();
        long imgCount = flatNodes.stream()
            .filter(b -> b instanceof LytImage || b instanceof LytImageBlock)
            .count();
        GuideDebugLog
            .warnAlways("Layout: serializing {} flat nodes ({} para, {} img)", flatNodes.size(), paraCount, imgCount);

        for (int i = 0; i < flatNodes.size(); i++) {
            LytBlock block = flatNodes.get(i);
            MarginAccum mo = marginOffsets.getOrDefault(block, MarginAccum.ZERO);
            List<Integer> childIndices = getChildIndices(block);
            int flags = LayoutStyleExtractor.Flags.NONE;
            byte nodeType = LayoutNodeSerializer.resolveNodeType(block);
            // SIZE_FROM_JAVA_BOUNDS was previously set for opaque leaf containers
            // that had no Rust measure function. The Java layout pre-pass has been
            // removed — opaque containers now declare their size through explicit
            // dimensions or Rust-side measure functions. See Flag constant in
            // LayoutStyleExtractor (kept as historical comment).
            byte alignIntent = alignIntents.getOrDefault(block, (byte) 0);
            var adj = new LayoutStyleExtractor.NodeAdjustments(
                (int) mo.top(),
                (int) mo.right(),
                (int) mo.bottom(),
                (int) mo.left(),
                absoluteFloats.get(block),
                floatIntents.getOrDefault(block, 0),
                columnWidths.getOrDefault(block, 0),
                alignIntent);
            int styleOff = LayoutStyleExtractor.build(fbb, block, flags, adj);
            List<LayoutNodeSerializer.InlineRef> inlineRefs = new ArrayList<>();
            if (block instanceof LytParagraph par) {
                for (LytFlowContent fc : par.getContent()) {
                    collectInlineRefs(fc, inlineRefs);
                }
            }
            nodeOffsets[i] = LayoutNodeSerializer.build(fbb, block, styleOff, childIndices, inlineRefs);
        }

        int nodesVec = fbb.createVectorOfTables(nodeOffsets);
        // Narrow-column typography: justify is only enabled when the available
        // width is wide enough for ~60 chars/line; below that the stretched
        // inter-word spaces form rivers/word caves, so the paragraph falls back
        // to left alignment. Per-paragraph alignment (center/right) is untouched:
        // Rust's resolve_alignment already ranks it above justify.
        byte justify = 0;
        if (availWidth >= JUSTIFY_MIN_WIDTH) {
            justify = (byte) com.hfstudio.guidenh.guide.style.token.GuideThemeManager.instance()
                .active()
                .int_(TEXT_JUSTIFY)
                .value();
        }
        int inputOff = LayoutInput.createLayoutInput(fbb, availWidth, visualScale, renderScale, justify, nodesVec);
        fbb.finish(inputOff);
        return fbb.sizedByteArray();
    }

    @Nullable
    public LytNode getNodeByFlatIndex(int index) {
        return index >= 0 && index < flatNodes.size() ? flatNodes.get(index) : null;
    }

    public int getFlatIndex(LytNode node) {
        return nodeToIndex.getOrDefault(node, -1);
    }

    /**
     * Test-only view of the float rects registered during the last serialize()
     * call (document coordinates). Backs LayoutPipelineHarness's
     * glyph-vs-float overlap invariant.
     */
    List<FloatRect> getFloatRects() {
        return floatRects;
    }

    /**
     * Recursively walk flow content to collect inline-block refs. Float-aligned
     * inline blocks (FLOAT_LEFT / FLOAT_RIGHT) carry their alignment from the
     * {@link LytFlowInlineBlock} wrapper; inline blocks use type-based alignment.
     */
    private void collectInlineRefs(LytFlowContent fc, List<LayoutNodeSerializer.InlineRef> out) {
        if (fc instanceof LytFlowInlineBlock ib && ib.getBlock() != null) {
            int ibIdx = getFlatIndex(ib.getBlock());
            if (ibIdx >= 0) {
                out.add(inlineRefOf(ibIdx, ib.getBlock(), ib.getAlignment()));
            }
        } else if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                collectInlineRefs(child, out);
            }
        }
    }

    /**
     * Vertical alignment request for one inline block (see InlineBlockRef in the
     * schema). Float aligns (3/4) override type-based alignment (0/1/2).
     */
    private static LayoutNodeSerializer.InlineRef inlineRefOf(int flatIndex, LytBlock ib,
        InlineBlockAlignment flowAlign) {
        if (flowAlign == InlineBlockAlignment.FLOAT_LEFT) {
            return new LayoutNodeSerializer.InlineRef(flatIndex, 3, 0f);
        }
        if (flowAlign == InlineBlockAlignment.FLOAT_RIGHT) {
            return new LayoutNodeSerializer.InlineRef(flatIndex, 4, 0f);
        }
        if (ib instanceof LytLatexBlock latex) {
            return new LayoutNodeSerializer.InlineRef(flatIndex, 1, latex.getBaselineAscent());
        }
        if (ib instanceof com.hfstudio.guidenh.guide.document.block.LytItemImage img && img.isInline()
            && img.isShowingIcon()) {
            return new LayoutNodeSerializer.InlineRef(flatIndex, 2, img.getInlineVerticalOffset());
        }
        return new LayoutNodeSerializer.InlineRef(flatIndex, 0, 0f);
    }

    /**
     * Accumulated margins from eliminated intermediate nodes that should be
     * pushed onto the nearest non-eliminated descendant block's own margins.
     */
    private record MarginAccum(float top, float right, float bottom, float left) {

        static MarginAccum ZERO = new MarginAccum(0, 0, 0, 0);

        MarginAccum add(LytBlock block) {
            return new MarginAccum(
                top + block.getMarginTop(),
                right + block.getMarginRight(),
                bottom + block.getMarginBottom(),
                left + block.getMarginLeft());
        }
    }

    private void flattenTree(LytNode node) {
        flattenTree(node, MarginAccum.ZERO, 0, null);
    }

    /**
     * @param inherited       margins accumulated from eliminated ancestors
     * @param pendingFloat    a float wrapper's anchor node was just emitted
     *                        up-stack; the first non-eliminated block descendant
     *                        becomes its absolutely-positioned child
     * @param nearestAncestor the most recent non-eliminated {@link LytBlock}
     *                        ancestor in the current tree path, or {@code null}
     *                        at the root. When an {@link LytAlignedBlock} with
     *                        non-{@link ContentAlign#LEFT} is eliminated, its
     *                        alignment intent is recorded on this ancestor so
     *                        its style can be adjusted (align_items → Center/End)
     *                        during serialization.
     */
    private void flattenTree(LytNode node, MarginAccum inherited, int pendingFloatSide,
        @javax.annotation.Nullable LytBlock nearestAncestor) {
        if (shouldEliminate(node)) {
            // Add this node's margins to the inherited accumulator
            MarginAccum total = inherited;
            if (node instanceof LytBlock elided) {
                total = total.add(elided);
            }
            // A float wrapper is eliminated here: register its rect for the
            // harness overlap oracle (from the Java-laid-out inner bounds) and
            // forward the float side to the inner, which becomes a real float.
            int childSide = pendingFloatSide;
            if (node instanceof LytDocumentFloat df) {
                LytRect inner = df.getBounds();
                int gap = LytDocumentFloat.FLOAT_GAP;
                LytRect frect = df.isFloatRight()
                    ? new LytRect(inner.x() - gap, inner.y(), inner.width() + gap, inner.height() + gap)
                    : new LytRect(inner.x(), inner.y(), inner.width() + gap, inner.height() + gap);
                floatRects.add(new FloatRect(frect, df.isFloatRight()));
                childSide = df.isFloatRight() ? 2 : 1;
            }
            // Capture alignment from eliminated AlignedBlock on the nearest
            // non-eliminated ancestor (the wrapper that owns the flex container
            // for its children). The ancestor's align_items will override the
            // default Stretch so children keep natural width and are positioned.
            if (node instanceof LytAlignedBlock aligned && nearestAncestor != null) {
                ContentAlign a = aligned.getAlign();
                if (a == ContentAlign.CENTER) {
                    alignIntents.put(nearestAncestor, (byte) 1); // Center
                } else if (a == ContentAlign.RIGHT) {
                    alignIntents.put(nearestAncestor, (byte) 2); // End
                }
            }
            for (LytNode child : node.getChildren()) {
                flattenTree(child, total, childSide, nearestAncestor);
            }
            return;
        }

        // Assign index and register
        int idx = flatNodes.size();
        // Only LytBlock subclasses can be flat nodes; skip non-block content
        if (node instanceof LytBlock block) {
            if (inherited != MarginAccum.ZERO) {
                marginOffsets.put(block, inherited);
            }
            flatNodes.add(block);
            nodeToIndex.put(node, idx);
            if (pendingFloatSide != 0) {
                // The float wrapper was eliminated up-stack; this inner becomes
                // a real CSS float for the Rust pusher (the float gap is added
                // to its margin during style extraction).
                floatIntents.put(block, pendingFloatSide);
                pendingFloatSide = 0;
            }
            if (block instanceof LytTable table && !table.getColumns().isEmpty()) {
                // Column widths must be resolved before serialization so cells
                // carry their column width constraint. The Java pre-pass is
                // removed, so layoutColumns is called here with the available
                // width from the serialize() parameter. (x=0 is safe — column.x
                // is overwritten by Rust.)
                // R4-18 fix: When ALL columns have declared preferred widths,
                // use the sum of declared widths as the table's natural width
                // instead of the full available width. This allows tables to
                // shrink to content width when not explicitly set to fullWidth.
                int availW = Math.round(serializeAvailWidth - 2.0f * CONTENT_PAD);
                boolean allDeclared = table.getColumns().stream().allMatch(c -> c.getPreferredWidth() > 0);
                if (allDeclared && !table.isFullWidth()) {
                    int sumPreferred = table.getColumns().stream().mapToInt(c -> c.getPreferredWidth()).sum();
                    int borders = (table.getColumns().size() + 1) * LytTable.CELL_BORDER;
                    int naturalW = sumPreferred + borders;
                    table.layoutColumns(0, Math.min(naturalW, availW));
                } else {
                    table.layoutColumns(0, availW);
                }
                // Table cells keep the Java-computed COLUMN widths (the column
                // model resolves preferred/flexible widths) so cell content
                // wraps at the column width; heights are Rust-measured so
                // wrapped content grows its row instead of overflowing a
                // pinned box.
                for (LytTableRow row : table.getChildren()) {
                    int ci = 0;
                    for (LytTableCell cell : row.getChildren()) {
                        var column = table.getColumns()
                            .get(ci++);
                        columnWidths.put(cell, column.getWidth());
                    }
                }
            }
            if (block instanceof LytParagraph par) {
                // Inline blocks embedded in the paragraph's flow content become
                // absolutely positioned leaves; the Rust inline post-pass
                // anchors each at its U+FFFC placeholder's pen position. The
                // serialized size is the block's VISUAL box (the legacy line-
                // expansion insets of e.g. LytLatexBlock must not leak into the
                // anchor math — the post-pass aligns the visual box itself).
                for (LytBlock ib : par.getInlineBlocks()) {
                    int vw;
                    int vh;
                    if (ib instanceof LytLatexBlock latex && latex.getFormulaDisplayW() > 0) {
                        vw = latex.getFormulaDisplayW();
                        vh = latex.getFormulaDisplayH();
                    } else if (ib instanceof LytLatexDisplayBlock ldb && ldb.getFormulaDisplayW() > 0) {
                        vw = ldb.getFormulaDisplayW();
                        vh = ldb.getFormulaDisplayH();
                    } else if (ib instanceof LytSlot slot) {
                        int sz = slot.isLargeSlot() ? LytSlot.OUTER_SIZE_LARGE : LytSlot.OUTER_SIZE;
                        vw = vh = sz;
                    } else if (ib instanceof LytThematicBreak) {
                        LytRect b = ib.getBounds();
                        vw = b != null ? b.width() : 0;
                        vh = 6;
                    } else if (ib instanceof LytGuiSprite gs) {
                        vw = Math.max(1, gs.getExplicitWidth());
                        vh = Math.max(1, gs.getExplicitHeight());
                    } else if (ib instanceof LytImage image) {
                        // LytImage after script materialization: compute displayed
                        // dimensions from texture, crop, scale, and DEFAULT_LAYOUT_SCALE
                        // (mirrors LytImage.computeLayout).  The Java layout pre-pass is
                        // removed so ib.getBounds() is LytRect.empty(); relying on the
                        // explicit-dimensions guard alone would leave size_h=0, making
                        // Rust inline_block_height() return 0 and inline_line_growth()
                        // stay 0 — the paragraph never grows to include the image, and
                        // the parent flex container (LytListItem) omits the image height.
                        if (image.getExplicitWidth() > 0) {
                            vw = image.getExplicitWidth();
                        } else {
                            var tex = image.getTexture();
                            if (tex != null && !tex.isMissing()) {
                                var size = tex.getSize();
                                int sourceW = image.getCropWidth() > 0
                                    ? image.getCropWidth() : size.width();
                                vw = Math.max(1, (int) Math.round(
                                    sourceW * LytImage.DEFAULT_LAYOUT_SCALE * image.getScaleX()));
                            } else {
                                LytRect b = ib.getBounds();
                                vw = b != null ? b.width() : 0;
                            }
                        }
                        if (image.getExplicitHeight() > 0) {
                            vh = image.getExplicitHeight();
                        } else {
                            var tex = image.getTexture();
                            if (tex != null && !tex.isMissing()) {
                                var size = tex.getSize();
                                int sourceH = image.getCropHeight() > 0
                                    ? image.getCropHeight() : size.height();
                                vh = Math.max(1, (int) Math.round(
                                    sourceH * LytImage.DEFAULT_LAYOUT_SCALE * image.getScaleY()));
                            } else {
                                LytRect b = ib.getBounds();
                                vh = b != null ? b.height() : 0;
                            }
                        }
                        // Apply visual scale (mirrors LytImage.computeLayout)
                        float vs = serializeVisualScale;
                        if (vs < 0.999f) {
                            vw = Math.max(1, Math.round(vw * vs));
                            vh = Math.max(1, Math.round(vh * vs));
                        }
                    } else if (ib instanceof LytImageBlock imb && imb.getExplicitWidth() > 0
                        && imb.getExplicitHeight() > 0) {
                        vw = imb.getExplicitWidth();
                        vh = imb.getExplicitHeight();
                    } else if (ib instanceof LytItemImage itemImg) {
                        int[] size = itemImg.measureSerializedInlineSize();
                        vw = size[0];
                        vh = size[1];
                    } else {
                        LytRect b = ib.getBounds();
                        vw = b != null ? b.width() : 0;
                        vh = b != null ? b.height() : 0;
                    }
                    absoluteFloats.put(ib, new LayoutStyleExtractor.FloatAbs(vw, vh));
                }
            }
        }

        LytBlock ancestorForChildren = (node instanceof LytBlock b) ? b : nearestAncestor;
        for (LytNode child : node.getChildren()) {
            flattenTree(child, MarginAccum.ZERO, pendingFloatSide, ancestorForChildren);
        }
    }

    private static boolean shouldEliminate(LytNode node) {
        // Flow classes don't extend LytNode — use class name check
        String name = node.getClass()
            .getName();
        if (name.contains("LytFlowSpan") || name.contains("LytFlowAnchor")
            || name.contains("LytFlowBreak")
            || name.contains("LytFlowInlineBlock")) {
            return true;
        }
        // Blocks that are layout wrappers — eliminated in tree. LytDocumentFloat
        // is eliminated too: its inner is re-emitted as a real CSS float (the
        // wrapper's only remaining job — registering the harness oracle rect —
        // happens in the elimination branch of flattenTree).
        if (node instanceof LytAlignedBlock) {
            return true;
        }
        if (node instanceof LytDocumentFloat) {
            return true;
        }
        return false;
    }

    private List<Integer> getChildIndices(LytBlock block) {
        List<Integer> indices = new ArrayList<>();
        if (block instanceof LytParagraph) {
            // Text paragraphs stay measured text leaves in Taffy: their inline
            // blocks are hoisted to the parent's vector and paired through
            // TextData.inline_blocks instead.
            return indices;
        }
        collectBlockChildren(block, indices);
        return indices;
    }

    /**
     * Collect flat-node indices for all {@link LytBlock} descendants of
     * {@code node}, skipping eliminated intermediate nodes.
     */
    private void collectBlockChildren(LytNode node, List<Integer> out) {
        for (LytNode child : node.getChildren()) {
            if (child instanceof LytParagraph par) {
                Integer idx = nodeToIndex.get(child);
                if (idx != null) {
                    out.add(idx);
                }
                // Inline blocks hoist to this (grand)parent's child list right
                // after their paragraph, as absolute out-of-flow nodes.
                for (LytBlock ib : par.getInlineBlocks()) {
                    Integer ibIdx = nodeToIndex.get(ib);
                    if (ibIdx != null) {
                        out.add(ibIdx);
                    }
                }
                continue;
            }
            if (shouldEliminate(child)) {
                // Skip eliminated wrapper, descend into its children
                collectBlockChildren(child, out);
            } else {
                Integer idx = nodeToIndex.get(child);
                if (idx != null) {
                    out.add(idx);
                }
            }
        }
    }

    /**
     * Returns true when a node should be excluded from the bounds dump JSON:
     * the node is eliminated and its bounds are zero-size (width &lt;= 0 or height &lt;= 0).
     */
    public static boolean shouldSkipInBoundsDump(LytNode node) {
        return shouldEliminate(node) && node.getBounds() != null
            && (node.getBounds().width() <= 0 || node.getBounds().height() <= 0);
    }
}
