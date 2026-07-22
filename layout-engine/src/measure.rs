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
    /// Relative glyphs (paragraph-local coordinates, no node offset).
    pub glyphs: Vec<crate::parley_text::OutGlyph>,
    /// Inline-block anchors in shaping order, consumed by the inline
    /// post-pass in layout.rs.
    pub markers: Vec<InlineMarker>,
}

/// One inline-block anchor in paragraph-local coordinates: pen position on
/// the line's baseline plus the line metrics the anchor's block needs for
/// its vertical alignment.
#[derive(Clone, Debug)]
pub struct InlineMarker {
    pub pen_x: f32,
    pub baseline_y: f32,
    pub line_top: f32,
    pub line_height: f32,
    pub line_index: usize,
    pub advance: f32,
}

/// Space an inline block needs above its line's baseline / below its line's
/// bottom, per alignment mode (see InlineBlockRef in the schema). Positive
/// values grow the line; the legacy layout grew line boxes the same way.
pub(crate) fn marker_needs(m: &InlineMarker, block_h: f32, align: i8, param: f32) -> (f32, f32) {
    let line_ascent = m.baseline_y - m.line_top;
    let line_descent = (m.line_top + m.line_height) - m.baseline_y;
    match align {
        // Baseline ascent: block top sits `param` above the baseline.
        1 => (
            (param - line_ascent).max(0.0),
            ((block_h - param) - line_descent).max(0.0),
        ),
        // Center on the line, then shift down by `param`.
        2 => {
            let top_off = (m.line_height - block_h) / 2.0 + param;
            ((-top_off).max(0.0), (top_off + block_h - m.line_height).max(0.0))
        }
        // Default: block bottom sits 2px below the baseline.
        _ => (
            (block_h - 2.0 - line_ascent).max(0.0),
            (2.0 - line_descent).max(0.0),
        ),
    }
}

/// Explicit pixel height of an inline block node (0 when not px-sized).
pub(crate) fn inline_block_height(nodes: &[FlatNode], idx: usize) -> f32 {
    let Some(style) = nodes[idx].style() else { return 0.0 };
    let Some(d) = style.size_h() else { return 0.0 };
    if d.unit() == 1 { d.value() } else { 0.0 }
}

/// Explicit pixel width of an inline block node (0 when not px-sized).
pub(crate) fn inline_block_width(nodes: &[FlatNode], idx: usize) -> f32 {
    let Some(style) = nodes[idx].style() else { return 0.0 };
    let Some(d) = style.size_w() else { return 0.0 };
    if d.unit() == 1 { d.value() } else { 0.0 }
}

/// Build the measure closure for compute_layout_with_measure.
/// Dispatches by node_type to the appropriate measurement function.
pub fn create_measure_closure<'a>(
    font_system: &'a mut GuideFontSystem,
    flat_nodes: &'a [FlatNode],
    glyph_acc: &'a mut HashMap<usize, GlyphAccum>,
    justify: bool,
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

        let measured = match ctx.node_type {
            1 => measure_text(
                font_system, flat_nodes, index, glyph_acc, available, justify, &[], 0.0, 0.0, &[],
            ),
            2 => measure_image(flat_nodes, index),
            3 => measure_slot(flat_nodes, index),
            4 => measure_thematic_break(flat_nodes, index, known),
            8 => measure_latex(flat_nodes, index),
            _ => Size::ZERO,
        };
        // Explicit style sizes win over content measurement (CSS behavior):
        // Taffy passes them in as known dimensions; honoring them is what lets
        // opaque fixed-size leaves (buttons, sprites, px-pinned boxes) keep
        // their declared size instead of collapsing to the measured ZERO.
        Size {
            width: known.width.unwrap_or(measured.width),
            height: known.height.unwrap_or(measured.height),
        }
    }
}

