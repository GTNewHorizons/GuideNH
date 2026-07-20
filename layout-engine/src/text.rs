use cosmic_text::{
    Attrs, Buffer, Family, FontSystem, Metrics, Shaping, SwashCache, Wrap,
};

/// Wrapper around cosmic-text FontSystem + SwashCache.
/// Single persistent instance per LayoutBridge handle.
pub struct GuideFontSystem {
    pub font_system: FontSystem,
    pub swash_cache: SwashCache,
    /// Parley-side contexts (migration target; both engines coexist during
    /// the transition and share the same registered font data).
    pub parley: crate::parley_text::ParleyFonts,
}

impl GuideFontSystem {
    pub fn new() -> Self {
        Self {
            font_system: FontSystem::new(),
            swash_cache: SwashCache::new(),
            parley: crate::parley_text::ParleyFonts::new(),
        }
    }

    pub fn load_font_data(&mut self, data: Vec<u8>) {
        self.font_system.db_mut().load_font_data(data.clone());
        self.parley.load_font_data(data);
    }

    /// Shape text and return glyphs with relative coordinates (buffer-local),
    /// plus the shaped content height in document units.
    pub fn shape_text(
        &mut self,
        text: &str,
        font_size: f32,
        bold: bool,
        italic: bool,
        font_scale: f32,
        max_width: Option<f32>,
    ) -> ShapedText {
        let scaled_size = font_size * font_scale;
        // Line height mirrors the guide's legacy model: em 9px (MC FONT_HEIGHT)
        // with line height FONT_HEIGHT+1 = 10px, i.e. size × 10/9.
        let line_height = scaled_size * (10.0 / 9.0);
        let metrics = Metrics::new(scaled_size, line_height);

        let mut buffer = Buffer::new(&mut self.font_system, metrics);
        // The buffer is already at the scaled font size, so ALL shaping output
        // (advances, line metrics, wrap width) is in final document units —
        // no further scaling may be applied anywhere downstream (D-1).
        buffer.set_size(max_width, None);
        // WordOrGlyph: prefer word breaks, but hard-break long tokens (URLs,
        // long words) that would otherwise overflow the line width (阶段 A).
        buffer.set_wrap(Wrap::WordOrGlyph);

        let mut attrs = Attrs::new().family(Family::SansSerif);
        if bold {
            attrs = attrs.weight(cosmic_text::Weight::BOLD);
        }
        if italic {
            attrs = attrs.style(cosmic_text::Style::Italic);
        }
        buffer.set_text(text, &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut self.font_system, false);

        collect_shaped(&mut buffer)
    }

    /// Rich-text variant of [`Self::shape_text`]: spans are shaped together via
    /// [`Buffer::set_rich_text`] so wrapping and cross-boundary kerning work
    /// across spans. Each glyph's span attribution rides cosmic's
    /// `Attrs.metadata` (span index), which shaping forwards verbatim to every
    /// glyph it produces. Font size/scale stay buffer-wide (per-span fontScale
    /// is not supported by cosmic's line metrics).
    pub fn shape_rich_text(
        &mut self,
        spans: &[RichSpan],
        font_size: f32,
        font_scale: f32,
        max_width: Option<f32>,
    ) -> ShapedText {
        let scaled_size = font_size * font_scale;
        let line_height = scaled_size * (10.0 / 9.0);
        let metrics = Metrics::new(scaled_size, line_height);

        let mut buffer = Buffer::new(&mut self.font_system, metrics);
        buffer.set_size(max_width, None);
        buffer.set_wrap(Wrap::WordOrGlyph);

        let default_attrs = Attrs::new().family(Family::SansSerif);
        let rich: Vec<(&str, Attrs)> = spans
            .iter()
            .map(|s| {
                // NB: italic is NOT forwarded to attrs.style — cosmic would
                // rasterize real/fake-italic bitmaps, and Pass B already marks
                // italic groups with GlyphRun.shear for the engine's synthetic
                // slant (MC §o parity). Forwarding both would double-slant.
                let mut attrs = Attrs::new()
                    .family(Family::SansSerif)
                    .metadata(s.span_index as usize);
                if s.bold {
                    attrs = attrs.weight(cosmic_text::Weight::BOLD);
                }
                (s.text.as_str(), attrs)
            })
            .collect();
        buffer.set_rich_text(rich, &default_attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut self.font_system, false);

        collect_shaped(&mut buffer)
    }

    /// Get content width after shaping (for measure closure).
    pub fn measure_text_width(
        &mut self,
        text: &str,
        font_size: f32,
        bold: bool,
        italic: bool,
        max_width: Option<f32>,
    ) -> f32 {
        self
            .shape_text(text, font_size, bold, italic, 1.0, max_width)
            .glyphs
            .iter()
            .map(|g| g.x + g.w)
            .fold(0.0f32, |a, b| a.max(b))
    }
}

