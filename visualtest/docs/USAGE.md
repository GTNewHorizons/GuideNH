# Usage Guide — GuideNH Visual Test System

## Purpose

This document describes how to operate the headless rendering pipeline, the watchdog wrapper, the list file format, render output artifacts, verification gating, batch constraints, and the assertion ratchet. Every command is specified for execution from the repository root.

---

## 1. Headless Batch Rendering

### 1.1 Prerequisite

Stop any lingering Gradle daemon before starting a new render session:

```bash
./gradlew --stop
```

### 1.2 List File

The `--list` parameter expects a **path to a text file**, not a comma-separated list of page IDs. Each line in the list file holds one page ID; lines beginning with `#` are treated as comments. Passing a raw page ID string instead of a file path causes an `InvalidPathException` — the system has been hardened to report this error explicitly rather than being silently swallowed by FML.

**Page ID rule**: Every fixture page ID follows the pattern `guidenh:visualtest/<subfolder>/<file>.md`. Example:

```
guidenh:visualtest/mermaid/mindmap.md
guidenh:visualtest/text/headings.md
guidenh:visualtest/scenes/entities.md
```

**Page ID components**:
| Part | Description |
|---|---|
| `guidenh:` | Namespace prefix (engine scans the fixed folder `guidenh` via `DataDrivenGuideLoader.AUTO_GUIDE_FOLDER`) |
| `visualtest/` | Subtree root under `_en_us/` |
| `<subfolder>/` | Semantic group folder (e.g., `text/`, `charts/`, `mermaid/`) |
| `<file>.md` | Fixture page filename |

### 1.3 Render Command Template

All headless renders **must** be wrapped by the watchdog script. Do not invoke `gradlew` directly.

```bash
py -3 tools/visual-inspection/render_watchdog.py --timeout 600 --log <log-path> --   cmd //c "gradlew.bat runClient25     -Dguidenh.guide.sources=<sources-path>     -Dguidenh.headlessRender=true     -Dguidenh.renderpage.guide=guidenh:guidenh     -Dguidenh.renderpage.list=<list-file-path>     -Dguidenh.renderpage.out=<output-dir>     -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2     -Dguidenh.renderpage.bounds=true -Dguidenh.renderpage.overlay=true"
```

**Platform note (git-bash)**: Use `cmd //c` (double slash) and wrap the entire Gradle command in quotes. A single `/c` is mangled to `C:/` by MSYS; bare bash resolves to WSL bash.

### 1.4 `-D` Parameter Reference

The following table is derived from `parseConfig` and the `runClient25` Gradle task (see `build.gradle.kts` lines 79–83).

| Parameter | Semantics | Default | Remarks |
|---|---|---|---|
| `guidenh.guide.sources` | Dev resource-pack directory (forwarded to `guideme.resourcePack.sources`) | — | Required for fixture injection; no effect in production |
| `guidenh.headlessRender` | Activate headless driver (hide window + register driver) | `false` | Must be `=true` |
| `guidenh.renderpage.guide` | Target guide ID | Required | Use `guidenh:guidenh` |
| `guidenh.renderpage.page` / `.md` | Single-page mode: page ID / `.md` file path | — | Mutually exclusive with batch mode |
| `guidenh.renderpage.allPages` | Batch: render all pages of the guide (**do not use** — includes the main guide) | — | — |
| `guidenh.renderpage.list` | Batch: **path to list file** (one ID per line) | — | See list file format above |
| `guidenh.renderpage.out` | Output directory (relative to `run/client_new/`) | `screenshots` | — |
| `guidenh.renderpage.width` | Page width in px (100–4096) | `900` | — |
| `guidenh.renderpage.scale` | Render scale multiplier (1–4) | `1` | **Use `2`** for readable fonts and 3D content |
| `guidenh.renderpage.lang` | Language | `en_us` | — |
| `guidenh.renderpage.bounds` | Write bounds JSON alongside PNG | `false` | Must be `=true` for geometric screening |
| `guidenh.renderpage.overlay` | Write layout overlay PNG | `false` | — |
| `guidenh.renderpage.world` | Save to use | `screenshot-world` | Already exists under `saves/` |

