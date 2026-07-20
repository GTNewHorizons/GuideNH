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
    /// Inline-block placeholders (U+FFFC) in shaping order, consumed by the
    /// inline post-pass in layout.rs.
    pub markers: Vec<InlineMarker>,
}

/// One U+FFFC placeholder in buffer-local coordinates: pen position on the
/// line's baseline, its line metrics, and the placeholder's advance (to be
/// replaced by the block width).
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

        let measured = match ctx.node_type {
            1 => measure_text(
                font_system, flat_nodes, index, glyph_acc, known, available,
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

    // Rich multi-style spans (TextData.spans): shaped together via
    // Buffer::set_rich_text; empty = legacy single-style shaping. Spans cover
    // the full text in document order, so span byte boundaries index into it.
    let rich_spans: Vec<crate::text::RichSpan> = match td.spans() {
        Some(v) if !v.is_empty() => {
            let mut out = Vec::with_capacity(v.len());
            for (i, s) in v.iter().enumerate() {
                let st = s.style().unwrap();
                out.push(crate::text::RichSpan {
                    text: s.text().unwrap_or("").to_string(),
                    bold: st.bold(),
                    italic: st.italic(),
                    span_index: i as u32,
                });
            }
            out
        }
        _ => Vec::new(),
    };
    let span_bounds: Vec<(usize, usize)> = {
        let mut bounds = Vec::with_capacity(rich_spans.len());
        let mut pos = 0usize;
        for s in &rich_spans {
            bounds.push((pos, pos + s.text.len()));
            pos += s.text.len();
        }
        bounds
    };

    // The buffer is already at the scaled font size (text.rs), so the wrap
    // width is used as-is — dividing by font_scale here would wrap early (D-1).
    let max_w = match available.width {
        AvailableSpace::Definite(w) => Some(w as f32),
        // Min-content probe: wrap at zero width so every breakable point is
        // taken — the measured width is then the longest unbreakable word,
        // not the whole unwrapped line (D-5).
        AvailableSpace::MinContent => Some(0.0),
        _ => None,
    };

    // Shape text — gets relative (buffer-local) glyphs. With float-wrap bands
    // (TextData.bands), the paragraph is shaped band by band at per-band widths
    // and stacked, mirroring CSS float wrapping (narrow beside the float, full
    // width below it).
    let bands = td.bands();
    let mut max_x = 0.0f32;
    let mut content_height = 0.0f32;
    let mut glyphs_out: Vec<crate::text::ShapedGlyph> = Vec::new();
    if let Some(bands) = bands.filter(|b| b.len() >= 2) {
        let mut y_off = 0.0f32;
        let mut line_off = 0usize;
        for bi in 0..bands.len() {
            let band = bands.get(bi);
            let start = band.split_byte() as usize;
            let end = if bi + 1 < bands.len() {
                (bands.get(bi + 1).split_byte() as usize).min(text.len())
            } else {
                text.len()
            };
            if start >= end || start >= text.len() {
                continue;
            }
            let segment = &text[start..end];
            let bw = band.width();
            // Rich path: clip the spans overlapping this band's byte range.
            // span_index stays the global span number, so attribution is
            // unaffected by band splitting.
            let band_spans: Vec<crate::text::RichSpan> = if rich_spans.is_empty() {
                Vec::new()
            } else {
                let mut pieces = Vec::new();
                for (si, (ss, se)) in span_bounds.iter().enumerate() {
                    let lo = (*ss).max(start);
                    let hi = (*se).min(end);
                    if lo < hi {
                        let src = &rich_spans[si];
                        pieces.push(crate::text::RichSpan {
                            text: text[lo..hi].to_string(),
                            bold: src.bold,
                            italic: src.italic,
                            span_index: si as u32,
                        });
                    }
                }
                pieces
            };
            let shaped = if !band_spans.is_empty() {
                fs.shape_rich_text(
                    &band_spans,
                    font_size,
                    font_scale,
                    if bw > 0.0 { Some(bw) } else { None },
                )
            } else {
                fs.shape_text(
                    segment,
                    font_size,
                    bold,
                    italic,
                    font_scale,
                    if bw > 0.0 { Some(bw) } else { None },
                )
            };
            let band_lines = shaped
                .glyphs
                .iter()
                .map(|g| g.line_index)
                .max()
                .map_or(0, |m| m + 1);
            for mut g in shaped.glyphs {
                g.x += band.margin_left();
                g.y += y_off;
                g.line_top += y_off;
                g.line_index += line_off;
                // Glyph byte ranges are segment-local — rebase to the full text.
                g.start += start as u32;
                g.end += start as u32;
                if g.x + g.w > max_x {
                    max_x = g.x + g.w;
                }
                glyphs_out.push(g);
            }
            y_off += shaped.content_height;
            line_off += band_lines;
        }
        content_height = y_off;
    } else if !rich_spans.is_empty() {
        let shaped = fs.shape_rich_text(&rich_spans, font_size, font_scale, max_w);
        content_height = shaped.content_height;
        for g in &shaped.glyphs {
            if g.x + g.w > max_x {
                max_x = g.x + g.w;
            }
        }
        glyphs_out = shaped.glyphs;
    } else {
        let shaped = fs.shape_text(text, font_size, bold, italic, font_scale, max_w);
        content_height = shaped.content_height;
        for g in &shaped.glyphs {
            if g.x + g.w > max_x {
                max_x = g.x + g.w;
            }
        }
        glyphs_out = shaped.glyphs;
    }

    let h = if content_height > 0.0 {
        content_height
    } else {
        font_size * font_scale * (10.0 / 9.0)
    };

    // Inline-block placeholders: reserve the height their lines grow by, so
    // Taffy flows following siblings below the block (the paragraph's measured
    // height must already include it — growing it in the post-pass would come
    // too late).
    let markers: Vec<InlineMarker> = glyphs_out
        .iter()
        .filter(|g| g.inline_placeholder)
        .map(|g| InlineMarker {
            pen_x: g.x,
            baseline_y: g.y,
            line_top: g.line_top,
            line_height: g.h,
            line_index: g.line_index,
            advance: g.w,
        })
        .collect();
    let mut h = h;
    if !markers.is_empty() {
        h += inline_line_growth(nodes, idx, &markers);
    }

    // Store relative glyphs and inline markers for later absolute-coord fixup
    acc.insert(
        idx,
        GlyphAccum {
            glyphs: glyphs_out,
            markers,
        },
    );

    Size {
        width: max_x.max(1.0),
        height: h.max(1.0),
    }
}

/// Extra paragraph height from inline blocks, mirroring the legacy per-line
/// box growth: every line holding a placeholder grows by the space its blocks
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
