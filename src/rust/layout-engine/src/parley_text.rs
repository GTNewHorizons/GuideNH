//! Parley text layout: the guide's line-box layer and the migration target for
//! the cosmic shaping path.
//!
//! Owns the parley contexts (fontique FontContext + LayoutContext) and the
//! paragraph layout → positioned-glyph pipeline. Parley's `BreakLines` gives
//! per-line geometry control (`BreakerState.set_line_max_advance` /
//! `set_line_x`), so float wrapping is done by feeding each line its
//! available width from the clip query: no pre-split bands, no byte rebase,
//! no segmented re-shaping. Inline blocks are first-class `InlineBox`es
//! (width participates in wrapping; vertical growth stays with the Java-side
//! post-pass). Rasterization stays on the swash bridge.

use std::sync::Arc;

use parley::fontique;
use parley::{
    Alignment, AlignmentOptions, FontContext, FontData, FontFamily, FontFamilyName, FontWeight,
    GenericFamily, InlineBox, InlineBoxKind, Layout, LayoutContext, LineHeight, OverflowWrap,
    PositionedLayoutItem, StyleProperty, TextWrapMode,
};

/// Custom brush: carries the source span index (TextData.spans) and the
/// italic marker through shaping so each emitted glyph run can be
/// tinted/decorated per span and the boundary correction can read
/// which runs are italic (§o).
///
/// The italic MARKER lives here, in the brush, and NOT in
/// `StyleProperty::FontStyle`. `FontStyle::Italic` would make fontique run
/// the CSS font-style algorithm with a true-italic request: on real systems
/// (system_fonts) the matcher picks a genuine italic face (Arial Italic) and
/// the Latin outlines get swapped (an observed visual regression). Shaping
/// therefore always runs with `FontStyle::Normal`, so every glyph comes from
/// the regular face; the marker survives for the boundary correction while shaping stays
/// pixel-identical to the upright case.
#[derive(Clone, Debug, Default, PartialEq)]
pub struct SpanBrush {
    /// Source span index (TextData.spans); 0 for single-style paragraphs.
    pub span_index: u32,
    /// Italic boundary-detection marker: this run is italic (§o).
    /// Carried in the brush so it never enters font matching.
    pub italic: bool,
    /// The font family this run requested, taken from TextData.style.font_family
    /// or from the span's TextStyle.font_family. None means "not declared", which
    /// falls back to the SansSerif generic exactly as behavior was before this
    /// field existed. The value rides the brush until collect_layout reads it back,
    /// so wire round-trip tests and later stages can observe it; a later stage
    /// keys its font cache on the family. The actual font match is injected
    /// through StyleProperty::FontFamily, and this field is only the readable
    /// carrier of the request.
    pub family: Option<String>,
}

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

    /// Register an additional font file (e.g. a symbol font like
    /// seguisym.ttf) and append its families to the Han fallback key so the
    /// fontique Hani-fallback hack (collection/query.rs `set_fallbacks`) can
    /// land Common-script symbols (ⓘ ✦ ➤ ⚠ ☢, U+24D8/2726/27A4/26A0/2622)
    /// that msyh.ttc lacks.
    ///
    /// Uses `append_fallbacks`, never `set_fallbacks`, so the system CJK
    /// fallback list for the key is preserved and the symbol families land
    /// AFTER it. Best-effort: empty data is a no-op.
    pub fn load_fallback_font_data(&mut self, data: Vec<u8>) {
        if data.is_empty() {
            return;
        }
        let key = fontique::FallbackKey::new(fontique::Script::from_bytes(*b"Hani"), None);
        let ids: Vec<fontique::FamilyId> = self
            .font_cx
            .collection
            .register_fonts(fontique::Blob::new(Arc::new(data)), None)
            .into_iter()
            .map(|(id, _)| id)
            .collect();
        if ids.is_empty() {
            return;
        }
        // Warm the system Hani fallback cache first so the appended symbol
        // families are ordered after the system CJK fallbacks.
        let _warm: Vec<_> = self.font_cx.collection.fallback_families(key).collect();
        for id in &ids {
            self.font_cx.collection.append_fallbacks(key, core::iter::once(*id));
        }
        // Register the symbol families as the leading generic Emoji
        // family. ⚠ (U+26A0) / ☢ (U+2622) are classified emoji/pictograph by
        // parley_data, so parley shapes them via `GenericFamily::Emoji`
        // (parley shape/mod.rs:580-591): the Hani fallback key above is
        // bypassed entirely and the system resolves Segoe UI Emoji (COLR
        // composite glyph), which collapses to a 22×10 fragment at 22px.
        // `append_generic_families` lands the ids in `ours`, iterated before
        // the system generic families (fontique collection/mod.rs:350-362),
        // so the emoji query hits seguisym (Outline path, consistent with the
        // monochrome pipeline) and stops.
        self.font_cx
            .collection
            .append_generic_families(fontique::GenericFamily::Emoji, ids.iter().copied());
    }
}

