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
import com.hfstudio.guidenh.guide.document.block.LytBox;
import com.hfstudio.guidenh.guide.document.block.LytCodeBlockToolbar;
import com.hfstudio.guidenh.guide.document.block.LytDocumentFloat;
import com.hfstudio.guidenh.guide.document.block.LytImage;
import com.hfstudio.guidenh.guide.document.block.LytImageBlock;
import com.hfstudio.guidenh.guide.document.block.LytNode;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.recipes.LytStandardRecipeBox;
import com.hfstudio.guidenh.guide.document.block.table.LytTable;
import com.hfstudio.guidenh.guide.document.block.table.LytTableCell;
import com.hfstudio.guidenh.guide.document.block.table.LytTableRow;
import com.hfstudio.guidenh.guide.document.flow.LytFlowBreak;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
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
     * Absolute-position lowering for {@link LytDocumentFloat} inners and
     * paragraph-inline blocks: inset + size in px relative to the Rust parent's
     * content box. For document floats the parent is the wrapper itself, kept
     * in the tree as an in-flow, zero-height anchor at the float's document
     * slot — the floated block is emitted as {@code position:absolute} with
     * {@code insetTop = 0} (out of flow, exactly the CSS float "zero flow
     * height" semantics) so its vertical position always tracks the Rust flow
     * instead of a replayed Java coordinate.
     */
    private final Map<LytBlock, LayoutStyleExtractor.FloatAbs> absoluteFloats = new IdentityHashMap<>();
    /**
     * Lane pinning for float-adjacent blocks: margin-left displacement +
     * explicit width in px, taken from the Java layout (the float registry is
     * order-dependent, so the compiler replays it while flattening).
     */
    private final Map<LytBlock, LayoutStyleExtractor.LanePin> lanePins = new IdentityHashMap<>();
    /** Flex-grow overrides (e.g. the code toolbar's language label). */
    private final Map<LytBlock, Float> flexGrowOverrides = new IdentityHashMap<>();
    /** Extra margin-left for a specific child (e.g. the recipe output slot's arrow gap). */
    private final Map<LytBlock, Integer> marginLeftAdjust = new IdentityHashMap<>();
    /** Float-wrap forbidden intervals for text paragraphs (per-line clip query). */
    private final Map<LytBlock, List<LayoutNodeSerializer.FloatClipSpec>> floatClips = new IdentityHashMap<>();
    /**
     * Clear-break bridge: a paragraph containing a {@code <br clear="..."/>
     * } has
     * its Java flow height EXTENDED below the cleared floats (LineBuilder jumps
     * lineBoxY), but the break contributes no text, so Rust measures only the
     * text lines and the following blocks would stack too high — into the
     * float's zone. The Java height is bridged as {@code min_h} so Taffy
     * reserves the space.
     */
    private final Map<LytBlock, Integer> clearMinHeights = new IdentityHashMap<>();
    /** Float rects registered so far in document order (document coordinates + side). */
    private final List<FloatRect> floatRects = new ArrayList<>();
    /** Available layout width of the current serialize() call (for lane computation). */
    private float availWidth;

    record FloatRect(LytRect rect, boolean right) {}

    /**
     * A float wrapper (already emitted as an in-flow, zero-height anchor node)
     * whose first real block descendant must become its absolutely-positioned
     * child. {@code laneW} is the content width of the anchor's flattened
     * parent — the anchor stretches to it, so the right-float inset is
     * {@code laneW - innerWidth}.
     */
    record PendingFloat(LytDocumentFloat df, int laneW) {}

    public byte[] serialize(LytNode root, float availWidth, float visualScale, float renderScale) {
        flatNodes.clear();
        nodeToIndex.clear();
        marginOffsets.clear();
        absoluteFloats.clear();
        lanePins.clear();
        flexGrowOverrides.clear();
        marginLeftAdjust.clear();
        floatClips.clear();
        clearMinHeights.clear();
        floatRects.clear();
        this.availWidth = availWidth;

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
            if (childIndices.isEmpty() && LayoutNodeSerializer.resolveNodeType(block) != 1) {
                // Rust cannot measure leaf containers (they would come back
                // zero-sized); reserve the box the Java layout pass computed.
                // Text (node_type 1) is excluded — a paragraph's box must come
                // from Rust text measurement so the glyph runs stay consistent.
                flags |= LayoutStyleExtractor.Flags.SIZE_FROM_JAVA_BOUNDS;
            }
            var adj = new LayoutStyleExtractor.NodeAdjustments(
                (int) mo.top(),
                (int) mo.right(),
                (int) mo.bottom(),
                (int) mo.left() + marginLeftAdjust.getOrDefault(block, 0),
                absoluteFloats.get(block),
                lanePins.get(block),
                flexGrowOverrides.getOrDefault(block, 0f),
                clearMinHeights.getOrDefault(block, 0));
            int styleOff = LayoutStyleExtractor.build(fbb, block, flags, adj);
            List<LayoutNodeSerializer.InlineRef> inlineRefs = new ArrayList<>();
            if (block instanceof LytParagraph par) {
                for (LytBlock ib : par.getInlineBlocks()) {
                    int ibIdx = getFlatIndex(ib);
                    if (ibIdx >= 0) {
                        inlineRefs.add(inlineRefOf(ibIdx, ib));
                    }
                }
            }
            nodeOffsets[i] = LayoutNodeSerializer
                .build(fbb, block, styleOff, childIndices, inlineRefs, floatClips.getOrDefault(block, List.of()));
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
     * Vertical alignment request for one inline block (see InlineBlockRef in the
     * schema): LaTeX formulas align their math baseline (param = ascent above the
     * text baseline), inline item icons center on the line plus their configured
     * y offset, everything else sits with its bottom 2px below the baseline.
     */
    private static LayoutNodeSerializer.InlineRef inlineRefOf(int flatIndex, LytBlock ib) {
        if (ib instanceof com.hfstudio.guidenh.guide.document.block.LytLatexBlock latex) {
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
        // Document content origin/width: Java pads the document by 5 (matches
        // the synthetic root padding in layout.rs).
        flattenTree(node, MarginAccum.ZERO, null, 5, 5, Math.max(1, Math.round(availWidth) - 10));
    }

    /**
     * @param inherited          margins accumulated from eliminated ancestors
     * @param pendingFloat       a float wrapper's anchor node was just emitted
     *                           up-stack; the first non-eliminated block descendant
     *                           becomes its absolutely-positioned child
     * @param parentContentX/Y/W content origin and width of the nearest flattened
     *                           ancestor (for lane computation and absolute insets)
     */
    private void flattenTree(LytNode node, MarginAccum inherited, @Nullable PendingFloat pendingFloat,
        int parentContentX, int parentContentY, int parentContentW) {
        if (shouldEliminate(node)) {
            // Add this node's margins to the inherited accumulator
            MarginAccum total = inherited;
            if (node instanceof LytBlock elided) {
                total = total.add(elided);
            }
            for (LytNode child : node.getChildren()) {
                flattenTree(child, total, pendingFloat, parentContentX, parentContentY, parentContentW);
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
            if (block instanceof LytDocumentFloat df) {
                // The float wrapper is NOT eliminated: it stays in the tree as
                // an in-flow, zero-height ANCHOR (its own Java flow rect is
                // (x, y, 0, 0)). Its position comes from the Rust flow, so the
                // float can never detach from its document slot — replaying the
                // Java-computed float y as a root-relative absolute inset broke
                // whenever the two engines' flow cursors disagreed (margin
                // collapse, line-height rounding), sliding floats up over
                // earlier content. The inner becomes the anchor's absolute
                // child below.
                //
                // Register the rect for the lane/clip replay (Java geometry).
                // LytDocumentFloat.getBounds() returns the INNER's bounds; the
                // registered rect mirrors the Java LayoutContext registration,
                // which extends the rect by FLOAT_GAP (on the left for right
                // floats, on the right for left floats).
                LytRect inner = df.getBounds();
                int gap = LytDocumentFloat.FLOAT_GAP;
                LytRect frect = df.isFloatRight()
                    ? new LytRect(inner.x() - gap, inner.y(), inner.width() + gap, inner.height() + gap)
                    : new LytRect(inner.x(), inner.y(), inner.width() + gap, inner.height() + gap);
                floatRects.add(new FloatRect(frect, df.isFloatRight()));
                pendingFloat = new PendingFloat(df, parentContentW);
            } else if (pendingFloat != null) {
                // The float's inner: absolute child of the anchor, pinned at
                // its near top corner (left edge for left floats, right edge
                // for right floats). Top is 0 — the anchor already sits at the
                // float's flow slot.
                LytRect fb = block.getFlowBounds();
                int insetL = pendingFloat.df()
                    .isFloatRight() ? Math.max(0, pendingFloat.laneW() - fb.width()) : 0;
                absoluteFloats.put(block, new LayoutStyleExtractor.FloatAbs(insetL, 0, fb.width(), fb.height()));
                pendingFloat = null;
            } else {
                pinLaneIfFloatAdjacent(block, parentContentX, parentContentW);
            }
            applyChildRules(block);
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
                        lanePins.put(cell, new LayoutStyleExtractor.LanePin(0, column.getWidth()));
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
                    LytRect fb = ib.getFlowBounds();
                    int vw = fb.width();
                    int vh = fb.height();
                    if (ib instanceof com.hfstudio.guidenh.guide.document.block.LytLatexBlock latex
                        && latex.getFormulaDisplayW() > 0) {
                        vw = latex.getFormulaDisplayW();
                        vh = latex.getFormulaDisplayH();
                    }
                    absoluteFloats.put(
                        ib,
                        new LayoutStyleExtractor.FloatAbs(
                            fb.x() - contentOriginX(par),
                            fb.y() - contentOriginY(par),
                            vw,
                            vh));
                }
            }
            if (block instanceof LytParagraph par && hasClearBreak(par)) {
                clearMinHeights.put(
                    par,
                    par.getFlowBounds()
                        .height());
            }
            // This block becomes the nearest flattened ancestor for its children.
            parentContentX = contentOriginX(block);
            parentContentY = contentOriginY(block);
            parentContentW = contentWidth(block);
        }

        for (LytNode child : node.getChildren()) {
            flattenTree(child, MarginAccum.ZERO, pendingFloat, parentContentX, parentContentY, parentContentW);
        }
    }

    /**
     * Float lane computation, replayed from the Java layout results.
     * <p>
     * <b>Text paragraphs without inline blocks</b> get one {@code FloatClip}
     * per vertically-overlapping float (paragraph-relative forbidden
     * interval): the Rust line breaker subtracts clips from the available
     * width per line — true CSS float wrapping, no lane pinning, no band
     * pre-splitting.
     * <b>Paragraphs with inline blocks</b> keep the whole-lane pin (grown
     * lines would invalidate the clip's y math), and <b>non-text blocks</b>
     * are only pinned when their Java bounds actually abut the float lane
     * (edge-abutment detection; the registered float rect already includes
     * FLOAT_GAP) — children of flex rows and naturally narrow blocks pass
     * through unpinned.
     */
    private void pinLaneIfFloatAdjacent(LytBlock block, int parentContentX, int parentContentW) {
        if (floatRects.isEmpty()) return;
        LytRect b = block.getFlowBounds();
        int naturalX = parentContentX + block.getMarginLeft();
        int naturalRight = parentContentX + parentContentW - block.getMarginRight();

        boolean isPlainText = LayoutNodeSerializer.resolveNodeType(block) == 1 && block instanceof LytParagraph par
            && par.getInlineBlocks()
                .isEmpty();
        if (isPlainText) {
            List<LayoutNodeSerializer.FloatClipSpec> clips = null;
            for (FloatRect fr : floatRects) {
                LytRect f = fr.rect();
                if (f.bottom() <= b.y() || f.y() >= b.bottom()) continue;
                int yTop = Math.max(f.y(), b.y()) - b.y();
                int yBottom = Math.min(f.bottom(), b.bottom()) - b.y();
                LayoutNodeSerializer.FloatClipSpec c = fr.right()
                    // Right float: forbidden interval starts at the registered
                    // rect's left edge — that edge already carries FLOAT_GAP,
                    // so text wraps FLOAT_GAP px clear of the float's content
                    // (mirrors the legacy getRightFloatLeftEdge() wrap edge).
                    ? new LayoutNodeSerializer.FloatClipSpec(
                        yTop,
                        yBottom,
                        f.x() - b.x(),
                        Math.max(1, b.right() - f.x()))
                    // Left float: forbidden interval [0, lane left).
                    : new LayoutNodeSerializer.FloatClipSpec(yTop, yBottom, 0, f.right() - b.x());
                if (clips == null) clips = new ArrayList<>();
                clips.add(c);
            }
            if (clips != null) floatClips.put(block, clips);
            return;
        }

        for (FloatRect fr : floatRects) {
            LytRect f = fr.rect();
            if (f.bottom() <= b.y() || f.y() >= b.bottom()) continue; // no vertical overlap
            boolean isText = LayoutNodeSerializer.resolveNodeType(block) == 1;
            if (fr.right()) {
                // Right float: the registered rect's left edge already carries
                // FLOAT_GAP, so the lane ends there — text stays FLOAT_GAP px
                // clear of the float's content edge.
                int laneRight = f.x();
                int laneWidth = Math.max(1, laneRight - naturalX);
                if (isText) {
                    if (laneRight < naturalRight) {
                        lanePins.put(block, new LayoutStyleExtractor.LanePin(0, laneWidth));
                        return;
                    }
                }
                if (!isText && b.x() == naturalX && Math.abs(b.right() - laneRight) <= 1) {
                    lanePins.put(block, new LayoutStyleExtractor.LanePin(0, b.width()));
                    return;
                }
            } else {
                // Left float: the lane starts at the registered rect's right
                // edge (content + gap).
                int laneLeft = f.right();
                int laneWidth = Math.max(1, naturalRight - laneLeft);
                if (isText) {
                    if (laneLeft > naturalX) {
                        lanePins.put(block, new LayoutStyleExtractor.LanePin(laneLeft - naturalX, laneWidth));
                        return;
                    }
                }
                if (!isText && b.x() != naturalX && Math.abs(b.x() - laneLeft) <= 1) {
                    lanePins.put(block, new LayoutStyleExtractor.LanePin(b.x() - naturalX, b.width()));
                    return;
                }
            }
        }
    }

    /**
     * Does this paragraph's flow content contain a float-clearing break
     * ({@code <br clear="left|right|all"/>
     * })? Mirrors the recursive span walk
     * of {@code LayoutNodeSerializer.hasFloatAlignedInlineBlock}.
     */
    private static boolean hasClearBreak(LytParagraph par) {
        for (LytFlowContent fc : par.getContent()) {
            if (hasClearBreak(fc)) return true;
        }
        return false;
    }

    private static boolean hasClearBreak(LytFlowContent fc) {
        if (fc instanceof LytFlowBreak fb && (fb.isClearLeft() || fb.isClearRight())) {
            return true;
        }
        if (fc instanceof LytFlowSpan fs) {
            for (LytFlowContent child : fs.getChildren()) {
                if (hasClearBreak(child)) return true;
            }
        }
        return false;
    }

    /** Per-parent lowering rules for specific children (margins, flex-grow). */
    private void applyChildRules(LytBlock block) {
        var kids = block.getChildren();
        if (kids.isEmpty()) return;
        if (block instanceof LytStandardRecipeBox && kids.get(kids.size() - 1) instanceof LytBlock last) {
            // The crafting arrow sits between the inputs grid and the output
            // slot: reserve GAP + arrow width + GAP as the output's margin-left.
            marginLeftAdjust.put(last, LytStandardRecipeBox.GAP * 2 + LytStandardRecipeBox.ARROW_W);
        }
        if (block instanceof LytCodeBlockToolbar && kids.get(0) instanceof LytBlock first) {
            // The language label takes the remaining width; buttons stay fixed.
            flexGrowOverrides.put(first, 1.0f);
        }
    }

    private static int contentOriginX(LytBlock block) {
        LytRect b = block.getFlowBounds();
        int padL = block instanceof LytBox box ? LayoutStyleExtractor.readLytBoxPadding(box, "paddingLeft") : 0;
        return b.x() + padL
            + block.getBorderLeft()
                .width();
    }

    private static int contentOriginY(LytBlock block) {
        LytRect b = block.getFlowBounds();
        int padT = block instanceof LytBox box ? LayoutStyleExtractor.readLytBoxPadding(box, "paddingTop") : 0;
        return b.y() + padT
            + block.getBorderTop()
                .width();
    }

    private static int contentWidth(LytBlock block) {
        LytRect b = block.getFlowBounds();
        int padL = block instanceof LytBox box ? LayoutStyleExtractor.readLytBoxPadding(box, "paddingLeft") : 0;
        int padR = block instanceof LytBox box ? LayoutStyleExtractor.readLytBoxPadding(box, "paddingRight") : 0;
        return Math.max(
            1,
            b.width() - padL
                - padR
                - block.getBorderLeft()
                    .width()
                - block.getBorderRight()
                    .width());
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
        // Blocks that are layout wrappers — eliminated in tree. Note that
        // LytDocumentFloat is deliberately NOT eliminated: it becomes the
        // in-flow zero-height anchor for its floated child (see flattenTree).
        if (node instanceof LytAlignedBlock) {
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
