use std::collections::HashMap;

use crate::fb::{
    DecorationRect, DecorationRectArgs, FlatLayout, FlatLayoutArgs, FlatNode, GlyphBitmap,
    GlyphBitmapArgs, GlyphRun, GlyphRunArgs, LayoutInput, LayoutResult, LayoutResultArgs,
    PlacedGlyph, PlacedGlyphArgs,
};
use crate::measure::{create_measure_closure, measure_text, GlyphAccum, NodeContext};
use crate::parley_text::FloatRect;
use crate::style_convert::flat_style_to_taffy;
use crate::text::GuideFontSystem;
use flatbuffers::FlatBufferBuilder;
use taffy::prelude::*;

/// Document content-box padding, matching the legacy synthetic root and the
/// Java document padding (14px each side).
const CONTENT_PAD: f32 = 14.0;

/// The synthetic shear coefficient K (GLYPH_SHEAR_K, now render-only on the Java
/// side). Java no longer declares it, since
/// the LayoutInput.shear_k wire field is DEPRECATED with its slot retained and a
/// default of 0.25, so the Rust side owns the K value for that boundary correction end
/// to end and hardens it into this constant. Its only consumer is the italic-to-upright
/// boundary correction (apply_italic_boundary_correction /
/// apply_italic_atom_boundary_correction); the draw-time slant remains the Java
/// renderer's concern (GuideRenderEngine.SHEAR_K holds the same value, 0.25).
const SHEAR_K: f32 = 0.25;

/// Compute the available horizontal lane (absolute x and width) for a block
/// at the given absolute Y, consulting the float table.  If floats fully
/// block the lane at start_y, push down past the nearest blocking float's
/// bottom and retry.  Returns (lane_x, lane_width, adjusted_y).
///
/// Mirrors the Java LytFloatAwareBlock.computeLayout loop:
/// query left/right floats at the current Y; if the resulting lane has
/// positive width, use it; otherwise find the next float bottom below and
/// skip to it.
fn compute_lane(
    start_y: f32,
    content_x: f32,
    content_w: f32,
    float_table: &[FloatRect],
) -> (f32, f32, f32) {
    let mut lane_y = start_y;
    loop {
        let mut x0 = content_x;
        let mut x1 = content_x + content_w;
        let mut max_bottom: Option<f32> = None;
        for f in float_table {
            if f.y + f.h <= lane_y || f.y >= lane_y + 1.0 {
                continue;
            }
            if f.right {
                x1 = x1.min(f.x);
            } else {
                x0 = x0.max(f.x + f.w);
            }
            let b = f.y + f.h;
            max_bottom = Some(max_bottom.map_or(b, |p| p.max(b)));
        }
        let lane_w = x1 - x0;
        if lane_w > 0.0 {
            return (x0, lane_w, lane_y);
        }
        // Fully blocked: jump below the blocking floats.
        match max_bottom {
            Some(b) => lane_y = b,
            None => return (content_x, content_w, lane_y),
        }
    }
}

/// Document-flow pusher (v1). Owns the single authoritative float table and
/// drives the top-level sequence in document order: top-level paragraphs are
/// shaped directly against the live table (real per-line wrapping: the
/// "bridge" is this in-process query, no precomputed clip table crosses any
/// boundary); top-level floats register into the table at the current cursor
/// without advancing it; top-level blocks are laid out as taffy subtrees.
/// Nested paragraphs inside those subtrees still wrap at full width (the
/// pusher does not yet recurse into block float contexts, which is transitional).
pub fn compute_layout(
    input_bytes: &[u8],
    font_system: &mut GuideFontSystem,
) -> Vec<u8> {
    let input = flatbuffers::root::<LayoutInput>(input_bytes)
        .expect("Invalid LayoutInput FlatBuffer");

    let avail_width = input.available_width();
    // NB: visual_scale is intentionally NOT applied to the root width,
    // Java blocks already pre-apply it per block (ResponsiveVisualSizing).
    let visual_scale = input.visual_scale();
    // Display pixel ratio (MC guiScale): glyph bitmaps are rasterized at
    // font_size * render_scale so 1 texel maps to 1 physical pixel; quad
    // coordinates are then divided back into document units.
    let render_scale = input.render_scale().max(0.25);
    // K no longer travels the wire: LayoutInput.shear_k is DEPRECATED and
    // Java stopped declaring it, so Rust owns it end to end (see the SHEAR_K constant).
    // Consumed only by the italic→upright boundary correction; the draw-time slant itself
    // stays a Java renderer concern.
    let shear_k = SHEAR_K;
    let fb_nodes = input.nodes();
    let justify = input.justify() != 0;

    let flat_nodes: Vec<FlatNode> = fb_nodes
        .map_or_else(Vec::new, |v| (0..v.len()).map(|i| v.get(i)).collect());

    let content_x = CONTENT_PAD;
    let content_y = CONTENT_PAD;
    let content_w = (avail_width - 2.0 * CONTENT_PAD).max(1.0);

    // Document-top sequence = flat nodes not claimed as any container's child
    // (the orphans the legacy synthetic root adopted; the pusher drives them).
    let mut claimed = vec![false; flat_nodes.len()];
    for fb in flat_nodes.iter() {
        if let Some(ch) = fb.children() {
            for ci in ch.iter() {
                claimed[ci as usize] = true;
            }
        }
    }
    let top_seq: Vec<usize> = (0..flat_nodes.len()).filter(|i| !claimed[*i]).collect();

    let mut taffy: TaffyTree<NodeContext> = TaffyTree::new();
    let mut abs_positions: Vec<(f32, f32)> = vec![(0.0, 0.0); flat_nodes.len()];
    let mut sizes: Vec<(f32, f32)> = vec![(0.0, 0.0); flat_nodes.len()];
    let mut glyph_acc: HashMap<usize, GlyphAccum> = HashMap::new();
    let mut float_table: Vec<FloatRect> = Vec::new();
    let mut cursor: f32 = 0.0;

    for &idx in &top_seq {
        let fb = &flat_nodes[idx];
        let (ml, mt, mr, mb, pos_abs, float_side, node_type) = match fb.style() {
            Some(s) => (
                s.margin_left(),
                s.margin_top(),
                s.margin_right(),
                s.margin_bottom(),
                s.position() == 1,
                s.float(),
                fb.node_type(),
            ),
            None => (0.0, 0.0, 0.0, 0.0, false, 0, fb.node_type()),
        };

        if pos_abs {
            // Inline block: position is assigned later by the inline post-pass;
            // only its subtree size is needed here.
            let (w, h, _sub) = build_subtree(
                &mut taffy,
                idx,
                &flat_nodes,
                font_system,
                &mut glyph_acc,
                justify,
                visual_scale,
                &mut abs_positions,
                &mut sizes,
                0.0,
                0.0,
                None,
            );
            sizes[idx] = (w, h);
            abs_positions[idx] = (0.0, 0.0);
            continue;
        }

        if float_side == 1 || float_side == 2 {
            let right = float_side == 2;
            let (w, h, sub) = build_subtree(
                &mut taffy,
                idx,
                &flat_nodes,
                font_system,
                &mut glyph_acc,
                justify,
                visual_scale,
                &mut abs_positions,
                &mut sizes,
                0.0,
                0.0,
                None,
            );
            let fy = content_y + cursor;
            let fx = if right {
                content_x + content_w - w
            } else {
                content_x
            };
            for &si in &sub {
                abs_positions[si] = (abs_positions[si].0 + fx, abs_positions[si].1 + fy);
            }
            sizes[idx] = (w, h);
            // The float's gap is expressed as the inner's margin (CSS-correct):
            // the registered rectangle is the margin box, the drawn box is the
            // content box.
            float_table.push(FloatRect {
                x: fx - ml,
                y: fy - mt,
                w: w + ml + mr,
                h: h + mt + mb,
                right,
            });
            // A float does not advance the vertical cursor (zero flow height).
            continue;
        }

        if node_type == 1 {
            // CSS-preposed margin: this paragraph's top margin opens the gap
            // above its box, so the box starts at the already-advanced cursor
            // (mirrors taffy subtrees and the legacy Java pusher).
            cursor += mt;
            let para_abs_y = content_y + cursor;
            let para_x = content_x;
            let avail = Size {
                width: AvailableSpace::Definite(content_w),
                height: AvailableSpace::MaxContent,
            };
            let clears_raw: Vec<(usize, u8)> = flat_nodes[idx]
                .text()
                .and_then(|t| t.clears())
                .map(|v| {
                    v.iter()
                        .map(|c| (c.raw_offset() as usize, c.side() as u8))
                        .collect()
                })
                .unwrap_or_default();
            let (sz, clear_floor) = measure_text(
                font_system,
                &flat_nodes,
                idx,
                &mut glyph_acc,
                avail,
                justify,
                &float_table,
                para_abs_y,
                para_x,
                &clears_raw,
            );
            abs_positions[idx] = (para_x, para_abs_y);
            sizes[idx] = (sz.width, sz.height);
            cursor += sz.height + mb;
            // A trailing in-paragraph clear does not stretch this paragraph's
            // box; it pushes the flow that follows it below the cleared float.
            // Advance the cursor to that floor so the next block (a callout)
            // starts below the float while this paragraph hugs its text.
            if let Some(f) = clear_floor {
                let f_rel = (f - content_y).max(0.0);
                if f_rel > cursor {
                    cursor = f_rel;
                }
            }
            continue;
        }

        // Block container / image / slot / latex / break: compute the
        // horizontal lane from the float table so blocks avoid overlapping
        // with left/right floats (Java LytFloatAwareBlock behavior).
        // CSS-preposed margin: advance past the top margin first, so the lane
        // query and the block box both start at the margin box top.
        cursor += mt;
        let by = content_y + cursor;
        let (lane_x, lane_w, lane_y) =
            compute_lane(by, content_x, content_w, &float_table);
        let (w, h, _sub) = build_subtree(
            &mut taffy,
            idx,
            &flat_nodes,
            font_system,
            &mut glyph_acc,
            justify,
            visual_scale,
            &mut abs_positions,
            &mut sizes,
            lane_x,
            lane_y,
            Some(lane_w),
        );
        sizes[idx] = (w, h);
        if let Some(c) = fb.style().map(|s| s.clear()) {
            if c != 0 {
                let mut cleared: f32 = 0.0;
                for f in &float_table {
                    let side_match =
                        c == 3 || (c == 1 && !f.right) || (c == 2 && f.right);
                    if side_match {
                        cleared = cleared.max(f.y + f.h);
                    }
                }
                let cleared_rel = (cleared - content_y).max(0.0);
                if cleared_rel > cursor {
                    cursor = cleared_rel;
                }
            }
        }
        // Advance cursor past the block. If compute_lane pushed the block
        // down (lane_y > by), account for the gap; if CSS clear already
        // pushed beyond that, respect the clear.
        cursor = (lane_y - content_y).max(cursor) + h + mb;
    }

    // Inline post-pass: anchor inline blocks at their parley InlineBox
    // positions and grow lines vertically per their align modes.
    inline_post_pass(&flat_nodes, &mut glyph_acc, &mut abs_positions, &mut sizes);

    // List-marker pass.
    // Java only declares the data (ListMarkerData text plus style); Rust shapes the
    // marker string through the same shaping pipeline as body text and positions it:
    //   * the vertical baseline is the content first-line baseline, taken from the first
    //     content child that has glyph content, with the fallback described in the
    //     list_marker_pass documentation;
    //   * marker glyphs are appended to the list item node's GlyphAccum as ordinary
    //     glyph quads, travelling the same rasterize/emit path as paragraph glyphs,
    //     with no new primitive type.
    // The horizontal right alignment, with the ink right edge on the list item's own
    // left edge, happens at emit time, because it needs the rasterized ink width.
    // This must run after inline_post_pass, so content paragraph baselines already carry
    // the line growth shift, and before the rasterize pre-pass, so the marker glyphs
    // reach quad_cache.
    let marker_anchors = list_marker_pass(font_system, &flat_nodes, &mut glyph_acc, &abs_positions);

    // Italic→atom boundary pre-pass
    // Rasterize every paragraph once, apply the text boundary correction
    // then the atom boundary correction (which shifts atom abs_positions),
    // and cache the corrected quads for the emit loop below. The pre-pass MUST
    // run before any FlatLayout is emitted: the atom boundary correction shifts
    // the ATOM's layout position, and atom nodes can precede their paragraph in
    // the flat index order (paragraph index < atom index holds for the Java
    // serializer, but the contract must not depend on it).
    let mut quad_cache: HashMap<
        usize,
        (Vec<crate::parley_text::ParleyRasterGlyph>, Vec<(u64, u32, u32, Vec<u8>)>),
    > = HashMap::new();
    for (i, acc) in glyph_acc.iter() {
        if acc.glyphs.is_empty() {
            continue;
        }
        let span_styles = span_style_table(&flat_nodes[*i]);
        let base_italic = flat_nodes[*i]
            .text()
            .and_then(|t| t.style())
            .map(|s| s.italic())
            .unwrap_or(false);
        let (mut quads, new_bitmaps) =
            crate::parley_text::rasterize_out_glyphs(&acc.glyphs, render_scale);
        // Rejection fix: the text boundary pass receives this paragraph's
        // markers, and when an atom lies between an italic and an upright boundary, its
        // pen falling between the two runs, that boundary is skipped so it cannot
        // double-apply alongside the atom correction's pass 2 tail shift of the same
        // upright quads.
        apply_italic_boundary_correction(&mut quads, &acc.markers, &span_styles, base_italic, shear_k);
        apply_italic_atom_boundary_correction(
            &mut quads,
            &acc.markers,
            &flat_nodes[*i],
            &sizes,
            &mut abs_positions,
            &span_styles,
            base_italic,
            shear_k,
        );
        quad_cache.insert(*i, (quads, new_bitmaps));
    }

    // Content height = cursor plus any trailing float that extends below it.
    let mut total_height = content_y + cursor;
    for f in &float_table {
        total_height = total_height.max(f.y + f.h);
    }

    // ── Collect results ──
    let mut fbb = FlatBufferBuilder::with_capacity(4096);
    let mut flat_layout_offsets: Vec<flatbuffers::WIPOffset<FlatLayout>> = Vec::new();
    let mut glyph_run_offsets: Vec<flatbuffers::WIPOffset<GlyphRun>> = Vec::new();
    let mut decoration_offsets: Vec<flatbuffers::WIPOffset<DecorationRect>> = Vec::new();

    let mut bitmap_keys: Vec<u64> = Vec::new();
    let mut bitmap_data: Vec<(u32, u32, Vec<u8>)> = Vec::new();
    let mut bitmap_index: std::collections::HashSet<u64> = std::collections::HashSet::new();

    for (i, _fb_node) in flat_nodes.iter().enumerate() {
        let (x, y) = abs_positions[i];
        let (w, h) = sizes[i];

        flat_layout_offsets.push(FlatLayout::create(
            &mut fbb,
            &FlatLayoutArgs {
                x,
                y,
                w,
                h,
                order: 0,
            },
        ));

        if let Some(acc) = glyph_acc.remove(&i) {
            let span_styles = span_style_table(&flat_nodes[i]);
            // Base (span-uncovered) style: a single-style paragraph carries its
            // color AND italic on TextData.style, not on any span. Reads both
            // so GlyphRun.shear falls back to the base italic: a fully-italic
            // paragraph (base style italic, no span override) must shear
            // (re-applied on the Rust side).
            // A list item marker node has no TextData, so its run color and
            // shear come from ListMarkerData.style; Java declares COL_MARKER gray with a
            // regular body style.
            let base_style = flat_nodes[i].text().and_then(|t| t.style());
            let marker_style = flat_nodes[i].list_marker().and_then(|m| m.style());
            let base_color = marker_style
                .map(|s| s.color())
                .or_else(|| base_style.map(|s| s.color()))
                .unwrap_or(0xFFFFFFFF);
            let base_italic = marker_style
                .map(|s| s.italic())
                .or_else(|| base_style.map(|s| s.italic()))
                .unwrap_or(false);
            // The boundary pre-pass already rasterized + applied both boundary
            // corrections (text and atom) to the cached quads.
            let (mut quads, new_bitmaps) = quad_cache.remove(&i).unwrap_or_default();
            // Right-aligned marker placement. The ink right edge lands
            // exactly on this level's text line, whose target is the list item's own
            // left edge (abs_positions[i].x = that nesting level's document text line:
            // L1=14, L2=24, L3=34), so the whole box hangs into the left margin. x is
            // the list item's absolute x and the quads are list-item relative, so a
            // single dx translation brings the ink right edge onto x. The anchor now
            // only supplies run ownership and the vertical baseline (see
            // list_marker_pass) and no longer decides the horizontal position. Only the
            // marker node's quads are shifted, and that node's accumulator holds marker
            // glyphs alone, because a list item is a container with no body text, so
            // content geometry is untouched.
            let marker_anchor = marker_anchors.get(&i).copied();
            if marker_anchor.is_some() {
                let target = x; // the list item's own left edge = this level's text line
                let max_right = quads.iter().map(|q| q.x + q.w).fold(0.0f32, f32::max);
                let dx = target - (x + max_right);
                if dx.abs() > 1e-4 {
                    for q in quads.iter_mut() {
                        q.x += dx;
                    }
                }
            }
            for (key, bw, bh, rgba) in new_bitmaps {
                if bitmap_index.insert(key) {
                    bitmap_keys.push(key);
                    bitmap_data.push((bw, bh, rgba));
                }
            }
            let mut groups: std::collections::BTreeMap<
                u32,
                (f32, Vec<flatbuffers::WIPOffset<PlacedGlyph>>),
            > = Default::default();
            for q in quads {
                let e = groups.entry(q.span_index).or_insert_with(|| (q.baseline, Vec::new()));
                e.1.push(PlacedGlyph::create(
                    &mut fbb,
                    &PlacedGlyphArgs {
                        bitmap_key: q.bitmap_key,
                        x: x + q.x,
                        y: y + q.y,
                        w: q.w,
                        h: q.h,
                        start: 0,
                        end: 0,
                        line_index: q.line_index,
                        // Baseline round-trip, following the
                        // divide-by-scale-then-truncate convention; q.baseline is
                        // already truncated, and adding the node's absolute y gives
                        // document coordinates.
                        baseline: y + q.baseline,
                    },
                ));
            }
            for (si, (run_baseline, placed_offsets)) in groups {
                if placed_offsets.is_empty() {
                    continue;
                }
                let (argb, shear) = span_styles
                    .get(si as usize)
                    .map(|s| (s.color, s.italic))
                    .unwrap_or((base_color, base_italic));
                let glyphs_vec = fbb.create_vector(&placed_offsets);
                // The marker run attaches to the content anchor child
                // (node_index = anchor) rather than to the list item container, because
                // Java's runsByNode only dispatches a glyph run to a GlyphRunHolder
                // (LytParagraph). The content paragraph is that holder, so the marker
                // renders naturally with the quad stream through no special path. The
                // list item container itself is not a holder.
                let run_node_index = marker_anchor.unwrap_or(i) as u32;
                glyph_run_offsets.push(GlyphRun::create(
                    &mut fbb,
                    &GlyphRunArgs {
                        node_index: run_node_index,
                        glyphs: Some(glyphs_vec),
                        argb,
                        // The run-level baseline is the first glyph's
                        // baseline, in absolute document coordinates and following the
                        // divide-by-scale-then-truncate convention, for line
                        // anchoring.
                        baseline: y + run_baseline,
                        shear,
                    },
                ));
            }
            emit_decorations(
                &acc.glyphs,
                &span_styles,
                i as u32,
                x,
                y,
                &mut fbb,
                &mut decoration_offsets,
            );
            // Emit separator-line window (kind=3) for heading paragraphs.
            // The rect spans the full float-compressed line width, not just
            // the glyph extents: Java LytHeading draws the themed separator
            // across this interval.
            if let Some((x_off, line_width)) = acc.last_line_window {
                if flat_nodes[i].text().map(|t| t.separator()).unwrap_or(false) {
                    decoration_offsets.push(DecorationRect::create(
                        &mut fbb,
                        &DecorationRectArgs {
                            node: i as u32,
                            x: x + x_off,
                            y: 0.0,
                            w: line_width,
                            h: 0.0,
                            argb: 0,
                            kind: 3,
                        },
                    ));
                }
            }
        }
    }

    let nodes_vec = fbb.create_vector(&flat_layout_offsets);
    let glyph_runs_vec = fbb.create_vector(&glyph_run_offsets);
    let decorations_vec = fbb.create_vector(&decoration_offsets);

    let mut bitmap_offsets: Vec<flatbuffers::WIPOffset<GlyphBitmap>> = Vec::new();
    for (i, (bw, bh, rgba)) in bitmap_data.iter().enumerate() {
        let rgba_vec = fbb.create_vector(rgba);
        bitmap_offsets.push(GlyphBitmap::create(
            &mut fbb,
            &GlyphBitmapArgs {
                key: bitmap_keys[i],
                w: *bw,
                h: *bh,
                rgba: Some(rgba_vec),
            },
        ));
    }
    let bitmaps_vec = fbb.create_vector(&bitmap_offsets);

    let debug_info_str = fbb.create_string(&format!(
        "total_height={} nodes={} top={} floats={}",
        total_height,
        flat_nodes.len(),
        top_seq.len(),
        float_table.len(),
    ));

    let result = LayoutResult::create(
        &mut fbb,
        &LayoutResultArgs {
            nodes: Some(nodes_vec),
            glyph_runs: Some(glyph_runs_vec),
            bitmaps: Some(bitmaps_vec),
            decorations: Some(decorations_vec),
            content_height: total_height,
            debug_info: Some(debug_info_str),
        },
    );

    fbb.finish(result, None);
    fbb.finished_data().to_vec()
}