// renderTextParley

impl ParleyFonts {
    /// Lay out one single-style paragraph at `scaled_size` wrapped to
    /// `max_width` (None = unwrapped), with line height
    /// `scaled_size × line_height_rel`.
    ///
    /// `italic` is carried as a brush MARKER (see [`SpanBrush`]), NOT as
    /// `StyleProperty::FontStyle`. Forwarding `FontStyle::Italic` into
    /// shaping made fontique run the CSS font-style algorithm with a
    /// true-italic request: on real systems (system_fonts:true, Windows) the
    /// matcher resolves a genuine italic face for Latin text (Arial Italic),
    /// which swaps the glyph outlines, an observed visual regression.
    /// Shaping therefore always matches `FontStyle::Normal`, so glyphs and
    /// advances are byte-identical to the upright case; the italic marker
    /// rides the brush and is restored into the output by `collect_layout`
    /// (read as `OutGlyph.italic` by the boundary correction). The
    /// synthetic draw-time slant stays on the Java side (MC §o parity): it
    /// is NOT re-applied here, and since shaping always selects the upright
    /// face there is no double-slant.
    pub fn layout_styled(
        &mut self,
        text: &str,
        scaled_size: f32,
        line_height_rel: f32,
        bold: bool,
        italic: bool,
        max_width: Option<f32>,
        family: Option<&str>,
    ) -> Layout<SpanBrush> {
        let mut builder = self
            .layout_cx
            .ranged_builder(&mut self.font_cx, text, 1.0, true);
        builder.push_default(StyleProperty::FontSize(scaled_size));
        builder.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(
            line_height_rel,
        )));
        // A non-empty font_family becomes FontFamily::named(...); the system
        // fonts are already in the fontique collection, so the Named lookup hits
        // directly (Consolas, for example). An empty or absent family falls back
        // to the SansSerif generic, the hard-coded default from before this field
        // existed, byte for byte unchanged.
        builder.push_default(StyleProperty::FontFamily(resolve_family(family)));
        // Emergency-break unbreakable runs (no-space CJK strings /
        // overlong titles) at the line's advance limit instead of overflowing
        // the content box. Mirrors CSS `overflow-wrap: break-word`: only
        // lines with no fitting soft break point get intra-word breaks; normal
        // text keeps breaking at spaces (NOT BreakAll, so Latin words do not
        // fragment).
        builder.push_default(StyleProperty::OverflowWrap(OverflowWrap::BreakWord));
        if bold {
            builder.push_default(StyleProperty::FontWeight(FontWeight::BOLD));
        }
        // Italic fix: the italic marker rides the brush; FontStyle is
        // deliberately NOT pushed, so shaping always matches Normal (see
        // SpanBrush docs). span_index stays 0 for a single-style paragraph.
        builder.push_default(StyleProperty::Brush(SpanBrush {
            span_index: 0,
            italic,
            family: requested_family(family),
        }));
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
        self.layout_styled(text, font_size, line_height_rel, false, false, max_width, None)
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
    /// coverage as alpha: the guide text pipeline is monochrome, tinted at draw
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

// Paragraph shaping core

/// One registered document-level float, in absolute document coordinates.
/// The pusher owns the float table and hands it to paragraph shaping so the
/// line breaker can query the free interval per line in real time: there is
/// no precomputed clip table and no cross-boundary geometry (the "bridge" is
/// this in-process query). `right` mirrors CSS float side.
#[derive(Clone, Copy)]
pub struct FloatRect {
    pub x: f32,
    pub y: f32,
    pub w: f32,
    pub h: f32,
    pub right: bool,
}

/// One styled span: byte range into the ORIGINAL (unstripped) text + bold
/// flag. Index = position in TextData.spans; glyphs shaped from the range
/// report it back as their brush.
pub struct SpanStyle {
    pub start: usize,
    pub end: usize,
    pub bold: bool,
    /// Italic marker: this span is italic (§o). Fed into the span brush
    /// (never into `StyleProperty::FontStyle`) so it survives shaping
    /// without affecting font matching.
    pub italic: bool,
    pub baseline_shift: f32,
    /// This span's font family, taken from its TextStyle.font_family. None means
    /// "not declared", so the paragraph default is inherited and no ranged family
    /// is injected. When it is non-empty, push_spans injects a ranged
    /// `FontFamily::named(...)` style.
    pub family: Option<String>,
}

/// One inline block to place: byte index of its U+FFFC anchor in the
/// ORIGINAL text + its layout width (paragraph-relative units).
/// `float_side`: None = regular inline, Some(1) = float-left, Some(2) = float-right.
pub struct InlineSpec {
    pub anchor_byte: usize,
    pub width: f32,
    pub height: f32,
    pub float_side: Option<u8>,
    pub node: usize,
}