pub(crate) fn measure_text(
    fs: &mut GuideFontSystem,
    nodes: &[FlatNode],
    idx: usize,
    acc: &mut HashMap<usize, GlyphAccum>,
    available: Size<AvailableSpace>,
    justify: bool,
    floats: &[crate::parley_text::FloatRect],
    para_abs_y: f32,
    para_x: f32,
    clears: &[(usize, u8)],
) -> Size<f32> {
    let node = &nodes[idx];
    let td = match node.text() {
        Some(t) => t,
        None => return Size::ZERO,
    };
    let text = td.text().unwrap_or("");
    let style = td.style().unwrap();
    let font_size = style.font_size();
    let font_scale = style.font_scale();

    // Rich multi-style spans (TextData.spans) → builder ranges. Spans cover
    // the full text in document order, so span byte boundaries index into it.
    let mut span_styles: Vec<crate::parley_text::SpanStyle> = Vec::new();
    if let Some(v) = td.spans() {
        if !v.is_empty() {
            let mut pos = 0usize;
            for s in v.iter() {
                let t = s.text().unwrap_or("");
                let st = s.style().unwrap();
                span_styles.push(crate::parley_text::SpanStyle {
                    start: pos,
                    end: pos + t.len(),
                    bold: st.bold(),
                });
                pos += t.len();
            }
        }
    }

    // Inline blocks: anchor bytes are the U+FFFC placeholders in document
    // order; each box's width comes from its node's explicit pixel size.
    let mut inlines: Vec<crate::parley_text::InlineSpec> = Vec::new();
    if let Some(refs) = td.inline_blocks() {
        if !refs.is_empty() {
            let anchors: Vec<usize> = text
                .char_indices()
                .filter(|(_, ch)| *ch == '\u{FFFC}')
                .map(|(i, _)| i)
                .collect();
            for (k, r) in refs.iter().enumerate() {
                if k >= anchors.len() {
                    break;
                }
                inlines.push(crate::parley_text::InlineSpec {
                    anchor_byte: anchors[k],
                    width: inline_block_width(nodes, r.node() as usize),
                });
            }
        }
    }

    // The buffer is already at the scaled font size (parley_text), so the
    // wrap width is used as-is (D-1).
    let max_w = match available.width {
        AvailableSpace::Definite(w) => w as f32,
        // Min-content probe: wrap at zero width so every breakable point is
        // taken — the measured width is then the longest unbreakable word,
        // not the whole unwrapped line (D-5).
        AvailableSpace::MinContent => 0.0,
        _ => f32::MAX,
    };

    let req = crate::parley_text::ShapeRequest {
        text,
        spans: &span_styles,
        inlines: &inlines,
        floats,
        para_abs_y,
        para_x,
        clears,
        font_size,
        font_scale,
        max_width: max_w,
        justify,
    };
    let shaped = crate::parley_text::shape_paragraph(&mut fs.parley, &req);

    let mut h = shaped.content_height;
    if h <= 0.0 {
        h = font_size * font_scale * (10.0 / 9.0);
    }
    // Inline-block vertical growth: reserve the space their lines grow by,
    // so Taffy flows following siblings below the block (the paragraph's
    // measured height must already include it — growing it in the post-pass
    // would come too late).
    if !shaped.markers.is_empty() {
        h += inline_line_growth(nodes, idx, &shaped.markers);
    }

    acc.insert(
        idx,
        GlyphAccum {
            glyphs: shaped.glyphs,
            markers: shaped.markers,
        },
    );

    Size {
        width: shaped.max_x.max(1.0),
        height: h.max(1.0),
    }
}

/// Extra paragraph height from inline blocks, mirroring the legacy per-line
/// box growth: every line holding an anchor grows by the space its blocks
/// need above the baseline plus below the line, and later lines are pushed
/// down by the accumulated growth (applied in the inline post-pass).
fn inline_line_growth(nodes: &[FlatNode], idx: usize, markers: &[InlineMarker]) -> f32 {
    let Some(refs) = nodes[idx].text().and_then(|t| t.inline_blocks()) else {
        return 0.0;
    };
    let mut by_line: std::collections::BTreeMap<usize, (f32, f32)> = Default::default();
    for (mi, m) in markers.iter().enumerate() {
        if mi >= refs.len() {
            break;
        }
        let r = refs.get(mi);
        let bh = inline_block_height(nodes, r.node() as usize);
        let (na, nb) = marker_needs(m, bh, r.align(), r.param());
        let e = by_line.entry(m.line_index).or_default();
        e.0 = e.0.max(na);
        e.1 = e.1.max(nb);
    }
    by_line.values().map(|(a, b)| a + b).sum()
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
