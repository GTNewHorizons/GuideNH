use std::collections::HashMap;

use crate::fb::{
    FlatLayout, FlatLayoutArgs, FlatNode, GlyphRun, GlyphRunArgs, LayoutInput, LayoutResult,
    LayoutResultArgs, PlacedGlyph, PlacedGlyphArgs,
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
    let visual_scale = input.visual_scale();
    let fb_nodes = input.nodes();

    // Collect FlatNode references into a Vec for indexed access
    let flat_nodes: Vec<FlatNode> = fb_nodes
        .map_or_else(Vec::new, |v| (0..v.len()).map(|i| v.get(i)).collect());

    // ── Build Taffy tree ──
    let mut taffy: TaffyTree<NodeContext> = TaffyTree::new();
    let mut node_map: Vec<(NodeId, bool)> = Vec::with_capacity(flat_nodes.len());

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
                node_map[i] = (id, true);
                changed = true;
            }
        }
    }

    // Find root (last created node)
    let root_idx = (0..flat_nodes.len())
        .rev()
        .find(|&i| node_map[i].1)
        .unwrap_or(0);
    let root_id = node_map[root_idx].0;

    // ── Compute layout with measure ──
    let mut glyph_acc: HashMap<usize, GlyphAccum> = HashMap::new();
    let measure_fn = create_measure_closure(font_system, &flat_nodes, &mut glyph_acc);

    let available = Size {
        width: AvailableSpace::Definite(avail_width * visual_scale),
        height: AvailableSpace::MaxContent,
    };

    taffy
        .compute_layout_with_measure(root_id, available, measure_fn)
        .expect("Layout computation failed");

    // ── Collect results ──
    let mut fbb = FlatBufferBuilder::with_capacity(4096);

    let mut flat_layout_offsets: Vec<flatbuffers::WIPOffset<FlatLayout>> = Vec::new();
    let mut glyph_run_offsets: Vec<flatbuffers::WIPOffset<GlyphRun>> = Vec::new();
    let mut total_height = 0.0f32;

    for (i, _fb_node) in flat_nodes.iter().enumerate() {
        if !node_map[i].1 {
            continue;
        }
        let layout = taffy
            .layout(node_map[i].0)
            .expect("Failed to get layout");

        let x = layout.location.x;
        let y = layout.location.y;
        let w = layout.size.width;
        let h = layout.size.height;

        flat_layout_offsets.push(FlatLayout::create(
            &mut fbb,
            &FlatLayoutArgs { x, y, w, h, order: layout.order },
        ));

        if y + h > total_height {
            total_height = y + h;
        }

        // Glyph runs: add node.location → absolute coordinates
        if let Some(acc) = glyph_acc.remove(&i) {
            let mut placed_offsets: Vec<flatbuffers::WIPOffset<PlacedGlyph>> = Vec::new();
            for g in &acc.glyphs {
                placed_offsets.push(PlacedGlyph::create(
                    &mut fbb,
                    &PlacedGlyphArgs {
                        glyph_id: g.glyph_id,
                        x: x + g.x,
                        y: y + g.y,
                        w: g.w,
                        h: g.h,
                    },
                ));
            }
            let glyphs_vec = fbb.create_vector(&placed_offsets);
            glyph_run_offsets.push(GlyphRun::create(
                &mut fbb,
                &GlyphRunArgs {
                    node_index: i as u32,
                    font_atlas_id: 0,
                    glyphs: Some(glyphs_vec),
                },
            ));
        }
    }

    let nodes_vec = fbb.create_vector(&flat_layout_offsets);
    let glyph_runs_vec = fbb.create_vector(&glyph_run_offsets);

    let result = LayoutResult::create(
        &mut fbb,
        &LayoutResultArgs {
            nodes: Some(nodes_vec),
            glyph_runs: Some(glyph_runs_vec),
            content_height: total_height,
        },
    );

    fbb.finish(result, None);
    fbb.finished_data().to_vec()
}