/// One styled run of a rich paragraph, as fed to [`Buffer::set_rich_text`].
/// `span_index` is the span's index in the source TextData.spans vector and is
/// passed through `Attrs.metadata` so every shaped glyph reports its span.
pub struct RichSpan {
    pub text: String,
    pub bold: bool,
    pub italic: bool,
    pub span_index: u32,
}

/// The outcome of shaping one text buffer: glyphs (buffer-local) + content height.
pub struct ShapedText {
    pub glyphs: Vec<ShapedGlyph>,
    /// Content height in document units (top of first line to bottom of last).
    pub content_height: f32,
    /// First-line baseline offset below the line top, in document units.
    pub ascent: f32,
}

/// Collect laid-out glyphs from a shaped buffer (shared by the single-style
/// and rich-text paths). Span attribution comes from `Attrs.metadata` — the
/// single-style path leaves it at 0.
fn collect_shaped(buffer: &mut Buffer) -> ShapedText {
    let mut glyphs: Vec<ShapedGlyph> = Vec::new();
    let mut content_height = 0.0f32;
    let mut ascent = 0.0f32;
    // NB: the enumeration index is the VISUAL (wrapped) line index — do not
    // use run.line_i, which is the SOURCE line index shared by all visual
    // lines a wrapped paragraph produces (the inline post-pass groups
    // kerning and line growth by this index).
    for (vline, run) in buffer.layout_runs().enumerate() {
        content_height = content_height.max(run.line_top + run.line_height);
        if vline == 0 {
            // First-line baseline offset below the line top — the single
            // baseline authority for the unified text pipeline (GuideText).
            ascent = run.line_y - run.line_top;
        }
        for glyph in run.glyphs {
            // x_offset/y_offset are in font_size-relative units; multiply by font_size
            let x_off = glyph.x_offset * glyph.font_size;
            let y_off = glyph.y_offset * glyph.font_size;
            let is_placeholder = run
                .text
                .get(glyph.start..glyph.end)
                .map(|s| s.contains('\u{FFFC}'))
                .unwrap_or(false);
            glyphs.push(ShapedGlyph {
                glyph_id: glyph.glyph_id as u32,
                x: glyph.x + x_off,
                // Placeholders are never rasterized; for them y carries the
                // line's true baseline (not the .notdef glyph's own y_offset)
                // so the inline post-pass anchors blocks to the same baseline
                // the surrounding text sits on.
                y: if is_placeholder {
                    run.line_y
                } else {
                    run.line_y - y_off
                },
                w: glyph.w,
                h: run.line_height,
                line_top: run.line_top,
                line_index: vline,
                start: glyph.start as u32,
                end: glyph.end as u32,
                inline_placeholder: is_placeholder,
                span_index: glyph.metadata as u32,
                font_id: glyph.font_id,
                font_weight: glyph.font_weight,
                cache_key_flags: glyph.cache_key_flags,
                font_size: glyph.font_size,
            });
        }
    }

    ShapedText {
        glyphs,
        content_height,
        ascent,
    }
}

/// A shaped glyph in buffer-local coordinates: (x, y) is the pen position on
/// the baseline; w is the advance. The font_* fields capture the shaping
/// outcome (font fallback, weight, flags, size) so a matching swash
/// [`cosmic_text::CacheKey`] can be rebuilt later for rasterization.
#[derive(Clone, Debug)]
pub struct ShapedGlyph {
    pub glyph_id: u32,
    pub x: f32,
    pub y: f32,
    pub w: f32,
    pub h: f32,
    /// Line top (buffer-local) of the glyph's line; used by the inline
    /// post-pass for center-on-line alignment.
    pub line_top: f32,
    /// Index of the glyph's layout line within the buffer. Glyphs on the same
    /// line share it even when per-glyph y_offsets make their pen Y differ —
    /// line identity, not baseline equality, groups kerning and line growth.
    pub line_index: usize,
    /// Byte range of this glyph's cluster in the source text (for band-split
    /// computations and span attribution).
    pub start: u32,
    pub end: u32,
    /// True when this glyph is a U+FFFC inline-block placeholder: it is not
    /// rasterized; the inline post-pass replaces its advance with the block's
    /// real width and anchors the block at this pen position.
    pub inline_placeholder: bool,
    /// Source span index (TextData.spans) this glyph belongs to; 0 for
    /// single-style paragraphs. Rides cosmic's Attrs.metadata through shaping.
    pub span_index: u32,
    pub font_id: cosmic_text::fontdb::ID,
    pub font_weight: cosmic_text::fontdb::Weight,
    pub cache_key_flags: cosmic_text::CacheKeyFlags,
    pub font_size: f32,
}
