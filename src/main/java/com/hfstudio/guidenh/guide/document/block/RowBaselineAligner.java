package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.render.GlyphRunHolder;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;

/**
 * Post-layout baseline alignment for BASELINE-aligned row containers.
 *
 * <p>
 * taffy 0.12.1 reports no leaf baselines ({@code first_baselines} is always
 * NONE), so {@code AlignItems::BASELINE} degrades to bottom-edge alignment.
 * This pass runs on the Java side AFTER the taffy writeback and glyph-run
 * injection in {@link LytDocument#createLayout}: for every row container whose
 * {@code alignItems == BASELINE}, each direct child's
 * <em>first-line text baseline</em> is aggregated from its per-glyph
 * baselines ({@link com.hfstudio.guidenh.guide.render.GuideRenderPrimitive.PlacedGlyph#baseline()},
 * {@code lineIndex() == 0}) and the children are translated vertically so all
 * baselines land on one line.
 *
 * <p>
 * <b>Anchor rule (CSS flexbox baseline semantics)</b>: the flex line's
 * baseline sits at {@code rowTop + max(ascent_i + marginTop_i)} where
 * {@code ascent_i = baseline_i - bounds.y_i} is the distance from the child's
 * content-box top to its text baseline. The child whose
 * {@code ascent + marginTop} is largest keeps its cross-start (margin-top)
 * edge on the line's cross-start and every other child is shifted DOWN to meet
 * the shared baseline (never up relative to its own margin edge, so nothing
 * overflows the line's top). Children without any text (images and scenes,
 * CSS replaced elements) use their bottom edge as the baseline, which keeps
 * image-only BASELINE rows exactly where taffy already put them.
 *
 * <p>
 * <b>Movement mechanism</b>: translation goes through
 * {@link LytBlock#moveLayoutPos}, the same path used by scroll replay and
 * smooth scrolling, so {@code onLayoutMoved} shifts glyph quads AND their
 * baselines together (baseline - y stays invariant; see
 * LytParagraph#onLayoutMoved).
 *
 * <p>
 * <b>Zero-behaviour contract</b>: the pass is a pure walk that only mutates
 * when a {@code BASELINE} row container is found. Non-BASELINE containers
 * (and documents without any BASELINE row) are byte-identical to the taffy
 * writeback. The second-pass geometry intentionally deviates from the raw
 * taffy output for BASELINE containers only; that deviation is recorded in the
 * fixture assertions through the documented bounds exemption protocol.
 *
 * <p>
 * <b>Known limitation</b> (in scope for this wave): the per-glyph baseline
 * is the line's baseline, and the Rust engine emits baseline for every line of
 * every glyph, but the aggregation here uses {@code lineIndex() == 0}, the
 * child's <em>first</em> line. Multi-line child blocks whose later-line
 * baselines matter (true multi-line baseline alignment) are out of scope; row
 * container children are single-line in practice.
 */
public final class RowBaselineAligner {

    private RowBaselineAligner() {}

    /**
     * Align the children of every {@code alignItems=BASELINE} row container in
     * the subtree. Document-order walk: an outer BASELINE row aligns (and thus
     * translates) a nested BASELINE row before the nested one aligns its own
     * children relative to its already-final bounds.
     *
     * @param root the document root (or any subtree root)
     */
    public static void align(LytNode root) {
        for (var child : root.getChildren()) {
            if (child instanceof LytBlock block) {
                alignBlock(block);
            }
        }
    }

    private static void alignBlock(LytBlock block) {
        if (block instanceof LytHBox hbox && hbox.getAlignItems() == AlignItems.BASELINE) {
            alignRow(hbox);
        }
        for (var child : block.getChildren()) {
            if (child instanceof LytBlock childBlock) {
                alignBlock(childBlock);
            }
        }
    }