pub struct ShapeRequest<'a> {
    pub text: &'a str,
    /// Rich spans covering the whole text in document order (empty =
    /// single-style paragraph).
    pub spans: &'a [SpanStyle],
    /// Inline blocks in document order (paired with U+FFFC anchors).
    pub inlines: &'a [InlineSpec],
    /// Document-level floats registered so far (absolute coords); empty =
    /// uniform-width paragraph. Queried per line against the line's absolute y.
    pub floats: &'a [FloatRect],
    /// Absolute document y of this paragraph's top edge; line y for the float
    /// query is `para_abs_y + line_relative_y`.
    pub para_abs_y: f32,
    /// Absolute document x of this paragraph's content origin; float edges are
    /// converted to paragraph-relative by subtracting this.
    pub para_x: f32,
    /// In-paragraph clear breaks (raw UTF-8 byte offset into the original text
    /// + side 1=left 2=right 3=both), in document order. Mapped to cleaned-text
    /// offsets inside shape_paragraph.
    pub clears: &'a [(usize, u8)],
    pub font_size: f32,
    pub font_scale: f32,
    pub max_width: f32,
    pub justify: bool,
    /// Per-paragraph text alignment. 0=Start(Left) 1=Center 2=End(Right)
    pub alignment: i8,
    /// FlatBuffer TextData.white_space: 0=Normal 1=PreWrap 2=Pre/NoWrap
    /// (code blocks). Value 2 disables soft wrapping AND emergency
    /// (break-word) wrapping so a long unbreakable code line stays on one
    /// line and overflows horizontally instead of wrapping inside the
    /// container.
    pub white_space: i8,
    /// Paragraph default font family, taken from TextData.style.font_family. None
    /// means "not declared" and falls back to the SansSerif generic, the same
    /// hard-coded default push_defaults used before. A span-level family lives in
    /// [`SpanStyle::family`] and overrides this default over its own range when it
    /// is non-empty.
    pub family: Option<&'a str>,
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
    /// Italic boundary-detection marker: this glyph's run is italic (§o).
    /// Restored from the span brush by `collect_layout` (the brush carried
    /// it through shaping without letting it into font matching). Read by
    /// the boundary correction from the glyph-run output.
    pub italic: bool,
    /// The font family requested by the run this glyph belongs to, as declared on
    /// the wire by its span or paragraph and normalized through clean_family. None
    /// means "not declared" and falls back to the SansSerif generic. Restored from
    /// the span brush, so it survives with the glyph and lets the full-pipeline test
    /// assert that an inline_code span carries a monospace family while a body span
    /// falls back to SansSerif.
    pub family: Option<String>,
    pub font: FontData,
    pub font_size: f32,
}

pub struct ParleyShaped {
    pub glyphs: Vec<OutGlyph>,
    /// Inline-box anchors in document order (consumed by the post-pass).
    pub markers: Vec<crate::measure::InlineMarker>,
    pub content_height: f32,
    pub max_x: f32,
    /// Absolute document y that the flow AFTER this paragraph must not rise
    /// above, when the paragraph ends with a `<br clear>` whose drop no later
    /// line inside the paragraph can carry (the common, trailing form). The
    /// paragraph's own `content_height` is NOT inflated by it; the pusher
    /// advances its cursor to this floor after the paragraph so the following
    /// block (e.g. a callout) starts below the cleared float while this
    /// paragraph's box still hugs its text. `None` when no such clear applies.
    pub clear_floor: Option<f32>,
    /// (x_off, line_width) of the last line in the paragraph, in paragraph-
    /// relative coordinates. Used by the separator-line mechanism (kind=3
    /// DecorationRect) so LytHeading draws a themed separator across the
    /// float-compressed full line window.
    pub last_line_window: Option<(f32, f32)>,
    /// Float-aligned inline block anchors: (flat_node_index, paragraph-relative y).
    /// The x coordinate is computed later in inline_post_pass from the block's
    /// align mode and the paragraph's content width.
    pub float_anchors: Vec<(usize, f32)>,
}

