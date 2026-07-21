//! Parley text layout — the guide's line-box layer (migration target for the
//! cosmic shaping path).
//!
//! Owns the parley contexts (fontique FontContext + LayoutContext) and the
//! paragraph layout → positioned-glyph pipeline. Parley's `BreakLines` gives
//! per-line geometry control (`BreakerState.set_line_max_advance` /
//! `set_line_x`), so float wrapping is done by feeding each line its
//! available width from the clip query — no pre-split bands, no byte rebase,
//! no segmented re-shaping. Inline blocks are first-class `InlineBox`es
//! (width participates in wrapping; vertical growth stays with the Java-side
//! post-pass). Rasterization stays on the swash bridge.

use std::sync::Arc;

use parley::fontique;
use parley::{
    Alignment, AlignmentOptions, FontContext, FontData, FontFamily, FontFamilyName, FontWeight,
    GenericFamily, InlineBox, InlineBoxKind, Layout, LayoutContext, LineHeight,
    PositionedLayoutItem, StyleProperty,
};

/// Custom brush: carries the source span index (TextData.spans) through
/// shaping so each emitted glyph run can be tinted/decorated per span.
#[derive(Clone, Debug, Default, PartialEq)]
pub struct SpanBrush(pub u32);

/// Parley-side font state riding inside [`crate::text::GuideFontSystem`]
/// alongside the (legacy) cosmic one during the migration.
pub struct ParleyFonts {
    pub font_cx: FontContext,
    pub layout_cx: LayoutContext<SpanBrush>,
}

impl ParleyFonts {
    pub fn new() -> Self {
        Self {
            font_cx: FontContext::new(),
            layout_cx: LayoutContext::new(),
        }
    }

    pub fn load_font_data(&mut self, data: Vec<u8>) {
        self.font_cx
            .collection
            .register_fonts(fontique::Blob::new(Arc::new(data)), None);
    }
}

// ═══════════════ Phase 1: renderTextParley (window A/B) ═══════════════

impl ParleyFonts {
    /// Lay out one single-style paragraph at `scaled_size` wrapped to
    /// `max_width` (None = unwrapped), with line height
    /// `scaled_size × line_height_rel`.
    pub fn layout_styled(
        &mut self,
        text: &str,
        scaled_size: f32,
        line_height_rel: f32,
        bold: bool,
        max_width: Option<f32>,
    ) -> Layout<SpanBrush> {
        let mut builder = self
            .layout_cx
            .ranged_builder(&mut self.font_cx, text, 1.0, true);
        builder.push_default(StyleProperty::FontSize(scaled_size));
        builder.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(
            line_height_rel,
        )));
        builder.push_default(StyleProperty::FontFamily(FontFamily::Single(
            FontFamilyName::Generic(GenericFamily::SansSerif),
        )));
        if bold {
            builder.push_default(StyleProperty::FontWeight(FontWeight::BOLD));
        }
        let mut layout = builder.build(text);
        layout.break_all_lines(max_width);
        layout.align(Alignment::Start, AlignmentOptions::default());
        layout
    }

    /// Lay out one single-style paragraph at `font_size` wrapped to
    /// `max_width` (None = unwrapped), with line height
    /// `font_size × line_height_rel` (callers pass 10/9 for guide text, 1.5
    /// to A/B against the legacy renderText test window).
    pub fn layout_paragraph(
        &mut self,
        text: &str,
        font_size: f32,
        line_height_rel: f32,
        max_width: Option<f32>,
    ) -> Layout<SpanBrush> {
        self.layout_styled(text, font_size, line_height_rel, false, max_width)
    }
}

/// One rasterized glyph quad (RGBA bitmap + pixel position), matching the
/// RenderGlyph wire shape renderText emits.
pub struct RasterQuad {
    pub x: i32,
    pub y: i32,
    pub w: u32,
    pub h: u32,
    pub rgba: Vec<u8>,
}