    private static void alignRow(LytHBox row) {
        LytRect rowBounds = row.getBounds();
        if (rowBounds == null || rowBounds.height() <= 0) {
            return;
        }
        List<LytBlock> children = new ArrayList<>();
        for (var child : row.getChildren()) {
            if (child instanceof LytBlock block) {
                children.add(block);
            }
        }
        if (children.isEmpty()) {
            return;
        }
        // Wrapped flex rows put each flex line's first item back at the row's
        // cross-start (x resets), so non-monotonic x identifies a multi-line
        // row. Baseline alignment is a per-LINE operation; this wave only
        // handles single-line rows (row children are single-line in practice),
        // so a wrapped row is skipped untouched.
        for (int i = 1; i < children.size(); i++) {
            LytRect prev = children.get(i - 1)
                .getBounds();
            LytRect cur = children.get(i)
                .getBounds();
            if (prev == null || cur == null) {
                continue;
            }
            if (cur.x() <= prev.x()) {
                GuideDebugLog.warnAlways(
                    "Layout: baseline row at ({}) wrapped to multiple lines - skipped (single-line scope only)",
                    rowBounds.x() + " " + rowBounds.y());
                return;
            }
        }

        // Baseline per child: first-line text baseline when the subtree shapes
        // text, otherwise the box bottom edge (CSS replaced-element baseline).
        // ascentFromCrossStart = baseline - (contentTop - marginTop): the
        // distance from the child's margin-box top to its baseline; this is
        // what the flex line's baseline position maximizes (CSS flexbox).
        float[] baselines = new float[children.size()];
        float[] ascents = new float[children.size()];
        float maxAscent = 0f;
        for (int i = 0; i < children.size(); i++) {
            LytBlock child = children.get(i);
            LytRect cb = child.getBounds();
            if (cb == null || cb.height() <= 0) {
                // Degenerate (empty / zero-height) child: no measurable
                // baseline, leave it untouched and out of the anchor race.
                ascents[i] = Float.NEGATIVE_INFINITY;
                baselines[i] = cb != null ? cb.y() : rowBounds.y();
                continue;
            }
            Float textBaseline = firstLineBaseline(child);
            float base = textBaseline != null ? textBaseline : cb.bottom();
            baselines[i] = base;
            ascents[i] = (base - cb.y()) + child.getMarginTop();
            if (ascents[i] > maxAscent) {
                maxAscent = ascents[i];
            }
        }

        if (!(maxAscent > 0f)) {
            return; // nothing has a measurable baseline, leave the row untouched
        }
        float target = rowBounds.y() + maxAscent;

        StringBuilder log = null;
        for (int i = 0; i < children.size(); i++) {
            if (!(ascents[i] > Float.NEGATIVE_INFINITY)) {
                continue; // degenerate child skipped
            }
            float delta = target - baselines[i];
            int deltaY = Math.round(delta);
            if (deltaY != 0) {
                children.get(i)
                    .moveLayoutPos(0, deltaY);
            }
            if (log == null) {
                log = new StringBuilder();
            }
            log.append(
                children.get(i)
                    .getClass()
                    .getSimpleName())
                .append(" baseline=")
                .append(Math.round(baselines[i] * 100) / 100.0)
                .append(" ascent+margin=")
                .append(Math.round(ascents[i] * 100) / 100.0)
                .append(" delta=")
                .append(deltaY)
                .append(' ');
        }
        // Evidence trail for the visual bounds ratchet: only BASELINE rows reach
        // this line, so the log volume is zero for the default corpus.
        GuideDebugLog.warnAlways(
            "Layout: baseline row ({} {}) targetBaseline={} maxAscent={} -> {}",
            rowBounds.x(),
            rowBounds.y(),
            Math.round(target * 100) / 100.0,
            Math.round(maxAscent * 100) / 100.0,
            log != null ? log.toString() : "(no translation)");
    }

    /**
     * First-line text baseline of {@code block}'s subtree, in document
     * coordinates. Walks holder glyphs first (lineIndex == 0), then recurses
     * into children in document order. Returns {@code null} when the subtree
     * contains no shaped text.
     */
    @Nullable
    private static Float firstLineBaseline(LytBlock block) {
        if (block instanceof GlyphRunHolder holder) {
            var data = holder.getGlyphData();
            if (data != null) {
                for (var group : data.runs()) {
                    for (var glyph : group.glyphs()) {
                        if (glyph.lineIndex() == 0) {
                            // All glyphs of one line share the same baseline.
                            return glyph.baseline();
                        }
                    }
                }
            }
        }
        for (var child : block.getChildren()) {
            if (child instanceof LytBlock childBlock) {
                Float baseline = firstLineBaseline(childBlock);
                if (baseline != null) {
                    return baseline;
                }
            }
        }
        return null;
    }
}
