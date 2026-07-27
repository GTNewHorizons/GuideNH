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
    /// Float-aligned inline block anchors: (flat_node_index, paragraph-relative y).
    pub float_anchors: Vec<(usize, f32)>,
    /// (x_off, line_width) of the last line's full float-compressed window
    /// (paragraph-relative). None for empty paragraphs. Used by separator
    /// line (kind=3 DecorationRect) for heading paragraphs.
    pub last_line_window: Option<(f32, f32)>,
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
    visual_scale: f32,
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

        let (measured, _clear_floor) = match ctx.node_type {
            1 => measure_text(
                font_system, flat_nodes, index, glyph_acc, available, justify, &[], 0.0, 0.0, &[],
            ),
            2 => (measure_image(flat_nodes, index), None),
            3 => (measure_slot(flat_nodes, index), None),
            4 => (measure_thematic_break(flat_nodes, index, known), None),
            8 => (measure_latex(flat_nodes, index), None),
             20 => (measure_recipe_box(flat_nodes, index), None),
             21 => (measure_pie_chart(flat_nodes, index, known, available, visual_scale), None),
              22 | 23 | 24 | 25 => (measure_chart(flat_nodes, index, known, available, visual_scale), None),
              26 => (measure_structure_view(flat_nodes, index, known, available, visual_scale), None),
              27 => (measure_guidebook_scene(flat_nodes, index, known, available, visual_scale), None),
              28 => (measure_function_graph(flat_nodes, index, known, available, visual_scale), None),
              29 => (measure_mediawiki_generated_list(flat_nodes, index, known, available), None),
              30 => (measure_mediawiki_special_generated(flat_nodes, index, known, available), None),
             _ => (Size::ZERO, None),
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
) -> (Size<f32>, Option<f32>) {
    let node = &nodes[idx];
    let td = match node.text() {
        Some(t) => t,
        None => return (Size::ZERO, None),
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
                let align = r.align();
                let float_side = match align {
                    3 => Some(1u8),
                    4 => Some(2u8),
                    _ => None,
                };
                inlines.push(crate::parley_text::InlineSpec {
                    anchor_byte: anchors[k],
                    width: inline_block_width(nodes, r.node() as usize),
                    height: inline_block_height(nodes, r.node() as usize),
                    float_side,
                    node: r.node() as usize,
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

    // Hard breaks (<br>): raw byte offsets (in the break-free text) at which the
    // paragraph is split into independently shaped pieces. A <br> is a hard line
    // break that must hold under ANY white-space mode, but parley 0.11 exposes no
    // white-space control and its normal collapse would fold a literal '\n' into a
    // space — so the break is realised structurally by shaping each piece on its
    // own and stacking them vertically (piece k+1 starts at the accumulated height
    // of the pieces before it). Pieces carry no inline boxes (a paragraph that has
    // both inline boxes and hard breaks falls through to the single-shape path
    // below, keeping inline geometry correct at the cost of the hard break).
    let breaks: Vec<usize> = td
        .breaks()
        .map(|v| v.iter().map(|x| x as usize).collect())
        .unwrap_or_default();

    let (shaped_glyphs, shaped_markers, shaped_h, shaped_max_x, shaped_floor, shaped_float_anchors, shaped_last_window) =
        if !breaks.is_empty() && inlines.is_empty() {
            let mut last_window: Option<(f32, f32)> = None;
            let mut bounds: Vec<usize> = Vec::with_capacity(breaks.len() + 2);
            bounds.push(0);
            for &b in &breaks {
                if b > *bounds.last().unwrap() && b <= text.len() {
                    bounds.push(b);
                }
            }
            if *bounds.last().unwrap() != text.len() {
                bounds.push(text.len());
            }
            let mut all_glyphs: Vec<crate::parley_text::OutGlyph> = Vec::new();
            let mut all_markers: Vec<crate::measure::InlineMarker> = Vec::new();
            let mut acc_h: f32 = 0.0;
            let mut max_x: f32 = 0.0;
            let mut floor: Option<f32> = None;
            for w in bounds.windows(2) {
                let lo = w[0];
                let hi = w[1];
                if lo >= hi {
                    continue;
                }
                let sub_text = &text[lo..hi];
                let sub_spans: Vec<crate::parley_text::SpanStyle> = span_styles
                    .iter()
                    .filter_map(|sp| {
                        let ns = sp.start.max(lo);
                        let ne = sp.end.min(hi);
                        if ns < ne {
                            Some(crate::parley_text::SpanStyle {
                                start: ns - lo,
                                end: ne - lo,
                                bold: sp.bold,
                            })
                        } else {
                            None
                        }
                    })
                    .collect();
                let sub_clears: Vec<(usize, u8)> = clears
                    .iter()
                    .filter_map(|(o, s)| {
                        if *o >= lo && *o < hi {
                            Some((*o - lo, *s))
                        } else {
                            None
                        }
                    })
                    .collect();
                let seg_top = para_abs_y + acc_h;
                let req = crate::parley_text::ShapeRequest {
                    text: sub_text,
                    spans: &sub_spans,
                    inlines: &[],
                    floats,
                    para_abs_y: seg_top,
                    para_x,
                    clears: &sub_clears,
                    font_size,
                    font_scale,
                    max_width: max_w,
                    justify,
                };
                let shaped = crate::parley_text::shape_paragraph(&mut fs.parley, &req);
                for mut g in shaped.glyphs {
                    g.y += acc_h;
                    g.line_top += acc_h;
                    all_glyphs.push(g);
                }
                for mut m in shaped.markers {
                    m.baseline_y += acc_h;
                    m.line_top += acc_h;
                    all_markers.push(m);
                }
                let sh = if shaped.content_height <= 0.0 {
                    font_size * font_scale * (10.0 / 9.0)
                } else {
                    shaped.content_height
                };
                acc_h += sh;
                max_x = max_x.max(shaped.max_x);
                floor = match (floor, shaped.clear_floor) {
                    (Some(a), Some(b)) => Some(a.max(b)),
                    (a, b) => a.or(b),
                };
                // Track last segment's window for the separator-line mechanism
                last_window = shaped.last_line_window;
            }
            (all_glyphs, all_markers, acc_h, max_x, floor, Vec::new(), last_window)
        } else {
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
            if !shaped.markers.is_empty() {
                h += inline_line_growth(nodes, idx, &shaped.markers);
            }
            (shaped.glyphs, shaped.markers, h, shaped.max_x, shaped.clear_floor, shaped.float_anchors, shaped.last_line_window)
        };

    acc.insert(
        idx,
        GlyphAccum {
            glyphs: shaped_glyphs,
            markers: shaped_markers,
            float_anchors: shaped_float_anchors,
            last_line_window: shaped_last_window,
        },
    );

    (
        Size {
            width: shaped_max_x.max(1.0),
            height: shaped_h.max(1.0),
        },
        shaped_floor,
    )
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

/// Measure an NEI recipe box (node_type = 20). The formula mirrors
/// LytNeiRecipeBox.computeLayout term for term, with Java-computed
/// pixel values (title_text_width, title_height, body dimensions)
/// provided via RecipeBoxData. Constants replicate the Java class's
/// static fields.
fn measure_recipe_box(nodes: &[FlatNode], idx: usize) -> Size<f32> {
    const FRAME_BORDER: f32 = 4.0;
    const TITLE_GAP_AFTER_ICON: f32 = 3.0;
    const TITLE_GAP_BEFORE_ACTION: f32 = 3.0;
    const ACTION_BUTTON_SIZE: f32 = 12.0;
    const BODY_MARGIN: f32 = 2.0;

    let node = &nodes[idx];
    let rb = match node.recipe_box() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    // titleWidth = iconSize + (iconSize > 0 ? TITLE_GAP_AFTER_ICON : 0) + titleTextWidth
    let mut title_width = rb.icon_size()
        + (if rb.icon_size() > 0.0 { TITLE_GAP_AFTER_ICON } else { 0.0 })
        + rb.title_text_width();
    // if recipeJumpEnabled: titleWidth += TITLE_GAP_BEFORE_ACTION + ACTION_BUTTON_SIZE
    if rb.recipe_jump_enabled() {
        title_width += TITLE_GAP_BEFORE_ACTION + ACTION_BUTTON_SIZE;
    }
    // innerW = max(bodyWidth, titleWidth)
    let inner_w = f32::max(rb.body_width(), title_width);
    // w = FRAME_BORDER + innerW + FRAME_BORDER
    let w = FRAME_BORDER + inner_w + FRAME_BORDER;

    // h = FRAME_BORDER + titleHeight + BODY_MARGIN + bodyTopInset + bodyHeight + bodyYShift + FRAME_BORDER
    let h = FRAME_BORDER
        + rb.title_height()
        + BODY_MARGIN
        + rb.body_top_inset()
        + rb.body_height()
        + rb.body_y_shift()
        + FRAME_BORDER;

    Size { width: w, height: h }
}

/// Shared chart measurement formula, extracted from LytChartBase.computeLayout.
/// Used by both PieChart (node_type=21) via PieChartData and Cartesian charts
/// (BarChart node_type=22, future Column/Line/Scatter) via ChartData.
///
/// Chrome height is now computed in Rust from the final width w, eliminating
/// the T6a-found discrepancy where the Java lazy getter used unscaled
/// preferredWidth for legend wrapping (T6a ticket: chromeHeight lazy 用未缩放宽度).
fn chart_measurement(
    preferred_w: f32,
    total_h: f32,
    title_chrome: f32,
    legend_position: i8,
    legend_row_height: f32,
    legend_label_widths: &[f32],
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
    visual_scale: f32,
) -> Size<f32> {
    // Constants mirrored from LytChartBase:
    const PADDING: f32 = 8.0;
    const LEGEND_GAP: f32 = 6.0;
    const LEGEND_ENTRY_GAP: f32 = 12.0;
    const HORIZONTAL_ROW_GAP: f32 = 2.0;
    const MIN_PLOT_HEIGHT: f32 = 72.0;

    // Width formula — mirrors LytChartBase.computeLayout:
    //   preferredWidth = (explicitW > 0 ? explicitW : DEFAULT_WIDTH) + extraPlotWidth
    //   scaledWidth = scaleWidth(preferredWidth, visualScale, 64)
    //   width = max(1, min(scaledWidth, availableWidth))
    //
    // When explicitWidth > 0 (user-set via setExplicitSize), Taffy passes
    // it as known.width and we use it directly. Otherwise known.width is
    // None and we compute the width from the Java-precomputed preferred_width.
    let w = match known.width {
        Some(explicit) => explicit,
        None => {
            let scaled = scale_width(preferred_w, visual_scale, 64.0);
            let avail = match available.width {
                AvailableSpace::Definite(a) => a,
                _ => f32::MAX,
            };
            (scaled.min(avail)).max(1.0)
        }
    };

    // Chrome height — Rust-computed from the final width w.
    // Mirrors LytChartBase.estimateFixedChromeHeight:
    //   chrome = PADDING * 2
    //       + title_chrome  (Java-precomputed: lineHeight(titleStyle) + TITLE_GAP, 0 if no title)
    //       + legendHeight  (only for TOP/BOTTOM legend)
    //       + LEGEND_GAP    (if legend present and TOP/BOTTOM)
    let content_w = (w - PADDING * 2.0).max(1.0);
    let legend_h = chart_legend_height(
        legend_row_height,
        legend_label_widths,
        content_w,
        LEGEND_ENTRY_GAP,
        HORIZONTAL_ROW_GAP,
    );
    let mut chrome = PADDING * 2.0;
    chrome += title_chrome;
    // Chrome includes legend_h (may be 0 when entries empty) + LEGEND_GAP
    // when legend is TOP/BOTTOM, matching LytChartBase.estimateFixedChromeHeight
    // which always adds LEGEND_GAP for TOP/BOTTOM regardless of legend height.
    let has_legend = legend_position == 1 || legend_position == 2;
    if has_legend {
        chrome += legend_h + LEGEND_GAP;
    }

    // Height formula — mirrors LytChartBase.computeLayout:
    //   totalHeight  = explicitH > 0 ? explicitH : DEFAULT_HEIGHT
    //   bodyHeight   = max(1, totalHeight - clamp(chrome, 0, totalHeight - 1))
    //   scaledBody   = scaleHeightForWidth(preferredW, bodyHeight, width, MIN_PLOT_HEIGHT)
    //   height       = chrome + scaledBody
    let raw_h = total_h;
    // Guard against raw_h < 1.0: clamp upper bound to at least 0 so the
    // range is valid even when totalHeight is 0 or negative (T5.2 legacy).
    let body = (raw_h - chrome.clamp(0.0, (raw_h - 1.0).max(0.0))).max(1.0);
    let scaled_body = scale_height_for_width(preferred_w, body, w, MIN_PLOT_HEIGHT);
    let h = chrome + scaled_body;

    Size { width: w, height: h }
}

/// Compute the total height of a horizontal (TOP/BOTTOM) legend given per-entry
/// widths, row height, and layout constants. Mirrors
/// ChartLegendRenderer.measureHorizontalLegendHeight term for term, including
/// the first-item-no-gap rule and the computation:
///   rows * rowHeight + max(0, rows-1) * rowGap
fn chart_legend_height(
    row_height: f32,
    item_widths: &[f32],
    available_width: f32,
    entry_gap: f32,
    row_gap: f32,
) -> f32 {
    if row_height <= 0.0 || item_widths.is_empty() {
        return 0.0;
    }
    let mut rows: i32 = 1;
    let mut row_w: f32 = 0.0;
    for &item_w in item_widths {
        if item_w <= 0.0 {
            continue;
        }
        let needed = if row_w == 0.0 {
            item_w
        } else {
            row_w + entry_gap + item_w
        };
        if row_w > 0.0 && needed > available_width {
            rows += 1;
            row_w = item_w;
        } else {
            row_w = needed;
        }
    }
    // If rows stayed at 1 but no items had width > 0, return 0 (matching Java).
    if row_w == 0.0 {
        return 0.0;
    }
    rows as f32 * row_height + (rows - 1) as f32 * row_gap
}

/// Measure a pie chart (node_type = 21). The formula mirrors
/// LytChartBase.computeLayout term for term, with Java-computed
/// font-metric values (title_chrome, legend_row_height,
/// legend_label_widths) provided via PieChartData. Chrome height
/// is computed in Rust from the actual width, using the legend
/// wrapping algorithm transplanted from ChartLegendRenderer.
/// Pure-arithmetic helper functions (scale_width, scale_height_for_width)
/// replicate ResponsiveVisualSizing on the Rust side.
fn measure_pie_chart(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
    visual_scale: f32,
) -> Size<f32> {
    let node = &nodes[idx];
    let pd = match node.pie_chart() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    // Collect legend label widths from FlatBuffer vector into a Vec<f32>.
    // This is needed because chart_measurement operates on &[f32] and the
    // flatbuffers Vector does not implement Deref<Target=[f32]>.
    let widths: Vec<f32> = pd
        .legend_label_widths()
        .map(|v| (0..v.len()).map(|i| v.get(i)).collect())
        .unwrap_or_default();

    chart_measurement(
        pd.preferred_width(),
        pd.total_height(),
        pd.title_chrome(),
        pd.legend_position(),
        pd.legend_row_height(),
        &widths,
        known,
        available,
        visual_scale,
    )
}

/// Measure a Cartesian chart (node_type = 22: BarChart, future Column/Line/Scatter).
/// Uses the same LytChartBase.computeLayout formula as PieChart, reading sizing
/// data from the ChartData table instead. Chrome height is computed in Rust
/// from the final width w, using the legend wrapping algorithm transplanted
/// from ChartLegendRenderer.
fn measure_chart(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
    visual_scale: f32,
) -> Size<f32> {
    let node = &nodes[idx];
    let cd = match node.chart_data() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    // Collect legend label widths from FlatBuffer vector into a Vec<f32>.
    let widths: Vec<f32> = cd
        .legend_label_widths()
        .map(|v| (0..v.len()).map(|i| v.get(i)).collect())
        .unwrap_or_default();

    chart_measurement(
        cd.preferred_width(),
        cd.total_height(),
        cd.title_chrome(),
        cd.legend_position(),
        cd.legend_row_height(),
        &widths,
        known,
        available,
        visual_scale,
    )
}

/// Measure an isometric structure view (node_type = 26). The formula mirrors
/// LytStructureView.computeLayout term for term, with Java-precomputed
/// view_width and view_height (setViewSize or DEFAULT_WIDTH/HEIGHT) provided
/// via StructureViewData. Uses the shared scale_width/scale_height_for_width
/// helpers that replicate ResponsiveVisualSizing on the Rust side.
fn measure_structure_view(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
    visual_scale: f32,
) -> Size<f32> {
    let node = &nodes[idx];
    let sv = match node.structure_view_data() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    let view_w = sv.view_width();
    let view_h = sv.view_height();

    // targetWidth = scaleWidth(viewWidth, visualScale, 32)
    let target_w = scale_width(view_w, visual_scale, 32.0);
    // width = clamp(targetWidth, 1, availableWidth)
    let avail_w = match available.width {
        AvailableSpace::Definite(a) => a,
        _ => f32::MAX,
    };
    let w = target_w.max(1.0).min(avail_w);
    // height = scaleHeightForWidth(viewWidth, viewHeight, width, 32)
    let h = scale_height_for_width(view_w, view_h, w, 32.0);

    // Explicit style sizes (known dimensions from Taffy) win over content
    // measurement — the caller's known.unwrap_or already handles this for
    // the general case, but we include the logic for clarity.
    Size {
        width: w,
        height: h,
    }
}

/// Measure a guidebook scene (node_type = 27). The formula mirrors
/// LytGuidebookScene.computeLayout term for term, with Java-precomputed
/// dock sizes, button column reserve, button total height, and bottom
/// control area height provided via GuidebookSceneData. The responsive
/// scene sizing (scale_width, dock clamping, computeResponsiveSceneHeight)
/// is replicated in Rust using available_width and visual_scale.
fn measure_guidebook_scene(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
    visual_scale: f32,
) -> Size<f32> {
    const MIN_RESPONSIVE_SCENE_SIZE: f32 = 16.0;
    const BLOCK_STATS_DOCK_GAP: f32 = 4.0;  // only used in dock clamping logic
    const BLOCK_STATS_MIN_WIDTH: f32 = 32.0;

    let node = &nodes[idx];
    let gs = match node.guidebook_scene_data() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    let scene_w = gs.scene_width();
    let scene_h = gs.scene_height();
    let reserve = gs.button_column_reserve();
    let buttons_total_h = gs.buttons_total_height();
    let mut left_dock = gs.left_dock();
    let mut right_dock = gs.right_dock();
    let top_dock = gs.top_dock();
    let bottom_dock = gs.bottom_dock();
    let bottom_ctrl_h = gs.bottom_control_area_height();
    let reserve_bottom = gs.reserve_bottom_control();

    // targetSceneWidth = scaleWidth(width, visualScale, MIN_RESPONSIVE_SCENE_SIZE)
    let target_scene_w = scale_width(scene_w, visual_scale, MIN_RESPONSIVE_SCENE_SIZE);

    // totalDesired = targetSceneWidth + reserve + leftDock + rightDock
    let total_desired = target_scene_w + reserve + left_dock + right_dock;

    // availableWidth from Taffy
    let avail_w = match available.width {
        AvailableSpace::Definite(a) => a,
        _ => f32::MAX,
    };

    // w = min(totalDesired, max(reserve + MIN_RESPONSIVE_SCENE_SIZE, availableWidth))
    let w = total_desired.min((reserve + MIN_RESPONSIVE_SCENE_SIZE).max(avail_w));

    // availableForDocks = max(0, w - reserve - targetSceneWidth)
    let available_for_docks = (w - reserve - target_scene_w).max(0.0);

    // Dock clamping: if left+right > availableForDocks, shrink proportionally
    if left_dock + right_dock > available_for_docks {
        if left_dock > 0.0 && right_dock > 0.0 {
            left_dock = left_dock.min(available_for_docks / 2.0);
            right_dock = right_dock.min(available_for_docks - left_dock);
        } else if left_dock > 0.0 {
            left_dock = left_dock.min(available_for_docks);
        } else {
            right_dock = right_dock.min(available_for_docks);
        }
    }

    // minDockSpace = BLOCK_STATS_MIN_WIDTH + BLOCK_STATS_DOCK_GAP
    let min_dock_space = BLOCK_STATS_MIN_WIDTH + BLOCK_STATS_DOCK_GAP;
    if left_dock > 0.0 && left_dock < min_dock_space {
        left_dock = 0.0;
    }
    if right_dock > 0.0 && right_dock < min_dock_space {
        right_dock = 0.0;
    }

    // sceneW = max(MIN_RESPONSIVE_SCENE_SIZE, w - reserve - leftDock - rightDock)
    let scene_w_responsive = (w - reserve - left_dock - right_dock).max(MIN_RESPONSIVE_SCENE_SIZE);

    // computeResponsiveSceneHeight(sceneW, buttonsTotalH)
    let scene_h_responsive = compute_responsive_scene_height(scene_w, scene_h, scene_w_responsive, buttons_total_h, MIN_RESPONSIVE_SCENE_SIZE);

    // h = topDock + sceneH + (reserveBottomControlArea ? bottomControlAreaHeight : 0) + bottomDock
    let h = top_dock + scene_h_responsive + (if reserve_bottom { bottom_ctrl_h } else { 0.0 }) + bottom_dock;

    // known dimensions (explicit style sizes) win over measured
    Size {
        width: known.width.unwrap_or(w),
        height: known.height.unwrap_or(h),
    }
}

/// Mirrors LytGuidebookScene.computeResponsiveSceneHeight.
/// scene_width / scene_height are the intrinsic dimensions (setSceneSize or
/// defaults); actual_width is the responsive scene width after dock clamping.
fn compute_responsive_scene_height(
    base_width: f32,
    base_height: f32,
    actual_width: f32,
    buttons_total_h: f32,
    min_size: f32,
) -> f32 {
    let base_w = base_width.max(1.0);
    let base_h = base_height.max(1.0);
    if actual_width >= base_w {
        return base_h.max(buttons_total_h);
    }
    let scale = actual_width / base_w;
    let scaled_h = (base_h * scale).round().max(1.0).max(min_size);
    scaled_h.max(buttons_total_h)
}

/// Measure a function graph (node_type = 28). The formula mirrors
/// LytFunctionGraph.computeLayout term for term, with Java-precomputed
/// title_chrome, legend_row_height, and per-plot label_item_widths
/// (via FunctionGraphData). Uses shared scale_width/scale_height_for_width
/// helpers that replicate ResponsiveVisualSizing on the Rust side.
fn measure_function_graph(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
    visual_scale: f32,
) -> Size<f32> {
    const PADDING: f32 = 8.0;
    const AXIS_PAD_LEFT: f32 = 28.0;
    const AXIS_PAD_BOTTOM: f32 = 14.0;
    const LEGEND_GAP_ABOVE: f32 = 4.0;
    const LEGEND_ITEM_GAP: f32 = 10.0;
    const LEGEND_ROW_GAP: f32 = 2.0;
    const MIN_PLOT_HEIGHT: f32 = 88.0;

    let node = &nodes[idx];
    let fgd = match node.function_graph_data() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    let base_w = fgd.base_width();
    let base_h = fgd.base_height();

    // width = scaleWidth(baseWidth, visualScale, 72), clamped to [1, availableWidth]
    let target_w = scale_width(base_w, visual_scale, 72.0);
    let avail_w = match available.width {
        AvailableSpace::Definite(a) => a,
        _ => f32::MAX,
    };
    let w = target_w.max(1.0).min(avail_w);

    // plotWidth = max(0, width - PADDING * 2 - AXIS_PAD_LEFT)
    let plot_w = (w - PADDING * 2.0 - AXIS_PAD_LEFT).max(0.0);

    // fixedChromeHeight = PADDING * 2 + AXIS_PAD_BOTTOM
    let mut fixed_chrome = PADDING * 2.0 + AXIS_PAD_BOTTOM;

    // if (title != null && !title.isEmpty()) fixedChrome += titleChrome (precomputed)
    fixed_chrome += fgd.title_chrome();

    // legendHeight = measureLegendHeight(plotWidth)
    // Precomputed row height and per-label item widths; wrapping algorithm
    // replicates LytFunctionGraph.measureLegendHeight.
    let legend_row_h = fgd.legend_row_height();
    let legend_h = if legend_row_h > 0.0 {
        let mut rows: i32 = 1;
        let mut row_w: f32 = 0.0;
        if let Some(widths) = fgd.label_item_widths() {
            for i in 0..widths.len() {
                let item_w = widths.get(i);
                if item_w <= 0.0 {
                    continue;
                }
                let needed = if row_w == 0.0 {
                    item_w
                } else {
                    row_w + LEGEND_ITEM_GAP + item_w
                };
                if row_w > 0.0 && needed > plot_w {
                    rows += 1;
                    row_w = item_w;
                } else {
                    row_w = needed;
                }
            }
        }
        // If rows stays 1 but no items had width > 0, the loop never
        // ran (all labels empty or no plots). The Java code returns 0
        // for this case. Since we start at rows=1, check: legend_h = 0
        // when no items have been processed (row_w == 0.0).
        if row_w == 0.0 {
            0.0
        } else {
            rows as f32 * legend_row_h + (rows - 1) as f32 * LEGEND_ROW_GAP
        }
    } else {
        0.0
    };
    if legend_h > 0.0 {
        fixed_chrome += legend_h + LEGEND_GAP_ABOVE;
    }

    // height = scaleBodyHeightForWidth(baseWidth, baseHeight, width, fixedChrome, MIN_PLOT_HEIGHT)
    let safe_total_h = base_h.max(1.0);
    let safe_fixed_h = fixed_chrome.clamp(0.0, (safe_total_h - 1.0).max(0.0));
    let body_h = (safe_total_h - safe_fixed_h).max(1.0);
    let scaled_body = scale_height_for_width(base_w, body_h, w, MIN_PLOT_HEIGHT);
    let h = safe_fixed_h + scaled_body;

    // Explicit style sizes (known dimensions from Taffy) win over content
    // measurement — the caller's known.unwrap_or already handles this.
    Size {
        width: known.width.unwrap_or(w),
        height: known.height.unwrap_or(h),
    }
}

/// Measure a MediaWiki generated list block (node_type = 29). The formula mirrors
/// MediaWikiGeneratedListBlock.computeLayout term for term. The column-planning
/// algorithm (planColumns) depends on Java object data (entry sort keys, titles,
/// section grouping) and cannot be replicated in Rust. Java precomputes the max
/// column content height via MediaWikiGeneratedListData.max_content_height.
/// Rust adds the padding constants to produce the total block height.
/// Width is always availableWidth — the block fills the parent's content box.
fn measure_mediawiki_generated_list(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
) -> Size<f32> {
    // Constants mirrored from MediaWikiGeneratedListBlock:
    //   private static final int TOP_PADDING = 6;
    //   private static final int BOTTOM_PADDING = 6;
    const TOP_PADDING: f32 = 6.0;
    const BOTTOM_PADDING: f32 = 6.0;

    let node = &nodes[idx];
    let data = match node.mediawiki_generated_list_data() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    let max_content_h = data.max_content_height();

    // Width: the block always fills available width (no intrinsic preference).
    // Mirrors computeLayout: return new LytRect(x, y, availableWidth, ...).
    let w = match available.width {
        AvailableSpace::Definite(a) => a.max(0.0),
        _ => 0.0,
    };

    // Height = TOP_PADDING + maxColumnContentHeight + BOTTOM_PADDING.
    // Mirrors computeLayout:
    //   return new LytRect(x, y, availableWidth, TOP_PADDING + maxColumnHeight + BOTTOM_PADDING);
    // and for empty entries:
    //   return new LytRect(x, y, availableWidth, TOP_PADDING + ROW_HEIGHT + BOTTOM_PADDING);
    let h = TOP_PADDING + max_content_h + BOTTOM_PADDING;

    Size {
        width: known.width.unwrap_or(w),
        height: known.height.unwrap_or(h),
    }
}

/// Measure a MediaWiki special generated block (node_type = 30).
///
/// Java passes width-independent font facts (title widths, subtitle word widths,
/// line-height constants, estimated heights). Rust computes columnWidth from
/// availableWidth, wraps subtitle words per entry, packs columns (shortest-column-first
/// for groups, even distribution for flat entries), and produces maxColumnHeight.
/// Width is always availableWidth — the block fills the parent's content box.
///
/// Mirrors MediaWikiSpecialGeneratedBlock.computeLayout term for term, with the
/// entry-height computation now owned by Rust using the pre-measured word-width
/// array. This eliminates the T6a-found discrepancy between the lazy estimateEntryHeight
/// (hardcoded 9px, no wrapping) and computeEntryHeight (10px, wrapped by columnWidth).
fn measure_mediawiki_special_generated(
    nodes: &[FlatNode],
    idx: usize,
    known: Size<Option<f32>>,
    available: Size<AvailableSpace>,
) -> Size<f32> {
    // Layout constants, mirrored from MediaWikiSpecialGeneratedBlock static fields.
    const TOP_PADDING: f32 = 6.0;
    const BOTTOM_PADDING: f32 = 6.0;
    const SIDE_PADDING: f32 = 2.0;
    const COLUMN_GAP: f32 = 10.0;
    const GROUP_MARGIN: f32 = 6.0;
    const HEADER_HEIGHT: f32 = 20.0;
    const HEADER_MARGIN_TOP: f32 = 5.0;
    const HEADER_MARGIN_BOTTOM: f32 = 5.0;
    const ENTRY_HEIGHT: f32 = 20.0;
    const ENTRY_GAP: f32 = 2.0;
    const ICON_SIZE: f32 = 16.0;
    const ICON_GAP: f32 = 4.0;
    const LIST_MARKER_SIZE: f32 = 3.0;
    const LIST_MARKER_GAP: f32 = 6.0;
    const TITLE_SUBTITLE_GAP: f32 = 3.0;
    const ENTRY_VERTICAL_PADDING_TOP: f32 = 3.0;
    const ENTRY_VERTICAL_PADDING_BOTTOM: f32 = 3.0;
    const LOAD_MORE_HEIGHT: f32 = 18.0;
    const LOAD_MORE_MARGIN_TOP: f32 = 2.0;
    // Line heights: getLineHeight(LINK_STYLE) = 10, getLineHeight(SUBTITLE_STYLE) = 10
    const TITLE_LINE_HEIGHT: f32 = 10.0;
    const SUBTITLE_LINE_HEIGHT: f32 = 10.0;

    let node = &nodes[idx];
    let data = match node.mediawiki_special_generated_data() {
        Some(d) => d,
        None => return Size::ZERO,
    };

    // Width: the block always fills available width (no intrinsic preference).
    let w = match available.width {
        AvailableSpace::Definite(a) => a.max(0.0),
        _ => 0.0,
    };

    // ── Column width (mirrors Java computeLayout) ──────────────────────────
    let inner_width = (w - SIDE_PADDING * 2.0).max(0.0);
    let column_count = data.column_count().max(1) as usize;
    let column_width =
        ((inner_width - COLUMN_GAP * (column_count as f32 - 1.0)) / column_count as f32).max(1.0);

    // ── Read font-fact vectors ─────────────────────────────────────────────
    let total_entries = data.total_entry_count() as usize;
    let entry_title_widths = data.entry_title_widths();
    let entry_has_icon = data.entry_has_icon();
    let entry_subtitle_line_counts = data.entry_subtitle_line_counts();
    let subtitle_line_word_counts = data.subtitle_line_word_counts();
    let subtitle_word_widths = data.subtitle_word_widths();
    let subtitle_space_width = data.subtitle_space_width();

    let group_cnt = data.group_count().max(0) as usize;
    let group_title_widths = data.group_title_widths();
    let group_entry_counts = data.group_entry_counts();
    let group_estimated_heights = data.group_estimated_heights();

    let has_more = data.has_more();

    // ── Compute real entry height given columnWidth ────────────────────────
    // Mirrors computeEntryHeight: wrapping + line-height constants.
    let compute_entry_height = |entry_idx: usize| -> f32 {
        let has_icon = entry_has_icon
            .as_ref()
            .and_then(|v| if entry_idx < v.len() { Some(v.get(entry_idx)) } else { None })
            .unwrap_or(0i8)
            != 0;

        let text_max_width = {
            let reserve = LIST_MARKER_SIZE
                + LIST_MARKER_GAP
                + if has_icon { ICON_SIZE + ICON_GAP } else { 0.0 };
            (column_width - reserve).max(1.0)
        };

        let line_count = entry_subtitle_line_counts
            .as_ref()
            .and_then(|v| {
                if entry_idx < v.len() {
                    Some(v.get(entry_idx) as usize)
                } else {
                    None
                }
            })
            .unwrap_or(0);

        if line_count == 0 {
            return ENTRY_HEIGHT;
        }

        // ── Word-level wrapping ────────────────────────────────────────
        // Find the global line-index start for this entry within the flat arrays.
        let line_start: usize = entry_subtitle_line_counts
            .as_ref()
            .map(|v| (0..entry_idx).map(|i| v.get(i) as usize).sum::<usize>())
            .unwrap_or(0);

        let mut total_wrapped: usize = 0;
        if let (Some(wc_vec), Some(ww_vec)) = (subtitle_line_word_counts.as_ref(), subtitle_word_widths.as_ref()) {
            // Compute word-index start: sum word counts of all lines before line_start.
            let mut word_start: usize = 0;
            for li in 0..line_start {
                if li < wc_vec.len() {
                    word_start += wc_vec.get(li) as usize;
                }
            }

            for li in 0..line_count {
                let global_line = line_start + li;
                if global_line >= wc_vec.len() {
                    break;
                }
                let n_words = wc_vec.get(global_line) as usize;
                if n_words == 0 {
                    continue;
                }

                // Raw line width = word widths + spaces between words.
                let raw_line_width: f32 = {
                    let mut sum = 0.0f32;
                    for wi in word_start..word_start + n_words {
                        if wi < ww_vec.len() {
                            sum += ww_vec.get(wi);
                        }
                    }
                    if n_words > 1 {
                        sum += subtitle_space_width * (n_words - 1) as f32;
                    }
                    sum
                };

                if raw_line_width <= text_max_width {
                    total_wrapped += 1;
                } else {
                    // Word-wrap: mirrors Java appendWrappedLine.
                    let mut current_w: f32 = 0.0;
                    let mut sub_lines: usize = 0;
                    for wi in word_start..word_start + n_words {
                        if wi >= ww_vec.len() {
                            break;
                        }
                        let word_w = ww_vec.get(wi);
                        let needed = if current_w == 0.0 {
                            word_w
                        } else {
                            current_w + subtitle_space_width + word_w
                        };
                        if current_w > 0.0 && needed > text_max_width {
                            sub_lines += 1;
                            current_w = word_w;
                        } else {
                            current_w = needed;
                        }
                    }
                    if current_w > 0.0 {
                        sub_lines += 1;
                    }
                    total_wrapped += sub_lines;
                }

                word_start += n_words;
            }
        }

        if total_wrapped == 0 {
            return ENTRY_HEIGHT;
        }

        // Mirrors computeEntryHeight:
        //   contentHeight = max(ICON_SIZE, getLineHeight(LINK_STYLE) + TITLE_SUBTITLE_GAP
        //                               + getLineHeight(SUBTITLE_STYLE) * subtitleLines.size())
        //   height = max(ENTRY_HEIGHT, ENTRY_VERTICAL_PADDING_TOP + contentHeight + ENTRY_VERTICAL_PADDING_BOTTOM)
        let content_height = (TITLE_LINE_HEIGHT + TITLE_SUBTITLE_GAP
            + SUBTITLE_LINE_HEIGHT * total_wrapped as f32)
            .max(ICON_SIZE);
        (ENTRY_VERTICAL_PADDING_TOP + content_height + ENTRY_VERTICAL_PADDING_BOTTOM)
            .max(ENTRY_HEIGHT)
    };

    // ── Packing & per-column height ────────────────────────────────────────
    let max_column_height: f32;

    if total_entries == 0 {
        // Empty result: one row at ENTRY_HEIGHT (mirrors isEmpty branch).
        max_column_height = ENTRY_HEIGHT;
    } else if group_cnt > 0 {
        // ── Grouped: shortest-column-first packing ─────────────────────
        // Mirrors layoutColumns: assign each group to the shortest column
        // using estimated heights, then recompute real heights.
        let mut col_heights_est: Vec<f32> = vec![0.0; column_count];
        // For tracking which groups go to which column.
        let mut col_groups: Vec<Vec<usize>> = vec![Vec::new(); column_count];

        for g in 0..group_cnt {
            // Find shortest column.
            let target = col_heights_est
                .iter()
                .enumerate()
                .min_by(|(_, a), (_, b)| a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal))
                .map(|(i, _)| i)
                .unwrap_or(0);
            col_groups[target].push(g);
            let est_h = group_estimated_heights
                .as_ref()
                .and_then(|v| if g < v.len() { Some(v.get(g)) } else { None })
                .unwrap_or(0.0);
            col_heights_est[target] += est_h;
        }

        // Compute real column heights using actual entry heights.
        // Build per-group entry-index ranges.
        let mut group_entry_start: Vec<usize> = Vec::with_capacity(group_cnt);
        let mut cur: usize = 0;
        for g in 0..group_cnt {
            group_entry_start.push(cur);
            let cnt = group_entry_counts
                .as_ref()
                .and_then(|v| if g < v.len() { Some(v.get(g) as usize) } else { None })
                .unwrap_or(0);
            cur += cnt;
        }

        let mut real_max: f32 = 0.0;
        for col_g in &col_groups {
            if col_g.is_empty() {
                // Empty column gets no height contribution but we need to
                // still consider it (prev max will capture the tallest).
                continue;
            }
            let mut col_y: f32 = 0.0;
            for &g in col_g {
                // Group header.
                let has_title = group_title_widths
                    .as_ref()
                    .and_then(|v| if g < v.len() { Some(v.get(g)) } else { None })
                    .unwrap_or(0.0)
                    > 0.0;
                if has_title {
                    col_y += HEADER_MARGIN_TOP + HEADER_HEIGHT + HEADER_MARGIN_BOTTOM;
                }
                // Entries in this group.
                let start = group_entry_start[g];
                let end = group_entry_start.get(g + 1).copied().unwrap_or(cur);
                for e in start..end {
                    col_y += compute_entry_height(e);
                    col_y += ENTRY_GAP;
                }
                col_y += GROUP_MARGIN;
            }
            real_max = real_max.max(col_y);
        }
        max_column_height = real_max;
    } else {
        // ── Flat: even distribution across columns ─────────────────────
        // Mirrors layoutFlatColumns: perColumn = ceil(N / columnCount).
        let per_column = ((total_entries + column_count - 1) / column_count).max(1);
        let mut real_max: f32 = 0.0;
        for col in 0..column_count {
            let start = col * per_column;
            if start >= total_entries {
                break;
            }
            let end = (start + per_column).min(total_entries);
            let mut col_y: f32 = 0.0;
            for e in start..end {
                col_y += compute_entry_height(e);
                col_y += ENTRY_GAP;
            }
            // Flat entries are wrapped in a single no-title group per column.
            col_y += GROUP_MARGIN;
            real_max = real_max.max(col_y);
        }
        max_column_height = real_max;
    }

    // hasMore adds LOAD_MORE_HEIGHT after the column content (mirrors Java).
    let max_h = if has_more {
        max_column_height + LOAD_MORE_MARGIN_TOP + LOAD_MORE_HEIGHT
    } else {
        max_column_height
    };

    let h = TOP_PADDING + max_h + BOTTOM_PADDING;

    Size {
        width: known.width.unwrap_or(w),
        height: known.height.unwrap_or(h),
    }
}

/// Mirrors ResponsiveVisualSizing.scaleWidth: apply a visual-scale factor
/// to a base width, then clamp.
fn scale_width(base_width: f32, visual_scale: f32, min_width: f32) -> f32 {
    let safe_base = base_width.max(1.0);
    let clamped = visual_scale.clamp(0.1, 1.0);
    if clamped >= 0.999 {
        return safe_base;
    }
    (safe_base * clamped).round().max(1.0).max(min_width)
}

/// Mirrors ResponsiveVisualSizing.scaleHeightForWidth: proportionally scale
/// a base height when the actual width is narrower than the base width.
fn scale_height_for_width(base_width: f32, base_height: f32, actual_width: f32, min_height: f32) -> f32 {
    let safe_base_w = base_width.max(1.0);
    let safe_base_h = base_height.max(1.0);
    let safe_actual_w = actual_width.max(1.0);
    if safe_actual_w >= safe_base_w {
        return safe_base_h;
    }
    let scale = safe_actual_w / safe_base_w;
    (safe_base_h * scale).round().max(1.0).max(min_height)
}
