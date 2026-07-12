use cosmic_text::{FontSystem, SwashCache};

/// Rasterize a single glyph into RGBA pixel data.
/// Returns (width, height, rgba_pixels).
/// Rasterize a single glyph into RGBA pixel data.
/// Returns (width, height, rgba_pixels).
pub fn rasterize_glyph(
    font_system: &mut FontSystem,
    swash_cache: &mut SwashCache,
    glyph_id: u32,
    font_size: f32,
) -> Option<(u32, u32, Vec<u8>)> {
    // Use cosmic-text's SwashCache to rasterize the glyph.
    use cosmic_text::CacheKey;
    use cosmic_text::fontdb::Weight;

    let db = font_system.db();
    let font_id = db.faces().next()?.id;

    // Get the font from cosmic-text's font system
    font_system.get_font(font_id, Weight::NORMAL)?;

    // Build a cache key
    let (cache_key, _, _) = CacheKey::new(
        font_id,
        glyph_id as u16,
        font_size,
        (0.0, 0.0),
        Weight::NORMAL,
        cosmic_text::CacheKeyFlags::empty(),
    );

    let image = swash_cache.get_image(font_system, cache_key);
    let image = image.as_ref()?;

    let w = image.placement.width as u32;
    let h = image.placement.height as u32;
    let mut rgba = vec![0u8; (w * h * 4) as usize];

    match image.content {
        cosmic_text::SwashContent::Mask => {
            for (i, &alpha) in image.data.iter().enumerate() {
                rgba[i * 4] = 255;
                rgba[i * 4 + 1] = 255;
                rgba[i * 4 + 2] = 255;
                rgba[i * 4 + 3] = alpha;
            }
        }
        cosmic_text::SwashContent::Color => {
            for i in 0..(image.data.len() / 4) {
                rgba[i * 4] = image.data[i * 4];
                rgba[i * 4 + 1] = image.data[i * 4 + 1];
                rgba[i * 4 + 2] = image.data[i * 4 + 2];
                rgba[i * 4 + 3] = image.data[i * 4 + 3];
            }
        }
        cosmic_text::SwashContent::SubpixelMask => {
            // Approximate: just use alpha
            for (i, &alpha) in image.data.iter().enumerate() {
                rgba[i * 4] = 255;
                rgba[i * 4 + 1] = 255;
                rgba[i * 4 + 2] = 255;
                rgba[i * 4 + 3] = alpha;
            }
        }
    }

    Some((w, h, rgba))
}
