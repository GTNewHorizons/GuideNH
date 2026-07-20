use cosmic_text::{FontSystem, SwashCache};

/// Convert a swash glyph image into tightly packed RGBA pixels (top-to-bottom rows).
/// Returns (width, height, rgba).
pub fn image_to_rgba(image: &cosmic_text::SwashImage) -> (u32, u32, Vec<u8>) {
    let w = image.placement.width;
    let h = image.placement.height;
    let mut rgba = vec![0u8; (w * h * 4) as usize];

    match &image.content {
        cosmic_text::SwashContent::Mask => {
            for (i, &alpha) in image.data.iter().enumerate() {
                rgba[i * 4] = 255;
                rgba[i * 4 + 1] = 255;
                rgba[i * 4 + 2] = 255;
                rgba[i * 4 + 3] = alpha;
            }
        }
        cosmic_text::SwashContent::Color => {
            // Swash BGRA → RGBA
            for i in 0..(image.data.len() / 4) {
                rgba[i * 4] = image.data[i * 4 + 2]; // R
                rgba[i * 4 + 1] = image.data[i * 4 + 1]; // G
                rgba[i * 4 + 2] = image.data[i * 4]; // B
                rgba[i * 4 + 3] = image.data[i * 4 + 3]; // A
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

    (w, h, rgba)
}

/// Rasterize a single glyph into RGBA pixel data.
/// pos_x/pos_y: document-space position (for subpixel rendering in CacheKey).
/// Returns (width, height, rgba, cache_x, cache_y, place_left, place_top).
/// cache_x/cache_y are the SubpixelBin integer positions — use these for placement.
pub fn rasterize_glyph(
    font_system: &mut FontSystem,
    swash_cache: &mut SwashCache,
    glyph_id: u32,
    font_size: f32,
    pos_x: f32,
    pos_y: f32,
) -> Option<(u32, u32, Vec<u8>, i32, i32, i32, i32)> {
    use cosmic_text::CacheKey;
    use cosmic_text::fontdb::Weight;

    let db = font_system.db();
    let font_id = db.faces().next()?.id;
    font_system.get_font(font_id, Weight::NORMAL)?;

    let (cache_key, cache_x, cache_y) = CacheKey::new(
        font_id,
        glyph_id as u16,
        font_size,
        (pos_x, pos_y),
        Weight::NORMAL,
        cosmic_text::CacheKeyFlags::empty(),
    );

    let image = swash_cache.get_image(font_system, cache_key);
    let image = image.as_ref()?;

    let place_left = image.placement.left;
    let place_top = image.placement.top;
    let (w, h, rgba) = image_to_rgba(image);

    Some((w, h, rgba, cache_x, cache_y, place_left, place_top))
}