---

## 2. Render Watchdog

### 2.1 Purpose

The watchdog (`tools/visual-inspection/render_watchdog.py`) prevents orphaned JVM processes that survive when the harness kills the shell without terminating the child process tree.

### 2.2 Usage

```bash
py -3 tools/visual-inspection/render_watchdog.py [options] -- <command>
```

### 2.3 Arguments

| Argument | Default | Description |
|---|---|---|
| `--timeout N` | `480` | Timeout in seconds. On expiry the watchdog runs `taskkill /T /F` on the process tree and cleans up orphaned JVMs, then exits with code 124. |
| `--log PATH` | ``C:/Temp/opencode/watchdog_last.log` (Windows temp convention, shared across machines)` | Path to the combined stdout/stderr log file. |
| `--kill-extra REGEX` | `''` | Additional regex pattern for orphan JVM matching (besides the built-in keywords `run/client_new`, `guidenh`, `launchwrapper`). |

### 2.4 Behaviour

1. Pre-snapshot of all running Java processes (WMIC, fallback PowerShell).
2. Launches the wrapped command with redirected output.
3. Heartbeat every 30 s on stderr.
4. On normal process exit: scans for orphaned JVMs, waits 10 s, kills survivors.
5. On timeout: kills the process tree, cleans orphans, exits 124.
6. Prints a summary of exit code, elapsed time, log path, and the last 15 log lines.

### 2.5 Process Management Rules

1. Always run `./gradlew --stop` before each render session.
2. After an abnormal exit, confirm the watchdog reports zero orphans.
3. **Never blindly retry with a longer timeout** after a failure — diagnose the log first.
4. Suggested timeout range: 480–600 s (covers cold-start latency; each page averages <1 min in batch).

### 2.6 Troubleshooting

| Symptom | Diagnosis (grep log) | Cause | Action |
|---|---|---|---|
| Stuck at main menu loop | No `Registering headless render driver` | Driver not registered — old config errors silently swallowed by FML | Verify `-Dguidenh.headlessRender=true` arrives in JVM; use `--info` to check `Starting process` |
| `Invalid headless render configuration` | Console output | Typo in `-D` name or wrong list semantics | Fix per the error message |
| `Guide not found` | Log | Wrong `renderpage.guide` value | Use `guidenh:guidenh` |
| Window visible (not hidden) | Visual inspection | `headlessRender` not reaching JVM (quoting issue) | Check `cmd //c` quoting form |
| Page FAIL in batch log | `Page FAIL` line | Page compilation or render error | Inspect stack trace in the same log |

---

## 3. Render Output

### 3.1 Output Location

All artifacts are written under `run/client_new/<output-dir>/` (where `<output-dir>` is the value of `renderpage.out`, default `screenshots`).

### 3.2 Artifact Files

| Suffix | Format | Description |
|---|---|---|
| `<page>_<timestamp>.png` | PNG | Full-page screenshot |
| `<page>_<timestamp>.json` | JSON | Bounds JSON — flat array of layout blocks with geometry. Required for geometric screening. |
| `<page>_<timestamp>_overlay.png` | PNG | Layout overlay visualisation (block outlines for manual review). |

**Page name pattern**: The file stem encodes the fixture page path relative to the
`visualtest/` corpus root, with `/` replaced by `_`. Example: page
`visualtest/charts/options.md` renders to:

```
visualtest_charts_options.md_2026-07-29_064319.png
```

### 3.3 Bounds JSON Schema

```json
[
  {
    "i": 0,
    "cls": "LytParagraph",
    "x": 5,
    "y": 5,
    "w": 393,
    "h": 40,
    "depth": 1
  }
]
```

| Field | Type | Description |
|---|---|---|
| `i` | int | Block sequence index on the page |
| `cls` | str | Block class (e.g., `LytParagraph`, `LytHeading`, `LytTable`, `LytFloatAwareBlock`, `LytCodeBlock`, `ScenePlaceholder`) |
| `x` | int | Left x-coordinate (px) |
| `y` | int | Top y-coordinate (px) |
| `w` | int | Width (px) |
| `h` | int | Height (px) |
| `depth` | int | Tree nesting depth (1-based). Blocks are depth-first; parent of a depth-D block is the preceding depth-(D-1) block. |

