use std::collections::HashMap;

use crate::fb::FlatNode;
use crate::text::GuideFontSystem;
use taffy::prelude::*;

/// Context stored in Taffy leaf nodes for measure closure dispatch.
#[derive(Debug, Clone)]
pub struct NodeContext {
    pub flat_index: usize,
    pub node_type: u8,
}

/// Accumulator for shaped glyphs during measure closure.
/// Inserted on each call; last call wins (Taffy may measure multiple times).
pub struct GlyphAccum {
    /// Relative glyphs (buffer-local coordinates, no node offset).
    pub glyphs: Vec<crate::text::ShapedGlyph>,
}

/// Build the measure closure for compute_layout_with_measure.
/// Dispatches by node_type to the appropriate measurement function.
pub fn create_measure_closure<'a>(
    font_system: &'a mut GuideFontSystem,
    flat_nodes: &'a [FlatNode],
    glyph_acc: &'a mut HashMap<usize, GlyphAccum>,
) -> impl FnMut(
    Size<Option<f32>>,
    Size<AvailableSpace>,
    NodeId,
    Option<&mut NodeContext>,
    &Style,
) -> Size<f32> + 'a {
    move |known, available, _node_id, ctx, _style| -> Size<f32> {
        let ctx = match ctx {
            Some(c) => c,
            None => return Size::ZERO,
        };
        let index = ctx.flat_index;

        match ctx.node_type {
            1 => measure_text(
                font_system, flat_nodes, index, glyph_acc, known, available,
            ),
            2 => measure_image(flat_nodes, index),
            3 => measure_slot(flat_nodes, index),
            4 => measure_thematic_break(flat_nodes, index, known),
            8 => measure_latex(flat_nodes, index),
            _ => Size::ZERO,
        }
    }
}

fn measure_text(
    fs: &mut GuideFontSystem,
    nodes: &[FlatNode],
    idx: usize,
    acc: &mut HashMap<usize, GlyphAccum>,
    _known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
) -> Size<f32> {
    let node = &nodes[idx];
    let td = match node.text() {
        Some(t) => t,
        None => return Size::ZERO,
    };
    let text = td.text().unwrap_or("");
    let style = td.style().unwrap();
    let font_size = style.font_size();
    let bold = style.bold();
    let italic = style.italic();
    let font_scale = style.font_scale();

    let max_w = match available.width {
        AvailableSpace::Definite(w) => Some(w as f32 / font_scale),
        _ => None,
    };

    // Shape text — gets relative (buffer-local) glyphs
    let glyphs = fs.shape_text(text, font_size, bold, italic, font_scale, max_w);

    // Compute content size
    let mut max_x = 0.0f32;
    let mut max_y = 0.0f32;
    for g in &glyphs {
        if g.x + g.w > max_x {
            max_x = g.x + g.w;
        }
        if g.y + g.h > max_y {
            max_y = g.y + g.h;
        }
    }
    let h = if max_y > 0.0 {
        max_y
    } else {
        font_size * font_scale * 1.4
    };

    // Store relative glyphs for later absolute-coord fixup
    acc.insert(idx, GlyphAccum { glyphs });

    Size {
        width: max_x.max(1.0),
        height: h.max(1.0),
    }
}

fn measure_image(nodes: &[FlatNode], idx: usize) -> Size<f32> {
    let node = &nodes[idx];
    let img = match node.image() {
        Some(i) => i,
        None => return Size::ZERO,
    };
    let w = if img.explicit_w() > 0.0 {
        img.explicit_w()
    } else {
        img.natural_w() * img.scale_x()
    };
    let h = if img.explicit_h() > 0.0 {
        img.explicit_h()
    } else {
        img.natural_h() * img.scale_y()
    };
    Size {
        width: w.max(1.0),
        height: h.max(1.0),
    }
}

fn measure_slot(nodes: &[FlatNode], idx: usize) -> Size<f32> {
    let node = &nodes[idx];
    let slot = match node.slot() {
        Some(s) => s,
        None => return Size::ZERO,
    };
    let sz = slot.slot_size();
    Size {
        width: sz,
        height: sz,
    }
}

fn measure_thematic_break(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
) -> Size<f32> {
    let node = &nodes[idx];
    let tb = match node.break_() {
        Some(t) => t,
        None => return Size::ZERO,
    };
    Size {
        width: known.width.unwrap_or(0.0),
        height: tb.height(),
    }
}

fn measure_latex(nodes: &[FlatNode], idx: usize) -> Size<f32> {
    let node = &nodes[idx];
    let latex = match node.latex() {
        Some(l) => l,
        None => return Size::ZERO,
    };
    Size {
        width: latex.raw_w() * latex.user_scale(),
        height: (latex.raw_h() + 8.0) * latex.user_scale(),
    }
}