/// Lay out one flat node and its descendants as an isolated taffy subtree,
/// returning the node's size and the list of flat indices it covers. Absolute
/// positions are written relative to `(base_x, base_y)`. Paragraphs inside the
/// subtree are measured at full width (transition: the pusher's float context
/// does not yet recurse into block subtrees).
fn build_subtree(
    taffy: &mut TaffyTree<NodeContext>,
    idx: usize,
    flat_nodes: &[FlatNode],
    font_system: &mut GuideFontSystem,
    glyph_acc: &mut HashMap<usize, GlyphAccum>,
    justify: bool,
    visual_scale: f32,
    abs_positions: &mut Vec<(f32, f32)>,
    sizes: &mut Vec<(f32, f32)>,
    base_x: f32,
    base_y: f32,
    known_w: Option<f32>,
) -> (f32, f32, Vec<usize>) {
    let mut node_id_of: Vec<Option<NodeId>> = vec![None; flat_nodes.len()];
    let mut sub: Vec<usize> = Vec::new();

    fn build(
        taffy: &mut TaffyTree<NodeContext>,
        idx: usize,
        flat_nodes: &[FlatNode],
        node_id_of: &mut Vec<Option<NodeId>>,
        sub: &mut Vec<usize>,
    ) -> NodeId {
        sub.push(idx);
        let fb = &flat_nodes[idx];
        let style = fb
            .style()
            .map(|s| flat_style_to_taffy(&s))
            .unwrap_or_default();
        let nt = fb.node_type();
        let has_children = fb.children().map_or(false, |c| !c.is_empty());
        if !has_children || nt == 1 {
            let id = taffy
                .new_leaf_with_context(
                    style,
                    NodeContext {
                        flat_index: idx,
                        node_type: nt as u8,
                    },
                )
                .expect("leaf");
            node_id_of[idx] = Some(id);
            return id;
        }
        let child_idxs: Vec<usize> = fb
            .children()
            .unwrap()
            .iter()
            .map(|ci| ci as usize)
            .collect();
        let child_ids: Vec<NodeId> = child_idxs
            .iter()
            .map(|ci| build(taffy, *ci, flat_nodes, node_id_of, sub))
            .collect();
        let id = taffy
            .new_with_children(style, &child_ids)
            .expect("container");
        if nt == 1 {
            let _ = taffy.set_node_context(
                id,
                Some(NodeContext {
                    flat_index: idx,
                    node_type: nt as u8,
                }),
            );
        }
        node_id_of[idx] = Some(id);
        id
    }

    let root_id = build(taffy, idx, flat_nodes, &mut node_id_of, &mut sub);

    let mut measure_fn = create_measure_closure(font_system, flat_nodes, glyph_acc, justify, visual_scale);
    let avail = Size {
        width: known_w
            .map(AvailableSpace::Definite)
            .unwrap_or(AvailableSpace::MaxContent),
        height: AvailableSpace::MaxContent,
    };
    taffy
        .compute_layout_with_measure(root_id, avail, &mut measure_fn)
        .expect("subtree layout");

    fn read(
        taffy: &TaffyTree<NodeContext>,
        idx: usize,
        flat_nodes: &[FlatNode],
        node_id_of: &[Option<NodeId>],
        abs_positions: &mut Vec<(f32, f32)>,
        sizes: &mut Vec<(f32, f32)>,
        parent_abs: (f32, f32),
    ) {
        let id = node_id_of[idx].expect("node id");
        let l = taffy.layout(id).expect("layout");
        let abs = (parent_abs.0 + l.location.x, parent_abs.1 + l.location.y);
        abs_positions[idx] = abs;
        sizes[idx] = (l.size.width, l.size.height);
        if let Some(ch) = flat_nodes[idx].children() {
            for ci in ch.iter() {
                read(taffy, ci as usize, flat_nodes, node_id_of, abs_positions, sizes, abs);
            }
        }
    }

    let rl = taffy.layout(root_id).expect("root layout");
    let root_abs = (base_x + rl.location.x, base_y + rl.location.y);
    abs_positions[idx] = root_abs;
    sizes[idx] = (rl.size.width, rl.size.height);
    if let Some(ch) = flat_nodes[idx].children() {
        for ci in ch.iter() {
            read(
                taffy,
                ci as usize,
                flat_nodes,
                &node_id_of,
                abs_positions,
                sizes,
                root_abs,
            );
        }
    }

    (rl.size.width, rl.size.height, sub)
}

/// Inline post-pass: for every text node with inline-block markers, anchor
/// each block at its marker and grow the lines vertically per the block's
/// align mode. Parley's InlineBox already accounts block widths in pen
/// positions, so no glyph kerning shifts are needed: only the vertical
/// handling, mirroring the legacy layout's per-line box growth: a line
/// holding blocks grows by the space they need above the baseline and below
/// the line, pushing later lines down (the paragraph's measured height
/// already reserves the total; see measure.rs).
fn inline_post_pass(
    flat_nodes: &[FlatNode],
    glyph_acc: &mut HashMap<usize, GlyphAccum>,
    abs_positions: &mut Vec<(f32, f32)>,
    sizes: &mut Vec<(f32, f32)>,
) {
    use crate::measure::marker_needs;

    for (i, acc) in glyph_acc.iter_mut() {
        if acc.markers.is_empty() && acc.float_anchors.is_empty() {
            continue;
        }
        let refs = match flat_nodes[*i].text().and_then(|t| t.inline_blocks()) {
            Some(v) => v,
            None => continue,
        };
        let (node_x, node_y) = abs_positions[*i];
        let content_w = sizes[*i].0;

        // 1) Per-line growth from regular inline markers.
        let mut by_line: std::collections::BTreeMap<usize, (f32, f32)> = Default::default();
        for (mi, m) in acc.markers.iter().enumerate() {
            if mi >= refs.len() {
                break;
            }
            let r = refs.get(mi);
            let bh = sizes[r.node() as usize].1;
            // The ink-box anchor arm, with the ink bottom edge
            // on the baseline, wins over the align mode; the align=2 centering hack is
            // retired, so leftover values fall through to the default arm.
            let ink_bp = crate::measure::ink_bottom_px(&r, bh);
            let (na, nb) = marker_needs(m, bh, r.align(), r.param(), ink_bp);
            let e = by_line.entry(m.line_index).or_default();
            e.0 = e.0.max(na);
            e.1 = e.1.max(nb);
        }
        let grown: Vec<(usize, f32, f32)> = by_line
            .iter()
            .map(|(l, (na, nb))| (*l, *na, *nb))
            .collect();
        let shift_of = |line: usize| -> f32 {
            let mut s = 0.0;
            for (l, na, nb) in &grown {
                if *l < line {
                    s += na + nb;
                } else {
                    if *l == line {
                        s += na;
                    }
                    break;
                }
            }
            s
        };
        if !grown.is_empty() {
            for g in acc.glyphs.iter_mut() {
                let s = shift_of(g.line_index);
                g.y += s;
            }
            for m in acc.markers.iter_mut() {
                let s = shift_of(m.line_index);
                m.baseline_y += s;
                m.line_top += s;
            }
        }

        // 2) Anchor regular inline blocks per their alignment mode.
        // Markers are paired with refs by document order (both exclude floats).
        let mut reg_mi = 0usize;
        for ri in 0..refs.len() {
            let r = refs.get(ri);
            if r.align() >= 3 {
                continue;
            }
            if reg_mi >= acc.markers.len() {
                break;
            }
            let m = &acc.markers[reg_mi];
            let ci = r.node() as usize;
            let (_, bh) = sizes[ci];
            // The atomic ink box's bottom edge is aligned with
            // the text baseline, that is, the ink bottom edge equals the baseline.
            // The align=2 centering hack is retired, so a leftover align=2 without an
            // ink box falls through to the default arm, where the block bottom sits 2px
            // below the baseline instead of being centered.
            // Review follow-up (yOffset consumption restored): param = yOffset, which
            // inlineYOffsetOverride puts on the wire unchanged through the serializer,
            // takes part in the vertical placement again: block top = baseline -
            // ink_bottom_px + param, or block top = baseline + 2 - bh + param in the
            // default arm. The sign convention matches the retired align=2 centering arm
            // top_off=(line_height-bh)/2+param, so a positive value moves the block down.
            let ink_bp = crate::measure::ink_bottom_px(&r, bh);
            let top = match ink_bp {
                Some(ib) => m.baseline_y - ib + r.param(),
                None => match r.align() {
                    1 => m.baseline_y - r.param(),
                    // Retired: the align=2 centering hack, which used to
                    // center an item icon on the line. Its consumer is gone and the hack
                    // itself, the align=2 arm of marker_needs, is retired, so leftover
                    // align=2 values fall through to the default arm with the block
                    // bottom 2px below the baseline. The review follow-up restored param
                    // (yOffset) consumption in that arm, where block top = baseline + 2 -
                    // bh + param.
                    _ => m.baseline_y + 2.0 - bh + r.param(),
                },
            };
            abs_positions[ci] = (node_x + m.pen_x, node_y + top);
            reg_mi += 1;
        }

        // 3) Anchor float-aligned inline blocks.
        // float_anchors contains (node_index, paragraph-relative-y) in order.
        // Pair with InlineBlockRef entries that have align=3 (float-left) or
        // align=4 (float-right).
        let mut float_i = 0usize;
        for ri in 0..refs.len() {
            let r = refs.get(ri);
            if r.align() < 3 {
                continue;
            }
            if float_i >= acc.float_anchors.len() {
                break;
            }
            let (_, para_rel_y) = acc.float_anchors[float_i];
            let ci = r.node() as usize;
            let (bw, _bh) = sizes[ci];
            // Float at paragraph edge; margins are already in sizes.
            let x = if r.align() == 3 {
                node_x
            } else {
                node_x + content_w - bw
            };
            abs_positions[ci] = (x, node_y + para_rel_y);
            float_i += 1;
        }
    }
}

/// The list item's shared-gutter marker becomes an atom.
///
/// Java declares the data (ListMarkerData text plus style) and Rust computes:
///   * shaping through the same pipeline as body text: parley `layout_styled`
///     without wrapping shapes the marker string;
///   * the vertical baseline equals the content first-line baseline: the first
///     glyph's pen y of the first content child that has glyphs, usually the first
///     paragraph, plus that child's absolute y. This is the same baseline as the body
///     text, since per-span baseline_shift and the inline_post_pass line growth are both
///     already reflected in the final glyph positions;
///   * marker glyphs are appended to that list item node's own GlyphAccum as ordinary
///     glyph quads and travel exactly the same rasterize/emit path as paragraph
///     glyphs, with no new primitive type and no Java special case;
///   * the marker takes no part in wrapping and changes no content-child geometry: it
///     writes no layout quantity beyond sizes/abs_positions, so content block bounds
///     are byte-identical, which is the hard gate.
///
/// The marker glyph's line_index is rewritten to the sentinel `usize::MAX`, because the
/// content paragraph's `firstLineBoundsFromGlyphs` in Java filters glyphs by line 0 to
/// take the first-line bbox and the marker must not pollute its minX/maxY. Rendering
/// and hit-testing do not filter by line, so the marker still draws normally;
/// hit-testing may include the marker, which is registered and gets no special case.
///
/// The horizontal right alignment, with the marker's ink right edge on the list item's
/// own left edge abs_positions[i].x, is done at emit time, because it needs the
/// rasterized ink width; this pass only performs shaping and vertical placement. The
/// anchor supplies only the vertical baseline and this pass's return value, which is
/// the run ownership used at emit.
/// Return value: a map from the marker node's flat index to the content anchor child's
/// flat index, used at emit time for the right-aligned placement and run ownership. The
/// marker run attaches to the content paragraph, and Java's runsByNode dispatches by
/// node_index, where the content paragraph is the GlyphRunHolder, so there is no
/// special path.
fn list_marker_pass(
    font_system: &mut GuideFontSystem,
    flat_nodes: &[FlatNode],
    glyph_acc: &mut HashMap<usize, GlyphAccum>,
    abs_positions: &[(f32, f32)],
) -> HashMap<usize, usize> {
    use crate::parley_text::collect_layout;
    let mut anchors: HashMap<usize, usize> = HashMap::new();
    for (i, fb) in flat_nodes.iter().enumerate() {
        let Some(md) = fb.list_marker() else { continue };
        let text = md.text().unwrap_or("");
        if text.is_empty() {
            continue;
        }
        // The content anchor is the first content child that has glyph content, which
        // gives both the start of the list item's content block and the authoritative
        // first-line baseline. The cascade order, as corrected, exhausts the item's own
        // direct content lines before descending into descendants:
        //   ① a direct child has glyphs, so the first line is a text paragraph and
        //      glyphs[0].y is the first-line baseline;
        //   ② a direct child has inline atoms only, with non-empty markers and empty
        //      glyphs, so the first line is a pure atom line and its
        //      markers[0].baseline_y is the first-line baseline. An atom line always has
        //      empty glyphs, so the anchor comes from the inline atom's own marker;
        //   ③ the first_glyph_descendant DFS, for a direct child with no content whose
        //      content sits in grandchildren, which happens only with nested lists such
        //      as the real-corpus AE2 getting-started items 13 and 22.
        // The marker is skipped only when the whole subtree has no glyph content, which
        // keeps the documented fallback: content geometry is unaffected and that marker
        // is simply not generated.
        // Step ② must precede step ③. For an item whose first line is pure atoms
        // followed by a nested list, running ③ first would walk the subtree DFS and
        // anchor the marker to the nested child's text line, which would put two markers on one row,
        // and ② would never get a turn.
        let anchor = fb.children().and_then(|ch| {
            (0..ch.len())
                .map(|k| ch.get(k) as usize)
                .find(|&c| glyph_acc.get(&c).map_or(false, |a| !a.glyphs.is_empty()))
                .or_else(|| {
                    (0..ch.len())
                        .map(|k| ch.get(k) as usize)
                        .find(|&c| {
                            glyph_acc
                                .get(&c)
                                .map_or(false, |a| a.glyphs.is_empty() && !a.markers.is_empty())
                        })
                })
                .or_else(|| {
                    (0..ch.len())
                        .map(|k| ch.get(k) as usize)
                        .find_map(|c| first_glyph_descendant(flat_nodes, glyph_acc, c))
                })
        });
        let Some(anchor) = anchor else {
            continue;
        };
        let baseline_abs = {
            let a = glyph_acc
                .get(&anchor)
                .expect("list marker anchor acc must exist");
            if a.glyphs.is_empty() {
                // A content child whose first line holds inline atoms only has no
                // glyphs, so anchor to its first inline atom marker's baseline, which is
                // that line's baseline with the line growth shift already included.
                abs_positions[anchor].1 + a.markers[0].baseline_y
            } else {
                abs_positions[anchor].1 + a.glyphs[0].y
            }
        };
        let st = md.style().unwrap_or_else(|| unreachable!("ListMarkerData.style declared"));
        let scaled = st.font_size() * st.font_scale();
        // The same shaping pipeline as body text, layout_styled, matching measure and
        // emitText: no wrapping, since a marker is a single line, and no baseline shift,
        // because there are no spans.
        let layout = font_system
            .parley
            .layout_styled(
                text,
                scaled,
                1.55,
                st.bold(),
                st.italic(),
                None,
                // Marker style font_family, shaped through the same pipeline as body
                // text; Java declares the list item's body font.
                st.font_family(),
            );
        let (glyphs, _markers, _max_x, _content_h, _clear_floor, _last_window) =
            collect_layout(&layout, &[], 0.0, 0.0, f32::MAX, &[]);
        if glyphs.is_empty() {
            continue;
        }
        // Vertical placement: the marker baseline equals the content first-line
        // baseline, in list-item relative coordinates, since emit adds the list item's
        // absolute y to every PlacedGlyph.
        //
        // Rejected-then-fixed behavior, where the marker dropped by one line: the
        // marker's own glyph y already includes its single-line paragraph's baseline
        // offset in marker-local coordinates, so glyphs[0].y is line 0's pen y, about
        // the in-line baseline distance, measured at 12.0 logical px or one line height.
        // The first version did `g.y += baseline_off`, which moved the marker as a whole
        // above baseline_off and then stacked its own baseline distance on top, the
        // one-line misalignment marker_top - item_top measured +19 against a
        // baseline delta of +6.5. The correct shift is baseline_off minus the marker's
        // own baseline, which lands the marker baseline exactly on the content first-line
        // baseline. Every glyph of a single-line marker shares one baseline, so
        // g.y - marker_baseline keeps the relative y_offset differences inside the font.
        let marker_baseline = glyphs[0].y;
        let shift = (baseline_abs - abs_positions[i].1) - marker_baseline;
        let acc = glyph_acc.entry(i).or_default();
        for mut g in glyphs {
            g.y += shift;
            g.line_top += shift;
            // Sentinel line index (see the function documentation), isolating the marker
            // from the body visual line.
            g.line_index = usize::MAX;
            acc.glyphs.push(g);
        }
        anchors.insert(i, anchor);
    }
    anchors
}

/// Last cascade step: search the subtree depth-first from `idx` and return the first
/// descendant child that has glyphs. This applies when a list item contains only a
/// nested list, with the content in a grandchild paragraph, and its direct children
/// have neither glyphs nor inline atom markers. The walk descends only after the item's
/// own direct content lines are exhausted, as described in list_marker_pass. Returns
/// None when the whole subtree has no glyphs, in which case list_marker_pass keeps its
/// documented fallback and skips the marker.
fn first_glyph_descendant(
    flat_nodes: &[FlatNode],
    glyph_acc: &HashMap<usize, GlyphAccum>,
    idx: usize,
) -> Option<usize> {
    if glyph_acc.get(&idx).map_or(false, |a| !a.glyphs.is_empty()) {
        return Some(idx);
    }
    let ch = flat_nodes[idx].children()?;
    (0..ch.len())
        .map(|k| ch.get(k) as usize)
        .find_map(|c| first_glyph_descendant(flat_nodes, glyph_acc, c))
}

/// Overhang compensation at an italic-to-upright text
/// boundary, with TeX \/ semantics.
///
/// The renderer's synthetic shear (GuideRenderEngine.emitGlyphQuads) pushes glyph tops
/// to the right along `xTop = x + K × (shearBaseY - y)`, where shearBaseY is the
/// largest quad bottom over all glyphs of that run. When an italic run sits directly
/// against an upright run on one line, the last glyph's top intrudes to the right into
/// the next run. This function inserts a one-shot gap at every italic-to-upright text
/// boundary:
///     correction = max(0, K × (shearBase - lastItalic.y) - inkGap)
/// where shearBase = max(y + h) over all quads of that italic run across lines,
/// matching the renderer's convention, and inkGap = firstUpright.x -
/// (lastItalic.x + lastItalic.w) is the existing gap between the two runs' ink when
/// nothing is sheared. The correction applies only to all glyphs on this line of the
/// upright run immediately following the boundary, as a uniform shift, and never moves
/// the advance inside a run. Multiple italic-to-upright boundaries on one line each
/// insert their own one-shot gap and never accumulate, the same cascading semantics as
/// An upright-to-italic boundary, the inside of a run and purely upright text are
/// all left alone.
///
/// Rejection fix, from a double-application analysis and
/// statically confirmed: in the italic-to-atom-to-upright configuration the text
/// boundary pass is blind to the atom, because an atom is not a quad. The last italic
/// glyph and the next upright glyph are adjacent in x order, so this function still
/// computes a correction for what it sees as a direct text boundary, and its `inkGap`
/// already contains the atom's whole advance (`inkGap = naturalGap + atomAdvance`).
/// Meanwhile pass 2 of the atom boundary correction
/// (apply_italic_atom_boundary_correction) applies `c_23 = max(0, S - naturalGap)` to
/// the tail of the upright quads after the atom on the same line. When both are
/// positive, and `S > inkGap` implies `S > naturalGap`, the tail is pushed by
/// `c_23 + correction_22` instead of a single `c_23`. The fix: this function receives
/// the paragraph's markers and skips the boundary when an atom marker's pen falls
/// between the two runs on this line. The atom ink box advance already carries the tail
/// past the overhang front, and `c_23 ≥ correction_22` always holds: the first upright
/// glyph after the atom pen, the pivot, has a left edge `first.x ≥ pivot + ink_left_px`,
/// since a valid ink box has inkRight ≥ inkLeft and the following glyph's left bearing
/// is non-negative, so first.x ≥ pivot + atomAdvance ≥ pivot + ink_left_px. It follows
/// that inkGap = first.x - (last.x + last.w) ≥ naturalGap and S - inkGap ≤
/// S - naturalGap, so after both take max(0, ·) the relation correction_22 ≤ c_23
/// holds. Valid ink boxes from IconMetrics together with ordinary glyphs always satisfy
/// those premises, which makes the text boundary correction purely redundant in this
/// configuration.
///
/// The data source is the rasterized quads, the same geometry the Java renderer uses,
/// so the correction is exact for the actual drawn output.
fn apply_italic_boundary_correction(
    quads: &mut Vec<crate::parley_text::ParleyRasterGlyph>,
    markers: &[crate::measure::InlineMarker],
    span_styles: &[SpanStyleInfo],
    base_italic: bool,
    shear_k: f32,
) {
    if quads.is_empty() || shear_k <= 0.0 {
        return;
    }
    let italic_of = |si: u32| -> bool {
        span_styles
            .get(si as usize)
            .map(|s| s.italic)
            .unwrap_or(base_italic)
    };
    // All-upright paragraph: geometry zero-change, output identical.
    if !quads.iter().any(|q| italic_of(q.span_index)) {
        return;
    }

    // Per-span shear baseline over the whole span and all its lines; mirrors the
    // renderer's per-DrawGlyphRun max(g.y + g.h).
    let mut shear_base: std::collections::HashMap<u32, f32> = Default::default();
    for q in quads.iter() {
        let e = shear_base.entry(q.span_index).or_insert(0.0f32);
        *e = e.max(q.y + q.h);
    }

    // Per-quad rightward shift (document units); 0 = untouched.
    let mut shifts: Vec<f32> = vec![0.0; quads.len()];

    // Group quad indices by visual line; walk each line in pen order.
    let mut by_line: std::collections::BTreeMap<u32, Vec<usize>> = Default::default();
    for (i, q) in quads.iter().enumerate() {
        by_line.entry(q.line_index).or_default().push(i);
    }

    for (line, idxs) in by_line.iter_mut() {
        idxs.sort_by(|&a, &b| {
            quads[a]
                .x
                .partial_cmp(&quads[b].x)
                .unwrap_or(std::cmp::Ordering::Equal)
        });
        let n = idxs.len();
        let mut i = 0usize;
        while i < n {
            let si = quads[idxs[i]].span_index;
            let mut j = i;
            while j < n && quads[idxs[j]].span_index == si {
                j += 1;
            }
            // [i, j) is one contiguous span run on this line. Only a
            // italic→upright flip at the run's right edge is a boundary
            // (upright→italic and run interiors are untouched).
            if j < n {
                let next_si = quads[idxs[j]].span_index;
                if italic_of(si) && !italic_of(next_si) {
                    let last = idxs[j - 1];
                    let first = idxs[j];
                    // Rejection fix: skip the boundary when an inline atom
                    // separates it, that is italic→atom→upright with the atom marker's
                    // pen falling between the two runs on this line. The text boundary
                    // pass is blind to the atom, so the inkGap it measures already
                    // contains the atom advance, while the atom correction's pass 2
                    // applies c_23 to the same upright quads, and firing both would
                    // double-apply. The atom ink box advance already carries the tail past
                    // the overhang front, and c_23 ≥ correction_22 always holds, because
                    // the first upright glyph after the atom pen has first.x ≥ pivot +
                    // ink_left_px: a valid ink box has inkRight ≥ inkLeft and the
                    // following glyph's left bearing is non-negative, which valid
                    // IconMetrics ink boxes plus ordinary glyphs satisfy. The text
                    // boundary correction is therefore purely redundant in this
                    // configuration.
                    let atom_between = markers.iter().any(|m| {
                        m.line_index as u32 == *line
                            && quads[last].x < m.pen_x
                            && m.pen_x < quads[first].x
                    });
                    if !atom_between {
                        let sb = shear_base[&si];
                        let shear_shift = shear_k * (sb - quads[last].y);
                        let ink_gap = quads[first].x - (quads[last].x + quads[last].w);
                        let correction = (shear_shift - ink_gap).max(0.0);
                        if correction > 0.0 {
                            // One-shot insertion: shift every glyph of the following
                            // upright run on this line by `correction`. Per-boundary
                            // (no accumulation across multiple boundaries).
                            let mut k = j;
                            while k < n && quads[idxs[k]].span_index == next_si {
                                shifts[idxs[k]] = shifts[idxs[k]].max(correction);
                                k += 1;
                            }
                        }
                    }
                }
            }
            i = j;
        }
    }

    for (i, d) in shifts.iter().enumerate() {
        if *d > 0.0 {
            quads[i].x += *d;
        }
    }
}

    /// The italic-to-atom boundary overhang correction, with
    /// TeX \/ semantics, applied to the atom's layout position rather than to a glyph
    /// quad shift, since the text boundary pass proved that an emit-level quad shift
    /// cannot reach an atom.
    ///
    /// The renderer's synthetic shear pushes an italic run's last glyph top to the
    /// right along `xTop = x + K × (shearBaseY - y)`. When an italic run sits directly
    /// against an inline atom, an icon InlineBox, that overhang intrudes into the atom
    /// ink box's left edge. This function inserts a one-shot gap at such a boundary,
    /// with the same semantics as the text boundary correction 
    ///     correction = max(0, K × (shearBase - lastQuadTop) - naturalGap)
    /// where shearBase = max(y + h) over all quads of that italic span across lines,
    /// matching the convention of the text boundary pass and of the renderer, and
    /// naturalGap = (marker.pen_x + ink_left_px) - (lastQuad.x + lastQuad.w) is the
    /// existing gap from the last italic glyph's ink right edge to the atom ink box's
    /// left edge, counting the box's left side bearing ink_left, because the atoms here
    /// are character-level atoms. The correction applies to the atom's layout position
    /// (abs_positions.x, the Rust placement layer) and shifts everything after the atom
    /// on the same line consistently, both the quads at x ≥ the atom pen_x and the
    /// block positions of later markers, keeping the pen stream consistent.
    /// Each boundary fires once and boundaries never accumulate: every correction is
    /// computed from natural geometry, the quads and pens before any insertion, and is
    /// never re-derived from earlier insertions.
    ///
    /// An upright-to-atom boundary is left alone, and an atom-to-text boundary needs no
    /// correction, because the overhang lies to the right of the italic glyph and by
    /// nature only affects what follows it. Relation to the text boundary pass under the
    /// Rejection fix: in the italic-to-atom-to-upright configuration the text
    /// boundary pass is blind to the atom and used to apply correction_22 as if this
    /// were a direct text boundary containing the atom advance, double-applying along
    /// with this pass's tail shift of the same upright quads. That is fixed: the text
    /// boundary pass now receives the markers and skips the boundary when an atom pen
    /// falls between the two runs (see apply_italic_boundary_correction), leaving this
    /// boundary to the single c_23 issued here.
    /// The safety of that skip rests on `c_23 ≥ correction_22`: the first upright glyph
    /// after the atom pen, the pivot, has a left edge `first.x ≥ pivot + ink_left_px`,
    /// because a valid ink box has inkRight ≥ inkLeft and the following glyph's left
    /// bearing is non-negative, so first.x ≥ pivot + atomAdvance ≥ pivot + ink_left_px.
    /// Both hold for the valid ink boxes IconMetrics produces together with ordinary
    /// glyphs, hence gap_22 ≥ natural_gap, and after both take max(0, ·) the relation
    /// correction_22 ≤ c_23 follows. Even if both fired, the net displacement would not
    /// change, so the text boundary correction is purely redundant here.