/// Rasterize every glyph of a laid-out paragraph into RGBA quads (white text,
/// coverage as alpha — the guide text pipeline is monochrome, tinted at draw
/// time; color emoji bitmaps are out of scope).
pub fn rasterize_layout(layout: &Layout<SpanBrush>, font_size: f32) -> Vec<RasterQuad> {
    let mut out = Vec::new();
    for line in layout.lines() {
        for item in line.items() {
            let PositionedLayoutItem::GlyphRun(gr) = item else {
                continue;
            };
            let fd = gr.run().font();
            let Some(font_ref) = swash::FontRef::from_index(fd.data.data(), fd.index as usize)
            else {
                continue;
            };
            let mut ctx = swash::scale::ScaleContext::new();
            let mut scaler = ctx
                .builder(font_ref)
                .size(font_size)
                .hint(true)
                .build();
            for g in gr.positioned_glyphs() {
                let Some(img) = swash::scale::Render::new(&[
                    swash::scale::Source::ColorOutline(0),
                    swash::scale::Source::ColorBitmap(swash::scale::StrikeWith::BestFit),
                    swash::scale::Source::Outline,
                ])
                .format(swash::zeno::Format::Alpha)
                .render(&mut scaler, g.id as u16) else {
                    continue;
                };
                let (w, h) = (img.placement.width as u32, img.placement.height as u32);
                if w == 0 || h == 0 {
                    continue;
                }
                out.push(RasterQuad {
                    x: g.x as i32 + img.placement.left,
                    y: g.y as i32 - img.placement.top,
                    w,
                    h,
                    rgba: alpha_to_rgba(&img.data),
                });
            }
        }
    }
    out
}

/// Alpha-mask bitmap → tightly packed white RGBA.
fn alpha_to_rgba(data: &[u8]) -> Vec<u8> {
    let mut rgba = vec![0u8; data.len() * 4];
    for (i, &a) in data.iter().enumerate() {
        rgba[i * 4] = 255;
        rgba[i * 4 + 1] = 255;
        rgba[i * 4 + 2] = 255;
        rgba[i * 4 + 3] = a;
    }
    rgba
}

// ═══════════════ Phase 2: paragraph shaping core ═══════════════

/// Forbidden interval (paragraph-relative); see FloatClip in the schema.
/// `x <= 0` is a left-side clip (text starts right of x+width), otherwise a
/// right-side clip (text ends at x).
pub struct Clip {
    pub y_top: f32,
    pub y_bottom: f32,
    pub x: f32,
    pub width: f32,
}

/// One styled span: byte range into the ORIGINAL (unstripped) text + bold
/// flag. Index = position in TextData.spans; glyphs shaped from the range
/// report it back as their brush.
pub struct SpanStyle {
    pub start: usize,
    pub end: usize,
    pub bold: bool,
}

/// One inline block to place: byte index of its U+FFFC anchor in the
/// ORIGINAL text + its layout width (paragraph-relative units).
pub struct InlineSpec {
    pub anchor_byte: usize,
    pub width: f32,
}

pub struct ShapeRequest<'a> {
    pub text: &'a str,
    /// Rich spans covering the whole text in document order (empty =
    /// single-style paragraph).
    pub spans: &'a [SpanStyle],
    /// Inline blocks in document order (paired with U+FFFC anchors).
    pub inlines: &'a [InlineSpec],
    /// Float-forbidden intervals (empty = uniform-width paragraph).
    pub clips: &'a [Clip],
    pub font_size: f32,
    pub font_scale: f32,
    pub max_width: f32,
    pub justify: bool,
}

/// A shaped glyph in paragraph-local coordinates: (x, y) is the pen position
/// on the baseline; w is the advance. The font bytes ride along so the swash
/// rasterizer needs no other font database.
pub struct OutGlyph {
    pub glyph_id: u32,
    pub x: f32,
    pub y: f32,
    pub w: f32,
    pub line_top: f32,
    pub line_height: f32,
    pub line_index: usize,
    pub span_index: u32,
    pub font: FontData,
    pub font_size: f32,
}

pub struct ParleyShaped {
    pub glyphs: Vec<OutGlyph>,
    /// Inline-box anchors in document order (consumed by the post-pass).
    pub markers: Vec<crate::measure::InlineMarker>,
    pub content_height: f32,
    pub max_x: f32,
}

