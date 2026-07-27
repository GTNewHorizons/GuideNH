package com.hfstudio.guidenh.guide.layout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.flatbuffers.FlatBufferBuilder;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.LytAlignedBlock;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytGuiSprite;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
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

    record FloatRect(LytRect rect, boolean right) {}

    public byte[] serialize(LytNode root, float availWidth, float visualScale, float renderScale) {
        flatNodes.clear();
        nodeToIndex.clear();
        marginOffsets.clear();
        absoluteFloats.clear();
        floatIntents.clear();
        columnWidths.clear();
        floatRects.clear();

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
            if (childIndices.isEmpty() && nodeType != 1 && nodeType != 2 && nodeType != 3 && nodeType != 4
                && nodeType != 8 && nodeType != 20 && nodeType != 21 && nodeType != 22
                && !(block instanceof LytGuiSprite)) {
                // Opaque leaf containers (charts, scenes, etc.) have no Rust
                // measure function — reserve the box Java computed. Types with
                // Rust measure (Text=1, Image=2, Slot=3, Break=4, Latex=8,
                // RecipeBox=20, PieChart=21) and LytGuiSprite (size from sprite
                // UV constants) are excluded: Rust sizes them from declared
                // content facts.
                flags |= LayoutStyleExtractor.Flags.SIZE_FROM_JAVA_BOUNDS;
            }
            var adj = new LayoutStyleExtractor.NodeAdjustments(
                (int) mo.top(),
                (int) mo.right(),
                (int) mo.bottom(),
                (int) mo.left(),
                absoluteFloats.get(block),
                floatIntents.getOrDefault(block, 0),
                columnWidths.getOrDefault(block, 0));
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
        byte justify = (byte) com.hfstudio.guidenh.guide.style.token.GuideThemeManager.instance()
            .active()
            .int_(TEXT_JUSTIFY)
            .value();
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
        flattenTree(node, MarginAccum.ZERO, 0);
    }

    /**
     * @param inherited    margins accumulated from eliminated ancestors
     * @param pendingFloat a float wrapper's anchor node was just emitted
     *                     up-stack; the first non-eliminated block descendant
     *                     becomes its absolutely-positioned child
     */
    private void flattenTree(LytNode node, MarginAccum inherited, int pendingFloatSide) {
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
            for (LytNode child : node.getChildren()) {
                flattenTree(child, total, childSide);
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
            if (LayoutNodeSerializer.isOpaqueSubtree(block)) {
                // Opaque subtree: children keep the Java layout and are not
                // serialized (e.g. LytFileTree's icon+payload rows).
                return;
            }
            if (pendingFloatSide != 0) {
                // The float wrapper was eliminated up-stack; this inner becomes
                // a real CSS float for the Rust pusher (the float gap is added
                // to its margin during style extraction).
                floatIntents.put(block, pendingFloatSide);
                pendingFloatSide = 0;
            }
            if (block instanceof LytTable table) {
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
                    } else if (ib instanceof LytImage image && image.getExplicitWidth() > 0
                        && image.getExplicitHeight() > 0) {
                        vw = image.getExplicitWidth();
                        vh = image.getExplicitHeight();
                    } else if (ib instanceof LytImageBlock imb && imb.getExplicitWidth() > 0
                        && imb.getExplicitHeight() > 0) {
                        vw = imb.getExplicitWidth();
                        vh = imb.getExplicitHeight();
                    } else {
                        LytRect b = ib.getBounds();
                        vw = b != null ? b.width() : 0;
                        vh = b != null ? b.height() : 0;
                    }
                    absoluteFloats.put(ib, new LayoutStyleExtractor.FloatAbs(vw, vh));
                }
            }
        }

        for (LytNode child : node.getChildren()) {
            flattenTree(child, MarginAccum.ZERO, pendingFloatSide);
        }
    }

    private boolean shouldEliminate(LytNode node) {
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
}