fn apply_italic_atom_boundary_correction(
    quads: &mut Vec<crate::parley_text::ParleyRasterGlyph>,
    markers: &[crate::measure::InlineMarker],
    node: &FlatNode,
    sizes: &[(f32, f32)],
    abs_positions: &mut [(f32, f32)],
    span_styles: &[SpanStyleInfo],
    base_italic: bool,
    shear_k: f32,
) {
    if shear_k <= 0.0 || markers.is_empty() || quads.is_empty() {
        return;
    }
    let refs = match node.text().and_then(|t| t.inline_blocks()) {
        Some(v) => v,
        None => return,
    };
    let italic_of = |si: u32| -> bool {
        span_styles
            .get(si as usize)
            .map(|s| s.italic)
            .unwrap_or(base_italic)
    };
    // All-upright paragraph: geometry zero-change, output identical.
    if !quads.iter().any(|q| italic_of(q.span_index)) {
        return;
    }

    // Per-span shear baseline over the whole span and all its lines; mirrors the text
    // boundary pass.
    let mut shear_base: std::collections::HashMap<u32, f32> = Default::default();
    for q in quads.iter() {
        let e = shear_base.entry(q.span_index).or_insert(0.0f32);
        *e = e.max(q.y + q.h);
    }

    // Per-line quad indices sorted by x (find the glyph immediately before each
    // atom's pen).
    let mut by_line: std::collections::BTreeMap<u32, Vec<usize>> = Default::default();
    for (i, q) in quads.iter().enumerate() {
        by_line.entry(q.line_index).or_default().push(i);
    }
    for (_line, idxs) in by_line.iter_mut() {
        idxs.sort_by(|&a, &b| {
            quads[a]
                .x
                .partial_cmp(&quads[b].x)
                .unwrap_or(std::cmp::Ordering::Equal)
        });
    }

    // Pair regular markers with their refs (skip float refs align>=3), mirroring
    // inline_post_pass's document-order pairing.
    let mut reg: Vec<(usize, usize)> = Vec::new(); // (marker_idx, ref_idx)
    let mut mi = 0usize;
    for ri in 0..refs.len() {
        if refs.get(ri).align() >= 3 {
            continue;
        }
        if mi >= markers.len() {
            break;
        }
        reg.push((mi, ri));
        mi += 1;
    }

    // Pass 1 computes every boundary's correction from NATURAL geometry, the quads and
    // pens before any insertion, so boundaries never accumulate into each other's
    // computation and each fires once.
    //
    // Pass-2 fix: record a natural-x snapshot of the whole line's quads for pass 2 to
    // test tail membership against. Pass 2 shifts quads in place, so `q.x` already
    // contains the accumulated displacement from earlier boundaries. If pass 2 kept
    // testing the polluted `q.x >= pivot - 0.5`, the trailing glyph selected by this
    // boundary in pass 1, whose natural x < pivot - 0.5, would be pushed past the
    // threshold by an earlier boundary and shifted AGAIN: a single glyph
    // double-applied and detached from its word body, measured as +2.36px
    // for the trailing glyph of "Gold Ingo t" against its word body. The snapshot puts
    // pass 2's membership test on the same geometry convention as pass 1, so the
    // documented contract holds: every boundary fires once from natural geometry and
    // they never accumulate.
    let natural_x: Vec<f32> = quads.iter().map(|q| q.x).collect();
    let mut corrections: Vec<(usize, usize, f32)> = Vec::new(); // (marker_idx, ref_idx, c)
    for &(marker_idx, ref_idx) in &reg {
        let m = &markers[marker_idx];
        let r = refs.get(ref_idx);
        let bh = sizes[r.node() as usize].1;
        let ink_left_px = r.ink_left() * bh / 16.0; // 16-cell icon space to px
        let pivot = m.pen_x;
        let last = by_line
            .get(&(m.line_index as u32))
            .and_then(|idxs| idxs.iter().rev().copied().find(|&qi| quads[qi].x < pivot - 0.5));
        let Some(qi) = last else { continue };
        let last_q = &quads[qi];
        if !italic_of(last_q.span_index) {
            continue; // upright→atom: nothing to do
        }
        let sb = shear_base[&last_q.span_index];
        let shear_shift = shear_k * (sb - last_q.y);
        let natural_gap = (pivot + ink_left_px) - (last_q.x + last_q.w);
        let c = (shear_shift - natural_gap).max(0.0);
        if c > 0.0 {
            corrections.push((marker_idx, ref_idx, c));
        }
    }

    // Pass 2 applies in pen order: each boundary shifts the atom's layout
    // position AND everything after it on the same line (consistent tail shift).
    for (marker_idx, ref_idx, c) in corrections {
        let r = refs.get(ref_idx);
        let ci = r.node() as usize;
        let m = &markers[marker_idx];
        let line = m.line_index as u32;
        let pivot = m.pen_x;
        // The boundary atom's own layout position, the Rust placement layer, which is
        // the position Java renders from.
        abs_positions[ci].0 += c;
        // Tail: same-line quads at/after the atom's pen shift with it.
        // Knife-edge window note: this pass's pass 1 searches for the last glyph with
        // `x < pivot - 0.5` (see above), while pass 2's tail shift uses
        // `x >= pivot - 0.5`, and the text boundary pass's atom skip requires
        // `pen_x < quads[first].x` (see apply_italic_boundary_correction). A
        // theoretical knife-edge window is squeezed between the three thresholds: a
        // quad whose left edge lands in [pivot - 0.5, pivot). Pass 1 never selects it as
        // this boundary's trailing glyph, so it takes no part in the correction
        // computation, yet pass 2 shifts it with no precondition. If it were also the
        // first upright glyph after the atom, the skip condition would fail because
        // pen_x >= first.x and the double-application risk would return. Filling that
        // window would need a glyph with a negative left bearing: first.x = pen_first +
        // lb_first < pivot together with pen_first >= pivot + atomAdvance for a valid
        // ink box of at least 1px gives lb_first < -atomAdvance < 0, meaning the glyph's
        // ink reaches left past its own pen by more than the atom advance. With ordinary
        // glyphs, whose left bearing is non-negative, and a valid IconMetrics ink box,
        // first.x >= pivot + atomAdvance >= pivot + 1 > pivot - 0.5 and the first glyph
        // lies after the pivot, so the window is always empty and the two thresholds
        // are equivalent under those premises, with 0.5 serving as a floating-point
        // safety margin.
        //
        // Pass-2 fix: membership is now tested against the natural-x snapshot recorded
        // in pass 1. Pass 2 shifts quads in place in document order, so `q.x` is the
        // accumulated position. Testing `q.x >= pivot - 0.5` there would shift this
        // boundary's pass-1 trailing glyph, whose natural x < pivot - 0.5, a second
        // time once an earlier boundary's tail shift had pushed it past the threshold:
        // a single glyph double-applied and detached from its word body,
        // measured at +2.36px. The snapshot puts pass 2 on the same convention as pass
        // 1, so glyphs with natural x < pivot - 0.5 belong left of the boundary, taking
        // no part in this boundary's shift and only accumulating earlier ones, while
        // glyphs at or above the threshold belong right of it and shift with this
        // boundary's atom. That makes the documented contract hold: every boundary
        // fires once from natural geometry and they never accumulate.
        for (qi, q) in quads.iter_mut().enumerate() {
            if q.line_index == line && natural_x[qi] >= pivot - 0.5 {
                q.x += c;
            }
        }
        // Tail: same-line later atoms' layout positions shift with it.
        for &(mj, rj) in &reg {
            if mj != marker_idx && markers[mj].line_index as u32 == line && markers[mj].pen_x >= pivot - 0.5 {
                let rj_ref = refs.get(rj);
                abs_positions[rj_ref.node() as usize].0 += c;
            }
        }
    }
}

/// Per-span style facts needed by Pass B (grouping + decorations), extracted
/// from the node's TextData.spans vector. Empty for single-style paragraphs.
struct SpanStyleInfo {
    color: u32,
    italic: bool,
    underline: bool,
    strikethrough: bool,
    highlight_argb: u32,
    inline_code: bool,
    wavy_underline: bool,
    dotted_underline: bool,
}

fn span_style_table(node: &FlatNode) -> Vec<SpanStyleInfo> {
    let mut out = Vec::new();
    if let Some(spans) = node.text().and_then(|t| t.spans()) {
        for s in spans.iter() {
            let st = s.style().unwrap();
            out.push(SpanStyleInfo {
                color: st.color(),
                italic: st.italic(),
                underline: st.underline(),
                strikethrough: st.strikethrough(),
                highlight_argb: st.highlight_argb(),
                inline_code: st.inline_code(),
                wavy_underline: st.wavy_underline(),
                dotted_underline: st.dotted_underline(),
            });
        }
    }
    out
}

/// Emit span decoration rects (background highlights, underline,
/// strikethrough) from per-line glyph extents, in absolute document
/// coordinates. Runs after the inline post-pass, so the shaped glyphs already
/// carry final (growth-adjusted) positions.
fn emit_decorations<'a>(
    glyphs: &[crate::parley_text::OutGlyph],
    span_styles: &[SpanStyleInfo],
    node_index: u32,
    node_x: f32,
    node_y: f32,
    fbb: &mut FlatBufferBuilder<'a>,
    out: &mut Vec<flatbuffers::WIPOffset<DecorationRect<'a>>>,
) {
    if span_styles.is_empty() {
        return;
    }
    struct Extent {
        min_x: f32,
        max_x: f32,
        baseline: f32,
        line_top: f32,
        line_height: f32,
    }
    let mut by_line: std::collections::BTreeMap<(u32, usize), Extent> = Default::default();
    for g in glyphs {
        let e = by_line.entry((g.span_index, g.line_index)).or_insert(Extent {
            min_x: g.x,
            max_x: g.x + g.w,
            baseline: g.y,
            line_top: g.line_top,
            line_height: g.line_height,
        });
        e.min_x = e.min_x.min(g.x);
        e.max_x = e.max_x.max(g.x + g.w);
    }
    for ((span_index, _line), e) in by_line {
        let Some(st) = span_styles.get(span_index as usize) else { continue };
        let w = e.max_x - e.min_x;
        if st.highlight_argb != 0 {
            // Background: inline-code hugs the text run; plain highlight pads
            // 1px on each side (mirrors the legacy LineTextRun geometry).
            let (bx, bw) = if st.inline_code {
                (e.min_x, w)
            } else {
                (e.min_x - 1.0, w + 2.0)
            };
            out.push(DecorationRect::create(
                fbb,
                &DecorationRectArgs {
                    node: node_index,
                    x: node_x + bx,
                    y: node_y + e.line_top - 1.0,
                    w: bw,
                    h: e.line_height,
                    argb: st.highlight_argb,
                    kind: 0,
                },
            ));
        }
        if st.underline {
            out.push(DecorationRect::create(
                fbb,
                &DecorationRectArgs {
                    node: node_index,
                    x: node_x + e.min_x,
                    y: node_y + e.baseline + 1.0,
                    w,
                    h: 1.0,
                    argb: st.color,
                    kind: 1,
                },
            ));
        }
        if st.wavy_underline {
            // Wavy underline, a wave amplitude band 2px tall (Java draws the
            // squiggle inside this rect). Same geometry as the underline band.
            out.push(DecorationRect::create(
                fbb,
                &DecorationRectArgs {
                    node: node_index,
                    x: node_x + e.min_x,
                    y: node_y + e.baseline + 1.0,
                    w,
                    h: 2.0,
                    argb: st.color,
                    kind: 4,
                },
            ));
        }
        if st.dotted_underline {
            // Dotted underline, the same band as the underline; Java rasterizes
            // the dot pattern inside the rect.
            out.push(DecorationRect::create(
                fbb,
                &DecorationRectArgs {
                    node: node_index,
                    x: node_x + e.min_x,
                    y: node_y + e.baseline + 1.0,
                    w,
                    h: 1.0,
                    argb: st.color,
                    kind: 5,
                },
            ));
        }
        if st.strikethrough {
            out.push(DecorationRect::create(
                fbb,
                &DecorationRectArgs {
                    node: node_index,
                    x: node_x + e.min_x,
                    y: node_y + e.line_top + e.line_height / 2.0,
                    w,
                    h: 1.0,
                    argb: st.color,
                    kind: 2,
                },
            ));
        }
    }
}

/// shapeText JNI command: shape + rasterize a single styled text, returning a
/// ShapeTextResult FlatBuffer with atlas-keyed buffer-local quads and metrics.
pub fn shape_text_cmd(font_system: &mut GuideFontSystem, input_bytes: &[u8]) -> Vec<u8> {
    use crate::fb::{ShapeTextInput, ShapeTextResult, ShapeTextResultArgs};

    let input = flatbuffers::root::<ShapeTextInput>(input_bytes)
        .expect("Invalid ShapeTextInput FlatBuffer");
    let text = input.text().unwrap_or("");
    let style = input.style().expect("ShapeTextInput.style missing");
    let render_scale = input.render_scale().max(0.25);
    let max_w = if input.max_width() > 0.0 {
        // Buffer is already at the scaled font size, so the width is used as-is.
        Some(input.max_width())
    } else {
        None
    };

    let scaled = style.font_size() * style.font_scale();
    // Italic fix: `italic` is carried as a brush
    // MARKER, NOT via `StyleProperty::FontStyle`. Forwarding
    // FontStyle::Italic made fontique pick a true italic face on real systems
    // (Arial Italic) and swap the Latin outlines. Shaping always matches
    // Normal now, so glyphs/advances are byte-identical to the upright case;
    // the marker is restored into the output (`OutGlyph.italic`) for the
    // boundary correction. The synthetic slant stays a Java draw-time
    // transform (MC §o parity), not re-applied here, and since shaping
    // selects the upright face there is no double-slant.
    let layout = font_system
        .parley
        .layout_styled(
            text,
            scaled,
            1.55,
            style.bold(),
            style.italic(),
            max_w,
            // TextStyle.font_family is now active: a non-empty value becomes
            // FontFamily::named, while an empty or absent one falls back to the
            // SansSerif generic, leaving the default path byte for byte unchanged.
            style.font_family(),
        );
    let content_height = layout.height();
    let ascent = layout
        .lines()
        .next()
        .map(|l| l.metrics().baseline - l.metrics().block_min_coord)
        .unwrap_or(scaled);
    // First-run real x_height/cap_height (shaped-size px, skrifa OS/2
    // sxHeight/sCapHeight scaled by font size); RunMetrics is Copy so this
    // value-extension ends all borrows before collect_layout below.
    let run_metrics = layout.lines().next().and_then(|l| l.runs().next()).map(|r| *r.metrics());
    let x_height = run_metrics.and_then(|m| m.x_height).unwrap_or(ascent * 0.625);
    let cap_height = run_metrics.and_then(|m| m.cap_height).unwrap_or(ascent * 0.7);
    let (glyphs, _markers, max_x, _content_height, _clear_floor, _last_window) =
        crate::parley_text::collect_layout(&layout, &[], 0.0, 0.0, max_w.unwrap_or(f32::MAX), &[]);
    let (quads, bitmaps) = crate::parley_text::rasterize_out_glyphs(&glyphs, render_scale);

    let mut fbb = flatbuffers::FlatBufferBuilder::with_capacity(4096);
    let glyph_offsets: Vec<flatbuffers::WIPOffset<PlacedGlyph>> = quads
        .iter()
        .map(|q| {
            PlacedGlyph::create(
                &mut fbb,
                &PlacedGlyphArgs {
                    bitmap_key: q.bitmap_key,
                    x: q.x,
                    y: q.y,
                    w: q.w,
                    h: q.h,
                    start: 0,
                    end: 0,
                    line_index: q.line_index,
                    // Buffer-local baseline, following the
                    // divide-by-scale-then-truncate convention; q.baseline is already
                    // truncated.
                    baseline: q.baseline,
                },
            )
        })
        .collect();
    let glyphs_vec = fbb.create_vector(&glyph_offsets);
    let bitmap_offsets: Vec<flatbuffers::WIPOffset<GlyphBitmap>> = bitmaps
        .iter()
        .map(|(key, w, h, rgba)| {
            let rgba_vec = fbb.create_vector(rgba);
            GlyphBitmap::create(
                &mut fbb,
                &GlyphBitmapArgs {
                    key: *key,
                    w: *w,
                    h: *h,
                    rgba: Some(rgba_vec),
                },
            )
        })
        .collect();
    let bitmaps_vec = fbb.create_vector(&bitmap_offsets);

    let result = ShapeTextResult::create(
        &mut fbb,
        &ShapeTextResultArgs {
            // Real advance, zero allowed (zero-width chars must measure 0;
            // the 1px clamp belongs to Taffy node sizing only, see measure.rs).
            width: max_x,
            height: content_height.max(1.0),
            ascent,
            line_height: scaled * 1.55,
            glyphs: Some(glyphs_vec),
            bitmaps: Some(bitmaps_vec),
            x_height,
            cap_height,
        },
    );
    fbb.finish(result, None);
    fbb.finished_data().to_vec()
}

#[cfg(test)]
mod tests {
    use crate::parley_text::{ParleyFonts, SpanBrush};
    use crate::text::GuideFontSystem;
    use crate::compute_layout;
    use parley::{FontStyle, Layout};

    /// Sum of run advances: the paragraph's shaped width, independent of
    /// line wrapping.
    fn total_advance(layout: &Layout<SpanBrush>) -> f32 {
        layout
            .lines()
            .flat_map(|l| l.runs())
            .map(|r| r.advance())
            .sum()
    }

