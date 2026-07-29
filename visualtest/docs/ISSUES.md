# Issue Registry — GuideNH Visual Test System

## Purpose

This document records all issues discovered during the first full visual inspection cycle (2026-07-28: 63 pages rendered, geometric screening 53 findings, VLM screen 810 findings → K3 adjudication). Every entry is backed by screenshot evidence.

---

## Status Labels

| Label | Meaning |
|---|---|
| `CLOSED` | Fixed and verified by visual inspection. |
| `CLOSED-WITH-RESIDUAL` | Main issue fixed; a residual edge case is tracked separately. |
| `PENDING-IN-GAME` | Not reproducible in offline render; requires in-game verification as the gold standard. |
| `REGISTERED-PREEXISTING` | Present in the initial baseline but predates this test cycle; tracked for future triage. |
| `OPEN` | Confirmed real engine issue awaiting fix assignment. |

---

## A. Real Engine Issues (Screenshot-Confirmed, in Unified Fix Queue)

| # | Issue | Evidence (Pages) | Status |
|---|---|---|---|
| A1 | Mermaid placeholder box (confirmed real engine issue; suspected ELK/async migration loss) | `mermaid/*`, `stress/mixed` | `CLOSED` |
| A2 | JSX `<table align>` empty-column crash at `LytTable.java:176` | `layout/align` (test cases disabled, to be restored) | `CLOSED` |
| A3 | **ContentTabs header height ≈ 0, content overlaps tab bar**; icon crushes title | `layout/content-tabs`, `floats/in-tabs`, `stress`; geometric confirms `ContentTabsHeader h=0` ×10 | `CLOSED` |
| A4 | **details body left edge cropped by ~1 character** ("T→is", "D→iamond", "S→ystem") | `layout/details` | `CLOSED` |
| A5 | **Corner legend clipped at TL/BL/BR by plot area edge** (TR normal) | `charts/options` | `CLOSED` |
| A6 | **Display LaTeX not centred** (all left-aligned, violates centre invariant) | `latex/display`, `stress` | `CLOSED` |
| A7 | `<Latex>` inline formula drops to next line (backlog entry, screenshot-confirmed) | `latex/inline` (entire page) | `CLOSED` |
| A8 | **`$...$` / `$$...$$` shorthand completely unparsed, rendered as literal text** (new discovery) | `latex/inline`, `stress` | `CLOSED` |
| A9 | ImportStructure followed by ReplaceBlock/RemoveBlocks does not affect imported blocks (three scenes render identically) | `scenes/import` | `CLOSED` |
| A10 | **RecipeFor/RecipesFor/handler filtering/fallbackText all render only "[Recipe]" placeholder** (only `<Recipe id>` works) | `nei/recipes` | `CLOSED-WITH-RESIDUAL` |
| A11 | **Code block inside fullWidth Column does not fill container width** (K4 family confirmed; tables do fill) | `images/fullwidth` | `CLOSED` |
| A12 | **Task list `- [x]` renders without checkbox or checkmark** (new discovery) | `stress` (lists/tasks pending recheck) | `CLOSED` |
| A13 | **§ colour/format codes completely ineffective, rendered as literal text** (`<Color>` tag works normally by comparison) | `text/section-codes` (entire page) | `CLOSED` |
| A14 | **Pipe table `{: widths=}` metadata ineffective**: metadata row rendered as literal table row, columns evenly divided (fixture syntax matches wiki docs) | `tables/metadata` (all 3 cases), `stress` | `CLOSED` |
| A15 | **FloatingImage document float anchor broken**: all floating images render at page origin (0,0) overlapping header; wrapping area has no float (explains prior br-attributed header-overlap) | `floats/wrap-modes`, `images/fullwidth`, `stress` | `CLOSED` |
| A16 | Task list checkbox absence confirmed (A12 cross-check: `lists/tasks` page same defect, some `[x]` literal remnants) | `lists/tasks`, `stress` | `CLOSED` |

