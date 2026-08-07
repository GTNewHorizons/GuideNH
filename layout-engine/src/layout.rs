use std::collections::HashMap;

use crate::fb::{
    DecorationRect, DecorationRectArgs, FlatLayout, FlatLayoutArgs, FlatNode, GlyphBitmap,
    GlyphBitmapArgs, GlyphRun, GlyphRunArgs, LayoutInput, LayoutResult, LayoutResultArgs,
    PlacedGlyph, PlacedGlyphArgs,
};
use crate::measure::{create_measure_closure, measure_text, GlyphAccum, NodeContext};
use crate::parley_text::{FloatRect, ParleyRasterGlyph};
use crate::style_convert::flat_style_to_taffy;
use crate::text::GuideFontSystem;
use flatbuffers::FlatBufferBuilder;
use taffy::prelude::*;

/// Document content-box padding, matching the legacy synthetic root and the
/// Java document padding (14px each side).
const CONTENT_PAD: f32 = 14.0;

/// Synthetic-italic shear factor — MUST stay identical to the engine's
/// `GuideRenderEngine.GLYPH_SHEAR_K` (0.25f). The draw-time shear moves each
/// glyph's top edge right by `K × (baseY − y_top)` (see
/// italic_kerning_compensate below). The Java constant is the single source
/// of truth; this is the layout-side mirror so the compensation and the draw
/// transform share the same slant parameter.
const GLYPH_SHEAR_K: f32 = 0.25;

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
        // Fully blocked — jump below the blocking floats.
        match max_bottom {
            Some(b) => lane_y = b,
            None => return (content_x, content_w, lane_y),
        }
    }
}