/// Shape one paragraph: spans → ranged styles, inline blocks → InlineBox,
/// float clips → per-line widths via BreakerState, then collect positioned
/// glyphs and inline markers.
///
/// Float-aligned inline blocks trigger a two-pass shaping: pass 1 (full
/// width, all inlines as boxes) determines each float's anchor line; pass 2
/// shapes with paragraph-level floats constraining subsequent line widths.
/// Regular paragraphs (no float inlines) take the fast one-pass path.
pub fn shape_paragraph(parley: &mut ParleyFonts, req: &ShapeRequest) -> ParleyShaped {
    let scaled = req.font_size * req.font_scale;

    // ── Pre-processing: strip U+FFFC, build clean text ──
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
    let adjust = |pos: usize| -> usize {
        let n = fffc_orig.iter().take_while(|&&p| p < pos).count();
        pos - 3 * n
    };
    let clean_clears: Vec<(usize, u8)> =
        req.clears.iter().map(|(k, s)| (adjust(*k), *s)).collect();

    let has_float = req.inlines.iter().any(|s| s.float_side.is_some());

    // ── One-pass: fast path when no float inlines ──
    if !has_float {
        let mut b = parley.layout_cx.ranged_builder(&mut parley.font_cx, &clean, 1.0, true);
        push_defaults(&mut b, scaled, req.white_space, req.family);
        push_spans(&mut b, req.spans, &adjust, &clean);
        push_inlines(&mut b, req.inlines, &box_clean_idx, false);
        let mut layout = b.build(&clean);
        break_and_align(&mut layout, req);
        let (mut glyphs, markers, max_x, h, floor, last_window) = collect_layout(
            &layout, req.floats, req.para_abs_y, req.para_x, req.max_width, &clean_clears,
        );
        // Apply per-span baseline shift to glyph Y coordinates
        for g in &mut glyphs {
            let bs = req.spans.get(g.span_index as usize).map_or(0.0, |s| s.baseline_shift);
            g.y += bs * scaled;
        }
        return ParleyShaped {
            glyphs,
            markers,
            content_height: h,
            max_x,
            clear_floor: floor,
            last_line_window: last_window,
            float_anchors: Vec::new(),
        };
    }

    // ── Two-pass: paragraph-level float inlines ──

    // Pass 1: all boxes (including floats), full width. Float blocks
    // participate as inline boxes so their anchor line positions are correct.
    let layout1 = {
        let mut b = parley.layout_cx.ranged_builder(&mut parley.font_cx, &clean, 1.0, true);
        push_defaults(&mut b, scaled, req.white_space, req.family);
        push_spans(&mut b, req.spans, &adjust, &clean);
        push_inlines(&mut b, req.inlines, &box_clean_idx, false);
        let mut layout = b.build(&clean);
        break_and_align(&mut layout, req);
        layout
    };

    // Find float anchor line Y positions from pass 1 layout.
    let mut para_floats: Vec<FloatRect> = Vec::new();
    let mut float_anchor_ys: Vec<(usize, f32)> = Vec::new();
    for line in layout1.lines() {
        let tr = line.text_range();
        let y = line.metrics().block_min_coord;
        for (k, spec) in req.inlines.iter().enumerate() {
            if k >= box_clean_idx.len() {
                break;
            }
            if spec.float_side.is_none() {
                continue;
            }
            if tr.contains(&box_clean_idx[k]) {
                let abs_y = req.para_abs_y + y;
                let side = spec.float_side.unwrap_or(1);
                let fl = FloatRect {
                    x: if side == 1 {
                        req.para_x
                    } else {
                        (req.para_x + req.max_width - spec.width).max(req.para_x)
                    },
                    y: abs_y,
                    w: spec.width.max(1.0),
                    h: spec.height.max(scaled * 1.55),
                    right: side == 2,
                };
                para_floats.push(fl);
                float_anchor_ys.push((spec.node, y));
                break;
            }
        }
    }

    // Merge document floats + paragraph-local floats for pass 2.
    let mut merged_floats: Vec<FloatRect> = req.floats.to_vec();
    merged_floats.extend(para_floats.iter().cloned());

    // Pass 2: skip float inline boxes, shape with constrained widths.
    let layout2 = {
        let mut b = parley.layout_cx.ranged_builder(&mut parley.font_cx, &clean, 1.0, true);
        push_defaults(&mut b, scaled, req.white_space, req.family);
        push_spans(&mut b, req.spans, &adjust, &clean);
        push_inlines(&mut b, req.inlines, &box_clean_idx, true);
        let mut layout = b.build(&clean);
        if req.white_space == 2 {
            layout.break_all_lines(None);
        } else {
            break_with_floats(&mut layout, req, &merged_floats, scaled);
        }
        layout.align(
            resolve_alignment(req.justify, req.alignment),
            AlignmentOptions::default(),
        );
        layout
    };

    let (mut glyphs, markers, max_x, content_height, clear_floor, last_window) = collect_layout(
        &layout2, &merged_floats, req.para_abs_y, req.para_x, req.max_width, &clean_clears,
    );

    // Apply per-span baseline shift to glyph Y coordinates
    for g in &mut glyphs {
        let bs = req.spans.get(g.span_index as usize).map_or(0.0, |s| s.baseline_shift);
        g.y += bs * scaled;
    }

    ParleyShaped {
        glyphs,
        markers,
        content_height,
        max_x,
        clear_floor,
        last_line_window: last_window,
        float_anchors: float_anchor_ys,
    }
}