    /// Build a LayoutInput FlatBuffer with a single node_type=1 paragraph.
    /// `position`: 1 = pos_abs inline paragraph (lowered inline block);
    /// `size_w`/`size_h`: declared px box (Some → Dimension px, None → auto).
    /// `avail_width`: document width; `children_of_0`: when Some, node 0
    /// becomes a flex-column container owning the given child flat indices.
    fn build_paragraph_input(
        text: &str,
        position: i8,
        size_w: Option<f32>,
        size_h: Option<f32>,
        avail_width: f32,
    ) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        let style_args = crate::fb::StyleArgs {
            position,
            size_w: size_w.map(|w| {
                crate::fb::Dimension::create(
                    &mut fbb,
                    &crate::fb::DimensionArgs { value: w, unit: 1 },
                )
            }),
            size_h: size_h.map(|h| {
                crate::fb::Dimension::create(
                    &mut fbb,
                    &crate::fb::DimensionArgs { value: h, unit: 1 },
                )
            }),
            ..Default::default()
        };
        let style = crate::fb::Style::create(&mut fbb, &style_args);
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let text_off = fbb.create_string(text);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                ..Default::default()
            },
        );
        let node = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[node]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// Build a LayoutInput with ONE relative-flow paragraph (position 0) whose
    /// rich spans are given as `(text, italic)` parts in document order. The
    /// concatenated span texts must equal TextData.text (they do, since we build
    /// it from the parts). Mirrors LayoutNodeSerializer's rich-span emission
    /// so the boundary correction can be tested end-to-end.
    fn build_rich_paragraph_input(parts: &[(&str, bool)], avail_width: f32) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        // Base paragraph style (spans override it; defaults not italic).
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let mut span_offsets = Vec::new();
        let mut full = String::new();
        for (t, italic) in parts {
            let st = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size: 11.0,
                    italic: *italic,
                    ..Default::default()
                },
            );
            let span_text_off = fbb.create_string(t);
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(span_text_off),
                    style: Some(st),
                },
            ));
            full.push_str(t);
        }
        let spans_vec = fbb.create_vector(&span_offsets);
        let text_off = fbb.create_string(&full);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                spans: Some(spans_vec),
                ..Default::default()
            },
        );
        let style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let node = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[node]);
        // The shear_k slot is DEPRECATED, with the Rust-owned SHEAR_K
        // constant (default 0.25) as the fallback, so tests no longer declare it.
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// Build a LayoutInput with a flex-column container (node 0, no declared
    /// size) owning one relative-flow paragraph (node 1, position 0): the
    /// path that must keep wrapping at the container's available width.
    fn build_relative_flow_input(text: &str, avail_width: f32) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let text_off = fbb.create_string(text);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                ..Default::default()
            },
        );
        let child_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let child = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(child_style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        // Container: display 0 = Flex, flex_direction 1 = Column (defaults).
        // FlatNode.children is a vector of u32 node indices into the nodes
        // vector, so the container (index 0) owns the child at index 1.
        let cont_style = crate::fb::Style::create(&mut fbb, &crate::fb::StyleArgs::default());
        let children = fbb.create_vector(&[1u32]);
        let cont = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(cont_style),
                node_type: 0,
                children: Some(children),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[cont, child]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// Highest visual line index across every emitted glyph of a result.
    fn max_glyph_line(result: &crate::fb::LayoutResult) -> u32 {
        let mut max_line = 0u32;
        if let Some(runs) = result.glyph_runs() {
            for ri in 0..runs.len() {
                let run = runs.get(ri);
                if let Some(glyphs) = run.glyphs() {
                    for gi in 0..glyphs.len() {
                        max_line = max_line.max(glyphs.get(gi).line_index());
                    }
                }
            }
        }
        max_line
    }

    /// Regression: a pos_abs (position:absolute) inline paragraph must
    /// shape at its NATURAL width, so its declared px size stays only as the
    /// box size and pen reservation, never as the wrap constraint. Taffy
    /// feeds a leaf's own declared width back as the measure available width
    /// (compute/leaf.rs maybe_set(node_size.width)); Java declares
    /// round(measureWidth), so a round-down lands the declared width a hair
    /// UNDER the natural advance and used to make parley wrap the paragraph
    /// at its last space, and the wrapped second line then overflowed the
    /// declared 17px box. This test drives the exact condition (declared <
    /// natural) and asserts the paragraph stays on its natural single line.
    #[test]
    fn pos_abs_inline_paragraph_does_not_wrap_at_declared_width() {
        const TEXT: &str = "Guide Error Fallback Text Item";
        let mut fs = GuideFontSystem::new();
        let natural = total_advance(&fs.parley.layout_styled(TEXT, 11.0, 1.55, false, false, None, None));
        // Java measureWidth = round(natural); a round-down declares a width
        // strictly below the natural advance. Force it unconditionally so the
        // old buggy path would ALWAYS wrap here.
        let declared_w = (natural - 0.25).floor();
        let declared_h: f32 = 17.0;

        let input = build_paragraph_input(TEXT, 1, Some(declared_w), Some(declared_h), 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        // Box stays the declared px size: width ≈ natural advance (box + pen
        // reservation), height covers the actual single shaped line.
        let node0 = result.nodes().expect("nodes").get(0);
        assert_eq!(
            node0.h(),
            declared_h,
            "inline block box height must cover its lines (17px for one line)"
        );
        assert!(
            (node0.w() - declared_w).abs() < 0.01,
            "inline block box width must stay the declared width ({declared_w}), got {}",
            node0.w()
        );

        // The text itself must NOT wrap at the declared width.
        let max_line = max_glyph_line(&result);
        assert_eq!(
            max_line,
            0,
            "pos_abs paragraph must shape as ONE natural line despite declared width \
             {declared_w} < natural advance {natural} (would wrap on last space before the fix)"
        );
        println!(
            "[inline_natural] natural={natural:.3} declared_w={declared_w} box_w={} box_h={} lines={}",
            node0.w(),
            node0.h(),
            max_line + 1
        );
    }

    /// Guard for the other half of the fix: relative-flow paragraphs (position
    /// 0) must keep wrapping at the container's available width: the
    /// MaxContent override is scoped to inline paragraphs only.
    #[test]
    fn relative_flow_paragraph_still_wraps_at_container_width() {
        const TEXT: &str = "Guide Error Fallback Text Item";
        let mut fs = GuideFontSystem::new();

        let input = build_relative_flow_input(TEXT, 120.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let max_line = max_glyph_line(&result);
        assert!(
            max_line >= 1,
            "relative-flow paragraph must still wrap at the narrow container width, \
             got {max_line} lines (MaxContent must NOT leak to relative-flow nodes)"
        );
    }

    /// Italic fix: the italic marker must NOT enter
    /// parley font matching; shaping always matches `FontStyle::Normal`, so
    /// every glyph comes from the regular face. The marker rides the brush
    /// and is restored into the glyph-run output (`OutGlyph.italic`) for
    /// the boundary correction.
    ///
    /// Assertions (reworked from the earlier test, which asserted
    /// `Run::font_attrs().style == FontStyle::Italic`, the regression
    /// source; the marker is now read from the brush instead):
    ///   * the new marker bit is readable on the output glyphs (italic=true
    ///     vs false);
    ///   * Latin/CJK advance diff == 0 (shaping-decided geometry identical);
    ///   * every glyph quad key (rasterize_out_glyphs `bitmap_key`) is
    ///     byte-identical under italic=true vs false. That is the core regression
    ///     criterion: a true-italic face swap (Arial Italic) would change the
    ///     font bytes / glyph ids and break this.
    #[test]
    fn italic_marker_rides_brush_and_shaping_stays_normal() {
        use crate::parley_text::{collect_layout, rasterize_out_glyphs};
        let mut pf = ParleyFonts::new();
        // Second entry is a CJK italic item name (byte-identical value via escapes).
        for text in ["Italic item", "\u{659C}\u{4F53}\u{7269}\u{54C1}\u{540D}"] {
            let normal = pf.layout_styled(text, 22.0, 1.55, false, false, None, None);
            let italic = pf.layout_styled(text, 22.0, 1.55, false, true, None, None);

            // Shaping must ALWAYS have matched Normal. font_attrs().style is
            // the matcher's verdict, and it must be Normal in BOTH cases
            // (that propagation would have made the italic layout
            // record FontStyle::Italic here).
            for line in normal.lines() {
                for r in line.runs() {
                    assert_eq!(
                        r.font_attrs().style,
                        FontStyle::Normal,
                        "upright run must keep FontStyle::Normal for {text:?}"
                    );
                }
            }
            for line in italic.lines() {
                for r in line.runs() {
                    assert_eq!(
                        r.font_attrs().style,
                        FontStyle::Normal,
                        "italic run must STILL match Normal (marker must not enter \
                         font matching) for {text:?}"
                    );
                }
            }

            // ① Marker readable on the glyph-run output layer.
            let (n_glyphs, ..) = collect_layout(&normal, &[], 0.0, 0.0, f32::MAX, &[]);
            let (i_glyphs, ..) = collect_layout(&italic, &[], 0.0, 0.0, f32::MAX, &[]);
            assert!(
                !n_glyphs.is_empty() && !i_glyphs.is_empty(),
                "expected shaped glyphs for {text:?} (system fonts required) - \
                 normal={} italic={}",
                n_glyphs.len(),
                i_glyphs.len()
            );
            assert!(
                n_glyphs.iter().all(|g| !g.italic),
                "upright glyphs must carry italic=false for {text:?}"
            );
            assert!(
                i_glyphs.iter().all(|g| g.italic),
                "italic marker must be readable on output glyphs for {text:?}"
            );

            // ② Latin/CJK advance diff == 0.
            let an = total_advance(&normal);
            let ai = total_advance(&italic);
            assert!(an.is_finite() && an >= 0.0, "normal advance {an} invalid");
            assert!(ai.is_finite() && ai >= 0.0, "italic advance {ai} invalid");
            assert!(
                (ai - an).abs() < 1e-3,
                "italic must NOT change shaping: advance diff must be 0 for \
                 {text:?}, got normal={an:.3} italic={ai:.3} diff={:+.3}",
                ai - an
            );

            // ③ Core regression criterion: quad keys byte-identical under
            // italic=true/false. A true-italic face selection would change
            // font bytes / glyph ids / advance and shift these keys.
            let (n_quads, _) = rasterize_out_glyphs(&n_glyphs, 2.0);
            let (i_quads, _) = rasterize_out_glyphs(&i_glyphs, 2.0);
            let n_keys: Vec<u64> = n_quads.iter().map(|q| q.bitmap_key).collect();
            let i_keys: Vec<u64> = i_quads.iter().map(|q| q.bitmap_key).collect();
            assert_eq!(
                n_keys, i_keys,
                "quad keys must be byte-identical under italic=true/false for \
                 {text:?} (a true-italic face swap would break this) - \
                 normal={n_keys:?} italic={i_keys:?}"
            );

            println!(
                "[italic_shaping] text={text:?} glyphs(normal/italic)={}/{} \
                 advance(normal)={an:.3} advance(italic)={ai:.3} diff={:+.3} \
                 quad-keys-identical={}",
                 n_glyphs.len(),
                i_glyphs.len(),
                ai - an,
                n_keys == i_keys
            );
        }
    }

    /// Atom-metrics declaration round-trip: build a FlatBuffer carrying
    /// the three new field groups the way the Java serializer writes them, and assert
    /// what the Rust reader reads back:
    ///   1. the InlineBlockRef ink box: ink_left/ink_right/ink_bottom
    ///   2. GlyphRun.baseline + PlacedGlyph.baseline, divide-by-scale-then-truncate
    ///   3. TextStyle.font_family, activating a previously dead field
    #[test]
    fn phase21_schema_fields_round_trip() {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        // TextStyle.font_family is written as a non-empty string.
        let ff = fbb.create_string("Minecraft Default");
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                font_family: Some(ff),
                ..Default::default()
            },
        );

        // InlineBlockRef ink box, written with non-zero values.
        let ibr = crate::fb::InlineBlockRef::create(
            &mut fbb,
            &crate::fb::InlineBlockRefArgs {
                node: 7,
                align: 2,
                param: 0.5,
                ink_left: 2.0,
                ink_right: 13.0,
                ink_bottom: 15.0,
            },
        );
        let ibr_vec = fbb.create_vector(&[ibr]);

        // TextData carries both.
        let text_off = fbb.create_string("A\u{FFFC}B");
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                inline_blocks: Some(ibr_vec),
                ..Default::default()
            },
        );
        let style = crate::fb::StyleArgs::default();
        let style = crate::fb::Style::create(&mut fbb, &style);
        let node = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[node]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: 500.0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        let buf = fbb.finished_data();

        // Read-back assertions on the reader side.
        let input = flatbuffers::root::<crate::fb::LayoutInput>(buf).expect("LayoutInput");
        let node0 = input.nodes().expect("nodes").get(0);
        let td = node0.text().expect("text");
        // 1) font_family
        let ts = td.style().expect("style");
        assert_eq!(
            ts.font_family(),
            Some("Minecraft Default"),
            "font_family must round-trip"
        );
        // 2) ink box
        let refs = td.inline_blocks().expect("inline_blocks");
        assert_eq!(refs.len(), 1);
        let r = refs.get(0);
        assert_eq!(r.node(), 7);
        assert_eq!(r.align(), 2);
        assert!((r.param() - 0.5).abs() < 1e-6);
        assert!((r.ink_left() - 2.0).abs() < 1e-6, "ink_left round-trip");
        assert!((r.ink_right() - 13.0).abs() < 1e-6, "ink_right round-trip");
        assert!((r.ink_bottom() - 15.0).abs() < 1e-6, "ink_bottom round-trip");
    }

    /// Font-family wire round-trip: the Java serializer is simulated by writing
    /// "Consolas" into both TextData.style.font_family and TextSpan.style.font_family,
    /// the Rust reader asserts what it reads back, and a full compute_layout run
    /// exercises the consumption path end to end, from the read in measure.rs through
    /// the ShapeRequest/SpanStyle carrier and shape_paragraph to the
    /// FontFamily::named injection.
    #[test]
    fn phase26_font_family_wire_round_trip() {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        // Write side, simulating LayoutNodeSerializer.buildTextData + buildFbTextStyle.
        let ff = fbb.create_string("Consolas");
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                font_family: Some(ff),
                ..Default::default()
            },
        );
        let span_style = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                font_family: Some(ff),
                ..Default::default()
            },
        );
        let span_text_off = fbb.create_string("Consolas probe");
        let span = crate::fb::TextSpan::create(
            &mut fbb,
            &crate::fb::TextSpanArgs {
                text: Some(span_text_off),
                style: Some(span_style),
            },
        );
        let spans_vec = fbb.create_vector(&[span]);
        let text_off = fbb.create_string("Consolas probe");
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                spans: Some(spans_vec),
                ..Default::default()
            },
        );
        let style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let node = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[node]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: 500.0,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        let buf = fbb.finished_data();

        // Read-back assertions on the reader side, at the wire level.
        let input = flatbuffers::root::<crate::fb::LayoutInput>(buf).expect("LayoutInput");
        let node0 = input.nodes().expect("nodes").get(0);
        let td = node0.text().expect("text");
        assert_eq!(
            td.style().expect("style").font_family(),
            Some("Consolas"),
            "base font_family must round-trip"
        );
        let span0 = td.spans().expect("spans").get(0);
        assert_eq!(
            span0.style().expect("style").font_family(),
            Some("Consolas"),
            "span font_family must round-trip"
        );

        // Full-pipeline consumption: a named family travels measure, shape and emit.
        let mut fs = GuideFontSystem::new();
        let out = compute_layout(buf, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
        let runs = result.glyph_runs().expect("glyph_runs");
        let mut total_glyphs = 0usize;
        for ri in 0..runs.len() {
            if let Some(g) = runs.get(ri).glyphs() {
                total_glyphs += g.len();
            }
        }
        assert!(
            total_glyphs > 0,
            "paragraph with font_family=\"Consolas\" must shape glyphs end-to-end \
             (system fonts required)"
        );
        println!(
            "[phase26_wire] base/span font_family round-trip OK, glyphs={total_glyphs}"
        );
    }

    /// Consumption injection assertion: once layout_styled's family parameter injects
    /// `FontFamily::named("Consolas")`, every glyph run's brush reads back
    /// `family == Some("Consolas")`, closing the observable loop from the write side
    /// through the wire and the carrier to the brush. This does not depend on Consolas
    /// actually being matched, because the brush carries the requested value.
    #[test]
    fn phase26_font_family_named_rides_brush() {
        use parley::PositionedLayoutItem;
        let mut pf = ParleyFonts::new();
        let layout = pf.layout_styled("Consolas", 22.0, 1.55, false, false, None, Some("Consolas"));
        let mut runs = 0usize;
        for line in layout.lines() {
            for item in line.items() {
                let PositionedLayoutItem::GlyphRun(gr) = item else {
                    continue;
                };
                runs += 1;
                assert_eq!(
                    gr.style().brush.family.as_deref(),
                    Some("Consolas"),
                    "named family must ride the run brush so the read-back is observable"
                );
            }
        }
        assert!(
            runs > 0,
            "expected shaped glyph runs for \"Consolas\" (system fonts required)"
        );
    }

    /// Fallback assertion, the key evidence that the change is behavior-neutral: when
    /// font_family is null, an empty string or all whitespace, no named family is
    /// injected, so brush.family is always None and shaping takes the SansSerif
    /// generic default of the original push_defaults, byte for byte unchanged.
    #[test]
    fn phase26_font_family_null_falls_back_to_sansserif() {
        use parley::PositionedLayoutItem;
        let mut pf = ParleyFonts::new();
        for (label, family) in [("null", None), ("empty", Some("")), ("whitespace", Some("   "))] {
            let layout = pf.layout_styled("abc", 22.0, 1.55, false, false, None, family);
            let mut runs = 0usize;
            for line in layout.lines() {
                for item in line.items() {
                    let PositionedLayoutItem::GlyphRun(gr) = item else {
                        continue;
                    };
                    runs += 1;
                    assert_eq!(
                        gr.style().brush.family,
                        None,
                        "family={label} must NOT inject a named family (SansSerif generic \
                         fallback - pre-wiring behavior)"
                    );
                }
            }
            assert!(
                runs > 0,
                "expected shaped glyph runs for family={label} (system fonts required)"
            );
        }
    }

    /// Observable assertion that an inline_code span's monospace declaration survives
    /// the whole pipeline. The write side, simulating
    /// LayoutNodeSerializer.buildFbTextStyle, writes font_family="monospace" for the
    /// inline_code span and nothing (null) for the body spans. After wire →
    /// measure_text → shape_paragraph → push_spans → collect_layout → OutGlyph:
    ///   1. glyphs of the mono span (span_index=1) carry Some("monospace"), the
    ///      monospace declaration;
    ///   2. glyphs of the body spans (span_index=0/2) carry None, meaning the
    ///      SansSerif generic fallback with no named family injected, which is the
    ///      criterion that distinguishes them from the mono span;
    /// the two being distinguishable means the inline_code monospace font really is
    /// active and does not contaminate the body runs.
    #[test]
    fn phase26b_inline_code_mono_glyph_family() {
        use flatbuffers::FlatBufferBuilder;
        use crate::measure::measure_text;

        let mut fbb = FlatBufferBuilder::with_capacity(1024);
        let mono_off = fbb.create_string("monospace");
        // spans: [0]="abc" body, [1]="123" inline_code mono, [2]="xyz" body
        let mut span_offsets = Vec::new();
        for (t, fam, inline_code) in [
            ("abc", None, false),
            ("123", Some(mono_off), true),
            ("xyz", None, false),
        ] {
            let st = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size: 11.0,
                    font_family: fam,
                    inline_code,
                    ..Default::default()
                },
            );
            let span_text_off = fbb.create_string(t);
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(span_text_off),
                    style: Some(st),
                },
            ));
        }
        let spans_vec = fbb.create_vector(&span_offsets);
        let text_off = fbb.create_string("abc123xyz");
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                spans: Some(spans_vec),
                ..Default::default()
            },
        );
        let style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let node = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let nodes_vec = fbb.create_vector(&[node]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: 500.0,
                justify: 0,
                nodes: Some(nodes_vec),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        let buf = fbb.finished_data();

        // Full pipeline: wire → measure_text → shape_paragraph → collect_layout → OutGlyph
        let input = flatbuffers::root::<crate::fb::LayoutInput>(buf).expect("LayoutInput");
        let fb_nodes = input.nodes().expect("nodes");
        let nodes: Vec<crate::fb::FlatNode> = (0..fb_nodes.len()).map(|i| fb_nodes.get(i)).collect();
        let mut fs = GuideFontSystem::new();
        let mut acc: std::collections::HashMap<usize, crate::measure::GlyphAccum> = Default::default();
        let _ = measure_text(
            &mut fs,
            &nodes,
            0,
            &mut acc,
            taffy::Size {
                width: taffy::AvailableSpace::Definite(500.0),
                height: taffy::AvailableSpace::MaxContent,
            },
            false,
            &[],
            0.0,
            0.0,
            &[],
        );
        let glyphs = &acc[&0].glyphs;
        assert!(
            !glyphs.is_empty(),
            "paragraph must shape glyphs (system fonts required)"
        );

        // span_index 0/2 are body spans (SansSerif fallback, brush.family = None);
        // span_index 1 is the inline_code span, declared "monospace", so
        // brush.family = Some("monospace").
        let mono: Vec<_> = glyphs.iter().filter(|g| g.span_index == 1).collect();
        let body: Vec<_> = glyphs.iter().filter(|g| g.span_index != 1).collect();
        assert!(!mono.is_empty(), "inline_code span must produce glyphs");
        assert!(!body.is_empty(), "body spans must produce glyphs");
        for g in &mono {
            assert_eq!(
                g.family.as_deref(),
                Some("monospace"),
                "inline_code span glyphs must carry the declared mono family"
            );
        }
        for g in &body {
            assert_eq!(
                g.family, None,
                "body span glyphs must fall back to SansSerif (no named family injected)"
            );
        }
        // Discrimination assertion: the family requests of the mono span and the body
        // spans must differ, showing that the inline_code monospace declaration
        // survives the whole pipeline without contaminating the body runs.
        assert!(
            mono.iter().any(|g| g.family.as_deref() == Some("monospace"))
                && body.iter().all(|g| g.family.is_none()),
            "mono and body glyph families must be distinguishable \
             (mono=Some(monospace), body=None)"
        );
        println!(
            "[phase26b] inline_code mono: {} mono glyphs (family=monospace), \
             {} body glyphs (family=None)",
            mono.len(),
            body.len()
        );

        // Observable at the brush level (the layout_styled single-style path, which is
        // the Java shapeText pipeline): after a "monospace" declaration goes through
        // resolve_family to Generic(Monospace), the run brush still carries the
        // requested name Some("monospace"), because the brush is not rewritten by the
        // generic mapping.
        use parley::PositionedLayoutItem;
        let layout = ParleyFonts::new().layout_styled(
            "mono", 22.0, 1.55, false, false, None, Some("monospace"));
        let mut mono_runs = 0usize;
        for line in layout.lines() {
            for item in line.items() {
                let PositionedLayoutItem::GlyphRun(gr) = item else {
                    continue;
                };
                mono_runs += 1;
                assert_eq!(
                    gr.style().brush.family.as_deref(),
                    Some("monospace"),
                    "mono declaration must ride the run brush (Generic(Monospace) \
                     resolution must not rewrite the brush request)"
                );
            }
        }
        assert!(
            mono_runs > 0,
            "expected shaped glyph runs for \"mono\" with family=monospace \
             (system fonts required)"
        );
    }

    /// GlyphRun/PlacedGlyph baseline round-trip. A real layout run asserts
    /// that the emitted baseline and the glyph y (the quad top) follow the same
    /// divide-by-scale-then-truncate convention:
    ///   q.baseline = (g.y * render_scale).trunc() / render_scale
    ///   q.y        = ((g.y * render_scale).trunc() - top) / render_scale
    /// Both derive from the same truncated pixel yi, and both add the same node
    /// absolute y on PlacedGlyph output, so baseline - y always equals the bitmap's
    /// placement.top, an integer.
    /// The test also asserts that the baseline falls inside the glyph quad's y..y+h
    /// range, that is, inside the glyph band.
    #[test]
    fn phase21_baseline_return_on_real_layout() {
        const TEXT: &str = "Baseline probe";
        let mut fs = GuideFontSystem::new();
        let input = build_paragraph_input(TEXT, 0, None, None, 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let runs = result.glyph_runs().expect("glyph_runs");
        assert!(
            !runs.is_empty(),
            "expected at least one glyph run for {TEXT:?} (system fonts required)"
        );
        let mut glyphs_seen = 0usize;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            // The run-level baseline is the first glyph's baseline, in absolute document
            // coordinates.
            let run_baseline = run.baseline();
            let gs = run.glyphs().expect("run glyphs");
            assert!(!gs.is_empty(), "run must have glyphs");
            for gi in 0..gs.len() {
                let pg = gs.get(gi);
                glyphs_seen += 1;
                let bl = pg.baseline();
                // Divide-by-scale-then-truncate consistency: the baseline and the quad
                // top y derive from the same truncated pixel yi, so their difference is
                // placement.top, an integer. At render_scale=1 this is pixel-grid
                // alignment.
                let diff = bl - pg.y();
                assert!(
                    (diff - diff.trunc()).abs() < 1e-3,
                    "baseline-y diff must be pixel-aligned (÷scale+trunc), got {diff}"
                );
                // The baseline falls inside the glyph quad's vertical range, that is,
                // within the glyph band.
                assert!(
                    bl >= pg.y() - 1.0 && bl <= pg.y() + pg.h() + 1.0,
                    "baseline {bl} must sit near glyph quad [y={}, y+h={}]",
                    pg.y(),
                    pg.y() + pg.h()
                );
                if gi == 0 {
                    assert!(
                        (bl - run_baseline).abs() < 1e-3,
                        "GlyphRun.baseline must equal its first glyph's baseline \
                         (run={run_baseline}, glyph={bl})"
                    );
                }
            }
        }
        assert!(glyphs_seen > 0, "expected shaped glyphs for {TEXT:?}");
        println!(
            "[phase21_baseline] runs={} glyphs={glyphs_seen} first_run_baseline={}",
            runs.len(),
            runs.get(0).baseline()
        );
    }

    /// One synthetic rasterized glyph quad for the functional boundary tests.
    fn mk_quad(x: f32, y: f32, w: f32, h: f32, line: u32, span: u32) -> crate::parley_text::ParleyRasterGlyph {
        crate::parley_text::ParleyRasterGlyph {
            bitmap_key: 0,
            x,
            y,
            w,
            h,
            line_index: line,
            span_index: span,
            baseline: y + h,
        }
    }

    fn mk_style(italic: bool) -> super::SpanStyleInfo {
        super::SpanStyleInfo {
            color: 0xFFFFFFFF,
            italic,
            underline: false,
            strikethrough: false,
            highlight_argb: 0,
            inline_code: false,
            wavy_underline: false,
            dotted_underline: false,
        }
    }

    /// The boundary-correction formula itself.
    /// Drives `apply_italic_boundary_correction` with synthetic quads so every
    /// acceptance branch is asserted exactly:
    ///   ① positive italic→upright boundary inserts the shear-overhang gap and
    ///      leaves the italic run's interior untouched (run-internal advance
    ///      unchanged, a uniform per-run shift);
    ///   ② negative boundary (natural gap > overhang) yields zero correction;
    ///   ③ upright→italic boundary is never corrected;
    ///   ④ cascading boundaries each insert their own one-shot gap with NO
    ///      accumulation across boundaries;
    ///   ⑤ all-upright paragraph is byte-identical output;
    ///   ⑥ REJECT fix: an atom marker's pen between the italic run and the next
    ///      upright run (italic→atom→upright) makes the boundary pass SKIP it; the
    ///      the atom boundary correction owns it (same-geometry control without
    ///      the marker still corrects, proving the skip is atom-scoped).
    #[test]
    fn phase22_italic_boundary_correction_functional() {
        use super::apply_italic_boundary_correction;

        // ① positive boundary: italic(span0) → upright(span1), zero natural gap.
        let mut quads = vec![
            mk_quad(0.0, 2.0, 8.0, 10.0, 0, 0),
            mk_quad(8.0, 5.0, 8.0, 10.0, 0, 0),
            mk_quad(16.0, 2.0, 8.0, 10.0, 0, 1),
            mk_quad(24.0, 2.0, 8.0, 10.0, 0, 1),
        ];
        let styles = vec![mk_style(true), mk_style(false)];
        apply_italic_boundary_correction(&mut quads, &[], &styles, false, 0.25);
        // shearBase(span0) = max(2+10, 5+10) = 15; last glyph top y = 5
        // shearShift = 0.25 × (15 − 5) = 2.5; inkGap = 16 − (8+8) = 0 → c = 2.5
        assert!((quads[0].x - 0.0).abs() < 1e-6, "italic run interior must not move");
        assert!((quads[1].x - 8.0).abs() < 1e-6, "italic run interior must not move");
        assert!(
            (quads[2].x - 18.5).abs() < 1e-6,
            "upright run shifted by correction, got {}",
            quads[2].x
        );
        assert!((quads[3].x - 26.5).abs() < 1e-6);
        // run-internal advance unchanged: upright glyph spacing stays 8.
        assert!(
            ((quads[3].x - quads[2].x) - 8.0).abs() < 1e-6,
            "run-internal advance must be preserved by a uniform shift"
        );

        // ② negative boundary: natural gap (4) > shear overhang (2.5) → zero.
        let mut quads = vec![
            mk_quad(0.0, 2.0, 8.0, 10.0, 0, 0),
            mk_quad(8.0, 5.0, 8.0, 10.0, 0, 0),
            mk_quad(20.0, 2.0, 8.0, 10.0, 0, 1), // gap = 20 − 16 = 4 > 2.5
        ];
        apply_italic_boundary_correction(&mut quads, &[], &styles, false, 0.25);
        assert!(
            (quads[2].x - 20.0).abs() < 1e-6,
            "zero correction when natural gap exceeds the overhang"
        );

        // ③ upright→italic boundary: never corrected.
        let mut quads = vec![
            mk_quad(0.0, 2.0, 8.0, 10.0, 0, 0),
            mk_quad(8.0, 5.0, 8.0, 10.0, 0, 1),
        ];
        let styles_rev = vec![mk_style(false), mk_style(true)];
        apply_italic_boundary_correction(&mut quads, &[], &styles_rev, false, 0.25);
        assert!(
            (quads[1].x - 8.0).abs() < 1e-6,
            "upright→italic boundary must be untouched"
        );

        // ④ cascading: italic → upright → italic → upright, one-shot each, no accumulation.
        let mut quads = vec![
            mk_quad(0.0, 4.0, 6.0, 12.0, 0, 0),
            mk_quad(6.0, 2.0, 6.0, 8.0, 0, 1),
            mk_quad(12.0, 4.0, 6.0, 12.0, 0, 2),
            mk_quad(18.0, 2.0, 6.0, 8.0, 0, 3),
        ];
        let styles4 = vec![
            mk_style(true),
            mk_style(false),
            mk_style(true),
            mk_style(false),
        ];
        apply_italic_boundary_correction(&mut quads, &[], &styles4, false, 0.25);
        // boundary1: span0 shearBase = 4+12 = 16; last top y = 4 → shift = 3; gap = 6−(0+6)=0 → c1 = 3
        // boundary2: span2 shearBase = 16; last top y = 4 → shift = 3; gap = 18−(12+6)=0 → c2 = 3
        assert!(
            (quads[1].x - 9.0).abs() < 1e-6,
            "first upright shifted by its own c1, got {}",
            quads[1].x
        );
        assert!(
            (quads[2].x - 12.0).abs() < 1e-6,
            "middle italic run must NOT shift"
        );
        assert!(
            (quads[3].x - 21.0).abs() < 1e-6,
            "second upright shifted by c2 ONLY (no accumulation), got {}",
            quads[3].x
        );

        // ⑤ all-upright paragraph: identical output.
        let mut quads = vec![
            mk_quad(0.0, 2.0, 8.0, 10.0, 0, 0),
            mk_quad(8.0, 2.0, 8.0, 10.0, 0, 0),
        ];
        let styles_flat = vec![mk_style(false)];
        apply_italic_boundary_correction(&mut quads, &[], &styles_flat, false, 0.25);
        assert!(
            (quads[1].x - 8.0).abs() < 1e-6,
            "all-upright paragraph must be byte-identical"
        );

        // ⑥ Rejection fix (double application): italic→atom→upright, with the atom
        // marker's pen falling between the last italic glyph and the next upright
        // glyph on the same line. The boundary pass is blind to the atom, so without
        // the skip it would apply correction_22 as if this were a direct text boundary
        // whose gap contains the atom advance, while the atom correction's pass 2
        // shifts the same upright quads' tail by c_23: a double application.
        // The geometry here is gap_22 = 17 - (8+8) = 1, below the shear overhang of
        // 2.5, so without the atom the upright text would move right by 1.5; with the
        // atom (pen=12 inside [8,17)) the whole boundary must be skipped and nothing
        // may move at all.
        let mut quads = vec![
            mk_quad(0.0, 2.0, 8.0, 10.0, 0, 0),
            mk_quad(8.0, 5.0, 8.0, 10.0, 0, 0),
            mk_quad(17.0, 2.0, 8.0, 10.0, 0, 1), // gap_22 = 1 < 2.5
            mk_quad(25.0, 2.0, 8.0, 10.0, 0, 1),
        ];
        let markers = vec![crate::measure::InlineMarker {
            pen_x: 12.0,
            baseline_y: 10.0,
            line_top: 0.0,
            line_height: 15.0,
            line_index: 0,
            advance: 5.0, // the atom box occupies [12,17), so the upright run starts at 17
        }];
        apply_italic_boundary_correction(&mut quads, &markers, &styles, false, 0.25);
        assert!(
            (quads[2].x - 17.0).abs() < 1e-6,
            "italic→atom→upright boundary must be SKIPPED by 2.2 (2.3 owns it), \
             got {} - double application would have shifted it by correction_22",
            quads[2].x
        );
        assert!(
            (quads[3].x - 25.0).abs() < 1e-6,
            "atom-between upright run must stay untouched, got {}",
            quads[3].x
        );
        assert!((quads[0].x - 0.0).abs() < 1e-6 && (quads[1].x - 8.0).abs() < 1e-6);
        // The same geometry with the marker removed, so the atom is gone and this is a
        // plain text boundary: the upright text must still move right by 1.5, proving
        // the skip is triggered precisely by an atom pen falling between the two runs
        // and does not affect ordinary boundaries.
        let mut quads2 = vec![
            mk_quad(0.0, 2.0, 8.0, 10.0, 0, 0),
            mk_quad(8.0, 5.0, 8.0, 10.0, 0, 0),
            mk_quad(17.0, 2.0, 8.0, 10.0, 0, 1),
            mk_quad(25.0, 2.0, 8.0, 10.0, 0, 1),
        ];
        apply_italic_boundary_correction(&mut quads2, &[], &styles, false, 0.25);
        assert!(
            (quads2[2].x - 18.5).abs() < 1e-6,
            "without the atom the same boundary must still correct (c=1.5), got {}",
            quads2[2].x
        );
    }

    /// End-to-end: a rich paragraph of `italic run + upright run` on one
    /// line gets a positive boundary gap in the emitted PlacedGlyph positions. That
    /// gap is the overhang the renderer's synthetic shear would otherwise push into
    /// the following upright run, and it matches the shear overhang
    /// K × (shearBase − lastTop) within rasterization rounding.
    #[test]
    fn phase22_italic_boundary_positive_gap_on_real_layout() {
        let mut fs = GuideFontSystem::new();
        let input = build_rich_paragraph_input(&[("Hi", true), ("tail", false)], 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
        let runs = result.glyph_runs().expect("glyph_runs");
        let mut italic_run = None;
        let mut upright_run = None;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.shear() {
                italic_run = Some(run);
            } else {
                upright_run = Some(run);
            }
        }
        let (italic_run, upright_run) = match (italic_run, upright_run) {
            (Some(a), Some(b)) => (a, b),
            _ => panic!("expected one italic and one upright run (system fonts required)"),
        };
        let it = italic_run.glyphs().expect("italic glyphs");
        let up = upright_run.glyphs().expect("upright glyphs");
        assert!(!it.is_empty() && !up.is_empty(), "both runs must have glyphs");

        let mut last_ix = f32::MIN;
        let mut last_top = 0.0f32;
        let mut last_w = 0.0f32;
        let mut shear_base = 0.0f32;
        for gi in 0..it.len() {
            let g = it.get(gi);
            shear_base = shear_base.max(g.y() + g.h());
            if g.x() > last_ix {
                last_ix = g.x();
                last_top = g.y();
                last_w = g.w();
            }
        }
        let mut first_ux = f32::MAX;
        for gi in 0..up.len() {
            let g = up.get(gi);
            first_ux = first_ux.min(g.x());
        }
        let gap = first_ux - (last_ix + last_w);
        let expected = 0.25 * (shear_base - last_top);
        assert!(
            gap > 0.0,
            "positive italic→upright boundary must insert a gap, got {gap}"
        );
        assert!(
            (gap - expected).abs() < 1.5,
            "boundary gap {gap} should match the shear overhang {expected} \
             (within 1px rasterization rounding)"
        );
        println!(
            "[italic_correction] runs={} gap={gap:.3} expected_overhang={expected:.3} \
             shear_base={shear_base:.3} last_top={last_top:.3}",
            runs.len()
        );
    }

    /// End-to-end, negative boundary: an italic run followed by a
    /// SPACE then an upright run already has a natural gap (the space advance).
    /// The correction may only ADD at most the shear overhang; it must not
    /// overshoot into the natural gap, i.e. the resulting gap must stay within
    /// [spaceAdvance, spaceAdvance + overhang].
    #[test]
    fn phase22_italic_boundary_negative_gap_stays_within_natural() {
        let mut fs = GuideFontSystem::new();
        // Same run pair, but the italic run carries a trailing space: natural
    /// gap = space advance (no space quad is emitted, since rasterize skips zero-ink
    /// bitmaps, so the gap is visible between the runs).
        let input = build_rich_paragraph_input(&[("Hi ", true), ("tail", false)], 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
        let runs = result.glyph_runs().expect("glyph_runs");
        let mut italic_run = None;
        let mut upright_run = None;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.shear() {
                italic_run = Some(run);
            } else {
                upright_run = Some(run);
            }
        }
        let (italic_run, upright_run) = match (italic_run, upright_run) {
            (Some(a), Some(b)) => (a, b),
            _ => panic!("expected one italic and one upright run (system fonts required)"),
        };
        let it = italic_run.glyphs().expect("italic glyphs");
        let up = upright_run.glyphs().expect("upright glyphs");

        let mut last_ix = f32::MIN;
        let mut last_top = 0.0f32;
        let mut last_w = 0.0f32;
        let mut shear_base = 0.0f32;
        for gi in 0..it.len() {
            let g = it.get(gi);
            shear_base = shear_base.max(g.y() + g.h());
            if g.x() > last_ix {
                last_ix = g.x();
                last_top = g.y();
                last_w = g.w();
            }
        }
        let mut first_ux = f32::MAX;
        for gi in 0..up.len() {
            let g = up.get(gi);
            first_ux = first_ux.min(g.x());
        }
        let gap = first_ux - (last_ix + last_w);
        let overhang = 0.25 * (shear_base - last_top);
        // Natural space gap is positive; the correction may at most close the
        // remaining overhang, never inflate the gap beyond natural + overhang.
        assert!(
            gap >= 0.5,
            "natural space gap must remain, got {gap}"
        );
        assert!(
            gap <= overhang + 6.0,
            "negative-boundary gap {gap} must not overshoot natural space + overhang ({overhang})"
        );
        println!(
            "[italic_correction_negative] gap={gap:.3} overhang={overhang:.3}",
        );
    }

    /// A SINGLE-STYLE (no spans) italic paragraph must still shear, so
    /// GlyphRun.shear falls back to TextData.style.italic. This
    /// direction is re-applied on the Rust side: without it the
    /// whole-paragraph italic fixture (*H I l H I l g y.*) renders unslanted.
    #[test]
    fn phase22_single_style_italic_paragraph_shears() {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                italic: true,
                ..Default::default()
            },
        );
        let text_off = fbb.create_string("H I l");
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                ..Default::default()
            },
        );
        let style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let node = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[node]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: 500.0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        let mut fs = GuideFontSystem::new();
        let out = compute_layout(&fbb.finished_data(), &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
        let runs = result.glyph_runs().expect("glyph_runs");
        assert!(
            !runs.is_empty(),
            "expected glyph runs for single-style italic paragraph"
        );
        let run = runs.get(0);
        assert!(
            run.shear(),
            "single-style italic paragraph must shear (base_italic fallback)"
        );
        assert!(
            run.glyphs().expect("glyphs").len() > 0,
            "run must have glyphs"
        );
        println!(
            "[italic_single_style] runs={} shear={} glyphs={}",
            runs.len(),
            run.shear(),
            run.glyphs().expect("glyphs").len()
        );
    }

    /// Test helper: build a LayoutInput with node 0 = paragraph whose
    /// rich spans are `before` (document order), then a U+FFFC atom placeholder,
    /// then `after`; node 1 = the atom block (node_type 2 image, explicit square
    /// size, position:absolute so the inline post-pass anchors it). `ink` =
    /// (ink_left, ink_right, ink_bottom) in 16-unit icon space, or None (all 0 =
    /// undeclared). `atom_align` is the InlineBlockRef.align (2 = the retired
    /// centering mode, meaningful only when ink is None). Default font 11.
    fn build_atom_input(
        before: &[(&str, bool)],
        after: &[(&str, bool)],
        atom_size: (f32, f32),
        ink: Option<(f32, f32, f32)>,
        atom_align: i8,
        avail_width: f32,
    ) -> Vec<u8> {
        build_atom_input_at(before, after, atom_size, ink, atom_align, avail_width, 11.0, 0.0)
    }

    /// `build_atom_input` with an explicit paragraph font size (REJECT-fix test
    /// magnifies the shear overhang S so that gap_22 < S holds and correction_22 > 0,
    /// which lets an end-to-end test tell a single c_23 from the double application
    /// c_23 + correction_22). `param` is InlineBlockRef.param, the yOffset on the
    /// serialization side, which the review test uses to check that yOffset
    /// consumption is restored.
    #[allow(clippy::too_many_arguments)]
    fn build_atom_input_at(
        before: &[(&str, bool)],
        after: &[(&str, bool)],
        atom_size: (f32, f32),
        ink: Option<(f32, f32, f32)>,
        atom_align: i8,
        avail_width: f32,
        font_size: f32,
        param: f32,
    ) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size,
                ..Default::default()
            },
        );
        let mut span_offsets = Vec::new();
        let mut full = String::new();
        for (t, italic) in before {
            let st = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size,
                    italic: *italic,
                    ..Default::default()
                },
            );
            let t_off = fbb.create_string(t);
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(t_off),
                    style: Some(st),
                },
            ));
            full.push_str(t);
        }
        // U+FFFC placeholder part in the paragraph base style, mirroring Java's
        // collectSpanParts ("Inline blocks become U+FFFC placeholder parts").
        {
            let st = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size,
                    italic: false,
                    ..Default::default()
                },
            );
            let t_off = fbb.create_string("\u{FFFC}");
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(t_off),
                    style: Some(st),
                },
            ));
            full.push('\u{FFFC}');
        }
        for (t, italic) in after {
            let st = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size,
                    italic: *italic,
                    ..Default::default()
                },
            );
            let t_off = fbb.create_string(t);
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(t_off),
                    style: Some(st),
                },
            ));
            full.push_str(t);
        }

        let (il, ir, ib) = ink.unwrap_or((0.0, 0.0, 0.0));
        let ibr = crate::fb::InlineBlockRef::create(
            &mut fbb,
            &crate::fb::InlineBlockRefArgs {
                node: 1,
                align: atom_align,
                param,
                ink_left: il,
                ink_right: ir,
                ink_bottom: ib,
            },
        );
        let ibr_vec = fbb.create_vector(&[ibr]);
        let spans_vec = fbb.create_vector(&span_offsets);
        let text_off = fbb.create_string(&full);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                inline_blocks: Some(ibr_vec),
                spans: Some(spans_vec),
                ..Default::default()
            },
        );
        let para_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let para = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(para_style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let sz_w = crate::fb::Dimension::create(
            &mut fbb,
            &crate::fb::DimensionArgs {
                value: atom_size.0,
                unit: 1,
            },
        );
        let sz_h = crate::fb::Dimension::create(
            &mut fbb,
            &crate::fb::DimensionArgs {
                value: atom_size.1,
                unit: 1,
            },
        );
        let atom_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 1, // pos_abs → anchored by the inline post-pass
                size_w: Some(sz_w),
                size_h: Some(sz_h),
                ..Default::default()
            },
        );
        let img = crate::fb::ImageData::create(
            &mut fbb,
            &crate::fb::ImageDataArgs {
                explicit_w: atom_size.0,
                explicit_h: atom_size.1,
                ..Default::default()
            },
        );
        let atom = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(atom_style),
                node_type: 2,
                image: Some(img),
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[para, atom]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// Multi-atom paragraph input: N words, each immediately followed by an
    /// inline-block atom in document order (`word_k → atom_k`), mirroring
    /// LayoutNodeSerializer's per-icon rich-span emission. Each word is its own
    /// TextSpan; each atom its own node (1+k) + InlineBlockRef with the given
    /// ink box. `words_italic` toggles the word spans (the upright twin builds
    /// the identical natural geometry, because italic only rides the brush).
    #[allow(clippy::too_many_arguments)]
    fn build_multi_atom_input(
        words: &[&str],
        words_italic: bool,
        inks: &[(f32, f32, f32)],
        atom_size: (f32, f32),
        avail_width: f32,
        font_size: f32,
    ) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size,
                ..Default::default()
            },
        );
        let mut span_offsets = Vec::new();
        let mut refs = Vec::new();
        let mut full = String::new();
        for (wi, w) in words.iter().enumerate() {
            let st = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size,
                    italic: words_italic,
                    ..Default::default()
                },
            );
            let t_off = fbb.create_string(w);
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(t_off),
                    style: Some(st),
                },
            ));
            full.push_str(w);
            // U+FFFC placeholder span (base style), mirroring collectSpanParts.
            let st2 = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size,
                    italic: false,
                    ..Default::default()
                },
            );
            let p_off = fbb.create_string("\u{FFFC}");
            span_offsets.push(crate::fb::TextSpan::create(
                &mut fbb,
                &crate::fb::TextSpanArgs {
                    text: Some(p_off),
                    style: Some(st2),
                },
            ));
            full.push('\u{FFFC}');
            let (il, ir, ib) = inks[wi];
            refs.push(crate::fb::InlineBlockRef::create(
                &mut fbb,
                &crate::fb::InlineBlockRefArgs {
                    node: (1 + wi) as u32,
                    align: 2,
                    param: 0.0,
                    ink_left: il,
                    ink_right: ir,
                    ink_bottom: ib,
                },
            ));
        }
        let refs_vec = fbb.create_vector(&refs);
        let spans_vec = fbb.create_vector(&span_offsets);
        let text_off = fbb.create_string(&full);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                inline_blocks: Some(refs_vec),
                spans: Some(spans_vec),
                ..Default::default()
            },
        );
        let para_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let para = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(para_style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );
        let mut node_offsets = vec![para];
        for _ in 0..words.len() {
            let sz_w = crate::fb::Dimension::create(
                &mut fbb,
                &crate::fb::DimensionArgs {
                    value: atom_size.0,
                    unit: 1,
                },
            );
            let sz_h = crate::fb::Dimension::create(
                &mut fbb,
                &crate::fb::DimensionArgs {
                    value: atom_size.1,
                    unit: 1,
                },
            );
            let atom_style = crate::fb::Style::create(
                &mut fbb,
                &crate::fb::StyleArgs {
                    position: 1,
                    size_w: Some(sz_w),
                    size_h: Some(sz_h),
                    ..Default::default()
                },
            );
            let img = crate::fb::ImageData::create(
                &mut fbb,
                &crate::fb::ImageDataArgs {
                    explicit_w: atom_size.0,
                    explicit_h: atom_size.1,
                    ..Default::default()
                },
            );
            node_offsets.push(crate::fb::FlatNode::create(
                &mut fbb,
                &crate::fb::FlatNodeArgs {
                    style: Some(atom_style),
                    node_type: 2,
                    image: Some(img),
                    ..Default::default()
                },
            ));
        }
        let nodes = fbb.create_vector(&node_offsets);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// All emitted glyphs of a result as (x, y, w, h, baseline), sorted by x
    /// (paragraph-absolute document coordinates, top-left origin).
    fn sorted_glyphs(result: &crate::fb::LayoutResult) -> Vec<(f32, f32, f32, f32, f32)> {
        let mut v = Vec::new();
        if let Some(runs) = result.glyph_runs() {
            for ri in 0..runs.len() {
                let run = runs.get(ri);
                if let Some(gs) = run.glyphs() {
                    for gi in 0..gs.len() {
                        let g = gs.get(gi);
                        v.push((g.x(), g.y(), g.w(), g.h(), g.baseline()));
                    }
                }
            }
        }
        v.sort_by(|a, b| a.0.partial_cmp(&b.0).unwrap());
        v
    }

    /// The atomic ink box decides advance and
    /// baseline.
    ///   ① the ink box values reach the wire and are non-zero (a
    ///      round-trip through the Rust reader);
    ///   ② the advance semantics. The older formula treated the ink box's
    ///      right edge position ir (0-15 within the cell) as the right side-bearing
    ///      width, which for real icons (il around 0..1, ir around 13..15) blew the
    ///      advance up to 27-33px against a cell width of only 16. Under the
    ///      inclusive-endpoint IconMetrics convention (width = ir - il + 1) the
    ///      correct right bearing is the cell right edge (15) minus the ink right
    ///      edge (ir), and the il term cancels, so the advance is always one full
    ///      16-unit cell = cell_h = 16px:
    ///      - full grid (il=0, ir=15): advance is exactly 16, one whole cell;
    ///      - typical (il=1, ir=14): advance = 16, and the optical gap from the icon
    ///        ink to the next glyph's ink left edge is the right bearing 15 - ir = 1
    ///        logical px, instead of the swallowed 29px gap;
    ///      - small-value boundary (il=2, ir=3): advance = 16 as well, since any
    ///        declared ink box is one whole cell, correcting the old 2+2+3=7
    ///        shrinkage;
    ///      - no-ink control = a 16px grid cell, so the advance semantics converge on
    ///        the same cell width.
    ///   ③ the atomic ink bottom edge sits on the text baseline, with
    ///      ink_bottom × cell/16 = 10px above the baseline.
    #[test]
    fn phase23_ink_atom_advance_and_baseline() {
        let mut fs = GuideFontSystem::new();
        // Realistic-magnitude ink boxes: full grid (0,15), typical (1,14) and the
        // small-value boundary (2,3).
        let full = Some((0.0, 15.0, 10.0));
        let typical = Some((1.0, 14.0, 10.0));
        let small = Some((2.0, 3.0, 10.0));
        let ink_input = build_atom_input(
            &[("A", false)],
            &[("B", false)],
            (16.0, 16.0),
            typical,
            2,
            500.0,
        );
        let full_input = build_atom_input(&[("A", false)], &[("B", false)], (16.0, 16.0), full, 2, 500.0);
        let small_input = build_atom_input(
            &[("A", false)],
            &[("B", false)],
            (16.0, 16.0),
            small,
            2,
            500.0,
        );
        let plain_input = build_atom_input(
            &[("A", false)],
            &[("B", false)],
            (16.0, 16.0),
            None,
            2,
            500.0,
        );

        // ① Wire truth: the ink box rides the wire nonzero (round-trip).
        let wire = flatbuffers::root::<crate::fb::LayoutInput>(&ink_input).expect("LayoutInput");
        let wref = wire
            .nodes()
            .expect("nodes")
            .get(0)
            .text()
            .expect("text")
            .inline_blocks()
            .expect("inline_blocks")
            .get(0);
        assert!(
            wref.ink_left() > 0.0 && wref.ink_right() > 0.0 && wref.ink_bottom() > 0.0,
            "ink box truth must ride the wire nonzero (got {} {} {})",
            wref.ink_left(),
            wref.ink_right(),
            wref.ink_bottom()
        );

        // Layout+measure helper: (atom_x, atom_y, next-glyph x, next-glyph baseline).
        let measure = |fs: &mut GuideFontSystem, bytes: &[u8]| -> (f32, f32, f32, f32) {
            let out = compute_layout(bytes, fs);
            let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
            let atom = result.nodes().expect("nodes").get(1);
            let (ax, ay) = (atom.x(), atom.y());
            let glyphs = sorted_glyphs(&result);
            assert_eq!(
                glyphs.len(),
                2,
                "expected the 'A' and 'B' glyphs, got {}",
                glyphs.len()
            );
            (ax, ay, glyphs[1].0, glyphs[1].4)
        };

        let (ax, ay, b_x, baseline) = measure(&mut fs, &ink_input);
        let (fx, _, f_bx, _) = measure(&mut fs, &full_input);
        let (sx, _, s_bx, _) = measure(&mut fs, &small_input);
        let (px, _, p_bx, _) = measure(&mut fs, &plain_input);

        // ② Full grid (il=0, ir=15): the advance is exactly 16, one whole cell.
        let full_advance = f_bx - fx;
        assert!(
            (full_advance - 16.0).abs() < 1.5,
            "full-grid ink box must advance exactly one cell (16), got {full_advance}"
        );
        // Typical (il=1, ir=14): advance = 16, so the old (1+14+14) = 29px explosion is corrected.
        let ink_advance = b_x - ax;
        assert!(
            (ink_advance - 16.0).abs() < 1.5,
            "typical ink box (ir=14) must advance a full cell (16), got {ink_advance}"
        );
        assert!(
            ink_advance < 20.0,
            "the fix must kill the old formula's 27-33px explosion (got {ink_advance})"
        );
        // Icon ink right edge = ax + (il+width) × cell/16 = ax + (ir+1) = ax + 15; the
        // optical gap to the next glyph's ink left edge b_x is the right bearing
        // 15 - ir = 1 logical px.
        let gap = b_x - (ax + 15.0);
        assert!(
            (gap - 1.0).abs() < 2.0,
            "typical icon ink→text optical gap must equal the right bearing 15−ir = 1, got {gap}"
        );
        assert!(
            gap < 5.0,
            "the old formula's ~14px post-icon gap must be gone (got {gap})"
        );
        // Small-value boundary (il=2, ir=3): advance = 16 as well, because the il term
        // cancels and any declared ink box is one whole cell, no longer shrinking to
        // the old 2+2+3=7.
        let small_advance = s_bx - sx;
        assert!(
            (small_advance - 16.0).abs() < 1.5,
            "small ink box (boundary) must also advance a full cell (16), got {small_advance}"
        );
        // No-ink control: advance = the declared cell width of 16px, so it converges on
        // the same cell width as the ink-box semantics.
        let cell_advance = p_bx - px;
        assert!(
            (cell_advance - 16.0).abs() < 1.5,
            "no-ink atom keeps the grid-cell advance, got {cell_advance}"
        );

        // ③ The ink bottom edge sits on the baseline: atom.y + ink_bottom_px = baseline,
        //    with ink_bottom_px = 10 × 16/16 = 10.
        let ink_bottom_px = 10.0;
        assert!(
            ((ay + ink_bottom_px) - baseline).abs() < 1.5,
            "ink bottom edge must sit on the text baseline: atom.y({ay}) + {ink_bottom_px} \
             vs baseline {baseline}"
        );
        println!(
            "[phase23_ink_atom] full_advance={full_advance:.3} ink_advance={ink_advance:.3} \
             gap_after_ink={gap:.3} small_advance={small_advance:.3} \
             cell_advance={cell_advance:.3} atom_y={ay:.3} baseline={baseline:.3}"
        );
    }

    /// The italic-to-atom boundary overhang correction acts on
    /// the atom's layout position. When the synthetic shear pushes the last italic
    /// glyph's top into the atom ink box's left edge, the atom x must move right by
    /// correction = max(0, K × (shearBase - lastTop) - naturalGap). Since the italic
    /// marker does not change glyph geometry, the upright control is the uncorrected
    /// natural geometry: it yields the expected correction, and the assertion checks
    /// that the italic atom's x moves right by exactly that amount.
    ///
    /// Two scenarios:
    ///   (a) "Hi" + ink_left=2: the ink box's left side bearing already covers the
    ///       overhang, so correction = 0;
    ///   (b) "W" + ink_left=0: a dense glyph sits on the pen, the overhang exceeds
    ///       the natural gap, so correction > 0 and the atom x moves right by that
    ///       amount.
    #[test]
    fn phase23_italic_to_atom_boundary_correction() {
        let mut fs = GuideFontSystem::new();
        for (text, ink_left) in [("Hi", 2.0f32), ("W", 0.0f32)] {
            let ink = Some((ink_left, 3.0, 10.0));
            let italic_input =
                build_atom_input(&[(text, true)], &[], (16.0, 16.0), ink, 2, 500.0);
            let upright_input =
                build_atom_input(&[(text, false)], &[], (16.0, 16.0), ink, 2, 500.0);

            let out = compute_layout(&italic_input, &mut fs);
            let res_i = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
            let out2 = compute_layout(&upright_input, &mut fs);
            let res_u = flatbuffers::root::<crate::fb::LayoutResult>(&out2).expect("LayoutResult");

            let atom_x_i = res_i.nodes().expect("nodes").get(1).x();
            let atom_x_u = res_u.nodes().expect("nodes").get(1).x();
            let para_x = res_u.nodes().expect("nodes").get(0).x();
            let para_y = res_u.nodes().expect("nodes").get(0).y();
            let pen_x = atom_x_u - para_x; // paragraph-relative pen (natural position)

            // Natural quad geometry from the upright control, byte-identical to the
            // italic one because italic only rides the brush.
            let quads: Vec<(f32, f32, f32, f32)> = sorted_glyphs(&res_u)
                .into_iter()
                .map(|(x, y, w, h, _)| (x - para_x, y - para_y, w, h))
                .collect();
            let last = *quads.last().expect("quads");
            let shear_base = quads.iter().map(|(_, y, _, h)| y + h).fold(0.0f32, f32::max);
            let shear_shift = 0.25 * (shear_base - last.1);
            let natural_gap = (pen_x + ink_left) - (last.0 + last.2);
            let expected = (shear_shift - natural_gap).max(0.0);
            let actual = atom_x_i - atom_x_u;

            assert!(
                actual >= 0.0,
                "{text:?}: the atom must never move left under the boundary correction (got {actual})"
            );
            assert!(
                (actual - expected).abs() < 1.5,
                "{text:?}: italic→atom correction must shift the ATOM's layout position by the \
                 overhang: got {actual}, expected {expected} \
                 (shear_shift={shear_shift:.3}, natural_gap={natural_gap:.3})"
            );
            if ink_left == 0.0 {
                assert!(
                    actual > 0.0,
                    "{text:?}: dense glyph + zero left bearing must yield a POSITIVE \
                     boundary correction (expected {expected}), got {actual}"
                );
            }
            println!(
                "[phase23_italic_atom] text={text:?} ink_left={ink_left} \
                 shear_shift={shear_shift:.3} natural_gap={natural_gap:.3} \
                 expected={expected:.3} atom_x_u={atom_x_u:.3} atom_x_i={atom_x_i:.3} actual={actual:.3}"
            );
        }
    }

    /// Rejection fix, end to end: the single-line italic-to-atom-to-upright
    /// configuration. The boundary correction is blind to the atom, so it applies
    /// correction_22 to what it sees as a direct text boundary using a gap that
    /// already contains the atom advance, while the atom correction's pass 2 shifts
    /// the tail of the upright quads after the atom on the same line by c_23. With the
    /// fix the boundary is skipped, because the atom pen falls between the two runs,
    /// and the upright tail receives a single c_23. This test asserts that the text
    /// offset after the atom equals c_23, not c_23 + correction_22.
    ///
    /// Geometry, re-derived after the advance correction: the atom ink box advance is always a whole
    /// 16px cell because the il term cancels, so the double-application window
    /// gap_22 = natural_gap + advance + left bearing must fall below S, which needs a
    /// large font size to magnify the shear overhang S. The old construction with
    /// ink=(0,0,10) produced a 1px advance from the incorrect formula and is no longer
    /// usable. Font size 120, still below MAX_RASTER_SIZE 128, gives S around 21-23
    /// logical px against gap_22 around 13, which reopens the correction_22 > 3.0
    /// discrimination window. Without the fix the correction would push X by an extra
    /// correction_22, which the assertion detects.
    #[test]
    fn phase23_italic_to_atom_to_upright_single_c23_tail_shift() {
        let mut fs = GuideFontSystem::new();
        let ink = Some((0.0, 0.0, 10.0));
        let italic_input = build_atom_input_at(
            &[("W", true)],
            &[("X", false)],
            (16.0, 16.0),
            ink,
            2,
            500.0,
            120.0,
            0.0,
        );
        let upright_input = build_atom_input_at(
            &[("W", false)],
            &[("X", false)],
            (16.0, 16.0),
            ink,
            2,
            500.0,
            120.0,
            0.0,
        );

        let out = compute_layout(&italic_input, &mut fs);
        let res_i = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
        let out2 = compute_layout(&upright_input, &mut fs);
        let res_u = flatbuffers::root::<crate::fb::LayoutResult>(&out2).expect("LayoutResult");

        let para_x_u = res_u.nodes().expect("nodes").get(0).x();
        let para_y_u = res_u.nodes().expect("nodes").get(0).y();
        let para_x_i = res_i.nodes().expect("nodes").get(0).x();
        let para_y_i = res_i.nodes().expect("nodes").get(0).y();
        let atom_x_u = res_u.nodes().expect("nodes").get(1).x();
        let atom_x_i = res_i.nodes().expect("nodes").get(1).x();
        let pen_x = atom_x_u - para_x_u; // paragraph-relative pen (natural position)

        // Natural quad geometry from the upright control; italic only rides the brush,
        // so the two inputs are byte-identical.
        let quads_u: Vec<(f32, f32, f32, f32)> = sorted_glyphs(&res_u)
            .into_iter()
            .map(|(x, y, w, h, _)| (x - para_x_u, y - para_y_u, w, h))
            .collect();
        let quads_i: Vec<(f32, f32, f32, f32)> = sorted_glyphs(&res_i)
            .into_iter()
            .map(|(x, y, w, h, _)| (x - para_x_i, y - para_y_i, w, h))
            .collect();
        assert_eq!(quads_u.len(), 2, "expected W and X glyphs, got {}", quads_u.len());
        assert_eq!(quads_i.len(), 2, "expected W and X glyphs, got {}", quads_i.len());
        let last = quads_u[0]; // the italic trailing glyph before the atom (the only before glyph, W)
        let first = quads_u[1]; // the upright leading glyph after the atom, X

        let shear_base = quads_u.iter().map(|(_, y, _, h)| y + h).fold(0.0f32, f32::max);
        let shear_shift = 0.25 * (shear_base - last.1);
        let natural_gap = (pen_x + 0.0) - (last.0 + last.2); // ink_left_px = 0
        let c_23 = (shear_shift - natural_gap).max(0.0);
        // The correction the boundary pass would apply without the fix, whose gap
        // contains the atom advance:
        let gap_22 = first.0 - (last.0 + last.2);
        let correction_22 = (shear_shift - gap_22).max(0.0);

        let atom_shift = atom_x_i - atom_x_u;
        let after_shift = quads_i[1].0 - quads_u[1].0;
        assert!(
            correction_22 > 3.0,
            "test geometry must make correction_22 > 3.0 for the double-application \
             branch to be reachable (discrimination window: real value ~7.25; tightening to 3.0 \
             exposes implementation drift below 3.0 immediately instead of passing a loose >1.0 window), got {correction_22:.3} \
             (shear_shift={shear_shift:.3}, gap_22={gap_22:.3})"
        );
        assert!(
            (atom_shift - c_23).abs() < 1.5,
            "the atom itself must shift by c_23: got {atom_shift}, expected {c_23}"
        );
        assert!(
            (after_shift - c_23).abs() < 1.5,
            "italic→atom→upright: the text AFTER the atom must shift by EXACTLY c_23, \
             got {after_shift}, expected {c_23:.3} - the double application would have \
             shifted it by c_23+correction_22 = {:.3}",
            c_23 + correction_22
        );
        assert!(
            (after_shift - (c_23 + correction_22)).abs() > 1.5,
            "the post-atom shift must be distinguishable from the double-applied \
             c_23+correction_22 = {:.3}, got {after_shift}",
            c_23 + correction_22
        );
        assert!(
            (atom_shift - after_shift).abs() < 0.1,
            "tail must move WITH the atom (pen consistency): atom_shift={atom_shift}, \
             after_shift={after_shift}"
        );
        println!(
            "[phase23_atom_tail] font=120 pen_x={pen_x:.3} shear_shift={shear_shift:.3} \
             natural_gap={natural_gap:.3} gap_22={gap_22:.3} c_23={c_23:.3} \
             correction_22={correction_22:.3} atom_shift={atom_shift:.3} \
             after_shift={after_shift:.3}"
        );
    }

    /// Knife-edge regression (pass-2 double-application fix): the accumulating
    /// case of a line carrying three or more italic-word to atom boundaries. Root
    /// cause: pass-1 picks the trailing italic glyph per boundary from natural
    /// geometry and computes a correction c, while pass-2's tail-shift membership
    /// test `q.x >= pivot - 0.5` reads the position already accumulated by earlier
    /// boundaries. The trailing glyph selected by the current boundary is pushed
    /// past the threshold by an earlier boundary and is then shifted again by the
    /// current one, so a single glyph receives a double application and detaches
    /// from its word body. The fix has pass-1 record a natural-geometry snapshot of
    /// the whole line's quads and pass-2 test membership against that same snapshot,
    /// so every boundary fires once from natural geometry and boundaries never
    /// accumulate into each other.
    ///
    /// Assertions: the third boundary's trailing glyph ends up displaced by the
    /// prior sum c_1 + c_2 only, never by its own c_3, and its relative spacing to
    /// the neighbouring glyph of the same word stays at the natural value within
    /// ±0.5. Counter-check: before the fix this test fails, because the trailing
    /// glyph, once pushed past the pass-2 threshold, is displaced by c_1 + c_2 + c_3.
    ///
    /// Construction (diagnostic values: icon ink_left=(0,0,0,3), all three chain
    /// corrections positive): font size 72 magnifies the shear overhang (still below
    /// MAX_RASTER_SIZE 128), and the word shape "pf" makes shear_base include the
    /// descender of p while the trailing glyph f is narrow and tall, so every
    /// boundary has a large c and a small pass-2 threshold, advance_f - lb - 0.5.
    /// All expected values are derived from the upright control's natural geometry
    /// through the pass-1 convention, which makes the test immune to system font
    /// metrics.
    #[test]
    fn phase23_italic_atom_pass2_no_double_application() {
        let mut fs = GuideFontSystem::new();
        let font = 72.0;
        let words = ["pf", "pf", "pf", "pf"];
        let inks = [
            (0.0, 0.0, 10.0),
            (0.0, 0.0, 10.0),
            (0.0, 0.0, 10.0),
            (3.0, 3.0, 10.0),
        ];
        let italic_input = build_multi_atom_input(&words, true, &inks, (16.0, 16.0), 500.0, font);
        let upright_input =
            build_multi_atom_input(&words, false, &inks, (16.0, 16.0), 500.0, font);

        let out_i = compute_layout(&italic_input, &mut fs);
        let res_i = flatbuffers::root::<crate::fb::LayoutResult>(&out_i).expect("LayoutResult");
        let out_u = compute_layout(&upright_input, &mut fs);
        let res_u = flatbuffers::root::<crate::fb::LayoutResult>(&out_u).expect("LayoutResult");

        let para_x_u = res_u.nodes().expect("nodes").get(0).x();
        let para_y_u = res_u.nodes().expect("nodes").get(0).y();
        let para_x_i = res_i.nodes().expect("nodes").get(0).x();
        let para_y_i = res_i.nodes().expect("nodes").get(0).y();
        let atom_x_u: Vec<f32> = (0..words.len())
            .map(|k| res_u.nodes().expect("nodes").get(1 + k).x() - para_x_u)
            .collect();
        let atom_x_i: Vec<f32> = (0..words.len())
            .map(|k| res_i.nodes().expect("nodes").get(1 + k).x() - para_x_i)
            .collect();

        // Natural geometry comes from the upright control; italic only rides the
        // brush, so the two inputs are byte-identical.
        let quads_u: Vec<(f32, f32, f32, f32)> = sorted_glyphs(&res_u)
            .into_iter()
            .map(|(x, y, w, h, _)| (x - para_x_u, y - para_y_u, w, h))
            .collect();
        let quads_i: Vec<(f32, f32, f32, f32)> = sorted_glyphs(&res_i)
            .into_iter()
            .map(|(x, y, w, h, _)| (x - para_x_i, y - para_y_i, w, h))
            .collect();
        // Four words × 2 glyphs ("pf"); atoms and U+FFFC placeholders produce no
        // glyphs. The whole line must fit the available width without wrapping,
        // otherwise the k*2 index assumption breaks.
        assert_eq!(
            quads_u.len(),
            words.len() * 2,
            "upright twin must shape {} glyphs, got {}",
            words.len() * 2,
            quads_u.len()
        );
        assert_eq!(quads_i.len(), words.len() * 2, "italic glyph count");
        let last_q_u = quads_u.last().expect("quads");
        assert!(
            last_q_u.0 + last_q_u.2 < 500.0,
            "test paragraph must stay on one line (right edge {:.1} >= avail 500)",
            last_q_u.0 + last_q_u.2
        );

        // Reproduce the pass-1 natural-geometry convention: each boundary's
        // trailing glyph is the rightmost quad before the atom's pen (x < pivot -
        // 0.5), and shear_base is taken over that span, which is the word's own two
        // glyphs. ink_left_px = ink_left × bh/16 = ink_left, since bh = 16.
        let pivot = |k: usize| atom_x_u[k];
        let c_of = |k: usize| -> f32 {
            let p = pivot(k);
            let l = quads_u
                .iter()
                .enumerate()
                .filter(|(_, q)| q.0 < p - 0.5)
                .max_by(|a, b| a.1.0.partial_cmp(&b.1.0).unwrap())
                .map(|(_, q)| *q)
                .expect("a glyph must precede every atom pen");
            let si = k * 2;
            let sb = quads_u[si..si + 2]
                .iter()
                .map(|(_, y, _, h)| y + h)
                .fold(0.0f32, f32::max);
            let ss = 0.25 * (sb - l.1);
            let ng = (p + inks[k].0) - (l.0 + l.2);
            (ss - ng).max(0.0)
        };
        let (c1, c2, c3) = (c_of(0), c_of(1), c_of(2));
        // Each word's trailing glyph must really sit left of its atom pen for the
        // pass-1 selection to hold.
        for k in 0..words.len() {
            assert!(
                quads_u[k * 2 + 1].0 < pivot(k) - 0.5,
                "word {k} last glyph must lie left of its atom pen"
            );
        }
        assert!(
            c3 > 2.0,
            "the 3rd boundary's correction must exceed the displacement tolerance \
             for discrimination (got c3={c3:.3}, c1={c1:.3}, c2={c2:.3})"
        );

        // Self-check of the geometry's discriminating power: under natural geometry
        // the prior tail shift c1+c2 must push the third boundary's trailing glyph
        // past the pass-2 threshold (pivot_3 - 0.5). Otherwise the old code never
        // double-applies and this test could not tell the fix from the bug.
        let l3 = quads_u[2 * 2 + 1];
        let l3_x = l3.0;
        let p3 = pivot(2);
        let prior_sum = c1 + c2;
        let trig_threshold = p3 - 0.5;
        assert!(
            l3_x + prior_sum >= trig_threshold,
            "test geometry must make the pre-fix pass-2 threshold crossing real: \
             last3.x({l3_x:.3}) + c1+c2({prior_sum:.3}) must be >= pivot3−0.5({trig_threshold:.3})"
        );

        let last3_shift = quads_i[2 * 2 + 1].0 - quads_u[2 * 2 + 1].0;
        let penult3_shift = quads_i[2 * 2].0 - quads_u[2 * 2].0;
        let atom3_shift = atom_x_i[2] - atom_x_u[2];

        // After the fix the third boundary's trailing glyph is displaced by the
        // prior sum only, never by its own c_3; a double application in the old
        // code produced c1+c2+c3.
        assert!(
            (last3_shift - (c1 + c2)).abs() < 1.5,
            "the 3rd boundary's last glyph must be displaced by ONLY the prior \
             boundary shifts c1+c2 = {:.3}, got {last3_shift:.3} (the pre-fix \
             double application would give c1+c2+c3 = {:.3})",
            c1 + c2,
            c1 + c2 + c3
        );
        assert!(
            (last3_shift - (c1 + c2 + c3)).abs() > 1.5,
            "the observed displacement must be distinguishable from the double-applied \
             c1+c2+c3 = {:.3}, got {last3_shift:.3}",
            c1 + c2 + c3
        );
        // The neighbouring glyph inside the word body receives only the prior tails,
        // in step with the trailing glyph, so their relative spacing stays at the
        // natural value within ±0.5.
        let spacing_u = l3.0 - quads_u[2 * 2].0;
        let spacing_i = quads_i[2 * 2 + 1].0 - quads_i[2 * 2].0;
        assert!(
            (spacing_i - spacing_u).abs() < 0.5,
            "intra-word spacing between the 3rd boundary's last glyph and its \
             word-mate must stay at the natural value {spacing_u:.3}, got \
             {spacing_i:.3} (last3_shift={last3_shift:.3}, \
             penult3_shift={penult3_shift:.3})"
        );
        // The atom itself still carries this boundary's correction (c1+c2+c3). The
        // single-shot semantics are unchanged; it just no longer reaches the
        // preceding glyph of the word body.
        assert!(
            (atom3_shift - (c1 + c2 + c3)).abs() < 1.5,
            "the 3rd atom must still shift by its own correction plus prior tails \
             c1+c2+c3 = {:.3}, got {atom3_shift:.3}",
            c1 + c2 + c3
        );
        println!(
            "[phase23_atom_pass2] font={font} c1={c1:.3} c2={c2:.3} c3={c3:.3} \
             pivot3={p3:.3} last3_natural={l3_x:.3} last3_shift={last3_shift:.3} \
             penult3_shift={penult3_shift:.3} atom3_shift={atom3_shift:.3} \
             spacing_natural={spacing_u:.3} spacing_italic={spacing_i:.3}"
        );
    }

    /// The align=2 centering hack is retired and its
    /// consumer removed. A leftover align=2 atom without an ink box must fall
    /// through to the default arm, with the block bottom 2px below the baseline,
    /// instead of being centered on the line. The centering top would be about
    /// baseline - line_height/2 + block_h/2, roughly 5px away from the default arm,
    /// which the assertion can distinguish.
    #[test]
    fn phase23_align2_centering_hack_retired() {
        let mut fs = GuideFontSystem::new();
        // No ink box → align=2 must fall through to the default anchoring.
        let input = build_atom_input(&[("A", false)], &[("B", false)], (16.0, 16.0), None, 2, 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");
        let atom = result.nodes().expect("nodes").get(1);
        let runs = result.glyph_runs().expect("glyph_runs");
        assert!(runs.len() > 0, "expected glyph runs");
        let baseline = runs.get(0).glyphs().expect("glyphs").get(0).baseline();
        let ay = atom.y();
        // Default arm: block top = baseline + 2 - block_h, so block bottom = baseline + 2.
        let expected_top = baseline + 2.0 - 16.0;
        assert!(
            (ay - expected_top).abs() < 1.5,
            "retired align=2 must anchor the block bottom 2px below the baseline \
             (top={expected_top}), got {ay}"
        );
        // The retired centering arm would have put the top near baseline -
        // line_height/2, about baseline - 8.3, some 5.7px away from the default
        // arm. Asserting the default value alone discriminates, since centering
        // would exceed the ±1.5 tolerance.
        println!(
            "[phase23_align2_retired] atom_y={ay:.3} baseline={baseline:.3} \
             expected_default_top={expected_top:.3}"
        );
    }

    /// Review follow-up (yOffset consumption restored): param, the yOffset on the
    /// serialization side fed by inlineYOffsetOverride, as on a real guide page
    /// such as `<ItemImage yOffset="2"/>`, takes part in the vertical
    /// placement again in both the ink-box anchor arm and the default arm, with the
    /// same sign convention as the retired align=2 centering arm
    /// `top_off=(line_height-bh)/2+param`: a positive value moves the block top down.
    /// The assertions compare block top minus baseline against the declared formulas
    /// directly, which is immune to line growth and baseline drift:
    ///   ink-box arm: block top = baseline - ink_bottom_px + param;
    ///   default arm (leftover align=2 without an ink box): block top =
    ///   baseline + 2 - block_h + param.
    /// A relative check follows: going from param 0 to 2 moves the block top exactly
    /// 2px down relative to the baseline.
    #[test]
    fn phase23_atom_param_yoffset_consumed() {
        let mut fs = GuideFontSystem::new();
        let ink = Some((2.0, 3.0, 10.0));
        // Ink-box arm: a param=0 control plus a param=2 case, the IconMetrics atom plus
        // yOffset shape.
        let out0 = compute_layout(
            &build_atom_input_at(&[("A", false)], &[("B", false)], (16.0, 16.0), ink, 2, 500.0, 11.0, 0.0),
            &mut fs,
        );
        let res0 = flatbuffers::root::<crate::fb::LayoutResult>(&out0).expect("LayoutResult");
        let out2 = compute_layout(
            &build_atom_input_at(&[("A", false)], &[("B", false)], (16.0, 16.0), ink, 2, 500.0, 11.0, 2.0),
            &mut fs,
        );
        let res2 = flatbuffers::root::<crate::fb::LayoutResult>(&out2).expect("LayoutResult");
        let g0 = sorted_glyphs(&res0);
        let g2 = sorted_glyphs(&res2);
        assert_eq!(g0.len(), 2, "expected A and B glyphs (param=0), got {}", g0.len());
        assert_eq!(g2.len(), 2, "expected A and B glyphs (param=2), got {}", g2.len());
        let baseline0 = g0[1].4;
        let baseline2 = g2[1].4;
        let ay0 = res0.nodes().expect("nodes").get(1).y();
        let ay2 = res2.nodes().expect("nodes").get(1).y();
        let ib = 10.0; // ink_bottom=10 × cell 16/16 → 10px
        assert!(
            (ay0 + ib - baseline0).abs() < 1.5,
            "param=0 ink tier must anchor top = baseline − ink_bottom_px: \
             ay0({ay0:.3}) + {ib} vs baseline0({baseline0:.3})"
        );
        assert!(
            (ay2 + ib - 2.0 - baseline2).abs() < 1.5,
            "param=2 ink tier must anchor top = baseline − ink_bottom_px + param: \
             ay2({ay2:.3}) + {ib} − 2 vs baseline2({baseline2:.3})"
        );
        // Relative check, immune to line growth: from param 0 to 2 the block top moves
        // 2px down relative to the baseline.
        assert!(
            (((ay2 - baseline2) - (ay0 - baseline0)) - 2.0).abs() < 1.0,
            "ink tier param must shift the atom 2px DOWN relative to the baseline: \
             top−baseline {:.3} → {:.3}",
            ay0 - baseline0,
            ay2 - baseline2
        );

        // Default arm (no ink box, leftover align=2 falls through): block top =
        // baseline + 2 - bh + param.
        let outn0 = compute_layout(
            &build_atom_input_at(&[("A", false)], &[("B", false)], (16.0, 16.0), None, 2, 500.0, 11.0, 0.0),
            &mut fs,
        );
        let resn0 = flatbuffers::root::<crate::fb::LayoutResult>(&outn0).expect("LayoutResult");
        let outn2 = compute_layout(
            &build_atom_input_at(&[("A", false)], &[("B", false)], (16.0, 16.0), None, 2, 500.0, 11.0, 2.0),
            &mut fs,
        );
        let resn2 = flatbuffers::root::<crate::fb::LayoutResult>(&outn2).expect("LayoutResult");
        let bn0 = sorted_glyphs(&resn0)[1].4;
        let bn2 = sorted_glyphs(&resn2)[1].4;
        let n0 = resn0.nodes().expect("nodes").get(1).y();
        let n2 = resn2.nodes().expect("nodes").get(1).y();
        assert!(
            (n0 - (bn0 + 2.0 - 16.0)).abs() < 1.5,
            "default tier param=0 must anchor top = baseline + 2 − bh: \
             n0({n0:.3}) vs bn0({bn0:.3}) + 2 − 16"
        );
        assert!(
            (n2 - (bn2 + 2.0 - 16.0 + 2.0)).abs() < 1.5,
            "default tier param=2 must shift the atom DOWN by param: \
             n2({n2:.3}) vs bn2({bn2:.3}) + 2 − 16 + 2"
        );
        println!(
            "[phase23_atom_param] ink tier: top0={ay0:.3} top2={ay2:.3} \
             (top−baseline {:.3} → {:.3}); default tier: top0={n0:.3} top2={n2:.3}",
            ay0 - baseline0,
            ay2 - baseline2
        );
    }

    // List markers

    /// Build a LayoutInput with a list-item container (node 0, padding_left=10 =
    /// LEVEL_MARGIN content indent, children=[para]) and one content paragraph
    /// (node 1). `marker` = Some((text, argb)) declares ListMarkerData on the
    /// container (the Java LytListItem write path); None leaves it undeclared.
    fn build_list_marker_input(
        marker: Option<(&str, u32)>,
        para_text: &str,
        avail_width: f32,
    ) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        // node 1: content paragraph (relative flow, default white).
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let text_off = fbb.create_string(para_text);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                ..Default::default()
            },
        );
        let para_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let para = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(para_style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );

        // node 0: list-item container with padding_left=10 (LEVEL_MARGIN).
        let li_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                padding_left: 10.0,
                ..Default::default()
            },
        );
        let lmd = marker.map(|(text, color)| {
            let md_text = fbb.create_string(text);
            let md_style = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size: 11.0,
                    color,
                    ..Default::default()
                },
            );
            crate::fb::ListMarkerData::create(
                &mut fbb,
                &crate::fb::ListMarkerDataArgs {
                    text: Some(md_text),
                    style: Some(md_style),
                },
            )
        });
        let children = fbb.create_vector(&[1u32]);
        let li = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(li_style),
                node_type: 0,
                children: Some(children),
                list_marker: lmd,
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[li, para]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// Builder dedicated to the fallback case: a list item (node 0,
    /// padding_left=10) holding a paragraph with inline atoms only (node 1, text =
    /// U+FFFC and no text glyphs at all) plus the inline atom block (node 2,
    /// position:absolute image 16×16, ink box (1,14,10) at realistic magnitude).
    /// collectBlockChildren promotes the inline atom to a direct child of the list
    /// item, level with the paragraph, so neither direct child (para, atom) has
    /// glyphs. This is the real-corpus shape (AE2 getting-started items 13 and 22),
    /// where the fallback must take the first-line baseline from the paragraph's own
    /// markers.
    fn build_list_marker_icon_input(marker: Option<(&str, u32)>, avail_width: f32) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(1024);

        // node 1: icons-only paragraph (text = "\u{FFFC}" placeholder, no glyphs).
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let text_off = fbb.create_string("\u{FFFC}");
        let ibr = crate::fb::InlineBlockRef::create(
            &mut fbb,
            &crate::fb::InlineBlockRefArgs {
                node: 2,
                align: 2,
                param: 0.0,
                ink_left: 1.0,
                ink_right: 14.0,
                ink_bottom: 10.0,
            },
        );
        let ibr_vec = fbb.create_vector(&[ibr]);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                inline_blocks: Some(ibr_vec),
                ..Default::default()
            },
        );
        let para_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let para = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(para_style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );

        // node 2: the inline atom (image, explicit 16×16 px, position:absolute).
        let sz_w = crate::fb::Dimension::create(
            &mut fbb,
            &crate::fb::DimensionArgs { value: 16.0, unit: 1 },
        );
        let sz_h = crate::fb::Dimension::create(
            &mut fbb,
            &crate::fb::DimensionArgs { value: 16.0, unit: 1 },
        );
        let atom_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 1, // pos_abs → anchored by the inline post-pass
                size_w: Some(sz_w),
                size_h: Some(sz_h),
                ..Default::default()
            },
        );
        let img = crate::fb::ImageData::create(
            &mut fbb,
            &crate::fb::ImageDataArgs {
                explicit_w: 16.0,
                explicit_h: 16.0,
                ..Default::default()
            },
        );
        let atom = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(atom_style),
                node_type: 2,
                image: Some(img),
                ..Default::default()
            },
        );

        // node 0: list-item container (padding_left=10), children = [para, atom].
        let li_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                padding_left: 10.0,
                ..Default::default()
            },
        );
        let lmd = marker.map(|(text, color)| {
            let md_text = fbb.create_string(text);
            let md_style = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size: 11.0,
                    color,
                    ..Default::default()
                },
            );
            crate::fb::ListMarkerData::create(
                &mut fbb,
                &crate::fb::ListMarkerDataArgs {
                    text: Some(md_text),
                    style: Some(md_style),
                },
            )
        });
        let children = fbb.create_vector(&[1u32, 2u32]);
        let li = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(li_style),
                node_type: 0,
                children: Some(children),
                list_marker: lmd,
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[li, para, atom]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// Builder for the cascade-order regression covering the nested case: a list
    /// item (node 0, padding_left=10) holding a paragraph with inline atoms only
    /// (node 1, text = U+FFFC and no text glyphs), the inline atom block (node 2,
    /// position:absolute image 16×16, ink box (1,14,10)) and a nested list container
    /// (node 3, children=[4]) whose nested text paragraph (node 4, "Nested item
    /// text") does have glyphs. This is the real-corpus shape of AE2
    /// getting-started "Certus Quartz Ore", whose first line is a bare icon followed
    /// by a nested sublist.
    /// The old cascade went from direct-child glyphs (none on nodes 1/2/3) straight
    /// to the DFS, which descended through node 3 and hit node 4's nested text line,
    /// producing two markers on one row. In the corrected order, after step one finds
    /// nothing, the inline-atom-only direct child (node 1, non-empty markers and
    /// empty glyphs) is checked before any descent, so the marker anchors to the atom
    /// line.
    fn build_list_marker_icon_nested_input(marker: Option<(&str, u32)>, avail_width: f32) -> Vec<u8> {
        use flatbuffers::FlatBufferBuilder;
        let mut fbb = FlatBufferBuilder::with_capacity(2048);

        // node 1: icons-only paragraph (text = "\u{FFFC}" placeholder, no glyphs).
        let ts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let text_off = fbb.create_string("\u{FFFC}");
        let ibr = crate::fb::InlineBlockRef::create(
            &mut fbb,
            &crate::fb::InlineBlockRefArgs {
                node: 2,
                align: 2,
                param: 0.0,
                ink_left: 1.0,
                ink_right: 14.0,
                ink_bottom: 10.0,
            },
        );
        let ibr_vec = fbb.create_vector(&[ibr]);
        let td = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(text_off),
                style: Some(ts),
                inline_blocks: Some(ibr_vec),
                ..Default::default()
            },
        );
        let para_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let para = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(para_style),
                node_type: 1,
                text: Some(td),
                ..Default::default()
            },
        );

        // node 2: the inline atom (image, explicit 16×16 px, position:absolute).
        let sz_w = crate::fb::Dimension::create(
            &mut fbb,
            &crate::fb::DimensionArgs { value: 16.0, unit: 1 },
        );
        let sz_h = crate::fb::Dimension::create(
            &mut fbb,
            &crate::fb::DimensionArgs { value: 16.0, unit: 1 },
        );
        let atom_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 1, // pos_abs → anchored by the inline post-pass
                size_w: Some(sz_w),
                size_h: Some(sz_h),
                ..Default::default()
            },
        );
        let img = crate::fb::ImageData::create(
            &mut fbb,
            &crate::fb::ImageDataArgs {
                explicit_w: 16.0,
                explicit_h: 16.0,
                ..Default::default()
            },
        );
        let atom = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(atom_style),
                node_type: 2,
                image: Some(img),
                ..Default::default()
            },
        );

        // node 4: nested text paragraph (has real glyphs).
        let nts = crate::fb::TextStyle::create(
            &mut fbb,
            &crate::fb::TextStyleArgs {
                font_size: 11.0,
                ..Default::default()
            },
        );
        let ntext_off = fbb.create_string("Nested item text");
        let ntd = crate::fb::TextData::create(
            &mut fbb,
            &crate::fb::TextDataArgs {
                text: Some(ntext_off),
                style: Some(nts),
                ..Default::default()
            },
        );
        let npara_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                position: 0,
                ..Default::default()
            },
        );
        let npara = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(npara_style),
                node_type: 1,
                text: Some(ntd),
                ..Default::default()
            },
        );

        // node 3: nested list container wrapping the nested text paragraph.
        let nlist_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                padding_left: 10.0,
                ..Default::default()
            },
        );
        let nlist_children = fbb.create_vector(&[4u32]);
        let nlist = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(nlist_style),
                node_type: 0,
                children: Some(nlist_children),
                ..Default::default()
            },
        );

        // node 0: list-item container (padding_left=10), children = [para, atom, nested list].
        let li_style = crate::fb::Style::create(
            &mut fbb,
            &crate::fb::StyleArgs {
                padding_left: 10.0,
                ..Default::default()
            },
        );
        let lmd = marker.map(|(text, color)| {
            let md_text = fbb.create_string(text);
            let md_style = crate::fb::TextStyle::create(
                &mut fbb,
                &crate::fb::TextStyleArgs {
                    font_size: 11.0,
                    color,
                    ..Default::default()
                },
            );
            crate::fb::ListMarkerData::create(
                &mut fbb,
                &crate::fb::ListMarkerDataArgs {
                    text: Some(md_text),
                    style: Some(md_style),
                },
            )
        });
        let children = fbb.create_vector(&[1u32, 2u32, 3u32]);
        let li = crate::fb::FlatNode::create(
            &mut fbb,
            &crate::fb::FlatNodeArgs {
                style: Some(li_style),
                node_type: 0,
                children: Some(children),
                list_marker: lmd,
                ..Default::default()
            },
        );
        let nodes = fbb.create_vector(&[li, para, atom, nlist, npara]);
        let input = crate::fb::LayoutInput::create(
            &mut fbb,
            &crate::fb::LayoutInputArgs {
                available_width: avail_width,
                justify: 0,
                nodes: Some(nodes),
                ..Default::default()
            },
        );
        fbb.finish(input, None);
        fbb.finished_data().to_vec()
    }

    /// The marker GlyphRun of a result: (node_index, [(x, y, w, h)...]) for the
    /// run whose argb equals the declared marker color. None = no such run
    /// (marker missing from the quad stream).
    fn marker_run(
        result: &crate::fb::LayoutResult,
        color: u32,
    ) -> Option<(u32, Vec<(f32, f32, f32, f32)>)> {
        let runs = result.glyph_runs()?;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.argb() != color {
                continue;
            }
            let gs = run.glyphs()?;
            let mut v = Vec::with_capacity(gs.len());
            for gi in 0..gs.len() {
                let g = gs.get(gi);
                v.push((g.x(), g.y(), g.w(), g.h()));
            }
            return Some((run.node_index(), v));
        }
        None
    }

    /// Acceptance case ①: marker glyphs attach to the content paragraph
    /// (node_index == 1, the GlyphRunHolder) as an ordinary GlyphRun, and the ink
    /// right edge lands exactly on the list item's own left edge, which is that
    /// nesting level's document text line (L1 = 14 = CONTENT_PAD), so the whole box
    /// hangs into the left margin and no gutter constant is involved. The marker
    /// glyphs also carry the sentinel line_index (u32::MAX), keeping them out of the
    /// body visual line so the content paragraph's firstLineBoundsFromGlyphs, which
    /// filters on line 0, is not polluted by the marker.
    #[test]
    fn phase25_list_marker_right_edge_lands_on_gutter() {
        const MARKER: u32 = 0xFFAAAAAA; // Java COL_MARKER gray (ColorUtils.MC_GRAY #AAA)
        let mut fs = GuideFontSystem::new();
        let input = build_list_marker_input(Some(("1.", MARKER)), "Item text", 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let (node, glyphs) = marker_run(&result, MARKER).expect("marker glyph run");        assert_eq!(
            node, 1,
            "marker run must attach to the content paragraph (flat index 1, the GlyphRunHolder)"
        );
        assert!(!glyphs.is_empty(), "marker run must carry glyph quads");

        // The target is the list item's own left edge (node 0 x = CONTENT_PAD = 14);
        // the marker's ink right edge lands exactly on that level's text line
        // (L1 = 14), not on content-child x minus a gutter, which the old formula put
        // at 24-5 = 19.
        let item_x = result.nodes().expect("nodes").get(0).x();
        let target = item_x;
        let max_right = glyphs
            .iter()
            .map(|(x, _, w, _)| x + w)
            .fold(0.0f32, f32::max);
        assert!(
            (max_right - target).abs() < 1e-3,
            "marker ink right edge must land exactly on the item left edge (L1 text line, {target}), \
             got {max_right} (marker glyphs: {glyphs:?})"
        );
        // Sentinel line_index: the marker must not enter body line 0, which
        // firstLineBoundsFromGlyphs filters on.
        let runs = result.glyph_runs().expect("glyph_runs");
        let mut seen_sentinel = false;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.argb() != MARKER {
                continue;
            }
            let gs = run.glyphs().expect("marker run glyphs");
            for gi in 0..gs.len() {
                assert_eq!(
                    gs.get(gi).line_index(),
                    u32::MAX,
                    "marker glyphs must carry the sentinel line index (isolated from body visual lines)"
                );
                seen_sentinel = true;
            }
        }
        assert!(seen_sentinel, "sentinel line index must be observed");
        println!(
            "[phase25_marker_right_edge] item_x={item_x:.3} target={target:.3} \
             max_right={max_right:.3} glyphs={}",
            glyphs.len()
        );
    }

    /// Acceptance case ②: a multi-digit marker "10." and a single-digit "1." both
    /// right-align to the same list item left edge, so the ink right edges are
    /// collinear (spread = 0). The wider "10." reaches further left, about 1.5
    /// logical px, the expected shape the owner accepted, with the whole box hanging
    /// into the left margin.
    #[test]
    fn phase25_multidigit_marker_right_aligned() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        let in1 = build_list_marker_input(Some(("1.", MARKER)), "Item text", 500.0);
        let in10 = build_list_marker_input(Some(("10.", MARKER)), "Item text", 500.0);
        let out1 = compute_layout(&in1, &mut fs);
        let r1 = flatbuffers::root::<crate::fb::LayoutResult>(&out1).expect("r1");
        let out10 = compute_layout(&in10, &mut fs);
        let r10 = flatbuffers::root::<crate::fb::LayoutResult>(&out10).expect("r10");

        let (n1, g1) = marker_run(&r1, MARKER).expect("1. run");
        let (n10, g10) = marker_run(&r10, MARKER).expect("10. run");
        assert_eq!(n1, 1);
        assert_eq!(n10, 1);
        let right =
            |g: &Vec<(f32, f32, f32, f32)>| g.iter().map(|(x, _, w, _)| x + w).fold(0.0f32, f32::max);
        let left =
            |g: &Vec<(f32, f32, f32, f32)>| g.iter().map(|(x, _, _, _)| *x).fold(f32::MAX, f32::min);
        let item_x = r1.nodes().expect("nodes").get(0).x();
        let target = item_x;
        assert!(
            (right(&g1) - target).abs() < 1e-3,
            "1. must right-align to the item left edge: right={} target={target}",
            right(&g1)
        );
        assert!(
            (right(&g10) - target).abs() < 1e-3,
            "10. must right-align to the SAME item left edge (collinear, spread=0): \
             right={} target={target}",
            right(&g10)
        );
        assert!(
            (right(&g1) - right(&g10)).abs() < 1e-3,
            "right edges must be collinear (spread=0): right1={} right10={}",
            right(&g1),
            right(&g10)
        );
        assert!(
            left(&g10) < left(&g1) - 2.0,
            "10. (wider) must extend further left than 1.: left1={} left10={}",
            left(&g1),
            left(&g10)
        );
        assert!(
            left(&g10) > -0.5 && left(&g10) < target,
            "10. ink must hang into the left margin but not cross the page edge \
             (owner-accepted ≈1.5): left10={}",
            left(&g10)
        );
        println!(
            "[phase25_multidigit] right1={:.3} right10={:.3} target={target:.3} \
             left1={:.3} left10={:.3}",
            right(&g1),
            right(&g10),
            left(&g1),
            left(&g10)
        );
    }

    /// Acceptance case ③: an unordered bullet "•" travels the glyph path as an
    /// ordinary GlyphRun attached to the content paragraph instead of Java drawing a
    /// square, since the old FillRect logic has been deleted.
    #[test]
    fn phase25_bullet_renders_as_glyph_run() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        let input = build_list_marker_input(Some(("\u{2022}", MARKER)), "Item text", 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let (node, glyphs) = marker_run(&result, MARKER)
            .expect("bullet must render through the glyph-quad path (Java FillRect removed)");
        assert_eq!(node, 1, "bullet run must attach to the content paragraph");
        assert!(
            !glyphs.is_empty(),
            "bullet run must carry at least one glyph quad (the '•' outline)"
        );
        let item_x = result.nodes().expect("nodes").get(0).x();
        let target = item_x;
        let max_right = glyphs
            .iter()
            .map(|(x, _, w, _)| x + w)
            .fold(0.0f32, f32::max);
        assert!(
            (max_right - target).abs() < 1e-3,
            "bullet ink right edge must land on the item left edge (B1): got {max_right}, target {target}"
        );
        println!(
            "[phase25_bullet_glyph] node={node} glyphs={} max_right={max_right:.3} target={target:.3}",
            glyphs.len()
        );
    }

    /// Acceptance case ④, the hard gate that content-block geometry is unchanged: a
    /// marker declaration must not alter any content block's bounds. For the same
    /// input with and without the list_marker declaration, every FlatLayout x/y/w/h
    /// matches, because the marker hangs into the left margin, does not take part in
    /// wrapping and writes neither sizes nor abs_positions.
    #[test]
    fn phase25_content_geometry_unchanged_by_marker_declaration() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        let with_marker = build_list_marker_input(Some(("1.", MARKER)), "Item text", 500.0);
        let without_marker = build_list_marker_input(None, "Item text", 500.0);
        let out_w = compute_layout(&with_marker, &mut fs);
        let rw = flatbuffers::root::<crate::fb::LayoutResult>(&out_w).expect("rw");
        let out_wo = compute_layout(&without_marker, &mut fs);
        let rwo = flatbuffers::root::<crate::fb::LayoutResult>(&out_wo).expect("rwo");

        let na = rw.nodes().expect("nodes");
        let nb = rwo.nodes().expect("nodes");
        assert_eq!(
            na.len(),
            nb.len(),
            "marker declaration must not add/remove layout blocks"
        );
        for i in 0..na.len() {
            let a = na.get(i);
            let b = nb.get(i);
            for (name, va, vb) in [
                ("x", a.x(), b.x()),
                ("y", a.y(), b.y()),
                ("w", a.w(), b.w()),
                ("h", a.h(), b.h()),
            ] {
                assert!(
                    (va - vb).abs() < 1e-6,
                    "content block bounds must be byte-identical with/without the marker \
                     declaration: node {i} {name} {va} vs {vb}"
                );
            }
        }
        println!("[phase25_geometry] nodes={} byte-identical (marker-invariant)", na.len());
    }

    /// Rejected-then-fixed vertical anchoring: the marker baseline must land exactly
    /// on the content first-line baseline, as a direct numeric equality within a
    /// tolerance of ±1 logical px, which at the render_scale=1 used by this test is
    /// ±1 device px. The first version added the marker's own baseline offset (about
    /// one line height, measured at +12) on top of baseline_off, dropping the whole
    /// marker by one line: marker_top - item_top measured +19 against a
    /// baseline delta of +6.5. An additional hard gate requires the marker ink band
    /// not to leave its own item band (list item y..y+h) and not to press into the
    /// next line.
    #[test]
    fn phase25_marker_vertical_baseline_matches_content_first_line() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        let input = build_list_marker_input(Some(("1.", MARKER)), "Item text", 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let runs = result.glyph_runs().expect("glyph_runs");
        let mut marker_baseline: Option<f32> = None;
        let mut content_baseline: Option<f32> = None;
        let mut marker_min_y = f32::MAX;
        let mut marker_max_y = f32::MIN;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.argb() == MARKER {
                marker_baseline = Some(run.baseline());
                if let Some(gs) = run.glyphs() {
                    for gi in 0..gs.len() {
                        let g = gs.get(gi);
                        marker_min_y = marker_min_y.min(g.y());
                        marker_max_y = marker_max_y.max(g.y() + g.h());
                    }
                }
            } else if run.node_index() == 1 {
                // Content paragraph run: one span means one run, whose baseline is
                // the first-line baseline.
                content_baseline = content_baseline.or(Some(run.baseline()));
            }
        }
        let marker_baseline = marker_baseline.expect("marker run");
        let content_baseline = content_baseline.expect("content run");
        assert!(
            (marker_baseline - content_baseline).abs() <= 1.0,
            "marker baseline must EQUAL the content first-line baseline (direct value): \
             marker={marker_baseline} content={content_baseline} (2.5 first version had \
             marker−content = +12 ≈ one full line)"
        );
        // Hard gate on the item band: the marker ink must not leave its own item
        // (list item y..y+h).
        let item = result.nodes().expect("nodes").get(0);
        let item_y = item.y();
        let item_h = item.h();
        assert!(
            marker_min_y >= item_y - 1.0 && marker_max_y <= item_y + item_h + 1.0,
            "marker ink must stay inside its own item band [{item_y}, {}]: got \
             [{marker_min_y}, {marker_max_y}]",
            item_y + item_h
        );
        println!(
            "[phase25_vertical_single] marker_baseline={marker_baseline:.3} \
             content_baseline={content_baseline:.3} delta={:.3} \
             ink=[{marker_min_y:.3},{marker_max_y:.3}] item=[{item_y:.3},{:.3}]",
            marker_baseline - content_baseline,
            item_y + item_h
        );
    }

    /// Rejected-then-fixed vertical anchoring for a wrapped item: when the content
    /// wraps onto two or more lines the marker still anchors to the first-line
    /// baseline, not the second and certainly not the last. That baseline is the
    /// minimum baseline among the content run's line_index==0 glyphs; the assertion
    /// checks that the marker baseline equals it directly and sits clearly below the
    /// second line's baseline.
    #[test]
    fn phase25_marker_vertical_anchors_first_line_when_content_wraps() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        // A narrow container forces the content to wrap.
        let long = "First segment of a wrapped item text that certainly spills onto a second line.";
        let input = build_list_marker_input(Some(("10.", MARKER)), long, 100.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let runs = result.glyph_runs().expect("glyph_runs");
        let mut marker_baseline: Option<f32> = None;
        let mut line0_baseline: Option<f32> = None;
        let mut line1_baseline: Option<f32> = None;
        let mut lines: std::collections::BTreeSet<u32> = Default::default();
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.argb() == MARKER {
                marker_baseline = Some(run.baseline());
                continue;
            }
            if run.node_index() != 1 {
                continue;
            }
            if let Some(gs) = run.glyphs() {
                for gi in 0..gs.len() {
                    let g = gs.get(gi);
                    lines.insert(g.line_index());
                    if g.line_index() == 0 {
                        line0_baseline =
                            Some(line0_baseline.map_or(g.baseline(), |b| b.min(g.baseline())));
                    }
                    if g.line_index() == 1 {
                        line1_baseline =
                            Some(line1_baseline.map_or(g.baseline(), |b| b.min(g.baseline())));
                    }
                }
            }
        }
        let line_count = lines.len();
        assert!(
            line_count >= 2,
            "content must actually wrap to >=2 lines for this test, got {line_count}"
        );
        let marker_baseline = marker_baseline.expect("marker run");
        let line0 = line0_baseline.expect("line-0 baseline");
        let line1 = line1_baseline.expect("line-1 baseline");
        assert!(
            (marker_baseline - line0).abs() <= 1.0,
            "wrapped item: marker baseline must equal the FIRST-line baseline (direct value): \
             marker={marker_baseline} line0={line0} line1={line1}"
        );
        assert!(
            marker_baseline < line1,
            "marker must sit on the FIRST line, not the wrapped second line: \
             marker={marker_baseline} line1={line1}"
        );
        println!(
            "[phase25_vertical_wrap] lines={line_count} marker={marker_baseline:.3} \
             line0={line0:.3} line1={line1:.3} delta0={:.3}",
            marker_baseline - line0
        );
    }

    /// Fallback case in the real-corpus shape of AE2 getting-started items 13 and 22:
    /// the list item's first line holds inline atoms only (paragraph text = U+FFFC
    /// with no text glyphs at all, the inline atom being promoted to a direct child
    /// of the list item). The old anchor lookup only examined direct-child glyphs,
    /// and neither direct child (para, atom) had any, so the marker was skipped
    /// entirely. After the fix the lookup falls back to the paragraph's own markers,
    /// the authoritative source of the first-line baseline, and the marker must be
    /// generated with:
    ///   ① its right edge exactly on the list item's own left edge (item_x =
    ///      CONTENT_PAD = 14);
    ///   ② its baseline equal to the first-line baseline, which the inline atom's
    ///      ink bottom edge anchors: atom.y + ink_bottom_px, where
    ///      ink_bottom = 10 × 16/16 = 10.
    #[test]
    fn phase25_marker_survives_inline_atom_only_first_line() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        let input = build_list_marker_icon_input(Some(("1.", MARKER)), 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        let (node, glyphs) = marker_run(&result, MARKER)
            .expect("icons-only item must still get a marker (B2 subtree/marker fallback)");
        assert_eq!(node, 1, "marker run must attach to the icons-only paragraph (GlyphRunHolder)");
        assert!(!glyphs.is_empty(), "marker run must carry glyph quads");

        // ① The ink right edge lands exactly on the list item's own left edge
        // (node 0 x = CONTENT_PAD = 14).
        let item_x = result.nodes().expect("nodes").get(0).x();
        let max_right = glyphs
            .iter()
            .map(|(x, _, w, _)| x + w)
            .fold(0.0f32, f32::max);
        assert!(
            (max_right - item_x).abs() < 1e-3,
            "marker ink right edge must land on the item left edge ({item_x}), got {max_right} \
             (marker glyphs: {glyphs:?})"
        );

        // ② Baseline = first-line baseline: the inline atom's ink bottom edge anchors
        //    it, atom.y + ink_bottom_px = 10 × 16/16 = 10. The marker run baseline
        //    comes from the paragraph's markers[0].baseline_y on the fallback path,
        //    and the atom y is derived from that same line baseline, so the two are
        //    directly numerically equal.
        let runs = result.glyph_runs().expect("glyph_runs");
        let mut marker_baseline: Option<f32> = None;
        let mut seen_sentinel = false;
        for ri in 0..runs.len() {
            let run = runs.get(ri);
            if run.argb() != MARKER {
                continue;
            }
            marker_baseline = Some(run.baseline());
            if let Some(gs) = run.glyphs() {
                for gi in 0..gs.len() {
                    assert_eq!(
                        gs.get(gi).line_index(),
                        u32::MAX,
                        "marker glyphs must carry the sentinel line index"
                    );
                    seen_sentinel = true;
                }
            }
        }
        assert!(seen_sentinel, "sentinel line index must be observed");
        let marker_baseline = marker_baseline.expect("marker run baseline");
        let atom_y = result.nodes().expect("nodes").get(2).y();
        let ink_bottom_px = 10.0; // 10 × 16/16
        assert!(
            (marker_baseline - (atom_y + ink_bottom_px)).abs() < 1.5,
            "icons-only item: marker baseline must equal the first-line baseline \
             (= atom ink-bottom anchor, atom.y({atom_y}) + {ink_bottom_px}): \
             marker={marker_baseline:.3}"
        );
        println!(
            "[phase25_b2_icons_only] item_x={item_x:.3} max_right={max_right:.3} \
             marker_baseline={marker_baseline:.3} atom_y={atom_y:.3} \
             atom_baseline={:.3}",
            atom_y + ink_bottom_px
        );
    }

    /// Cascade-order regression from the correction: when a list item's first line
    /// holds inline atoms only and a nested list with text items follows, the marker
    /// must anchor to the item's own first visible line, the atom-line baseline, and
    /// must not be captured by the second cascade step's subtree DFS onto a nested
    /// child's text line, which put two markers on one row: "• • We need to
    /// obtain…"
    /// The constructed item is [paragraph with inline atoms only, nested list with
    /// text items]:
    ///   node 0: list-item, children = [para(1), atom(2), nested_list(3)]
    ///   node 1: icons-only para (U+FFFC, no glyphs, non-empty markers)
    ///   node 2: image atom 16×16 (ink box (1,14,10))
    ///   node 3: nested list container, children = [4]
    ///   node 4: nested text paragraph (has glyphs)
    /// The old cascade went from direct-child glyphs (none) to the DFS, which hit
    /// node 4's nested text line and produced two markers on one row, while the atom
    /// paragraph never got a turn. In the corrected order, after step one still finds
    /// nothing, step two finds the inline-atom-only direct child at node 1. The
    /// assertion requires the marker run to attach to the atom paragraph (node 1, the
    /// GlyphRunHolder) with a baseline equal to the atom-line baseline,
    /// atom.y + ink_bottom_px.
    #[test]
    fn phase25_b2_marker_anchors_atom_first_line_before_nested_list() {
        const MARKER: u32 = 0xFFAAAAAA;
        let mut fs = GuideFontSystem::new();
        let input = build_list_marker_icon_nested_input(Some(("1.", MARKER)), 500.0);
        let out = compute_layout(&input, &mut fs);
        let result = flatbuffers::root::<crate::fb::LayoutResult>(&out).expect("LayoutResult");

        // Scenario validity: the nested text paragraph at node 4 must really produce
        // glyphs, otherwise the test degenerates into a plain icon item without a
        // nested list and cannot catch a cascade-order bug.
        let runs = result.glyph_runs().expect("glyph_runs");
        let nested_glyphs = (0..runs.len())
            .map(|ri| runs.get(ri))
            .any(|run| run.argb() != MARKER && run.node_index() == 4
                && run.glyphs().map_or(false, |g| g.len() > 0));
        assert!(
            nested_glyphs,
            "nested text paragraph (node 4) must produce glyphs for this scenario"
        );

        // The marker run must attach to the atom paragraph (node 1), not to the nested
        // text paragraph (node 4).
        let (node, glyphs) = marker_run(&result, MARKER)
            .expect("marker run must be generated (atom first line exists)");
        assert_eq!(
            node, 1,
            "marker must anchor to the item's OWN first visible line (icons-only \
             paragraph node 1), NOT to the nested list text line (node 4): got {node}"
        );
        assert!(!glyphs.is_empty(), "marker run must carry glyph quads");

        // Baseline = atom-line baseline: the atom's ink bottom edge anchors it
        // (atom.y + ink_bottom_px).
        let marker_baseline = (0..runs.len())
            .map(|ri| runs.get(ri))
            .find(|run| run.argb() == MARKER)
            .map(|run| run.baseline())
            .expect("marker run baseline");
        let atom_y = result.nodes().expect("nodes").get(2).y();
        let ink_bottom_px = 10.0; // 10 × 16/16
        assert!(
            (marker_baseline - (atom_y + ink_bottom_px)).abs() < 1.5,
            "atom-first-line item WITH nested list: marker baseline must equal the \
             atom line baseline (= atom.y({atom_y}) + {ink_bottom_px}), NOT the nested \
             text line: marker={marker_baseline:.3}"
        );
        println!(
            "[phase25_b2_nested_after_atom] anchor_node={node} \
             marker_baseline={marker_baseline:.3} atom_y={atom_y:.3} \
             atom_baseline={:.3}",
            atom_y + ink_bottom_px
        );
    }
}
