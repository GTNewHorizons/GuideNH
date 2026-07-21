pub mod guidenh_layout_generated;
// Re-export generated types for convenience
pub use guidenh_layout_generated::com::hfstudio::guidenh::guide::layout::flatbuffers as fb;
pub mod jni_bridge;
pub mod layout;
pub mod measure;
pub mod parley_text;
pub mod style_convert;
pub mod text;

use std::panic::{catch_unwind, AssertUnwindSafe};

use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jlong};

use crate::jni_bridge::{jbytearray_to_vec, vec_to_jbytearray};
use crate::layout::compute_layout;
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
