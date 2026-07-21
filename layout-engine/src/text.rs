/// Persistent font state per LayoutBridge handle. Only the parley contexts
/// remain; the legacy cosmic-text shaping/rasterization path was removed once
/// parley took over measureLayout and shapeText (P5).
pub struct GuideFontSystem {
    pub parley: crate::parley_text::ParleyFonts,
}

impl GuideFontSystem {
    pub fn new() -> Self {
        Self {
            parley: crate::parley_text::ParleyFonts::new(),
        }
    }

    pub fn load_font_data(&mut self, data: Vec<u8>) {
        self.parley.load_font_data(data);
    }
}