/// Document-flow pusher (v1). Owns the single authoritative float table and
/// drives the top-level sequence in document order: top-level paragraphs are
/// shaped directly against the live table (real per-line wrapping — the
/// "bridge" is this in-process query, no precomputed clip table crosses any
/// boundary); top-level floats register into the table at the current cursor
/// without advancing it; top-level blocks are laid out as taffy subtrees.
/// Nested paragraphs inside those subtrees still wrap at full width (the
/// pusher does not yet recurse into block float contexts — transition).
pub fn compute_layout(
    input_bytes: &[u8],
    font_system: &mut GuideFontSystem,
) -> Vec<u8> {
    let input = flatbuffers::root::<LayoutInput>(input_bytes)
        .expect("Invalid LayoutInput FlatBuffer");

    let avail_width = input.available_width();
    // NB: visual_scale is intentionally NOT applied to the root width (D-3) —
    // Java blocks already pre-apply it per block (ResponsiveVisualSizing).
    let visual_scale = input.visual_scale();
    // Display pixel ratio (MC guiScale): glyph bitmaps are rasterized at
    // font_size * render_scale so 1 texel maps to 1 physical pixel; quad
    // coordinates are then divided back into document units.
    let render_scale = input.render_scale().max(0.25);
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
            let base_color = flat_nodes[i]
                .text()
                .and_then(|t| t.style())
                .map(|s| s.color())
                .unwrap_or(0xFFFFFFFF);
            // Single-style paragraphs carry no TextData.spans (LayoutNodeSerializer
            // needsRichSpans), so their run falls back to the base TextStyle —
            // which already carries the resolved italic flag. Without this, a
            // whole-paragraph italic never sets shear (the shear=false bug) and
            // never gets the kerning compensation either.
            let base_italic = flat_nodes[i]
                .text()
                .and_then(|t| t.style())
                .map(|s| s.italic())
                .unwrap_or(false);
            let (quads, new_bitmaps) =
                crate::parley_text::rasterize_out_glyphs(&acc.glyphs, render_scale);
            for (key, bw, bh, rgba) in new_bitmaps {
                if bitmap_index.insert(key) {
                    bitmap_keys.push(key);
                    bitmap_data.push((bw, bh, rgba));
                }
            }
            // Group rasterized quads by span (one GlyphRun per span, glyph
            // order preserved within the run — parley emits a span's glyphs
            // contiguously per line), then apply the synthetic-italic
            // kerning compensation to sheared runs BEFORE emitting placed
            // glyphs. baseY is run-wide (matching the engine's shearBaseY
            // over the whole run), the cumulative shift accumulates per line
            // (lines stack vertically, so a line's first glyph must not
            // inherit the previous line's shift).
            let mut run_quads: std::collections::BTreeMap<
                u32,
                Vec<ParleyRasterGlyph>,
            > = Default::default();
            for q in quads {
                run_quads.entry(q.span_index).or_default().push(q);
            }
            let mut groups: std::collections::BTreeMap<
                u32,
                Vec<flatbuffers::WIPOffset<PlacedGlyph>>,
            > = Default::default();
            for (si, mut rq) in run_quads {
                if rq.is_empty() {
                    continue;
                }
                let italic = span_styles
                    .get(si as usize)
                    .map(|s| s.italic)
                    .unwrap_or(base_italic);
                if italic {
                    italic_kerning_compensate(&mut rq);
                }
                let placed = rq
                    .into_iter()
                    .map(|q| {
                        PlacedGlyph::create(
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
                            },
                        )
                    })
                    .collect::<Vec<_>>();
                groups.insert(si, placed);
            }
            for (si, placed_offsets) in groups {
                if placed_offsets.is_empty() {
                    continue;
                }
                let (argb, shear) = span_styles
                    .get(si as usize)
                    .map(|s| (s.color, s.italic))
                    .unwrap_or((base_color, base_italic));
                let glyphs_vec = fbb.create_vector(&placed_offsets);
                glyph_run_offsets.push(GlyphRun::create(
                    &mut fbb,
                    &GlyphRunArgs {
                        node_index: i as u32,
                        glyphs: Some(glyphs_vec),
                        argb,
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
            // the glyph extents — Java LytHeading draws the themed separator
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
/// positions, so no glyph kerning shifts are needed — only the vertical
/// handling, mirroring the legacy layout's per-line box growth: a line
/// holding blocks grows by the space they need above the baseline and below
/// the line, pushing later lines down (the paragraph's measured height
/// already reserves the total — see measure.rs).
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
            let (na, nb) = marker_needs(m, bh, r.align(), r.param());
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
            let top = match r.align() {
                1 => m.baseline_y - r.param(),
                2 => m.line_top + (m.line_height - bh) / 2.0 + r.param(),
                _ => m.baseline_y + 2.0 - bh,
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

/// Synthetic-italic kerning compensation: per-glyph cumulative advance shift
/// for a sheared run, restoring the upright sidebearings — the behavior of a
/// true italic face's hmtx advances (the mature synthetic-italic practice).
///
/// The engine draws a sheared run by moving each glyph's TOP edge right by
/// `K × (baseY − y_top)` and its bottom edge by the same amount minus
/// `K × h` (GuideRenderEngine.emitGlyphQuads: `xTop = x + K·(baseY − y)`,
/// `xBottom = x + K·(baseY − y − h)`), where `baseY` is the run's lowest
/// quad bottom (`shearBaseY = max(g.y + g.h)` over the run, same file). But
/// parley SHAPES the run upright — shaping knows nothing of the slant — so
/// the italic ink overhangs its advance by that shear amount and glyphs
/// visibly collide. Compensating each glyph by the CUMULATIVE overhang of the
/// glyphs before it on the same line:
///
/// ```text
/// x'_i = x_i + Σ_{j<i} K × (baseY − y_top_j)
/// ```
///
/// moves every glyph exactly as far right as the sheared ink of its
/// predecessors now intrudes, so the glyph-to-glyph spacing (at the tops)
/// is bit-for-bit the upright sidebearing again. baseY and K must match the
/// engine's values — baseY here is computed from the SAME quad geometry
/// (`q.y + q.h`) the engine shears against, and [`GLYPH_SHEAR_K`] is the
/// layout-side mirror of `GuideRenderEngine.GLYPH_SHEAR_K`.
fn italic_kerning_compensate(run: &mut [ParleyRasterGlyph]) {
    // Engine basis: baseY = max over the whole run of (quad top + quad
    // height), i.e. the lowest quad bottom in document units. Runs span
    // multiple lines (grouping is per span), so this is the global max —
    // identical to the engine's single shearBaseY for the run.
    let mut base_y = f32::MIN;
    for q in run.iter() {
        base_y = base_y.max(q.y + q.h);
    }
    let mut acc = 0.0f32;
    let mut line = run[0].line_index;
    for q in run.iter_mut() {
        // Lines stack vertically: the first glyph of a new line must not
        // inherit the accumulated shift of the line above.
        if q.line_index != line {
            acc = 0.0;
            line = q.line_index;
        }
        q.x += acc;
        acc += GLYPH_SHEAR_K * (base_y - q.y);
    }
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
            // T1: wavy underline — wave amplitude band 2px tall (Java draws the
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
            // T1: dotted underline — same band as underline; Java rasterizes
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
        // Buffer is already at the scaled font size — width used as-is (D-1).
        Some(input.max_width())
    } else {
        None
    };

    let scaled = style.font_size() * style.font_scale();
    // Italic is NOT forwarded to shaping — the engine applies the synthetic
    // slant at draw time (MC §o parity; forwarding both would double-slant).
    let layout = font_system
        .parley
        .layout_styled(text, scaled, 1.55, style.bold(), max_w);
    let content_height = layout.height();
    let ascent = layout
        .lines()
        .next()
        .map(|l| l.metrics().baseline - l.metrics().block_min_coord)
        .unwrap_or(scaled);
    // T4: first-run real x_height/cap_height (shaped-size px, skrifa OS/2
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
            // Real advance, zero allowed (zero-width chars must measure 0 —
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