/// Shape one paragraph: spans → ranged styles, inline blocks → InlineBox,
/// float clips → per-line widths via BreakerState, then collect positioned
/// glyphs and inline markers.
pub fn shape_paragraph(parley: &mut ParleyFonts, req: &ShapeRequest) -> ParleyShaped {
    let scaled = req.font_size * req.font_scale;

    // Strip the U+FFFC anchors: parley places InlineBoxes by byte index, and
    // the placeholder char itself must never shape into a .notdef glyph.
    let mut clean = String::with_capacity(req.text.len());
    let mut box_clean_idx: Vec<usize> = Vec::with_capacity(req.inlines.len());
    let mut fffc_orig: Vec<usize> = Vec::with_capacity(req.inlines.len());
    for (i, ch) in req.text.char_indices() {
        if ch == '\u{FFFC}' {
            box_clean_idx.push(clean.len());
            fffc_orig.push(i);
        } else {
            clean.push(ch);
        }
    }
    // Byte-index map original → cleaned (FFFC is 3 bytes in UTF-8).
    let adjust = |pos: usize| -> usize {
        let n = fffc_orig.iter().take_while(|&&p| p < pos).count();
        pos - 3 * n
    };

    let mut b = parley
        .layout_cx
        .ranged_builder(&mut parley.font_cx, &clean, 1.0, true);
    b.push_default(StyleProperty::FontSize(scaled));
    b.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(10.0 / 9.0)));
    b.push_default(StyleProperty::FontFamily(FontFamily::Single(
        FontFamilyName::Generic(GenericFamily::SansSerif),
    )));
    b.push_default(StyleProperty::Brush(SpanBrush(0)));
    for (i, sp) in req.spans.iter().enumerate() {
        let (s, e) = (adjust(sp.start), adjust(sp.end));
        if s >= e || e > clean.len() {
            continue;
        }
        b.push(StyleProperty::Brush(SpanBrush(i as u32)), s..e);
        if sp.bold {
            b.push(StyleProperty::FontWeight(FontWeight::BOLD), s..e);
        }
    }
    for (k, spec) in req.inlines.iter().enumerate() {
        if k >= box_clean_idx.len() {
            break;
        }
        b.push_inline_box(InlineBox {
            id: k as u64,
            kind: InlineBoxKind::InFlow,
            index: box_clean_idx[k],
            width: spec.width,
            // Width participates in wrapping and line breaking; the VERTICAL
            // growth (above/below the baseline per align mode) is applied by
            // the Java-side post-pass, so the box reports zero height here.
            height: 0.0,
        });
    }
    let mut layout = b.build(&clean);

    // Break: uniform fast path, or per-line widths under float clips (the
    // browser IFC loop: query the free interval at the line's y, hand it to
    // the breaker, repeat).
    if req.clips.is_empty() {
        layout.break_all_lines(Some(req.max_width));
    } else {
        let est_h = scaled * (10.0 / 9.0);
        let mut breaker = layout.break_lines();
        // floor 1.0: min-content probes pass max_width=0, and parley asserts
        // line_max ≤ layout_max (clip_query also floors at 1px).
        breaker
            .state_mut()
            .set_layout_max_advance(req.max_width.max(1.0));
        while !breaker.is_done() {
            let y = breaker.committed_y() as f32;
            // NOTE: no set_line_x here — under Justify, parley bakes line_x
            // into the glyph pens (the line fills [line_x, line_x+advance]),
            // which would double-apply the indent: collect_layout already
            // shifts every line by the clip's x0 once. The breaker only needs
            // the per-line WIDTH; the horizontal placement is applied at
            // collect time. The query uses the line TOP so a line that merely
            // straddles a clip's boundary is still clipped (CSS: any overlap
            // between the line box and the float's span narrows the line).
            let (_, w) = clip_query(y, est_h, req.max_width, req.clips);
            breaker.state_mut().set_line_max_advance(w);
            if breaker.break_next().is_none() {
                break;
            }
        }
        breaker.finish();
    }
    layout.align(
        if req.justify {
            Alignment::Justify
        } else {
            Alignment::Start
        },
        AlignmentOptions::default(),
    );

    let (glyphs, markers, max_x) = collect_layout(&layout, req.clips, req.max_width);
    ParleyShaped {
        glyphs,
        markers,
        content_height: layout.height(),
        max_x,
    }
}

/// Collect positioned glyphs + inline markers from a laid-out paragraph,
/// applying per-line clip x offsets. The left-float indent lives ONLY here:
/// the breaker sets just the per-line width — under Justify, parley bakes a
/// set_line_x origin into the glyph pens (line fills [line_x, line_x+adv]),
/// so setting it at break time would shift every left-clipped line twice.
pub fn collect_layout(
    layout: &Layout<SpanBrush>,
    clips: &[Clip],
    max_width: f32,
) -> (Vec<OutGlyph>, Vec<crate::measure::InlineMarker>, f32) {
    let mut glyphs = Vec::new();
    let mut markers: Vec<(u64, crate::measure::InlineMarker)> = Vec::new();
    let mut max_x = 0.0f32;
    for (li, line) in layout.lines().enumerate() {
        let m = line.metrics();
        let (x_off, _) = if clips.is_empty() {
            (0.0, max_width)
        } else {
            // Same query as the breaker: line top + full height (overlap
            // semantics), so collect and break agree on every line's lane.
            clip_query(
                m.block_min_coord,
                m.line_height,
                max_width,
                clips,
            )
        };
        for item in line.items() {
            match item {
                PositionedLayoutItem::GlyphRun(gr) => {
                    let span_index = gr.style().brush.0;
                    let fd = gr.run().font().clone();
                    let fs = gr.run().font_size();
                    for g in gr.positioned_glyphs() {
                        max_x = max_x.max(g.x + x_off + g.advance);
                        glyphs.push(OutGlyph {
                            glyph_id: g.id,
                            x: g.x + x_off,
                            y: g.y,
                            w: g.advance,
                            line_top: m.block_min_coord,
                            line_height: m.line_height,
                            line_index: li,
                            span_index,
                            font: fd.clone(),
                            font_size: fs,
                        });
                    }
                }
                PositionedLayoutItem::InlineBox(bx) => {
                    max_x = max_x.max(bx.x + x_off + bx.width);
                    markers.push((
                        bx.id,
                        crate::measure::InlineMarker {
                            pen_x: bx.x + x_off,
                            baseline_y: m.baseline,
                            line_top: m.block_min_coord,
                            line_height: m.line_height,
                            line_index: li,
                            advance: bx.width,
                        },
                    ));
                }
            }
        }
    }
    markers.sort_by_key(|(id, _)| *id);
    (
        glyphs,
        markers
            .into_iter()
            .map(|(_, m)| m)
            .collect(),
        max_x,
    )
}

