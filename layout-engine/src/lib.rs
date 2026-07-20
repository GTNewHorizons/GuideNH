pub mod guidenh_layout_generated;
// Re-export generated types for convenience
pub use guidenh_layout_generated::com::hfstudio::guidenh::guide::layout::flatbuffers as fb;
pub mod jni_bridge;
pub mod layout;
pub mod measure;
pub mod raster;
pub mod style_convert;
pub mod text;

use std::panic::{catch_unwind, AssertUnwindSafe};

use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jlong};

use crate::jni_bridge::{jbytearray_to_vec, vec_to_jbytearray};
use crate::layout::compute_layout;
use crate::raster::rasterize_glyph;
use crate::text::GuideFontSystem;

/// Java: static native long init(byte[] fontTtfData, String locale);
#[no_mangle]
pub extern "system" fn Java_com_hfstudio_guidenh_guide_layout_LayoutBridge_init(
    mut env: JNIEnv,
    _class: JClass,
    font_data: JByteArray,
    locale: JString,
) -> jlong {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let font_bytes = jbytearray_to_vec(&mut env, &font_data).unwrap_or_default();
        let _locale_str: String = env
            .get_string(&locale)
            .map(|s| s.into())
            .unwrap_or_default();

        let mut font_system = GuideFontSystem::new();
        if !font_bytes.is_empty() {
            font_system.load_font_data(font_bytes);
        }

        let ptr = Box::into_raw(Box::new(font_system));
        ptr as jlong
    }));

    match result {
        Ok(h) => h,
        Err(_) => 0,
    }
}

/// Java: static native byte[] measureLayout(long handle, byte[] input);
#[no_mangle]
pub extern "system" fn Java_com_hfstudio_guidenh_guide_layout_LayoutBridge_measureLayout(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    input: JByteArray,
) -> jbyteArray {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if handle == 0 {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }
        let input_bytes = jbytearray_to_vec(&mut env, &input).unwrap_or_default();
        if input_bytes.is_empty() {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }

        let font_system = unsafe { &mut *(handle as *mut GuideFontSystem) };
        let output = compute_layout(&input_bytes, font_system);

        vec_to_jbytearray(&mut env, &output).unwrap_or(std::ptr::null_mut())
    }));

    match result {
        Ok(arr) => arr,
        Err(_) => env
            .new_byte_array(0)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
    }
}

/// Java: static native byte[] shapeText(long handle, byte[] input);
/// Unified-text-pipeline entry: shape + rasterize one styled text into
/// atlas-keyed quads + metrics (ShapeTextResult FlatBuffer).
#[no_mangle]
pub extern "system" fn Java_com_hfstudio_guidenh_guide_layout_LayoutBridge_shapeText(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    input: JByteArray,
) -> jbyteArray {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if handle == 0 {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }
        let input_bytes = jbytearray_to_vec(&mut env, &input).unwrap_or_default();
        if input_bytes.is_empty() {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }

        let font_system = unsafe { &mut *(handle as *mut GuideFontSystem) };
        let output = layout::shape_text_cmd(font_system, &input_bytes);

        vec_to_jbytearray(&mut env, &output).unwrap_or(std::ptr::null_mut())
    }));

    match result {
        Ok(arr) => arr,
        Err(_) => env
            .new_byte_array(0)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
    }
}