/// Single normalization point for family strings read off the wire: trim, then
/// drop empty values. The trim/empty filtering used to be spread over three call
/// sites (resolve_family, requested_family and push_spans) and is now collected
/// here. None means "not declared", which falls back to the SansSerif generic as
/// before.
fn clean_family(family: Option<&str>) -> Option<&str> {
    family.map(str::trim).filter(|f| !f.is_empty())
}

/// Single resolution point from a family name to a `FontFamily`, shared by
/// resolve_family and push_spans.
///
/// The reserved name `"monospace"` maps to `Generic(Monospace)`, because every
/// fontique backend resolves that generic to the platform's monospace default
/// (dwrite to Consolas, fontconfig through its "monospace" substitution chain,
/// coretext to its monospace default). That hit is more deterministic across
/// platforms than a hard-coded `Named("Consolas")`, which simply misses on a
/// system without Consolas and would be asymmetric with the SansSerif generic
/// fallback used elsewhere in this module.
/// Any other non-empty name maps to `FontFamily::named(...)`, which is
/// `Single(Named(name))`; system fonts are already in the fontique collection,
/// so the Named lookup hits.
/// An empty, all-whitespace or absent name falls back to the SansSerif generic,
/// the hard-coded push_defaults default, byte for byte unchanged.
fn resolve_family(family: Option<&str>) -> FontFamily<'_> {
    match clean_family(family) {
        Some("monospace") => FontFamily::Single(FontFamilyName::Generic(GenericFamily::Monospace)),
        Some(name) => FontFamily::named(name),
        None => FontFamily::Single(FontFamilyName::Generic(GenericFamily::SansSerif)),
    }
}

/// Brush-readable carrier of the requested font family. The value is normalized
/// through clean_family, and an empty, all-whitespace or absent one stays None,
/// which means the SansSerif generic fallback and no injected named family.
fn requested_family(family: Option<&str>) -> Option<String> {
    clean_family(family).map(str::to_owned)
}

fn push_defaults(
    b: &mut parley::RangedBuilder<SpanBrush>,
    scaled: f32,
    white_space: i8,
    family: Option<&str>,
) {
    b.push_default(StyleProperty::FontSize(scaled));
    b.push_default(StyleProperty::LineHeight(LineHeight::FontSizeRelative(1.55)));
    b.push_default(StyleProperty::FontFamily(resolve_family(family)));
    if white_space == 2 {
        // white_space=2 (Pre/NoWrap, code blocks). NoWrap disables soft
        // wrapping and OverflowWrap::Normal disables the emergency break-word
        // pass, so a long unbreakable code line keeps its natural single line
        // and overflows horizontally (the narrow container scrolls) instead of
        // being chopped at the container's advance limit.
        b.push_default(StyleProperty::TextWrapMode(TextWrapMode::NoWrap));
        b.push_default(StyleProperty::OverflowWrap(OverflowWrap::Normal));
    } else {
        // Emergency-break unbreakable runs (no-space CJK strings /
        // overlong titles) at the line's advance limit instead of overflowing
        // the content box. Mirrors CSS `overflow-wrap: break-word`: only
        // lines with no fitting soft break point get intra-word breaks; normal
        // text keeps breaking at spaces (NOT BreakAll, so Latin words do not
        // fragment).
        b.push_default(StyleProperty::OverflowWrap(OverflowWrap::BreakWord));
    }
    b.push_default(StyleProperty::Brush(SpanBrush {
        span_index: 0,
        italic: false,
        family: requested_family(family),
    }));
}

fn push_spans(
    b: &mut parley::RangedBuilder<SpanBrush>,
    spans: &[SpanStyle],
    adjust: &dyn Fn(usize) -> usize,
    text: &str,
) {
    for (i, sp) in spans.iter().enumerate() {
        let (s, e) = (adjust(sp.start), adjust(sp.end));
        if s >= e || e > text.len() {
            continue;
        }
        // A span-level font_family is injected over the span's range and resolved
        // through the single resolve_family point, so "monospace" becomes
        // Generic(Monospace); the brush carries the normalized original name as
        // well, so it can be read back.
        let span_family = clean_family(sp.family.as_deref());
        b.push(
            StyleProperty::Brush(SpanBrush {
                span_index: i as u32,
                // The span's italic flag rides the brush
                // (NOT StyleProperty::FontStyle) so shaping keeps matching
                // Normal; see the SpanBrush docs.
                italic: sp.italic,
                family: span_family.map(str::to_owned),
            }),
            s..e,
        );
        if span_family.is_some() {
            b.push(StyleProperty::FontFamily(resolve_family(span_family)), s..e);
        }
        if sp.bold {
            b.push(StyleProperty::FontWeight(FontWeight::BOLD), s..e);
        }
    }
}

fn push_inlines(
    b: &mut parley::RangedBuilder<SpanBrush>,
    inlines: &[InlineSpec],
    clean_idx: &[usize],
    skip_floats: bool,
) {
    for (k, spec) in inlines.iter().enumerate() {
        if k >= clean_idx.len() {
            break;
        }
        if skip_floats && spec.float_side.is_some() {
            continue;
        }
        b.push_inline_box(InlineBox {
            id: k as u64,
            kind: InlineBoxKind::InFlow,
            index: clean_idx[k],
            width: spec.width,
            height: 0.0,
        });
    }
}