/// The free horizontal interval for a line at [y, y+h): the node width minus
/// every clip intersecting that band.
fn clip_query(y: f32, h: f32, node_w: f32, clips: &[Clip]) -> (f32, f32) {
    let mut x0 = 0.0f32;
    let mut x1 = node_w;
    for c in clips {
        if c.y_bottom <= y || c.y_top >= y + h {
            continue;
        }
        if c.x <= 0.0 {
            x0 = x0.max(c.x + c.width);
        } else {
            x1 = x1.min(c.x);
        }
    }
    (x0, (x1 - x0).max(1.0))
}

/// One rasterized glyph quad with its atlas key (same wire role as
/// layout.rs's cosmic RasterizedGlyph, produced from parley shaping).
pub struct ParleyRasterGlyph {
    pub bitmap_key: u64,
    pub x: f32,
    pub y: f32,
    pub w: f32,
    pub h: f32,
    pub line_index: u32,
    pub span_index: u32,
}

/// Rasterize shaped glyphs: pen positions → swash bitmaps, deduplicated by a
/// stable content key (font bytes ptr, face index, glyph id, size). Pen
/// positions snap to the integer grid — the MC pixel grid wants integer
/// placement anyway (subpixel bins deferred as a calibration item).
pub fn rasterize_out_glyphs(
    glyphs: &[OutGlyph],
    render_scale: f32,
) -> (Vec<ParleyRasterGlyph>, Vec<(u64, u32, u32, Vec<u8>)>) {
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};

    let mut quads = Vec::new();
    let mut bitmaps: Vec<(u64, u32, u32, Vec<u8>)> = Vec::new();
    let mut placements: std::collections::HashMap<u64, (i32, i32, u32, u32)> = Default::default();
    let mut ctx = swash::scale::ScaleContext::new();

    for g in glyphs {
        let Some(font_ref) = swash::FontRef::from_index(g.font.data.data(), g.font.index as usize)
        else {
            continue;
        };
        let size = g.font_size * render_scale;
        let xi = (g.x * render_scale).trunc() as i32;
        let yi = (g.y * render_scale).trunc() as i32;

        let mut h = DefaultHasher::new();
        (g.font.data.data().as_ptr(), g.font.index, g.glyph_id, size.to_bits()).hash(&mut h);
        let key = h.finish();

        let placement = match placements.get(&key) {
            Some(p) => *p,
            None => {
                let mut scaler = ctx
                    .builder(font_ref)
                    .size(size)
                    .hint(true)
                    .build();
                let Some(img) = swash::scale::Render::new(&[
                    swash::scale::Source::ColorOutline(0),
                    swash::scale::Source::ColorBitmap(swash::scale::StrikeWith::BestFit),
                    swash::scale::Source::Outline,
                ])
                .format(swash::zeno::Format::Alpha)
                .render(&mut scaler, g.glyph_id as u16) else {
                    continue;
                };
                let (w, hh) = (img.placement.width as u32, img.placement.height as u32);
                if w == 0 || hh == 0 {
                    continue;
                }
                let p = (img.placement.left, img.placement.top, w, hh);
                bitmaps.push((key, w, hh, alpha_to_rgba(&img.data)));
                placements.insert(key, p);
                p
            }
        };
        let (left, top, w, hh) = placement;
        quads.push(ParleyRasterGlyph {
            bitmap_key: key,
            x: (xi + left) as f32 / render_scale,
            y: (yi - top) as f32 / render_scale,
            w: w as f32 / render_scale,
            h: hh as f32 / render_scale,
            line_index: g.line_index as u32,
            span_index: g.span_index,
        });
    }
    (quads, bitmaps)
}