/// Java: static native byte[] rasterizeGlyphs(long handle, byte[] input);
#[no_mangle]
pub extern "system" fn Java_com_hfstudio_guidenh_guide_layout_LayoutBridge_rasterizeGlyphs(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    input: JByteArray,
) -> jbyteArray {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if handle == 0 {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }
        let input_bytes = jbytearray_to_vec(&mut env, &input).unwrap_or_default();
        if input_bytes.is_empty() {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }

        let r_input = match flatbuffers::root::<crate::fb::RasterInput>(&input_bytes)
        {
            Ok(r) => r,
            Err(_) => {
                return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
            }
        };

        let font_system = unsafe { &mut *(handle as *mut GuideFontSystem) };
        let swash_cache = &mut font_system.swash_cache;

        let mut fbb = flatbuffers::FlatBufferBuilder::with_capacity(4096);
        let mut glyph_offsets: Vec<flatbuffers::WIPOffset<crate::fb::GlyphPixels>> = Vec::new();

        let mut dbg_count: u32 = 0;
        for i in 0..r_input.requests().map_or(0, |r| r.len()) {
            let req = r_input.requests().unwrap().get(i);
            let font_id = req.font_id();
            let font_size = req.font_size();
            let glyph_ids = req.glyph_ids();
            let pos_x = req.pos_x();
            let pos_y = req.pos_y();

            for j in 0..glyph_ids.map_or(0, |g| g.len()) {
                let glyph_id = glyph_ids.unwrap().get(j);
                let px = pos_x.and_then(|v| if (j as usize) < v.len() { Some(v.get(j as usize)) } else { None }).unwrap_or(0.0);
                let py = pos_y.and_then(|v| if (j as usize) < v.len() { Some(v.get(j as usize)) } else { None }).unwrap_or(0.0);

                if let Some((w, h, rgba, cache_x, cache_y, place_left, place_top)) = rasterize_glyph(
                    &mut font_system.font_system,
                    swash_cache,
                    glyph_id,
                    font_size,
                    px,
                    py,
                ) {
                    if dbg_count < 3 {
                        let non_zero: usize = rgba.iter().filter(|&&b| b != 0).count();
                        eprintln!(
                            "RAST[{}] glyph_id={} pos=({:.1},{:.1}) img={}x{} nonZero={}/{} cache=({},{}) place=({},{})",
                            dbg_count, glyph_id, px, py, w, h,
                            non_zero, rgba.len(), cache_x, cache_y, place_left, place_top
                        );
                        dbg_count += 1;
                    }
                    // Pack: [cacheX, cacheY, placeLeft, placeTop, rgba] — 16-byte header
                    let mut packed = Vec::with_capacity(16 + rgba.len());
                    packed.extend_from_slice(&cache_x.to_le_bytes());
                    packed.extend_from_slice(&cache_y.to_le_bytes());
                    packed.extend_from_slice(&place_left.to_le_bytes());
                    packed.extend_from_slice(&place_top.to_le_bytes());
                    packed.extend_from_slice(&rgba);
                    let packed_vec = fbb.create_vector(&packed);
                    glyph_offsets.push(
                        crate::fb::GlyphPixels::create(
                            &mut fbb,
                            &crate::fb::GlyphPixelsArgs {
                                font_id,
                                font_size,
                                glyph_id,
                                width: w,
                                height: h,
                                rgba: Some(packed_vec),
                            },
                        ),
                    );
                }
            }
        }

        let glyphs_vec = fbb.create_vector(&glyph_offsets);
        let result = crate::fb::RasterResult::create(
            &mut fbb,
            &crate::fb::RasterResultArgs {
                glyphs: Some(glyphs_vec),
            },
        );
        fbb.finish(result, None);

        vec_to_jbytearray(&mut env, fbb.finished_data()).unwrap_or(std::ptr::null_mut())
    }));

    match result {
        Ok(arr) => arr,
        Err(_) => env
            .new_byte_array(0)
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut()),
    }
}