fn break_and_align(layout: &mut Layout<SpanBrush>, req: &ShapeRequest) {
    if req.white_space == 2 {
        // white_space=2 (Pre/NoWrap). Break at hard breaks only:
        // break_all_lines(None) means max advance f32::MAX and NoWrap ignores
        // the advance anyway, so every code line stays on one line.
        layout.break_all_lines(None);
    } else if req.floats.is_empty() {
        layout.break_all_lines(Some(req.max_width));
    } else {
        let est_h = req.font_size * req.font_scale * 1.55;
        break_with_floats(layout, req, req.floats, est_h);
    }
    layout.align(
        resolve_alignment(req.justify, req.alignment),
        AlignmentOptions::default(),
    );
}

/// Resolve text alignment from justify flag and per-paragraph alignment.
/// Paragraph-level alignment (1=Center, 2=End) takes precedence over justify.
/// Justify is applied only when alignment is default (0=Start/Left).
/// 0=Start(Left) 1=Center 2=End(Right)
fn resolve_alignment(justify: bool, alignment: i8) -> Alignment {
    match alignment {
        1 => Alignment::Center,
        2 => Alignment::End,
        _ if justify => Alignment::Justify,
        _ => Alignment::Start,
    }
}

fn break_with_floats(
    layout: &mut Layout<SpanBrush>,
    req: &ShapeRequest,
    floats: &[FloatRect],
    est_h: f32,
) {
    let mut breaker = layout.break_lines();
    breaker.state_mut().set_layout_max_advance(req.max_width.max(1.0));
    while !breaker.is_done() {
        let rel_y = breaker.committed_y() as f32;
        let (_, w) =
            query_floats(req.para_abs_y + rel_y, est_h, req.para_x, req.max_width, floats);
        breaker.state_mut().set_line_max_advance(w);
        if breaker.break_next().is_none() {
            break;
        }
    }
    breaker.finish();
}

/// Collect positioned glyphs + inline markers from a laid-out paragraph,
/// applying per-line float x offsets and in-paragraph clear breaks. The
/// left-float indent lives ONLY here: the breaker sets just the per-line width
/// because under Justify, parley bakes a set_line_x origin into the glyph pens (line
/// fills [line_x, line_x+adv]), so setting it at break time would shift every
/// left-clipped line twice.
///
/// `clears` are in-paragraph `<br clear>` breaks (cleaned-text byte offset +
/// side), in document order. After the line whose text range covers a break's
/// offset is laid out, every following line is dropped to the cleared floats'
/// bottom edge (`clear_floor`). This is exact for the only real-world form, a
/// clear at the paragraph's end (the break has no trailing text, so no line is
/// re-wrapped by the drop); a mid-paragraph clear drops later lines without
/// re-wrapping them (first-order: never mispositions into a float, only the
/// wrap of those later lines is approximate, and no real page uses that form).
/// Returns the paragraph content height including any clear-induced growth.
pub fn collect_layout(
    layout: &Layout<SpanBrush>,
    floats: &[FloatRect],
    para_abs_y: f32,
    para_x: f32,
    max_width: f32,
    clears: &[(usize, u8)],
    ) -> (Vec<OutGlyph>, Vec<crate::measure::InlineMarker>, f32, f32, Option<f32>, Option<(f32, f32)>) {
    let mut glyphs = Vec::new();
    let mut markers: Vec<(u64, crate::measure::InlineMarker)> = Vec::new();
    let mut max_x = 0.0f32;
    let mut clear_floor: Option<f32> = None; // absolute y later lines must not rise above
    let mut next_clear = 0usize;
    let mut content_h = 0.0f32;
    let mut last_window: Option<(f32, f32)> = None;
    for (li, line) in layout.lines().enumerate() {
        let m = line.metrics();
        let tr = line.text_range();
        let orig_top_abs = para_abs_y + m.block_min_coord;
        let shift = clear_floor.map_or(0.0, |f| (f - orig_top_abs).max(0.0));
        let eff_top_abs = orig_top_abs + shift;
        let (x_off, line_width) = if floats.is_empty() {
            (0.0, max_width)
        } else {
            // Same query as the breaker, but at the (possibly clear-dropped)
            // effective line top: a line pushed below the floats sees the full
            // width, so its x-offset is 0 regardless of the exact dropped y.
            query_floats(eff_top_abs, m.line_height, para_x, max_width, floats)
        };
        for item in line.items() {
            match item {
                PositionedLayoutItem::GlyphRun(gr) => {
                    // Read-back point: the italic marker is
                    // restored from the brush (carried through shaping without
                    // affecting font matching; shaping always ran Normal).
                    let span_index = gr.style().brush.span_index;
                    let italic = gr.style().brush.italic;
                    let family = gr.style().brush.family.clone();
                    let fd = gr.run().font().clone();
                    let fs = gr.run().font_size();
                    for g in gr.positioned_glyphs() {
                        max_x = max_x.max(g.x + x_off + g.advance);
                        glyphs.push(OutGlyph {
                            glyph_id: g.id,
                            x: g.x + x_off,
                            y: g.y + shift,
                            w: g.advance,
                            line_top: m.block_min_coord + shift,
                            line_height: m.line_height,
                            line_index: li,
                            span_index,
                            italic,
                            family: family.clone(),
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
                            baseline_y: m.baseline + shift,
                            line_top: m.block_min_coord + shift,
                            line_height: m.line_height,
                            line_index: li,
                            advance: bx.width,
                        },
                    ));
                }
            }
        }
        content_h = content_h.max(eff_top_abs + m.line_height - para_abs_y);
        last_window = Some((x_off, line_width));
        // A clear takes effect after the line that covers its offset: the break
        // sits at/after that line's text, so the line itself is not dropped but
        // everything following it is.
        while next_clear < clears.len() && clears[next_clear].0 <= tr.end {
            let side = clears[next_clear].1;
            let mut floor = 0.0f32;
            for f in floats {
                let side_match =
                    side == 3 || (side == 1 && !f.right) || (side == 2 && f.right);
                if side_match {
                    floor = floor.max(f.y + f.h);
                }
            }
            if floor > 0.0 {
                clear_floor = Some(clear_floor.map_or(floor, |c| c.max(floor)));
            }
            next_clear += 1;
        }
    }
    // A trailing clear (break after the last line's text, the only real-world
    // form) has no following line inside this paragraph to carry its drop, so
    // its floor is NOT added to this paragraph's height (that would stretch the
    // box and leave a blank gap, the "callout not hugging" bug). Instead the
    // floor is returned for the pusher to advance its cursor past it. A mid-
    // paragraph clear needs no such hand-off: its dropped later lines already
    // raised content_h via eff_top_abs, so the natural cursor advance already
    // clears the floor.
    markers.sort_by_key(|(id, _)| *id);
    (
        glyphs,
        markers
            .into_iter()
            .map(|(_, m)| m)
            .collect(),
        max_x,
        content_h,
        clear_floor,
        last_window,
    )
}