---

## A-Class Fix Closure Ledger (2026-07-29, Unified Fix Round)

All entries verified by visual inspection.

| # | Status | Fix Commit(s) | Root Cause / Method Summary |
|---|---|---|---|
| A1 | `CLOSED` | `ad66653d` + `651eb4c4` | Fenced mermaid true creation point is in PreCompiler (MermaidScript is a dead path); shared precomputed `MermaidLayoutPrecomputer` + headless zoom/offset bypass + `pushScissor` centre cropping; `651eb4c4` rolls back K4 fix's fullWidth side effect on mermaid |
| A2 | `CLOSED` | `9e00e5ff` | JSX `<table align>` empty-column crash fix |
| A3 | `CLOSED` | `e6c3f03d` | ContentTabs header height ≈ 0 fix |
| A4 | `CLOSED` | `9e00e5ff` | details body left-edge crop fix |
| A5 | `CLOSED` | `188c6abc` | Corner legend coordinate system: `renderChart` changed to return inner rect, legend positioned relative to data area (was relative to label axis area); four corners PASS, pie/function no regression |
| A6 | `CLOSED` | `651eb4c4` | `$$` standalone paragraph detection in `ParagraphCompiler` (`PageCompiler.compileParagraphBlock` is dead code) + `alignSelf CENTER` + K4 width 100% makes centring effective |
| A7 | `CLOSED` | `651eb4c4` | Line-drop root cause = `LytLatexBlock.baselineAscent` always 0 (Rust `inline_post_pass` places formula at ascent); also fixed Rust double `user_scale` (`measure.rs:445`, DLL dual-directory rebuild) |
| A8 | `CLOSED` | `651eb4c4` (`$$`) + `188c6abc` (`$`) | `$` inline shorthand: Pandoc delimiter rules (open non-whitespace / close non-whitespace / close not followed by digit) + `\$` escape protection + shared mask/split Pattern; residual edge tracked as N2 |
| A9 | `CLOSED` | `e6c3f03d` | ImportStructure post-import scene change timing fix |
| A10 | `CLOSED-WITH-RESIDUAL` | `e6c3f03d` | RecipeFor/RecipesFor/handler/fallbackText all restored to real rendering (rendering confirmed 064319) |
| A11 | `CLOSED` | `9e00e5ff` + `651eb4c4` | K4 systemic root fix: taffy root `size.width=Auto` causes shrink-wrap → `align_self` all ineffective → fullWidth block `size_width=100%` (`LayoutStyleExtractor dimPercent`); code blocks/tabs/latex centring fixed as side effect |
| A12/A16 | `CLOSED` | `ad66653d` | `extractTaskMarker` mutates shared AST in-place causing second-compile prefix loss → save/restore + try/finally; marker slot `paddingLeft` + `computePrimitives` for drawing |
| A13 | `CLOSED` | `9e00e5ff` + `188c6abc` | Main fix early; residual `§f` white literal = sentinel collision (`0xFFFFFFFF == int -1` collides with "illegal code -1") → sentinel changed to 0; full colour table PASS |
| A14 | `CLOSED` | `ad66653d` | `{: widths=}` metadata rewritten as meta-expression parsing |
| A15 | `CLOSED` | `ad66653d` | Document float anchor reimplemented (floating images return to text wrapping region) |

---

## New Issues Registered During Fix Round

| # | Issue | Details |
|---|---|---|
| N1 | `nei/recipes` `handlerOrder=999` out-of-range fallback | Renders a broken narrow strip (glScissor clipping remnant) instead of `fallbackText`. Expected behaviour documented in-page. Rendering confirmed 2026-07-29 064319. |
| N2 | A8a edge case: escaped `\$x$` restores to `$x$` after unescaping, may be misidentified as a formula by the split pattern | Discovered by reviewer (D1). Rare in real corpus. Low priority. |
| N3 | `lists/rich.md` small table inside a list item stretches to full page width | Semantic consequence of the K4 width chain. Aesthetic concern, not a crash. If narrowing is desired, add a `natural-width` option at the table compiler layer. |
| N4 | `stress/mixed.md` contains outdated text: "known issue: renders placeholder box only" | Mermaid now renders genuinely. Fixture text needs updating. |