### 3.4 Timing

- Cold start to first screenshot: ~3–4 min (85 mods loading).
- Batch throughput: ~35 s per page (measured on a 5-page run).
- Terminal log line on completion: `Batch complete: total=N ok=N failed=0`. Process exits 0.

---

## 4. Screening Pipeline

The screening pipeline is implemented by `tools/visual-inspection/screen.py` with three sub-commands.

### 4.1 Geometric Screening (Layer 0)

Requires `bounds=true` render output.

```bash
py -3 tools/visual-inspection/screen.py geometric --shots <shots-dir> --page-width 1800 --out <geo-json-path>
```

Detection rules:
- **overflow_width**: block right edge exceeds page width (tolerance 2 px).
- **zero_size**: block width ≤ 0 or height ≤ 0. Classes `LytThematicBreak`, `LytItemImage` downgraded to `info`.
- **off_page**: block x < 0 or y < 0.
- **sibling_intersection**: IoU > 0.05 between direct children of the same parent. Skips `LytDocumentFloat` × text-class overlap (legitimate wrapping).

### 4.2 VLM Screening (Layer 1)

Requires a `.env` file in `tools/visual-inspection/` with a valid `DASHSCOPE_API_KEY`. Copy from `.env.example`.

```bash
py -3 tools/visual-inspection/screen.py vlm --shots <shots-dir> --out <vlm-json-path>
```

Optional flags: `--pages filter1,filter2`, `--model override`, `--tile-h 1400`, `--overlap 200`, `--dry-run`.

The selected model is `qwen3-vl-plus` (configured in `.env`).

### 4.3 Combined Report (Layer 2)

```bash
py -3 tools/visual-inspection/screen.py report --geo <geo-json> --vlm <vlm-json> --out <report-path>
```

---

## 5. Verification Gating

The Gradle build defines several tasks under the `verification` group:

| Task | Description |
|---|---|
| `test` | Runs the Java compiler, layout adapter, parser, and diagnostics regression tests. |
| `shadowJar` | Builds the distributable jar with the taffy-java backend included. |

**TOTAL ISSUES determination**: The gating criterion for the visual inspection pipeline is the combined finding count after triage. A batch is considered passing when:
- The render command exits 0,
- The watchdog exits 0,
- All pages in the list are accounted for (`total=N ok=N failed=0`),
- After K3 adjudication, no finding is classified as a genuine engine defect without a corresponding fix or acknowledged issue entry.

The geometric and VLM screening outputs feed into the K3 adjudication process; merged reports are compared against the issue registry (see `ISSUES.md`) to determine whether any new real engine defects have appeared.

**Layout backend**: Layout verification uses the embedded taffy-java implementation. No Rust compiler, JNI library, or native DLL is required.

---

## 6. Batch OOM Constraint

**Rule**: A single batch **must not exceed 40 pages**.

**Ground truth**: During the first full 63-page batch, the process ran out of Java heap space at approximately page 42 (NEI-worker thread OOM first). Pages 41/63 succeeded; subsequent pages failed consecutively. The root cause is suspected cross-page resource leaking (screenshot world, textures, NEI artifacts not fully released).

**Mitigation**: Split lists exceeding 40 pages into multiple batches. Render each batch separately. For example, the full fixture set (63 pages) must be split into at least two batches.

---

## 7. Assertion Ratchet

After an issue is confirmed and fixed, the invariant is encoded as a geometric bounds assertion into the ratchet harness.

### 7.1 Invocation

```bash
py -3 tools/visual-inspection/assert_bounds.py --shots <shots-dir> --assertions visualtest/ratchet/assertions.json
```

### 7.2 Workflow

1. Issue confirmed → write one or more bounds JSON assertions into `visualtest/ratchet/assertions.json`.
2. Assertions that cannot be expressed geometrically are marked `"visual-only": true`.
3. A fix is considered complete when the assertion is committed to the ratchet, the full gating pipeline passes, and the fixture screenshot is reviewed and accepted.
