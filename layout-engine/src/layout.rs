use std::collections::HashMap;

use crate::fb::{
    DecorationRect, DecorationRectArgs, FlatLayout, FlatLayoutArgs, FlatNode, GlyphBitmap,
    GlyphBitmapArgs, GlyphRun, GlyphRunArgs, LayoutInput, LayoutResult, LayoutResultArgs,
    PlacedGlyph, PlacedGlyphArgs,
};
use crate::measure::{create_measure_closure, GlyphAccum, NodeContext};
use crate::style_convert::flat_style_to_taffy;
use crate::text::GuideFontSystem;
use flatbuffers::FlatBufferBuilder;
use taffy::prelude::*;

/// Build a Taffy tree from FlatBuffer LayoutInput, compute layout,
/// and return LayoutResult FlatBuffer bytes.
pub fn compute_layout(
    input_bytes: &[u8],
    font_system: &mut GuideFontSystem,
) -> Vec<u8> {
    let input = flatbuffers::root::<LayoutInput>(input_bytes)
        .expect("Invalid LayoutInput FlatBuffer");

    let avail_width = input.available_width();
    // NB: visual_scale is intentionally NOT applied to the root width (D-3) —
    // Java blocks already pre-apply it per block (ResponsiveVisualSizing), so
    // multiplying the root by it double-counted and disagreed with every other
    // unscaled input (styles, lane pins, font metrics).
    let _visual_scale = input.visual_scale();
    // Display pixel ratio (MC guiScale): glyph bitmaps are rasterized at
    // font_size * render_scale so 1 texel maps to 1 physical pixel; quad
    // coordinates are then divided back into document units.
    let render_scale = input.render_scale().max(0.25);
    let fb_nodes = input.nodes();

    // Collect FlatNode references into a Vec for indexed access
    let flat_nodes: Vec<FlatNode> = fb_nodes
        .map_or_else(Vec::new, |v| (0..v.len()).map(|i| v.get(i)).collect());

    // ── Build Taffy tree ──
    let mut taffy: TaffyTree<NodeContext> = TaffyTree::new();
    let mut node_map: Vec<(NodeId, bool)> = Vec::with_capacity(flat_nodes.len());
    // Tracks which flat nodes are claimed as some container's child, so the
    // synthetic root only adopts genuine orphans. (Adopting EVERY node would
    // re-parent the whole tree to the root and flatten all nesting.)
    let mut claimed: Vec<bool> = vec![false; flat_nodes.len()];

    // Pass 1: Create leaf nodes (no children)
    for (i, fb_node) in flat_nodes.iter().enumerate() {
        if fb_node.children().map_or(true, |c| c.is_empty()) {
            let style = flat_style_to_taffy(&fb_node.style().unwrap());
            let ctx = NodeContext {
                flat_index: i,
                node_type: fb_node.node_type() as u8,
            };
            let id = taffy
                .new_leaf_with_context(style, ctx)
                .expect("Failed to create leaf node");
            node_map.push((id, true));
        } else {
            node_map.push((NodeId::from(0usize), false));
        }
    }

    // Pass 2: Create container nodes bottom-up
    let mut changed = true;
    let mut iterations = 0;
    let max_iter = flat_nodes.len() + 1;
    while changed && iterations < max_iter {
        changed = false;
        iterations += 1;
        for (i, fb_node) in flat_nodes.iter().enumerate() {
            if node_map[i].1 {
                continue;
            }
            let children = match fb_node.children() {
                Some(c) => c,
                None => continue,
            };
            if children.is_empty() {
                continue;
            }
            let all_ready = children.iter().all(|ci| node_map[ci as usize].1);
            if all_ready {
                let style = flat_style_to_taffy(&fb_node.style().unwrap());
                let child_ids: Vec<NodeId> =
                    children.iter().map(|ci| node_map[ci as usize].0).collect();
                let id = taffy
                    .new_with_children(style, &child_ids)
                    .expect("Failed to create container node");
                for ci in children.iter() {
                    claimed[ci as usize] = true;
                }
                // Text nodes (paragraphs) with inline-block children still need
                // their measure closure: attach the context so Taffy measures
                // the text content AND lays out the (absolute) children.
                if fb_node.node_type() == 1 {
                    taffy.set_node_context(
                        id,
                        Some(NodeContext {
                            flat_index: i,
                            node_type: fb_node.node_type() as u8,
                        }),
                    );
                }
                node_map[i] = (id, true);
                changed = true;
            }
        }
    }

    // Pass 3: Create a synthetic root to group all top-level flat nodes.
    // LytDocument extends LytNode, NOT LytBlock, so the document itself is not
    // a flat node and has no Taffy parent — every top-level block is an
    // "orphan" the synthetic root must adopt. Nodes already claimed as a
    // container's child in Pass 2 must NOT be adopted: adopting them again
    // would re-parent (and thereby flatten) the whole tree.
    let root_child_ids: Vec<NodeId> = node_map
        .iter()
        .enumerate()
        .filter(|(i, (_, created))| *created && !claimed[*i])
        .map(|(_, (id, _))| *id)
        .collect();
    let root_id = if root_child_ids.is_empty() {
        taffy
            .new_leaf(Style::default())
            .expect("Failed to create fallback root")
    } else {
        // Column flex so children stack vertically. Match Java LytDocument
        // padding 5 — unconditionally: single-child pages must get the same
        // origin/width as multi-child ones (D-2).
        let root_style = Style {
            display: Display::Flex,
            flex_direction: FlexDirection::Column,
            size: Size { width: Dimension::from_length(avail_width), height: Dimension::AUTO },
            padding: Rect {
                left: LengthPercentage::length(5.0),
                top: LengthPercentage::length(5.0),
                right: LengthPercentage::length(5.0),
                bottom: LengthPercentage::length(5.0),
            },
            ..Default::default()
        };
        taffy.new_with_children(root_style, &root_child_ids)
            .expect("Failed to create synthetic root")
    };

    // ── Compute layout with measure ──
    let mut glyph_acc: HashMap<usize, GlyphAccum> = HashMap::new();
    let measure_fn =
        create_measure_closure(font_system, &flat_nodes, &mut glyph_acc, input.justify() != 0);

    let available = Size {
        width: AvailableSpace::Definite(avail_width),
        height: AvailableSpace::MaxContent,
    };

    taffy
        .compute_layout_with_measure(root_id, available, measure_fn)
        .expect("Layout computation failed");

    // ── Collect results ──
    let mut fbb = FlatBufferBuilder::with_capacity(4096);

    // Taffy's Layout.location is relative to the PARENT's coordinate frame, so
    // absolute document positions must be accumulated down the tree. The flat
    // array registers parents before their children (depth-first flattening),
    // so a single forward pass suffices.
    let mut flat_parent: Vec<Option<usize>> = vec![None; flat_nodes.len()];
    for (i, fb) in flat_nodes.iter().enumerate() {
        if let Some(ch) = fb.children() {
            for ci in ch.iter() {
                flat_parent[ci as usize] = Some(i);
            }
        }
    }
    let mut abs_positions: Vec<(f32, f32)> = vec![(0.0, 0.0); flat_nodes.len()];
    let mut sizes: Vec<(f32, f32)> = vec![(0.0, 0.0); flat_nodes.len()];

    // Pass A: absolute positions and sizes for every node.
    for (i, _fb_node) in flat_nodes.iter().enumerate() {
        if !node_map[i].1 {
            continue;
        }
        let layout = taffy
            .layout(node_map[i].0)
            .expect("Failed to get layout");
        let parent_pos = flat_parent[i]
            .map(|p| abs_positions[p])
            .unwrap_or((0.0, 0.0));
        abs_positions[i] = (parent_pos.0 + layout.location.x, parent_pos.1 + layout.location.y);
        sizes[i] = (layout.size.width, layout.size.height);
    }

    // Inline post-pass: anchor inline blocks at their parley InlineBox
    // positions and grow lines vertically per their align modes.
    inline_post_pass(&flat_nodes, &mut glyph_acc, &mut abs_positions, &mut sizes);

    // Content height = the synthetic root's own box (its Column flex height
    // already sums in-flow children + padding). Do NOT max over all nodes:
    // scroll-clipped content inside viewports would inflate the page height
    // (D-4); absolute floats may extend below — Java takes max(rust, java)
    // where javaHeight already includes the float-bottom extension.
    let root_layout = taffy
        .layout(root_id)
        .expect("Failed to get root layout");
    let total_height = root_layout.location.y + root_layout.size.height;

    let mut flat_layout_offsets: Vec<flatbuffers::WIPOffset<FlatLayout>> = Vec::new();
    let mut glyph_run_offsets: Vec<flatbuffers::WIPOffset<GlyphRun>> = Vec::new();
    let mut decoration_offsets: Vec<flatbuffers::WIPOffset<DecorationRect>> = Vec::new();

    // Unique glyph bitmaps for the whole result, deduplicated by a stable
    // content key derived from the swash CacheKey (font, glyph, size, subpixel
    // bin, weight, flags). Java uses the u64 as an opaque atlas cache key.
    let mut bitmap_keys: Vec<u64> = Vec::new();
    let mut bitmap_data: Vec<(u32, u32, Vec<u8>)> = Vec::new();
    let mut bitmap_index: std::collections::HashSet<u64> = std::collections::HashSet::new();

    // Pass B: emit FlatLayouts, glyph runs and bitmaps.
    for (i, _fb_node) in flat_nodes.iter().enumerate() {
        if !node_map[i].1 {
            // Emit a placeholder so the FlatLayout vector stays index-aligned
            // with the input flat_nodes array (Java joins by index).
            flat_layout_offsets.push(FlatLayout::create(
                &mut fbb,
                &FlatLayoutArgs { x: 0.0, y: 0.0, w: 0.0, h: 0.0, order: 0 },
            ));
            continue;
        }
        let layout = taffy
            .layout(node_map[i].0)
            .expect("Failed to get layout");

        let (x, y) = abs_positions[i];
        let (w, h) = sizes[i];

        flat_layout_offsets.push(FlatLayout::create(
            &mut fbb,
            &FlatLayoutArgs { x, y, w, h, order: layout.order },
        ));

        // Glyph runs: rasterize each shaped glyph at its absolute document
        // position. The cache key comes from the shaping outcome (correct
        // font/weight/flags/subpixel bin) — never rebuilt from raw glyph ids.
        // Rich paragraphs yield one GlyphRun per span (argb = span color,
        // shear = span italic); decoration rects come from per-line span
        // glyph extents.
        if let Some(acc) = glyph_acc.remove(&i) {
            let span_styles = span_style_table(&flat_nodes[i]);
            let base_color = flat_nodes[i]
                .text()
                .and_then(|t| t.style())
                .map(|s| s.color())
                .unwrap_or(0xFFFFFFFF);
            let (quads, new_bitmaps) =
                crate::parley_text::rasterize_out_glyphs(&acc.glyphs, render_scale);
            for (key, bw, bh, rgba) in new_bitmaps {
                if bitmap_index.insert(key) {
                    bitmap_keys.push(key);
                    bitmap_data.push((bw, bh, rgba));
                }
            }
            let mut groups: std::collections::BTreeMap<
                u32,
                Vec<flatbuffers::WIPOffset<PlacedGlyph>>,
            > = Default::default();
            for q in quads {
                groups.entry(q.span_index).or_default().push(PlacedGlyph::create(
                    &mut fbb,
                    &PlacedGlyphArgs {
                        bitmap_key: q.bitmap_key,
                        x: x + q.x,
                        y: y + q.y,
                        w: q.w,
                        h: q.h,
                        // Byte ranges are unavailable in the parley path and
                        // have no consumers.
                        start: 0,
                        end: 0,
                        line_index: q.line_index,
                    },
                ));
            }
            for (si, placed_offsets) in groups {
                if placed_offsets.is_empty() {
                    continue;
                }
                let (argb, shear) = span_styles
                    .get(si as usize)
                    .map(|s| (s.color, s.italic))
                    // Single-style runs tint with the paragraph's base color
                    // (the atlas bitmaps are white — C-1).
                    .unwrap_or((base_color, false));
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
            emit_decorations(&acc.glyphs, &span_styles, i as u32, x, y, &mut fbb, &mut decoration_offsets);
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

    // Build per-node diagnostic string for Java-side logging
    let mut dbg = format!(
        "total_height={} |nodes={} created={} synth_root={}",
        total_height,
        flat_nodes.len(),
        node_map.iter().filter(|(_, ok)| *ok).count(),
        root_child_ids.len(),
    );
    for (i, fb) in flat_nodes.iter().enumerate() {
        if !node_map[i].1 { continue; }
        if let Ok(l) = taffy.layout(node_map[i].0) {
            let ty = fb.node_type();
            let (x, y, w_, h_) = (l.location.x, l.location.y, l.size.width, l.size.height);
            let tb = if ty == 1 {
                fb.text().and_then(|t| t.text()).map(|s| format!(" tlen={}", s.len())).unwrap_or_default()
            } else if ty == 0 {
                fb.children().map(|c| format!(" c={}", c.len())).unwrap_or_default()
            } else {
                String::new()
            };
            dbg.push_str(&format!(" |[{}]t{}@({:.0},{:.0})s({:.0},{:.0}){}",
                i, ty, x, y, w_, h_, tb));
        }
    }
    let debug_info_str = fbb.create_string(&dbg);

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
        if acc.markers.is_empty() {
            continue;
        }
        let refs = match flat_nodes[*i].text().and_then(|t| t.inline_blocks()) {
            Some(v) => v,
            None => continue,
        };
        let (node_x, node_y) = abs_positions[*i];

        // 1) Per-line growth from RAW marker metrics: line l's baseline and
        //    line_top shift down by the accumulated growth of earlier lines
        //    plus its own need-above.
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

        // 2) Anchor blocks per their alignment mode. Markers are in shaping
        //    order, matching the inline blocks' document order; pen positions
        //    already account for the blocks' widths (parley InlineBox).
        for (mi, m) in acc.markers.iter().enumerate() {
            if mi >= refs.len() {
                break;
            }
            let r = refs.get(mi);
            let ci = r.node() as usize;
            let (_, bh) = sizes[ci];
            let top = match r.align() {
                // Baseline ascent: block top sits `param` above the baseline.
                1 => m.baseline_y - r.param(),
                // Center on the line, then shift down by `param`.
                2 => m.line_top + (m.line_height - bh) / 2.0 + r.param(),
                // Default: block bottom sits 2px below the baseline.
                _ => m.baseline_y + 2.0 - bh,
            };
            abs_positions[ci] = (node_x + m.pen_x, node_y + top);
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
        .layout_styled(text, scaled, 10.0 / 9.0, style.bold(), max_w);
    let content_height = layout.height();
    let ascent = layout
        .lines()
        .next()
        .map(|l| l.metrics().baseline - l.metrics().block_min_coord)
        .unwrap_or(scaled);
    let (glyphs, _markers, max_x) =
        crate::parley_text::collect_layout(&layout, &[], max_w.unwrap_or(f32::MAX));
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
            line_height: scaled * (10.0 / 9.0),
            glyphs: Some(glyphs_vec),
            bitmaps: Some(bitmaps_vec),
        },
    );
    fbb.finish(result, None);
    fbb.finished_data().to_vec()
}