---

## Pre-existing Issues (Baseline 2026-07-28, Not Regressions, Awaiting Dedicated Triage)

| # | Issue | Page |
|---|---|---|
| P1 | MDX parse error for "Special Characters" section: `Unhandled MDX element in flow context: null mdxJsxTextElement (113:23)` + `##` heading rendered as literal text | `code/blocks.md` |
| P2 | Natural-width table and checkerboard image overlap in fullwidth page; red image vertical overflow compresses subsequent heading | `images/fullwidth.md` |
| P3 | Red image vertical overflow compresses text (same family as P2) | `lists/rich.md` |
| P4 | Floating red image and table first column slightly overlap | `stress/mixed.md` |

---

## Round 2 Issues (2026-07-29, Human-Reported, Executor-Characterized)

| # | Issue | Evidence | Root-Cause Hypothesis | Status |
|---|---|---|---|---|
| R2-1 | **Mermaid node/edge-label text renders in the vanilla bitmap font, not the engine (parley) text pipeline** — mixed fonts on one page; node text also overflows node borders (vanilla glyphs wider than engine-measured) | `mermaid/flowchart` render 2026-07-29 112407; VLM v3 category "字体渲染路径异常" ×2 | Mermaid canvas label drawing bypasses the GuideText/parley path; text measurement and rasterisation disagree | `OPEN` |
| R2-2 | **ItemImage as inline flow element reserves zero space** — icons overlap labels and neighbouring lines; line height not expanded | `text/inline-game-tags` render 112406; bounds JSON: all 8 `LytItemImage` nodes report `w=0, h=0` | Inline item-image size metadata not propagated to the flow layout (same family as pitfall PF6) | `OPEN` |
| R2-3 | **FileTree `{:iconItem=}`/`{:iconPng=}` icons misplaced** — icons missing, overlapping row text, or drawn below the row spilling outside the block | `code/special-langs` render 112406 (inventory tree; diamond icon spills onto next section) | Same zero-size family as R2-2 | `OPEN` |
| R2-4 | **Inline LaTeX much improved but visually heavy** — formulas render noticeably larger than surrounding text; tall formulas expand line height aggressively | `latex/inline` render 112407 | Inline style should render closer to text size (TeX inline vs display style); size/spacing refinement | `OPEN` |
| R2-5 | **Some errors render as normal text instead of red** — whole-page parse failures via `buildErrorPage` compile the error text as plain body paragraphs (no `ERROR_TEXT` styling); flow/block errors via `createErrorFlowContent` are red and unaffected | `PageCompiler.buildErrorPage` (:273-285) code reading; in-game observation | Error page builder bypasses error styling | `OPEN` |

### Round 2 Infrastructure Findings

| # | Issue | Details |
|---|---|---|
| D3 | **Geometric screener miscalibration masked R2-2/R2-3 for the entire round 1**: `screen.py` `ZERO_SIZE_BENIGN_CLASSES` contains `LytItemImage` (classified benign from round-1 measurements). Zero-size item images are now confirmed to be the defect, not benign. Action: remove `LytItemImage` from the benign list once R2-2/R2-3 are fixed, and add a ratchet assertion (`LytItemImage` w/h > 0). | `screen.py:56` |

### Screener Prompt Efficacy Test (2026-07-29, 4 probe pages, qwen3-vl-plus)

