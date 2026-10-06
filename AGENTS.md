# GuideNH Agent Guide

GuideNH is an in-game guide framework for Minecraft 1.7.10 / Forge 10.13.4.1614. The Java side
provides the guide engine, page parser, screens, and mod integration; the Rust side provides the
layout engine that the Java side loads as a native library. This file describes the repository
layout, the commands that build and verify it, and the conventions that all code and documentation
in the public tree must follow.

## Repository Layout

| Path | Contents |
|---|---|
| `src/main/java/` | Java sources: guide engine, layout bridge, page parser, screens, settings, item index. |
| `src/main/resources/` | Mod resources: assets, mixin configuration, `mcmod.info`. `natives/` holds the compiled layout engine library and is generated, not tracked. |
| `src/rust/layout-engine/` | Rust layout engine crate: layout, text measurement, JNI bridge, FlatBuffers schema. |
| `src/test/java/` | JUnit tests for the Java side. |
| `test/visual/` | Visual regression system: fixture resource pack, assertion ratchet, Python tools. |
| `test/visual/resourcepack/` | Fixture guide pack rendered during visual verification. Fixture pages live under `assets/guidenh/guidenh/_en_us/visualtest/`. |
| `test/visual/ratchet/` | Bounds assertions that lock accepted layout geometry in place. |
| `test/visual/tools/` | Python helpers for rendering, bounds assertions, screenshot freshness, log scanning, and screening. |
| `tools/` | Runtime bridge scripts and the FlatBuffers code regeneration helper. |
| `wiki/` | Published user documentation, maintained as English and Chinese pairs. |
| `gradle/` | Gradle wrapper and the daemon JDK toolchain configuration. |

## Build, Test, And Run

```powershell
./gradlew build
./gradlew compileJava compileTestJava test
./gradlew runClient
./gradlew runServer
./gradlew spotlessApply
```

Rust side, from the crate directory:

```powershell
cd src/rust/layout-engine
cargo test
cargo build --release
```

The Gradle wrapper provisions its own JDK 25 toolchain through
`gradle/gradle-daemon-jvm.properties`; the supported runtime matrix is JDK 8, 17, and 21. A Rust
toolchain installed with rustup is required only for the native layout engine.

## Native Library

The layout engine library is a build artifact, not a tracked file. `./gradlew buildRustNative`
compiles `src/rust/layout-engine`, and the Gradle build copies the resulting
`guide_layout_engine.dll` into `src/main/resources/natives/`. The copy is skipped when the library
has not been built, so a checkout without a Rust toolchain still builds. Let the Gradle task perform
the copy instead of copying the file by hand, because the cargo target directory can be relocated.

The cargo target directory is resolved from the first of these that is set:

1. the Gradle property `guidenh.rustTargetDir`;
2. the environment variable `GUIDENH_RUST_TARGET_DIR`;
3. the `target-dir` entry in `src/rust/layout-engine/.cargo/config.toml`, which is a local,
   untracked redirection, with relative values resolved against the crate directory;
4. the in-tree default `src/rust/layout-engine/target`.

## Visual Verification

Render the fixture corpus headlessly. Page ids are listed one per line, each as
`guidenh:visualtest/<path>.md`. Keep each batch to at most 40 pages, and always wrap a batch in the
render watchdog:

```powershell
py -3 test/visual/tools/render_watchdog.py --timeout 2400 --log <log> -- cmd /c "gradlew.bat runClient25 -Dguidenh.guide.sources=<repo>/test/visual/resourcepack -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.list=<list-file> -Dguidenh.renderpage.out=<shots> -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2 -Dguidenh.renderpage.bounds=true"
```

Run the layout harness as a gate, which must report a total issue count of 0:

```powershell
./gradlew compileJava compileTestJava test runLayoutDump
```

Then check the rendered bounds against the ratchet, which must exit 0:

```powershell
py -3 test/visual/tools/assert_bounds.py --shots run/client_new/<shots> --assertions test/visual/ratchet/assertions.json
```

The ratchet is append-only: add assertions for newly verified geometry, and adjust an existing
assertion only when the geometry change is intentional and reviewed. Assertions are keyed by page id
such as `visualtest_text_decorations.md` and assert class counts, node existence, or bounds.

## Logs And Paths

- Render and client logs are written under `run/client_new/logs/`.
- Render output, screenshots, bounds JSON, and logs stay under the untracked `run/` tree.
- Every path in code, comments, documentation, scripts, and configuration must be repository-relative.
  Never commit a machine-local absolute path.

## Comment And Documentation Conventions

Public code and documentation are written in English. This applies to comments, identifiers, string
messages, and section titles.

- Java doc comments use the standard block form with `@param`, `@return`, and `@throws` tags. Rust
  items use rustdoc comments with `# Arguments` and `# Returns` sections where they add value.
- Write comments that carry information the code does not already express. Do not restate the next
  line, and do not add banner or separator comment lines built from repeated `=`, `-`, `#`, or `*`
  characters.
- Do not use Markdown bold markers, meaning a doubled asterisk pair, inside comments.
- Do not use CJK dashes or CJK punctuation in code, comments, identifiers, or documentation.
- Do not reference internal research or working documents that are not distributed with the
  repository, and do not cite section numbers from them.
- Editing comments must not change executable behavior or rendering results.

Localized documents are the only exception to the English rule: `README_zh-CN.md` and the
`wiki/*-zh-CN.md` pages are maintained in Chinese, and fixture pages under
`test/visual/resourcepack/` may carry non-English text as test payload. Everything else in the
public tree is English.
