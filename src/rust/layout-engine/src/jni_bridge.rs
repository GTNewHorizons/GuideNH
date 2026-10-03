use jni::JNIEnv;
use jni::objects::JByteArray;

/// Convert JNI jbyteArray to Rust Vec<u8>.
pub fn jbytearray_to_vec(
    env: &mut JNIEnv,
    array: &JByteArray,
) -> Result<Vec<u8>, jni::errors::Error> {
    let size = env.get_array_length(array)? as usize;
    let mut buf = vec![0i8; size];
    env.get_byte_array_region(array, 0, &mut buf)?;
    Ok(buf.iter().map(|&b| b as u8).collect())
}

/// Convert Rust &[u8] to JNI jbyteArray.
pub fn vec_to_jbytearray(
    env: &mut JNIEnv,
    data: &[u8],
) -> Result<jni::sys::jbyteArray, jni::errors::Error> {
    let arr = env.new_byte_array(data.len() as i32)?;
    let data_i8: Vec<i8> = data.iter().map(|&b| b as i8).collect();
    env.set_byte_array_region(&arr, 0, &data_i8)?;
    Ok(arr.into_raw())
}