| Metric | Prompt v2 | Prompt v3 |
|---|---|---|
| R2-1 (mermaid font path) | Missed (symptoms flagged, misdiagnosed) | **Detected with correct diagnosis** (new category 字体渲染路径异常 ×2) |
| R2-2 (ItemImage overlap) | Detected | Detected, evidence anchored to page INVARIANTS |
| R2-3 (filetree icons) | Detected | Detected, evidence anchored to page Expected text |
| Findings / est. false positives | 25 / ~15 | 26 / ~14 |

Prompt v3 changes: new category 字体渲染路径异常; instruction to use in-page `Expected:` text as the authoritative intent baseline; spoiler black bars / TEST GOAL headers declared intended; anti-FP rules (tree indentation, section headings). Remaining FPs are low-cost adjudication noise. Geometric layer: 0 findings on all 4 pages (see D3).

---

## B. Not Reproduced → In-Game Verification Required (Gold Standard)

| # | Observation | Notes |
|---|---|---|
| B1 | K1 Giant `§`: 50 consecutive `§` codes all normal size, **not reproduced** | Requires in-game comparison against main guide `markdown.md` |
| B2 | K2 Thematic break through text: `headings` page separators clean, **not reproduced** | Requires in-game verification |
| B3 | Entity Y-offset: sheep/creeper/player on ground in entities page, **not reproduced** | Requires in-game verification |
| B4 | GameScene tab internal float fills entire row, text does not wrap | Weak evidence, marked suspect pending recheck |
| B5 | FloatingImage `align` accepts only `left`/`right` (`center` triggers engine validation error) — design limitation or engine constraint? | To be decided during fix phase |
| B6 | Six wrap modes: `tight`/`through` render identically to `square` (`FloatingImageCompiler` does not distinguish semantics) | Violates `ContentWrapMode` semantics; pending fix-phase comparison against `LytFloatAwareBlock` path |

---

## C. Fixture Defects

| # | Issue | Status |
|---|---|---|
| C1 | `clear="both"` illegal value ×47 occurrences | **Fixed and verified** (re-rendered, error text gone) |
| C2 | `<FloatingImage>` missing x/y causes compile error | **Fixed and verified** (9 occurrences patched, errors gone) |
| C3 | `content-tabs.md` header contains bare inline tag text | **Fixed and verified** (compile errors gone; A3 overlap remains as engine issue) |
| C4 | `headings.md` `---` as standalone line | **Fixed** |
| C5 | `entities.md` scene width fix (must be string form; `{240}` expression rejected) | **Fixed**; residual: zombie/skeleton still not rendering (suspected entity spawn failure, fixture improvement item) |
| C6 | `cjk/` `tables/` `headings/` "extra long" cases not long enough to trigger wrapping | Lengthen (low priority) |
| C7 | `stress` `{: widths=}` literal output | **Confirmed engine issue** (same root cause as A14: `tables/metadata` also literal) |

---

## D. Infrastructure Issues (Offline Rendering)

| # | Issue | Details |
|---|---|---|
| D1 | Batch OOM: full 63-page batch runs out of heap at ~page 42 | NEI-worker thread OOM first; 41/63 succeeded, consecutive failures after. Suspected cross-page resource leak (world, textures, NEI artifacts). **Mitigation**: batch size ≤ 40 pages |
| D2 | Screener treats `*_overlay.png` as independent pages | 125 vs 63 pages, cost doubled plus noise → `screen.py` must filter overlay files |

---

## E. Systematic VLM False Positives (Screener Prompt Tuning)

| Issue | Frequency | Description |
|---|---|---|
| Right-edge text "hard crop" | ~11 findings / 10 pages | Normal text reaching page width boundary + tile boundary artefact |
| Tile seam artefacts | ~20 findings | Seam between adjacent VLM tiles reported as layout defect |
| Bounds overlay auxiliary lines misinterpreted as content | Several | Overlay renders block outlines that VLM reports as rendering errors |

**Tuning actions**:
- Right-edge crop: report only when text is visibly halved at a non-boundary position.
- Overlay files: excluded from VLM scan.
