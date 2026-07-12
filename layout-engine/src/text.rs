use cosmic_text::{
    Attrs, Buffer, Family, FontSystem, Metrics, Shaping, SwashCache, Wrap,
};

/// Wrapper around cosmic-text FontSystem + SwashCache.
/// Single persistent instance per LayoutBridge handle.
pub struct GuideFontSystem {
    pub font_system: FontSystem,
    pub swash_cache: SwashCache,
}

impl GuideFontSystem {
    pub fn new() -> Self {
        Self {
            font_system: FontSystem::new(),
            swash_cache: SwashCache::new(),
        }
    }

    pub fn load_font_data(&mut self, data: Vec<u8>) {
        self.font_system.db_mut().load_font_data(data);
    }

    /// Shape text and return glyphs with relative coordinates (buffer-local).
    pub fn shape_text(
        &mut self,
        text: &str,
        font_size: f32,
        bold: bool,
        italic: bool,
        font_scale: f32,
        max_width: Option<f32>,
    ) -> Vec<ShapedGlyph> {
        let scaled_size = font_size * font_scale;
        let metrics = Metrics::new(scaled_size, scaled_size * 1.4);

        let mut buffer = Buffer::new(&mut self.font_system, metrics);
        buffer.set_size(
            max_width.map(|w| w / font_scale),
            None,
        );
        buffer.set_wrap(Wrap::Word);

        let mut attrs = Attrs::new().family(Family::SansSerif);
        if bold {
            attrs = attrs.weight(cosmic_text::Weight::BOLD);
        }
        if italic {
            attrs = attrs.style(cosmic_text::Style::Italic);
        }
        buffer.set_text(text, &attrs, Shaping::Advanced, None);
        buffer.shape_until_scroll(&mut self.font_system, false);

        let mut glyphs: Vec<ShapedGlyph> = Vec::new();
        for run in buffer.layout_runs() {
            for glyph in run.glyphs {
                // x_offset/y_offset are in font_size-relative units; multiply by font_size
                let x_off = glyph.x_offset * glyph.font_size;
                let y_off = glyph.y_offset * glyph.font_size;
                glyphs.push(ShapedGlyph {
                    glyph_id: glyph.glyph_id as u32,
                    x: (glyph.x + x_off) * font_scale,
                    y: (run.line_y - y_off) * font_scale,
                    w: glyph.w * font_scale,
                    h: glyph.w * font_scale, // approximate; glyphs don't carry explicit height
                });
            }
        }

        glyphs
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
        let glyphs = self.shape_text(text, font_size, bold, italic, 1.0, max_width);
        glyphs
            .iter()
            .map(|g| g.x + g.w)
            .fold(0.0f32, f32::max)
    }
}

#[derive(Clone, Debug)]
pub struct ShapedGlyph {
    pub glyph_id: u32,
    pub x: f32,
    pub y: f32,
    pub w: f32,
    pub h: f32,
}
