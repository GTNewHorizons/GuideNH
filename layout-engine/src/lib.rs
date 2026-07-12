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

        for i in 0..r_input.requests().map_or(0, |r| r.len()) {
            let req = r_input.requests().unwrap().get(i);
            let font_id = req.font_id();
            let font_size = req.font_size();
            let glyph_ids = req.glyph_ids();

            for j in 0..glyph_ids.map_or(0, |g| g.len()) {
                let glyph_id = glyph_ids.unwrap().get(j);

                if let Some((w, h, rgba)) = rasterize_glyph(
                    &mut font_system.font_system,
                    swash_cache,
                    glyph_id,
                    font_size,
                ) {
                    let rgba_vec = fbb.create_vector(&rgba);
                    glyph_offsets.push(
                        crate::fb::GlyphPixels::create(
                            &mut fbb,
                            &crate::fb::GlyphPixelsArgs {
                                font_id,
                                font_size,
                                glyph_id,
                                width: w,
                                height: h,
                                rgba: Some(rgba_vec),
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
