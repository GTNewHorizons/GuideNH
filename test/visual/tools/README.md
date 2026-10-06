# Visual Inspection Screener

A low-cost first-pass screener. It produces machine-readable findings that a later review step
adjudicates and merges.

## Requirements

- Python 3 (on Windows run it as `py -3`)
- Pillow (`pip install Pillow`)
- Everything else uses the standard library only. Do not add third-party dependencies.

## Configuration

Copy `.env.example` to `.env` and fill in the DashScope (Alibaba Cloud Model Studio) API key:

```
cp .env.example .env
```

`.env` file format (parsed by hand, `python-dotenv` is not supported):

```
DASHSCOPE_API_KEY=sk-your-key
VLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
VLM_MODEL=qwen3-vl-plus
VLM_CONCURRENCY=4
VLM_TIMEOUT=120
```

## Subcommands

### 1. geometric - layer 0 mechanical checks

```bash
py -3 screen.py geometric --shots <screenshot-dir> --page-width 1800 --out <output.json>
```

Rules:

- `overflow_width`: the right edge of a block exceeds the page width (`x + w > page-width`,
  2px tolerance).
- `zero_size`: block width or height is at most 0. `LytThematicBreak` and `LytItemImage` are
  demoted to info level (known-benign classes, pending calibration).
- `off_page`: block coordinate `x < 0` or `y < 0`.
- `sibling_intersection`: IoU > 0.05 between direct children of the same parent. Each block pair
  is reported only once (deduplicated by sorted block id). Legitimate float wrapping overlap
  between `LytDocumentFloat` and text classes (`LytParagraph`/`LytHeading`/`LytListBlock`) is
  skipped: float wrapping is the designed behaviour, not a layout defect.

### 2. vlm - layer 1 multimodal first pass

```bash
py -3 screen.py vlm --shots <screenshot-dir> [--pages name1,name2] [--model overrides-.env] [--tile-h 1400] [--overlap 200] [--dry-run] --out <output.json>
```

- Slices each PNG into full-width tiles of height `tile-h` with `overlap` overlap (`overlap`
  must be smaller than `tile-h`, otherwise a `ValueError` is raised to avoid an infinite loop).
- Calls the OpenAI-compatible VLM API once per tile.
- Converts tile-local response coordinates back to whole-page coordinates and deduplicates
  across tiles (same class and IoU > 0.5 are merged).
- `--dry-run` only prints information and issues no HTTP requests.
- Concurrency: `ThreadPoolExecutor(VLM_CONCURRENCY)`.
- On API failure it retries 3 times with exponential backoff (1s/4s/16s).

### ask - lightweight targeted visual question answering

A lightweight targeted entry point: name specific PNG files plus a custom question prompt, feed
the whole image straight to the VLM, and get a targeted answer quickly. It is meant for
point checks such as "is this icon rendered as a tofu box" or "are rows 3-4 of the navigation bar
ghosted".

```bash
py -3 screen.py ask --files <image1.png>[,<image2.png>] --prompt "is this icon rendered as a tofu box?" [--out output.json] [--model overrides-.env] [--dry-run]
```

Parameter table:

| Parameter | Required | Description |
|-----------|----------|-------------|
| `--files` | yes | PNG file paths (repeat the flag or separate several with commas; relative and absolute paths are both accepted) |
| `--prompt` | yes | Targeted question text (chosen by the caller) |
| `--out` | no | Output JSON path; when omitted the result is printed to stdout |
| `--model` | no | Overrides `VLM_MODEL` from `.env` |
| `--dry-run` | no | Only prints which files would be sent and the request body size, no HTTP request |

- Feeds the whole image, no tiling (this is the fundamental difference from the vlm first pass).
  Each file is called independently and processed sequentially (no concurrency).
- Custom prompt: the system prompt is the neutral `ASK_SYSTEM_PROMPT` (answer strictly the user
  question, rely only on visible evidence in the image, answer directly and concretely, follow
  the caller-supplied format when structured output is requested). This differs from the fixed
  layout-defect prompt used by the vlm first pass.
- Free-form answer with best-effort structure: `answer` is the model's verbatim reply (not
  post-processed); `findings` is best-effort - when the model produced structured JSON with a
  `findings` array as requested, that array is extracted, otherwise it is empty.
- When the image area exceeds 6,000,000 pixels it prints a warning (detail may suffer; consider
  cropping a region or using the vlm first pass) but still attempts the call.
- A single file failure does not interrupt the other files: that entry's `error` holds the error
  description (the `error_detail` returned by `call_vlm_api`, verbatim) and is appended to
  `errors`.

Output JSON schema:

```json
{
  "tool": "test/visual/tools/screen.py",
  "subcommand": "ask",
  "model": "qwen3-vl-plus",
  "files": ["<file1>", "<file2>"],
  "prompt": "<verbatim user prompt>",
  "answers": [
    {
      "file": "<file1>",
      "answer": "<verbatim model answer>",
      "findings": [
        {"bbox": [x, y, w, h], "class": "...", "severity": "...", "confidence": 0.0, "evidence": "..."}
      ],
      "error": null
    }
  ],
  "errors": []
}
```