/// The free horizontal interval (paragraph-relative) for a line whose absolute
/// top is `abs_y` and height `h`: the node width minus every float intersecting
/// that absolute band, with float edges converted to paragraph-relative by
/// `para_x`. Left floats push the left edge right; right floats pull the right
/// edge left.
fn query_floats(
    abs_y: f32,
    h: f32,
    para_x: f32,
    node_w: f32,
    floats: &[FloatRect],
) -> (f32, f32) {
    let mut x0 = 0.0f32;
    let mut x1 = node_w;
    for f in floats {
        if f.y + f.h <= abs_y || f.y >= abs_y + h {
            continue;
        }
        if f.right {
            x1 = x1.min(f.x - para_x);
        } else {
            x0 = x0.max(f.x + f.w - para_x);
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
    /// Baseline read-back: baseline Y of the line this glyph belongs
    /// to, in paragraph-relative coordinates, already reduced to the display pixel
    /// grid by division by render_scale and truncation, the same precision
    /// convention as x/y/w/h. The caller adds the node's absolute y to obtain
    /// document coordinates before writing PlacedGlyph.baseline.
    pub baseline: f32,
}

/// Glyph rasterization size ceiling: the bleed-stop (defense-in-depth).
///
/// Diagnosis (numerically closed): the live 23s/441-WARN flood comes from
/// glyphs rasterized at 1089-1452 ppem (bitmaps 966×1086 / 1402×1517),
/// traced back to `size = g.font_size × render_scale` below. The normal
/// live path tops out at 11 × 2.5(MAX_ZOOM) × 4 = 110 ppem; the
/// fontScale≈33 injection point that feeds this multiplier is NOT yet
/// located; every static path is value-bounded, so the oversized size must
/// arrive via an unidentified live path.
///
/// THIS CLAMP IS THE TOURNIQUET, NOT THE FIX. The primary repair is
/// locating and fixing the injection point (the diagnostic log emitted on
/// the first clamp per key is the tracing payload for that hunt). This
/// ceiling only stops the symptom: oversized glyphs rasterize at 128 ppem
/// and, because the atlas key below is derived from the CLAMPED size,
/// all oversize injections collapse onto one shared 128ppem atlas entry
/// instead of flooding the glyph atlas with giant bitmaps.
///
/// Normal glyphs (≤110 ppem, i.e. the entire legitimate range) take the
/// identical code path: `size` is untouched and the key is identical, so
/// rasterization output is byte-for-byte unchanged.
const MAX_RASTER_SIZE: f32 = 128.0;

/// Fallback rasterization size for pathological font sizes: the
/// root-cause repair (runtime-closed): ALL
/// 186 giant glyphs arrived with fontScale=0.0. Java passes ShapeTextInput
/// fontScale=0 → `scaled = font_size × font_scale = 0` → parley shapes at
/// size 0 → `OutGlyph.font_size = 0` (or NaN on some paths). Then
/// `raw_size = font_size × render_scale` is 0/NaN and the `> MAX_RASTER_SIZE`
/// clamp above is vacuous (0>128=false; NaN>128=false), so swash renders at
/// size 0/NaN and emits the glyph's raw outline coordinates as a giant
/// bitmap (1371×1516 etc).
///
/// This fallback is NOT a clamp: it replaces an invalid size with a
/// legitimate one. 22ppem = 11pt × 2, the standard body size at the
/// default 2× render scale, so these glyphs render at normal size instead
/// of raw outline coordinates. Because the atlas key is derived from the
/// final size, all fallback glyphs share one 22ppem atlas entry.
const DEFAULT_RASTER_SIZE: f32 = 22.0;

/// Atlas keys whose oversize clamp has already been reported. Process-wide
/// so a flood spanning many layout calls still logs exactly once per key:
/// the diagnostic must not itself become a log flood. A poisoned lock (a
/// panic elsewhere in this thread) is skipped silently: reporting is
/// best-effort and must never alter rasterization.
static CLAMP_REPORTED: std::sync::LazyLock<std::sync::Mutex<std::collections::HashSet<u64>>> =
    std::sync::LazyLock::new(|| std::sync::Mutex::new(std::collections::HashSet::new()));

/// Rasterize shaped glyphs: pen positions → swash bitmaps, deduplicated by a
/// stable content key (font bytes ptr, face index, glyph id, size). Pen
/// positions snap to the integer grid; the MC pixel grid wants integer
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
        let raw_size = g.font_size * render_scale;
        // Bleed-stop: clamp the rasterization size before it reaches the
        // swash scaler. Clamped glyphs share one key (derived below from the
        // clamped size), so the atlas cannot be flooded by giant bitmaps.
        let clamped = raw_size > MAX_RASTER_SIZE;
        // Root-cause repair: fontScale=0 injection makes `font_size` 0 (or NaN),
        // so raw_size is 0/NaN and the >MAX clamp above is vacuous
        // (0>128=false; NaN>128=false), so swash would render at size 0/NaN and
        // emit the glyph's raw outline coordinates as a giant bitmap. Fall
        // back to DEFAULT_RASTER_SIZE instead. `clamped` keeps its exact
        // meaning (`raw_size > MAX_RASTER_SIZE`, only ever true for finite
        // positive raw_size); the two conditions are mutually exclusive.
        let fallback = !raw_size.is_finite() || raw_size <= 0.0;
        let size = if fallback {
            DEFAULT_RASTER_SIZE
        } else {
            raw_size.min(MAX_RASTER_SIZE)
        };
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
                // Bleed-stop diagnostic: first oversize clamp per key logs
                // the injection shape (font_size, render_scale, raw size,
                // bitmap w×h) once, process-wide, enough to pinpoint the
                // live injection point on the next reproduction without the
                // diagnostic itself flooding the log.
                if clamped {
                    if let Ok(mut reported) = CLAMP_REPORTED.lock() {
                        if reported.insert(key) {
                            eprintln!(
                                "[guide_layout_engine][WARN] glyph rasterize size clamped \
                                 (F4 bleed-stop): font_size={} render_scale={} raw_size={} \
                                 bitmap={}x{} clamped_to={}ppem glyph_id={}",
                                g.font_size,
                                render_scale,
                                raw_size,
                                w,
                                hh,
                                MAX_RASTER_SIZE,
                                g.glyph_id
                            );
                        }
                    }
                }
                let p = (img.placement.left, img.placement.top, w, hh);
                bitmaps.push((key, w, hh, alpha_to_rgba(&img.data)));
                placements.insert(key, p);
                p
            }
        };
        let (left, top, w, hh) = placement;
        // The baseline uses the same precision convention as x/y
        // (divide by render_scale, then truncate), reusing the yi computed above
        // as (g.y * render_scale).trunc(). The value is in paragraph-relative
        // coordinates; the caller adds the node's absolute y for document
        // coordinates.
        quads.push(ParleyRasterGlyph {
            bitmap_key: key,
            x: (xi + left) as f32 / render_scale,
            y: (yi - top) as f32 / render_scale,
            w: w as f32 / render_scale,
            h: hh as f32 / render_scale,
            line_index: g.line_index as u32,
            span_index: g.span_index,
            baseline: yi as f32 / render_scale,
        });
    }
    (quads, bitmaps)
}