/// Java: static native byte[] renderText(long handle, String text, float fontSize, float availWidth);
/// Shapes + rasterizes in one call. Returns RenderResult FlatBuffer with per-glyph bitmaps
/// at SubpixelBin-corrected pixel positions. Java just blits.
#[no_mangle]
pub extern "system" fn Java_com_hfstudio_guidenh_guide_layout_LayoutBridge_renderText(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    text: JString,
    font_size: jni::sys::jfloat,
    avail_width: jni::sys::jfloat,
) -> jbyteArray {
    let result = catch_unwind(AssertUnwindSafe(|| {
        if handle == 0 {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }
        let text_str: String = env.get_string(&text).map(|s| s.into()).unwrap_or_default();
        if text_str.is_empty() {
            return vec_to_jbytearray(&mut env, &[]).unwrap_or(std::ptr::null_mut());
        }

        let font_system = unsafe { &mut *(handle as *mut GuideFontSystem) };
        let swash_cache = &mut font_system.swash_cache;

        // ── Shape text ──
        let mut buffer = cosmic_text::Buffer::new(
            &mut font_system.font_system,
            cosmic_text::Metrics::new(font_size, font_size * 1.5),
        );
        buffer.set_size(Some(avail_width), None);
        buffer.set_wrap(cosmic_text::Wrap::Word);
        buffer.set_text(
            &text_str,
            &cosmic_text::Attrs::new().family(cosmic_text::Family::SansSerif),
            cosmic_text::Shaping::Advanced,
            None,
        );
        buffer.shape_until_scroll(&mut font_system.font_system, false);

        let mut fbb = flatbuffers::FlatBufferBuilder::with_capacity(16384);

        let mut glyph_offsets: Vec<flatbuffers::WIPOffset<crate::fb::RenderGlyph>> = Vec::new();
        let mut total_w: f32 = 0.0;
        let mut total_h: f32 = 0.0;

        let white = cosmic_text::Color::rgb(255, 255, 255);

        for run in buffer.layout_runs() {
            for glyph in run.glyphs {
                let pg = glyph.physical((0., run.line_y), 1.0);
                let img = swash_cache.get_image(&mut font_system.font_system, pg.cache_key);
                let img = match img.as_ref() {
                    Some(i) => i,
                    None => continue,
                };

                let w = img.placement.width as u32;
                let h = img.placement.height as u32;
                if w == 0 || h == 0 { continue; }

                let place_left = img.placement.left;
                let place_top = img.placement.top;
                let gx = pg.x + place_left;
                let gy = pg.y - place_top;

                let mut rgba = vec![0u8; (w * h * 4) as usize];
                match &img.content {
                    cosmic_text::SwashContent::Mask => {
                        for (i, &alpha) in img.data.iter().enumerate() {
                            rgba[i * 4] = 255;
                            rgba[i * 4 + 1] = 255;
                            rgba[i * 4 + 2] = 255;
                            rgba[i * 4 + 3] = alpha;
                        }
                    }
                    cosmic_text::SwashContent::Color => {
                        for i in 0..(img.data.len() / 4) {
                            rgba[i * 4] = img.data[i * 4 + 2];     // B→R
                            rgba[i * 4 + 1] = img.data[i * 4 + 1]; // G
                            rgba[i * 4 + 2] = img.data[i * 4];     // R→B
                            rgba[i * 4 + 3] = img.data[i * 4 + 3]; // A
                        }
                    }
                    cosmic_text::SwashContent::SubpixelMask => {
                        for (i, &alpha) in img.data.iter().enumerate() {
                            rgba[i * 4] = 255;
                            rgba[i * 4 + 1] = 255;
                            rgba[i * 4 + 2] = 255;
                            rgba[i * 4 + 3] = alpha;
                        }
                    }
                }

                let rgba_vec = fbb.create_vector(&rgba);
                glyph_offsets.push(crate::fb::RenderGlyph::create(
                    &mut fbb,
                    &crate::fb::RenderGlyphArgs { x: gx, y: gy, w, h, rgba: Some(rgba_vec) },
                ));

                let right = gx + w as i32;
                let bottom = gy + h as i32;
                if right as f32 > total_w { total_w = right as f32; }
                if bottom as f32 > total_h { total_h = bottom as f32; }
            }
        }

        let glyphs_vec = fbb.create_vector(&glyph_offsets);
        let result = crate::fb::RenderResult::create(
            &mut fbb,
            &crate::fb::RenderResultArgs {
                width: total_w,
                height: total_h,
                glyphs: Some(glyphs_vec),
            },
        );
        fbb.finish(result, None);

        vec_to_jbytearray(&mut env, fbb.finished_data()).unwrap_or(std::ptr::null_mut())
    }));

    match result {
        Ok(arr) => arr,
        Err(_) => env.new_byte_array(0).map(|a| a.into_raw()).unwrap_or(std::ptr::null_mut()),
    }
}

/// Java: static native void destroy(long handle);
#[no_mangle]
pub extern "system" fn Java_com_hfstudio_guidenh_guide_layout_LayoutBridge_destroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            drop(Box::from_raw(handle as *mut GuideFontSystem));
        }
    }
}