Differences from the vlm first pass:

| Dimension | vlm first pass | ask |
|-----------|----------------|-----|
| Input | directory scan, all PNGs | explicitly named files |
| prompt | fixed 9-class layout-defect prompt | chosen by the caller |
| Imaging | tiling + concurrency + deduplication | whole image, sequential |
| Output | fixed findings schema | free-form answer with best-effort structure |

### 3. report - merged report

```bash
py -3 screen.py report --inputs a.json,b.json --out triage.json
```

- Merges findings from several sources, groups them by page, and sorts by severity
  (error, then warn, then info).
- Prints a per-page count table to stdout.

## Output formats

### geometric output

```json
{
  "tool": "test/visual/tools/screen.py",
  "subcommand": "geometric",
  "page_width": 1800,
  "total_pages": 50,
  "total_findings": 123,
  "pages": [
    {
      "page": "page-name",
      "source": "geometric",
      "findings": [
        {
          "page": "page-name",
          "rule": "overflow_width",
          "bbox": [x, y, w, h],
          "severity": "error|warn",
          "evidence": "description"
        }
      ]
    }
  ]
}
```

### vlm output

```json
{
  "tool": "test/visual/tools/screen.py",
  "subcommand": "vlm",
  "model": "qwen3-vl-plus",
  "total_pages": 5,
  "total_findings": 10,
  "findings": [
    {
      "page": "page-name",
      "source": "vlm",
      "rule": "text overlap",
      "bbox": [x, y, w, h],
      "severity": "error|warn|info",
      "confidence": 0.95,
      "evidence": "one-sentence description"
    }
  ],
  "errors": []
}
```

### report output

```json
{
  "tool": "test/visual/tools/screen.py",
  "subcommand": "report",
  "sources": ["a.json", "b.json"],
  "total_pages": 50,
  "total_findings": 200,
  "summary": {
    "page-name": {"error": 2, "warn": 5, "info": 0}
  },
  "pages": { ... }
}
```

## bounds JSON schema

Reading several real JSON files under `run/client_new/screenshots/` confirmed the following
bounds JSON structure:

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
|-------|------|-------------|
| `i` | int | Index of the block within the page |
| `cls` | str | Block type (for example `LytParagraph`, `LytHeading`, `LytTable`, `LytTableCell`, `LytFloatAwareBlock`, `LytCodeBlock`, `ScenePlaceholder`, `CategoryPlaceholder`) |
| `x` | int | Left edge x coordinate (px) |
| `y` | int | Top edge y coordinate (px) |
| `w` | int | Width (px) |
| `h` | int | Height (px) |
| `depth` | int | Tree nesting depth (1-based). Blocks are ordered depth-first; depth=1 is a root node, and the parent of a depth=D block is the closest preceding block at depth=D-1 |

### Deriving parent-child relations

The JSON is a flat array, so parent-child relations are derived from the `depth` field and the
element order:

- Keep a stack of the most recent ancestor per depth.
- The parent of block B is the closest block before B whose depth equals B.depth - 1.
- Siblings are direct children that share the same parent.

Example (from `scene-blocks.md`):

```
depth 1: LytFloatAwareBlock   <- parent: none (root)
depth 2: LytTable             <- parent: LytFloatAwareBlock
depth 3: LytTableRow          <- parent: LytTable
depth 4: LytTableCell         <- parent: LytTableRow
depth 5: LytParagraph         <- parent: LytTableCell
depth 4: LytTableCell         <- parent: LytTableRow (sibling of the previous LytTableCell)
```


## 4. check_freshness - screenshot freshness validation

Guards against stale screenshot list false positives: it checks that every page in the page list
has a screenshot in the most recent batch and that the screenshots are fresh.

```bash
py -3 test/visual/tools/check_freshness.py --shots <screenshot-dir> --list <page-list> [--stale-min 10]
```

- Page list format: one `guidenh:visualtest/<path>.md` per line; lines starting with `#` are
  comments.
- Screenshots are named `<page_stem>_<YYYY-MM-DD_HHMMSS>.png`; the newest batch is selected per
  stem.
- Three states: OK / MISSING (no screenshot) / STALE (newest screenshot mtime is older than the
  list mtime plus stale-min minutes).
- Exit code 1 when anything is MISSING or STALE; the TAP-style output mirrors assert_bounds.py.
- `--self-test`: built-in smoke cases for the OK/MISSING/STALE states.
- Standard library only, no third-party dependencies.

## 5. scan_logs - log scanning

Enforces log hygiene: when the client log of a render round contains a `glyph atlas full` or
`OutOfMemory` warning, that round is judged failed.

```bash
py -3 test/visual/tools/scan_logs.py [--logs run/client_new/logs] [--pattern "glyph atlas full"]
```

- Scans `*.log`, `*.log.gz` (streamed gzip decompression) and `latest.log`.
- Every hit prints the file, line number and line content; exit code 1 on any hit or read error.
- `--self-test`: built-in sample logs (both patterns plus noise) verifying hits and the exit code.
- Standard library only, no third-party dependencies.
