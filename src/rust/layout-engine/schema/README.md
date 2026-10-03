# layout-engine schema: guidenh_layout.fbs generation chain and change policy

Read this before changing the FlatBuffers schema. It describes where the schema lives, how each
side regenerates its bindings, which flatc version is pinned, and which changes are wire
compatible.

## 1. Schema location and namespace

- Schema file: `src/rust/layout-engine/schema/guidenh_layout.fbs`, 39 tables.
- namespace: `com.hfstudio.guidenh.guide.layout.flatbuffers`.
- Contract direction: Java serializes, the Rust layout engine consumes.
- Checked-in Java bindings: `src/main/java/com/hfstudio/guidenh/guide/layout/flatbuffers/`, 39
  `.java` files, one per table.

## 2. Generation chain

### Rust side (automatic)

- `src/rust/layout-engine/build.rs` calls the binary provided by the `flatc` crate
  (`flatc::flatc()`) during cargo build, running
  `flatc --rust -o src schema/guidenh_layout.fbs` with `src/rust/layout-engine` as the working
  directory.
- The output is `src/rust/layout-engine/src/guidenh_layout_generated.rs`, which build.rs then
  patches:
  1. it removes every `extern crate flatbuffers;` (Rust 2021 imports extern crates
     automatically);
  2. it rewrites `use self::flatbuffers::` to `use ::flatbuffers::`, because `self::flatbuffers`
     inside `mod flatbuffers { }` names the module rather than the crate.
- No manual regeneration is required on the Rust side. The generated file is a build artifact
  that Git ignores, so editing a schema comment changes only what the next build emits, never the
  wire format.

### Java side (script)

- `tools/regen_java_flatc.bat` locates flatc, verifies its version, runs `flatc --java` into a
  temporary directory, compares the result with the checked-in classes, and overwrites them when
  they differ (`--dry-run` previews, `--check-only` verifies in CI).
- Never hand-edit a generated class. Earlier hand-edits of generated
  classes made the checked-in classes drift from the schema. Status check (2026-08-02):
  `TextData.java` still carries hand-edit traces, because its doc comment and the order of its
  `addAlignment`/`addSeparator` calls differ from flatc output; running the script in apply mode
  normalizes it. That residue is deliberately left to the script.
- flatc comes from exactly one source: the binary built from the cargo `flatc` crate. That is a
  `flatc.exe` under the `flatc-*` build-script output directory of the cargo build, in its debug
  or release profile, so where it lands follows from how the cargo target directory resolves and
  must not be hardcoded. The version observed there is 23.5.26. Gradle has no flatc configuration
  (a repository-wide search finds none), so a Gradle build never regenerates the Java classes.

## 3. Change procedure

1. Edit `src/rust/layout-engine/schema/guidenh_layout.fbs`, respecting the wire-compatibility
   policy in section 5.
2. Regenerate the Java classes:
   - preview the difference: `tools\regen_java_flatc.bat --dry-run`;
   - apply the overwrite: `tools\regen_java_flatc.bat` (no argument means overwrite-regenerate);
   - verify for CI or by hand: `tools\regen_java_flatc.bat --check-only` (exit 1 on mismatch).
3. Do nothing on the Rust side: build.rs regenerates the file and patches it on the next cargo
   build.
4. Run the gate: `./gradlew compileJava compileTestJava test runLayoutDump`.
5. Commit the schema, the Java classes and the generated Rust file in one commit.

## 4. Version policy

- flatc must be 23.5.26, exactly matching the `flatbuffers-java` 23.5.26 runtime.
- Every generated Java class carries a version guard,
  `ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }`, so a mismatch with the runtime version
  fails at the deserialization entry point.
- Upgrading the flatbuffers-java runtime requires all of the following in one change: upgrade
  flatc, regenerate all 39 classes, and run the gate.
- `tools/regen_java_flatc.bat` verifies the version itself and exits with an error when
  `flatc --version` does not contain `23.5.26`.

## 5. Wire-compatibility policy

- Schema changes must be append-only: add fields or tables, and never remove a field, change a
  type, or change what a field means.
- Deprecation convention for a field: keep field, write zero. The field stays in the schema (its
  vtable slot number is unchanged) and the writing side sends 0 or the default value.
- Deprecated fields present today: 4 field names across 5 occurrences. The line numbers are a
  2026-10-03 snapshot and drift with later edits.

  | Table | Field | Location |
  |---|---|---|
  | `TextData` | `bands` | guidenh_layout.fbs:138 |
  | `TextData` | `float_clips` | guidenh_layout.fbs:140 |
  | `PieChartData` | `chrome_height` | guidenh_layout.fbs:259 |
  | `ChartData` | `chrome_height` | guidenh_layout.fbs:279 |
  | `MediaWikiSpecialGeneratedData` | `max_content_height` | guidenh_layout.fbs:328 |

- Current semantics of those fields: the writing side sends 0 or the default, and the Rust side
  computes the value internally (parley migration, chart chrome, maxColumnHeight).

## 6. Second generated set (not covered by this script)

- `src/main/java/guideme/flatbuffers/scene/` holds 16 `Exp*` classes (`ExpScene`, `ExpMesh`,
  `ExpMaterial` and so on) generated from the upstream GuideME schema.
- This repository has no corresponding `.fbs` source and `tools/regen_java_flatc.bat` does not
  cover that set, so changes there have to be made in the upstream project.
