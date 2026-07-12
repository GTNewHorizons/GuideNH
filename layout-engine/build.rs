use std::path::Path;
use std::process::Command;

fn main() {
    let schema = Path::new("schema").join("guidenh_layout.fbs");
    let out_dir = Path::new("src");
    let generated = out_dir.join("flatbuffer_generated.rs");

    println!("cargo:rerun-if-changed={}", schema.display());

    // flatc::flatc() returns PathBuf to the flatc binary built by the crate
    let flatc_path = flatc::flatc();

    let status = Command::new(&flatc_path)
        .args(["--rust", "-o", out_dir.to_str().unwrap(), schema.to_str().unwrap()])
        .status()
        .expect("flatc binary failed to execute");

    if !status.success() {
        panic!("flatc exited with code {:?}", status.code());
    }

    println!("cargo:warning=Generated {}", generated.display());
}
