use std::fs;
use std::path::Path;
use std::process::Command;

fn main() {
    let schema = Path::new("schema").join("guidenh_layout.fbs");
    let out_dir = Path::new("src");
    let generated = out_dir.join("guidenh_layout_generated.rs");

    println!("cargo:rerun-if-changed={}", schema.display());

    let flatc_path = flatc::flatc();

    let status = Command::new(&flatc_path)
        .args(["--rust", "-o", out_dir.to_str().unwrap(), schema.to_str().unwrap()])
        .status()
        .expect("flatc binary failed to execute");

    if !status.success() {
        panic!("flatc exited with code {:?}", status.code());
    }

    // Patch the generated file to work with Rust 2021:
    // 1. Remove all `extern crate flatbuffers;` — Rust 2021 auto-imports extern crates
    // 2. Change `use self::flatbuffers::` to `use ::flatbuffers::` — `self::flatbuffers`
    //    inside `mod flatbuffers { }` refers to the module, not the crate
    if generated.exists() {
        let content = fs::read_to_string(&generated).unwrap_or_default();
        // Remove ALL `extern crate flatbuffers;` regardless of indentation or line endings
        let patched = content
            .lines()
            .filter(|l| !l.trim().starts_with("extern crate flatbuffers"))
            .collect::<Vec<_>>()
            .join("\n")
            .replace("use self::flatbuffers::{EndianScalar, Follow};", "use ::flatbuffers::{EndianScalar, Follow};")
            .replace("use self::flatbuffers::Verifiable;", "use ::flatbuffers::Verifiable;");
        if patched != content {
            fs::write(&generated, patched).expect("Failed to patch generated file");
            println!("cargo:warning=Patched generated file (extern crate + self::flatbuffers fixed)");
        }
    }

    println!("cargo:warning=Generated {}", generated.display());
}
