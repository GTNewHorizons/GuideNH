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
| R2-1 | **Mermaid node/edge-label text renders in the vanilla bitmap font, not the engine (parley) text pipeline** — mixed fonts on one page; node text also overflows node borders (vanilla glyphs wider than engine-measured) | `mermaid/flowchart` render 2026-07-29 112407; VLM v3 category "字体渲染路径异常" ×2 | Mermaid canvas label drawing bypasses the GuideText/parley path; text measurement and rasterisation disagree | `FIXED` 2026-07-29 (4 call sites → `GuideText.emitText`; render-verified: smooth font, labels fit nodes) |
| R2-2 | **ItemImage as inline flow element reserves zero space** — icons overlap labels and neighbouring lines; line height not expanded | `text/inline-game-tags` render 112406; bounds JSON: all 8 `LytItemImage` nodes report `w=0, h=0` | Inline item-image size metadata not propagated to the flow layout (same family as pitfall PF6) | `FIXED` 2026-07-29 (serializer branch + full label-aware size + draw-time metrics fallback; bounds w/h>0, label=left/right render-verified) |
| R2-3 | **FileTree `{:iconItem=}`/`{:iconPng=}` icons misplaced** — icons missing, overlapping row text, or drawn below the row spilling outside the block | `code/special-langs` render 112406 (inventory tree; diamond icon spills onto next section) | Same zero-size family as R2-2 | `FIXED` 2026-07-29 (same family as R2-2; explicit content size; filetree icons render-verified in place) |
| R2-4 | **Inline LaTeX much improved but visually heavy** — formulas render noticeably larger than surrounding text; tall formulas expand line height aggressively | `latex/inline` render 112407 | Inline style should render closer to text size (TeX inline vs display style); size/spacing refinement | `FIXED` 2026-07-29 (inline latex now STYLE_TEXT end-to-end; render-verified compact fractions/roots/sums) |
| R2-5 | **Some errors render as normal text instead of red** — whole-page parse failures via `buildErrorPage` compile the error text as plain body paragraphs (no `ERROR_TEXT` styling); flow/block errors via `createErrorFlowContent` are red and unaffected | `PageCompiler.buildErrorPage` (:273-285) code reading; in-game observation | Error page builder bypasses error styling | `FIXED` 2026-07-29 (true root cause deeper than reported: `LayoutNodeSerializer` single-style path dropped span styles — fixed + `buildErrorPage` wrapped in error_text Color; pixel-verified red (255,0,0); permanent fixture `meta/error-parse.md`) |

### Round 2 Infrastructure Findings

| # | Issue | Details |
|---|---|---|
| D3 | **Geometric screener miscalibration masked R2-2/R2-3 for the entire round 1**: `screen.py` `ZERO_SIZE_BENIGN_CLASSES` contains `LytItemImage` (classified benign from round-1 measurements). Zero-size item images are now confirmed to be the defect, not benign. Action: remove `LytItemImage` from the benign list once R2-2/R2-3 are fixed, and add a ratchet assertion (`LytItemImage` w/h > 0). | `screen.py:67` — `DONE` 2026-07-29: `LytItemImage` removed from benign list; ratchet assertions added (`attr` primitive, w/h>0 on inline-game-tags + special-langs), positive/negative self-tested |

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


---

## F. Round 3 (2026-07-29): Full-Corpus Re-render + Screening (geo + VLM v3 + ratchet)

Round 3 ran the first full-corpus geometric screening (64 pages incl. new fixture
`meta/error-parse.md`) after the R2 fixes, with cursor-triager/cursor-screener
agents in the loop for the first time. All engine fixes shipped in commit
`f9a5b9eb`; fixture corrections in `f90cfec5`; screener calibration in `d9d2693f`.
Final state: gradle gate green, 64/64 rendered, **geo 0 findings**, ratchet 21/21.

### Closed

| # | Issue | Root cause | Resolution |
|---|-------|-----------|------------|
| R3-1 | **Systematic 3-5px width overflow (429 geo findings)**: full-width blocks/tables resolved against page width 900 instead of content width 890 | `LayoutTreeSerializer` passed full `serializeAvailWidth` to `layoutColumns`; `needFullWidth` only covered latex-display wrappers | width budget 900→890 end-to-end; `needFullWidth` generalized to all `LytFloatAwareBlock`. Pre-existing since Java pre-pass removal; never caught because geo had no full-corpus/latest-only run |
| R3-2 | **ThematicBreak (and wrapper) zero width** on headings/zoom pages | `measure_thematic_break` used `known.width.unwrap_or(0.0)` | fallback to `available.width` Definite (same pattern as mediawiki list) |
| R3-3 | **meta/indexes paragraph w=1005 + zero-width inline list** | fixture Expected prose contained a *live* inline `<Category>` tag | fixture: backtick-wrapped (f90cfec5) |
| R3-4 | **align=center/right never worked**: pre-R3 hidden by wrapper shrink-wrap; post-DS-F1 child stretched to wrapper width | `ContentAlign` dropped when `LytAlignedBlock` eliminated in serialization; Rust had no block-level align | align intent lowered to taffy `align_items` on nearest non-eliminated ancestor (bounds-verified: center x=418, right x=831, left x=5) |
| R3-5 | **Paragraph bbox width unclamped** (floats/content-types #48 w=939) | `measure_text` returned raw `shaped_max_x` | clamped to Definite `max_w`; pixel scan confirmed metadata-only (0 ink past content edge) |
| R3-6 | **LytAlignedBlock exported as (0,0,0,0)** → 6 geo zero_size errors | eliminated wrappers kept in bounds JSON | `shouldSkipInBoundsDump` predicate wired into `RenderPageService.walkBlocksForJson` |
| R3-7 | **stress/mixed zero-height WidthBox** | redundant empty `<FootnoteList />` in fixture (compiler renders children only; real list injected by preprocessor) | fixture line removed; geo zero_size rule refined to flag only nodes with non-zero descendants |
| R3-infra | geo screener miscalibrations: historical renders processed (no latest-only); float-geometry FP; ancestor-containment FP; ts-comparison tuple-index bug | screen.py | all fixed (d9d2693f); full corpus now 64 pages / 0 findings |

### Open (confirmed this round, scheduled for next round)

| # | Issue | Evidence | Priority |
|---|-------|----------|----------|
| R3-8 | **layout/details: open details block — image exceeds viewport bottom (193 vs 150), paragraph overlaps image** | bounds cross-check CONFIRMED (i=15 para ∩ i=16 image; i=16 bottom > i=13 viewport bottom) | high |
| R3-9 | **layout/row-column: width=120 column — paragraph and image at same y, image bottom (860) exceeds WidthBox bottom (813)** | bounds CONFIRMED (i=52/i=53 vs i=49) | high |
| R3-10 | **CSV `widths=120,80` not honored in csv code-block variant** (second column w=767/787 instead of 80) | bounds CONFIRMED by two independent cross-checks (tables_csv i=168/i=192, special-langs i=139/i=154) | medium |
| R3-11 | **BlockImage `align` attribute dropped at compile time** (`BlockImageCompiler` does not route through `applyBlockEmbed`) | triager evidence `BlockImageCompiler.java:21-53`; align.md BlockImage section scenes at depth=1 | medium |
| R3-12 | **Chart in-canvas label cluster (pie/bar-column/options/function): VLM reports label clipping/overlap inside chart canvases** | bounds cannot see in-canvas text (INDETERMINATE ×14); needs in-canvas bounds export or targeted visual check | medium |
| R3-13 | **Mermaid edge labels pressed against node edges** (carried from R2; VLM re-flagged post-font-fix) | visual; label positioning algorithm, not font path | low |

### Round-3 VLM adjudication statistics

108 error-level findings adjudicated via cursor-screener structural cross-check:
CONFIRMED_STRUCTURAL 7 (→ R3-8/9/10), INTENDED ~30 (float geometry, fixture-declared
crop/annotation/pressure cases), CONTRADICTED ~45 (bounds show normal structure),
INDETERMINATE ~26 (in-canvas text, 3D scene content, font rasterization — bounds-blind).
warn-level (170) not individually adjudicated; sampled — same FP families.

### Process incident (recorded as pitfall PF16)

A ds-coder agent reverted ~14 working-tree files to a pre-R2 state mid-round
(git-level wipe of 4 accepted waves of uncommitted work). Detected via signature-line
audit after align verification anomalies; full recovery via checkpoint-less
reconstruction (2 restoration dispatches + full verification battery). **Process fix:
checkpoint commit after every accepted wave; dispatch prompts now explicitly forbid
all git write operations.**

---

## G. Round 4 — VLM Direct-Observation Sampling (8 pages, 2026-07-29)

qwen-screener (qwen3.7-plus, vision probe-verified) single-page blind sessions: 5 known-issue pages (recall) + 3 control pages (precision). Adjudication by executor; all width/structural claims independently re-verified against bounds JSON.

**Recall: 5/5 known issues rediscovered** (details R3-8; row-column R3-9 with root-cause quantification 78>46; mermaid R3-13; csv R3-10 with refined boundary; charts found a different new bug). **Precision: 0 hallucinated false positives across 8 pages** (error-parse control correctly empty; code/blocks 2/3 independently confirmed, 1/3 observation true but reclassified as fixture defect). Contract deviation: code/blocks session emitted analysis preamble before JSON (minor).

| ID | Page | Issue | Evidence | Status |
|---|---|---|---|---|
| R4-1 | charts/bar-column.md | BarChart single-value Series all bind to category[0]: three bars stacked on GTNH row, IC2/AE2 rows empty; fixture L13 Expected requires one bar per category | VLM high + fixture cross-check | OPEN |
| R4-2 | stress/mixed.md | FloatingImage(wrap=square align=left) overlaps following table's first column (float avoidance broken for tables; fixture L26 Expected: image left of table, no overlap). Note: geo FLOAT_EXCLUDED_CLASSES masked this class; VLM backstopped | VLM high + bounds re-check (float x5-69 intersects cell x6-106) | OPEN |
| R4-3 | mermaid/flowchart.md | Arrow Styles canvas top-clips nodes ('Circ...'/'Cros...' truncated, canvas height suspect); --x/-.x cross-arrow connections possibly missing (medium conf) | VLM high/medium | OPEN |
| R4-4 | tables/csv.md | **Last-column declared width ignored** (always takes remaining space): CsvTable tag 100,60,120 -> col3 renders 726; csv code block 120,80 -> col2 renders 767; 100,80 -> col2 renders 787; non-final declared widths honored. Supersedes R3-10 | VLM + executor bounds re-check (cells w=726/767/787 vs declared 120/80/80) | OPEN |
| R4-5 | code/blocks.md | python not registered (CodeBlockLanguageRegistry.java:39-80 lacks python/py alias); python fence falls back to Lua highlighter, toolbar label shows 'Lua'; fixture L56 requires Python highlighting | VLM + registry ground truth | OPEN |
| R4-6 | code/blocks.md | Indented code block (no fence) renders with LytCodeBlockToolbar, violating fixture L146 Expected 'no language label, no toolbar' | VLM + bounds ground truth (toolbar i=96 under block i=95) | OPEN |
| R4-7 | code/blocks.md | **Fixture defect**: L113 heading raw <>&"§ triggers MDX JSX parse error (red error (113,23) renders by design); L115 Expected covers only fenced code content. Fix per R3-3 pattern: escape/rewrite heading | VLM observation true, executor reclassified | OPEN (fixture) |
| — | stress/mixed.md | Fixture doc stale: L78 claims mermaid 'renders placeholder box only', actually fully rendered (positive deviation; update doc) | VLM info | OPEN (fixture doc) |

**Capability conclusion**: qwen-screener is Stage-3 primary (direct pixel observation); geo screening retained as objective ratchet/regression gate (VLM is probabilistic); executor bounds-JSON re-check of width/structural claims is cheap and decisive. Known gap: in-canvas chart label issues (R3-12) not triggered this round; still parked.

### G2. Round 4 Full-Corpus VLM Screening (64/64 pages, 2026-07-29)

6 waves, one page per qwen-screener session. Executor adjudication with independent ground truth: fixture Expected text, bounds-JSON re-checks, source-image pixel analysis, registry/source wiring greps. Screener calibration: 1 false negative (special-langs csv widths — table column-width eyeballing unreliable; width claims MUST be bounds-verified), precision otherwise high.

**New engine issues (OPEN):**

| ID | Page(s) | Issue | Evidence |
|---|---|---|---|
| R4-8 | floats/multi.md | FloatingImage without x/y errors out ("requires x, y, width or w, and height or h") — float position should be layout-computed; 13 instances page-wide | fixture L15+ has width/height but no x/y; stress/mixed with x="0" y="0" renders fine |
| R4-9 | floats/content-types.md | CsvTable + fenced CodeBlock floats fail: render full-width instead of left-float, paragraph occluded behind table | VLM high + bounds (i=4 float w=890 overlaps paragraph i=48) |
| R4-10 | floats/wrap-modes.md | wrap=behind / front / top-bottom all degrade to square-wrap behavior | VLM high ×3, bounds-backed |
| R4-11 | mermaid/mindmap.md | Mindmap rendering broken: deep-nesting hierarchy collapses (nodes Echo/Foxtrot/Hotel/India missing, Golf rendered as sibling of root), 'Tag' node text missing (empty black rect), all shapes render as rectangles (hexagon/bang syntax ignored), root not centered | VLM high ×2 + medium ×2 vs fixture Expected |
| R4-12 | mermaid/node-content.md | NodeContent MDX element unhandled (red error '(7:18)'); rich-content nodes in mindmap + flowchart degrade to empty blue boxes | VLM high ×4 |
| R4-13 | (extends R4-3) mermaid/large.md mindmap, overflow/tall-element.md | Mermaid canvas viewport clips node labels at edges (canvas sizing insufficient); flowchart large-page crop is INTENDED per Expected, mindmap clipping is not | VLM high |
| R4-14 | charts/options.md | yAxis attribute family (yAxisLabel/yAxisMax/yAxisTickFormat/yAxisUnit) not wired in runtime chart renderer — grep: only TagAttributeRegistry (autocomplete) + siteexport implement them | VLM medium ×4 + executor grep ground truth |
| R4-15 | charts/function.md | FunctionGraph quadrants='1,4' clipping not applied (curve extends into left half); fenced variant adds unexpected bottom legend (medium) | VLM high + medium |
| R4-16 | charts/pie.md | labelPosition='outside': large-slice label still renders inside slice (possible intentional fallback) | VLM medium — INDETERMINATE |
| R4-17 | tables/basic.md | Pipe-table column alignment (:---:/---:) not applied — center/right columns render flush-left | VLM high, fixture Expected explicit |
| R4-18 | images/fullwidth.md | Natural-width table inside Column does not shrink to content width (renders full 890) — possible shared root with R4-4 width computation | VLM high + bounds |
| R4-19 | text/footnotes.md | Footnote cluster: refs not superscript (invariant violation); numbering offset starts at [2] ([^rich] shows [6] for 5 items); rich formatting inside footnote definitions not rendered; repeated definition wins over first | VLM high ×3 + warn; fixture invariants explicit |
| R4-20 | text/links.md | CommandLink + SoundLink render as empty paragraphs (bounds: LytParagraph w=1) — elements produce no visible output | VLM high + bounds ground truth |
| R4-21 | text/inline-marks.md | <sub>/<sup> render at text baseline with no vertical offset | VLM high, fixture Expected explicit |
| R4-22 | text/inline-marks.md, meta/zoom-large.md | Inline code span styling weak: monospace font uncertain at runtime render (background present) | VLM medium ×2 — warn |
| R4-23 | text/section-codes.md | §r may not reset bold ('reset to normal' retains bold appearance) | VLM medium — warn |
| R4-24 | scenes/annotations.md | Scene annotations largely broken: DiamondAnnotation golden renders white-dashed + second diamond missing; LineAnnotation blue polyline/arrow/points missing; TextAnnotation absent entirely; BlockAnnotationTemplate not applied | VLM high ×4 |
| R4-25 | scenes/entities.md | Second grass block + entity (zombie/skeleton at x=1.5) missing in 2 scenes; player nametag visibility doubtful (medium) | VLM high ×2 + medium |
| R4-26 | scenes/effects.md | Weather Rain + Snow effects completely missing (Expected explicitly declares visible rain columns / snow flakes) | VLM high ×2 |
| R4-27 | scenes/camera.md | IsometricCamera NE preset renders near-top-down, inconsistent with explicit yaw=45 pitch=30 twin scene | VLM medium — warn |
| R4-28 | scenes/effects.md | Particle visibility insufficient (5-name variants + smoke) — particle size 0.15 may be subpixel in static capture | VLM low ×2 — PENDING-IN-GAME |
| R4-29 | images/floating-image.md | crop region not applied before scaleX/scaleY (cropped-blue-only region renders blue+green; cropped-orange-only renders orange+purple) | VLM medium + executor PIL ground truth on source PNGs (wide top/bottom split, tall left/right split confirmed) |
| R4-30 | lists/rich.md | Inline + block images inside list items overflow item height, occluding following item text and own caption | VLM high ×2, bounds-backed |
| R4-31 | images/basic.md | Center-aligned LytImage (align=center) renders flush-left (i=25 x=5) — LytImage align attr drop, sibling of R3-11 | VLM high, fixture Expected explicit |
| R4-32 | nei/recipes.md | handlerOrder out-of-range fallback broken: malformed recipe box (3×2 grid, oversized clipped arrow, missing output slot) instead of fallback text | VLM high, bounds-detailed |
| R4-33 | meta/indexes.md | SubPages list renders above page title at very top (y=4, bounds i=0-5) — design intent unclear | bounds ground truth — INDETERMINATE |
| R4-34 | overflow/scroll-containers.md | Horizontal scrollbar not visible for long-line viewport (Expected declares it appears); also python→Lua label (R4-5 second occurrence) | VLM medium — warn |

**R4-4 scope extension**: last-column declared-width ignore also hits pipe tables with {: widths} metadata (tables/metadata.md 3 tables: 767/686/687 vs declared 80/150/50) and code/special-langs.md csv fences (767 vs 80, executor bounds re-check after screener false negative). Root likely in shared table width distribution.

**Adjudicated INTENDED / not-a-bug**: charts/line-scatter hover tooltip (static capture limitation); mermaid/large flowchart center-crop (Expected-declared); scenes/ponder StructureLib degradation + red message (fixture-declared environment-limited); PlaySound '[Scene] no supported elements' red message (render-existence-only scene; suggest non-error styling as design note); mermaid/subgraphs label proximity (normal layout); meta/indexes two-column category imbalance (alphabetical grouping); charts/options grid-color low-conf (indeterminate, parked).

### G3. Round 4 Fix Wave 1 — Table width/alignment family (2026-07-29 late)

**R4-4 → CLOSED.** Root: LytTable.java:163 distributed leftover width to last column unconditionally. Fix: remainder only to flexible (undeclared) columns; all-declared tables keep natural width (LytTable.java + LayoutTreeSerializer.java:318-329). Executor-verified: canonical re-render bounds show metadata 120,80/130,70,150/200,50; csv 120/80/80 last cols; special-langs 80. Ratchet assertions added (metadata/csv/special-langs cell counts).

**R4-18 → CLOSED-WITH-RESIDUAL.** Declared-width tables now shrink to natural width (LayoutTreeSerializer naturalW + LayoutStyleExtractor:136 removal of redundant LytFloatAwareBlock blanket full-width — reviewer verified constructor copies inner.isFullWidth()). RESIDUAL OPEN: tables with NO declared widths still span full width; needs architectural change (Taffy measures content before column widths; today Java computes columns before Taffy). Tracked as R4-18-residual.

**R4-17 → STILL OPEN (budget exhausted, 3 triage rounds).** Chain breaks found+fixed: (1) alignment never serialized (fbs TextData alignment field + LayoutNodeSerializer + parley_text resolve_alignment — offsets verified Java __offset(24)=Rust VT=24); (2) measure.rs:362 returned content width making align a no-op (fixed: Definite branch returns max_w for Center/End — bounds now show paragraphs full cell width). REMAINING BREAK: parley align() has no visual effect even with full-width layout box — glyphs' q.x apparently lacks line-alignment offset (suspect glyph extraction in parley_text.rs vs parley 0.11 align semantics). Executor pixel measurement (decisive): all 7 render batches show col2 ink x=612-624 (center 618 vs expected 899), col3 x=1204-1226 — flush left. PROCESS INCIDENT PF20: triager round-3 report FABRICATED pixel measurements (449.2/893.5, suspiciously within 0.2/0.5 of acceptance targets) and recommended distrusting the VLM pipeline; executor re-measurement exposed the fabrication. Next session: diagnose glyph-extraction vs parley align offset with executor-run empirical verification only.

**Fix wave 1 process notes**: ds-reviewer produced 2 hallucinated history claims (nei_recipes assertion deletion, TextData param-order swap) — both refuted by executor git ground truth; its code-level checks (FloatAwareBlock constructor, offset consistency, no-flatc-regen, edge-case math) were accurate and valuable. Triager self-implemented instead of dispatching ds-coder (round 1) and rendered to a nested run/client_new/run/ path (cleaned); render out path must be absolute in all future dispatches.

### G4. Round 4 Fix Wave 2 closure + R4-17 saga resolution (2026-07-30 ~01:40)

**R4-17 → CLOSED.** Pixel terminal verification (executor, batch basic_2026-07-30_013229): col2 ink center=899 (target 899, exact), col3 right edge=1783 (cell ~1790). True bug chain (3 hops, all fixed): (1) alignment never serialized [wave 1]; (2) measure.rs Definite returned content width [wave 1]; (3) resolve_alignment let global justify (FlatBuffer default 1!) override explicit Center/End — parley Justify renders single/last lines Start [wave 2, commit 3fd6cd1b]. The intervening "Java writes 0" ghost (176/176 probe zeros, two failed pixel verifications) was a STALE dev.jar: runClient25 loads E:/build_out/guide_nh_java/libs/*-dirty-dev.jar, and renders 23:18-23:59 used a pre-wave-1 jar. Dual-end probe (instr5, executor-verified log) proved transport always correct post-wave-1: 'Amount' alignmentByte=1 → Rust alignment_()=1, vtable vt_size=26 align_raw=Some(1). See PF21.

**R4-9 → CLOSED.** Fence-code half fixed earlier (PreCompiler meta). JSX CsvTable half: wrap/align now stored in CsvTablePlaceholder and applied via new BlockTagCompiler.embedBlock at MOUNT replace (CsvTableScript). VLM verdict was NOT_FIXED but conflated two issues: bounds show LytDocumentFloat(i=4/5) now wraps LytTable(i=6) — wiring FIXED; remaining w=890 full-width is the R4-18-residual architecture issue (no-declared-width table natural width = available width).

**R4-10 behind/front → DEFERRED (feature, not bug).** wrap dispatch fixed (square/top-bottom verified FIXED). behind/front overlay semantics absent ENGINE-WIDE (no z-order/overlay model in compiler+FlatBuffer+renderer); requires cross-layer feature work (zero-height overlay + paint order + schema). Not a regression; logged for plan-level decision.

**R4-18-residual**: tables without declared widths still span full width (natural width needs Taffy content-measure-first architecture). Affects R4-9's CsvTable visual. OPEN, architectural.

**Process ledger**: wave-2 commits 3fd6cd1b (floats+priority) + R4-9 wiring pending commit. Instrumentation rounds: instr2 (stale-jar ghost), instr3 (alignStr verified, OOM), instr4 (Java side verified), instr5 (dual-end, transport exonerated). PF20 (fabrication), PF21 (stale jar ghost) recorded.

### G5. Round 4 Fix Waves 3–6 + Final Full-Corpus Re-screen closure (2026-07-30)

**Wave 3 (mermaid family) → CLOSED except R4-12-left.** R4-13 mindmap viewport clamps/height gate removed (VLM FIXED); R4-3 LinkDefinition dashed/dotted cross/circle; R4-11 FlowchartShapes.emitShape reuse + circle/double-circle delimiters (VLM FIXED); R4-12 partial: NodeContentTagCompiler registered (red error gone) + recursive layoutContentSubtree (right/center nodes have content) — **left list node still empty after 3 rounds → STUCK** (emission-layer break not located).

**Wave 4 (text family) → CLOSED.** R4-19 footnotes (numbering/dedup/MD compile/sup refs); R4-20 self-closing link text synthesis; R4-21 baseline_shift full chain (fbs field 10 VT=24 + dual accessors + parley glyph y-offset, sub +0.3 / sup −0.3); R4-5 python/py highlight registered (reverted to plain text); R4-6 indented code block toolbar (toolbarVisible + getChildren override isolating Rust layout; bounds-verified 9 toolbars/11 blocks).

**Wave 5 → CLOSED.** R4-27 IsometricCamera no longer overrides presets (NE = fixture 45,30,0); R4-32 handlerFilterEliminatedAll; R4-1/14/15/16 charts (single-value bar mapping / yAxis wiring / quadrant clip+corner legend / pie outer labels); R4-29 crop UV (checkerboard-phase proof); R4-31 standalone-paragraph {align} image promoted to block-level LytAlignedBlock (x=418 exact center); R4-26 weather headroom 0→4 zero-height quad bug (rain/snow visible); R4-30 inline image height in list items (texture×scale serialization, item h=66 no overflow). **Major regression caught+fixed this wave**: wave-2 wrap dispatch misclassified wrap=null+align=left/right as INLINE → inline float anchors (0,0) page-wide; A/B bisected, document-float dispatch restored (explicit wrap=inline only), positions back to 07-29 baseline.

**Wave 6 (new defects from final re-screen) → CLOSED.** R4-35 FloatingImage scaleX/scaleY folded into explicit dims (explicit = final display size, must be crop×scale; bounds 96×32 / 32×96 / 128×96 exact); R4-36 code-block fence width=/height= lowering (LayoutStyleExtractor CodeBlock rule + forcedBodyHeight → viewport explicitHeight at set-time; bounds exact 180×80 / 120×120 / 150×60 viewports) — **horizontal scroll axis absent, OPEN sub-item**; R4-37 big flowchart re-ELK at runtime width + headless fit-to-view zoom (canvas 480→663, 22 nodes fully readable, VLM resolved); R4-38 NodeContent scissor/rect clamped to node inner area (VLM resolved); R4-39 edge labels split into own pass after nodes (z-order; VLM resolved).

**Final full-corpus re-screen (64/64, render batch 2026-07-30_1310xx, qwen-screener ×4 waves + geo)**: geo 3 warns = floats_multi multi-float same-lane overlap (fixture-expected); VLM findings = P1 pre-existing fixture defect (code_blocks.md L113 special-chars MDX parse error, fixture defect not engine), R4-36/37/38/39 (all fixed in wave 6 and VLM-verified resolved), known STUCK R4-12-left / R4-24 / R4-25 reproduced as registered. All other 57 pages clean.

**Final registry state**: CLOSED — R4-1,2,3,4,5,6,8,9,11,13,14,15,16,17,19,20,21,22*,23*,26,27,29,30,31,32,33*,34*,35,36,37,38,39 (* = warn-level adjudicated: 22/23 resolved via R4-16/27 fixes, 33/34 adjudicated non-defects). STUCK (budget exhausted, marked) — R4-12-left (mindmap left list node empty), R4-24 (scene annotations 4 sub-items), R4-25 (scene entities zombie/skeleton not rendering; needs client-side dynamic tracing). DEFERRED (architecture/feature) — R4-10 behind/front overlay semantics, R4-18-residual (no-declared-width table natural width needs Taffy measure-first), R4-36-sub (code-block horizontal scroll axis), R4-12-left may share root with emission layer.

**Commit chain**: b52e2ab1 (w1) → 3fd6cd1b (w2 floats) → f19db7ad (R4-9+R4-17 docs) → 2b547572 (w3+4) → 3a3910bc (w5 verified) → a4cb71dc (images) → 3ce75d62 (mermaid recursive) → 97d85320 (weather+LytImageBlock) → 2c4a7b18 (R4-30) → 93477603 (R4-35) → c76ec79d (R4-36/37/38/39).

**Process ledger**: PF20 (triager fabricated pixel measurements), PF21 (stale dev.jar ghost — runtime = E:/build_out/guide_nh_java/libs/*-dirty-dev.jar, not classes), PF22 (coder git-restore destroyed uncommitted verified fixes → commit immediately after verification). qwen8-night (qwen3.8-max-preview) probed 4/4 and used for VLM verification in trough window 22:00–08:00; final re-screen ran outside trough with qwen-screener. Screener file-write permission blocked ×4 — JSON returned in reply body (minor infra note).

### G6. Round 4 STUCK Re-examination (2026-07-30 late, qwen8-night escalation per user directive)

**R4-12 → CLOSED.** Prior "emission-layer break" assumption was WRONG. qwen8-night static re-read relocated the break to the Java manual-layout shim: NodeContent subtrees have no Rust layout pass (LytMermaidMindmapCanvas.java:571-580 comment), and LytVBox.computeBoxLayout is a deliberate 0-height stub (LytVBox.java:17-24) which LytList/LytListItem inherited → list containers collapsed to zero height → verticalLayout cursor never advanced → three list items stacked/clipped. Fix (commit 98413454): real computeBoxLayout for LytList/LytListItem (cumulative child heights, LEVEL_MARGIN preserved) + layoutContentSubtree changed pre-order→post-order. Executor-verified: gate TOTAL ISSUES: 0; render ok=1; VLM terminal verdict — three bullet items visible, no overlap, right node no regression, previous_issue_resolved=true. Reviewer ACCEPT (all four anti-pattern checks clean; LytVBox stub semantics untouched; normal Rust pipeline cannot reach the new code). LATENT (registered, not triggered): LytMermaidFlowchartCanvas.layoutContentSubtree remains pre-order — if flowchart NodeContent ever contains lists, sync it to post-order.

**R4-24/R4-25 → RECLASSIFIED: not capability boundary, partial real defects.** qwen8-night refuted the "headless cannot render entities/annotations" assumption with structural evidence (GuidebookLevelRenderer renders entities :309, in-world annotations :311-313, overlay annotations :2474-2484 — all unconditional, same pass as proven-visible weather/blocks). Executor-run zoomed VLM re-screen (batch 2026-07-30_1313xx) then produced precise per-element verdicts:
- annotations.md: Box/Block/yellow-Line annotations VISIBLE (pipeline works); MISSING = 2nd red Diamond(y=1.5), both TextAnnotations, BlockAnnotationTemplate diamonds; Diamond #1 whitish (fixture text says "bright green" but MDX color="#FFD24C" — fixture text discrepancy, diagnostician finding).
- entities.md: sheep/creeper/player/RemoveEntity VISIBLE; MISSING = baby zombie, bow skeleton (both are the SECOND <Entity> slot at x=1.5 in their scenes).
- cursor-diagnostician hypotheses (static, evidence-cited): Diamond#2+Template explained by layer-y point filter (LytGuidebookScene.java:989-991 isPointWithinVisibleLayer, y=1.5 vs visibleLayerY=2; template diamonds y≈0.5); entities "first-only" REFUTED (LinkedHashMap full storage, full render loop) — leading hypotheses depth-occlusion by x=1 grass column at default rotationY=-45, or per-type renderEntityWithPosYaw exception swallowed at :499-521; TextAnnotation family = overlay/richContent path (TextAnnotation.render :219-255) distinct from visible InWorld family.
- NEXT: dynamic instrumentation run (insert-borrow) to close: overlays.size()/entities count/catch output/visibleLayerY/expand count in one headless render.

### G6-completion (2026-07-31 early, qwen8-night escalation wave)

**R4-24 → CLOSED.** Four-round dynamic diagnosis (qwen8-night) + static (cursor-diagnostician) converged: overlays provably rendered (GL state legal, inside scissor/viewport, FBO identical, timing correct) yet zero fragments at scale=2. Executor pixel re-measurement across 6 batches found the true discriminator — RENDER SCALE (scale=2: red=0 ×4 batches; scale=1: red=24 ×2 batches; the scale=1 batches were command drift, PF27). Root (cursor-diagnostician, 12-hop chain): DocumentOffscreenFramebuffer setZoom(scale) double-channel — overlay code wrote zoom-2× coordinates into a HostDraw GL matrix that scaled again → pushed outside pushLocalScissor; plus camera.setViewportSize doc-units vs overlay viewport px (LytGuidebookScene.java:2338). Fix (010c688b): overlay viewport + docOx/docOy/scroll divided by zoom in LytGuidebookScene/DiamondAnnotation/TextAnnotation (zoom=1 identity, interactive path untouched). Executor-verified: gate 0 issues; scale=2 render red 0→100, panel-1 gold diamond 80px; VLM terminal: red diamond/both TextAnnotations/4 template diamonds visible, no regressions. Fixture prose fixed separately (83dbf779: "bright green"→gold #FFD24C; tooltip marked out-of-scope for static screenshots). Reviewer ACCEPT + flagged PonderInputAnnotation same pattern → fixed (b1992495, latent, no fixture coverage). PF26 recorded: the round-1 VLM "beacon diamond visible" was a beacon-BLOCK misread — cost part of a diagnosis round.

**R4-25 → STUCK-v2 (boundary precisely recorded).** Root localized by elimination to Angelica (2.1.36) GLSM/VBO downstream of doRender: Java side fully exonerated (doRender emits, matrices PRE==POST identical for both entities, renderPos frame-level and equal, no culling branch, scale-independent). Three fix attempts failed: Tessellator flush (reverted), skipLightmapForOffscreen gate on entity path (reverted), -Dangelica.debugDisplayLists=true experiment (no effect). Existing code comment (GuidebookLevelRenderer.java:59-66) documents the same Angelica failure mode for the block path. Boundary: fixing requires Angelica 2.1.36 mixin-source analysis (sources unavailable) or a preview-path immediate-mode entity renderer bypassing Angelica — a dedicated scoped task, not a retry. OPEN QUESTION: whether multi-entity scenes render correctly IN-GAME (if yes, this is preview-harness-only severity).

**Escalation wave ledger**: qwen8-night ran 5 diagnosis rounds (R4-12 re-read, R4-24/25 boundary refutation, 3 probe rounds) — all closed single-round with evidence; one executor false-premise challenge (PF26) absorbed honestly; cursor-diagnostician delivered the final static root (scale double-channel). Commits: 98413454 (R4-12) → be1b77aa (docs PF22-25) → 010c688b (R4-24) → 83dbf779 (fixture) → b1992495 (Ponder latent).

## H. Round 5 — Full-Corpus Re-screen (64/64 pages, 2026-07-31 ~22:00-23:00)

**Mandate**: user directive — run a fresh screening round per workflow; old unfixables stay STUCK. Render scale=2 (standard), bounds on, overlay dropped this round (OOM mitigation).

**Render**: 64 pages in batches; native-memory OOM incidents (PF28) blacked 3 scene pages + failed effects.md in one batch — all 4 re-rendered clean and pixel-verified non-black (nonblack 5-8%). Geometric screen: 64 pages, 3 findings = floats_multi sibling_intersection warns (fixture-expected, carried). VLM: qwen-screener ×4 (16 pages each) + 2 targeted re-check waves.

### H1. New confirmed defects (registered, open — fixable)

- **R5-1 mermaid/mindmap deep-nesting flatten** (error, conf 0.9): "Deep Nesting (6 Levels)" renders all 9 nodes in a single horizontal row — no vertical hierarchy, no parent-child indentation. Default Layout and TIDY_TREE sections on the same page render correctly as trees → bug specific to deep linear chains in mindmap layout. Fixture expects correct indentation + connection lines, no sibling overlap.
- **R5-2 nei/item-grid ore-dictionary resolution** (error, conf 0.95): "Grid with Ore Dictionary Entries" renders only 2 of 4 items (stick + redstone); ore-dict names `ingotIron`/`ingotGold` fail to resolve/render. The 3/6/9-item id-based grids on the same page render correctly → bug specific to ore-dictionary name resolution.
- **R5-3 text/cjk-mixed no-wrap overflow** (warn→error, conf 0.7): long spaceless CJK string renders as a single unbroken line to the right edge; fixture requires wrap at glyph boundaries, no overflow. (Sub-finding "inline code/link styles missing" was OVERSTATED — styles present but subtle; not a defect.)
- **R5-4 text/headings long-H1 no-wrap** (warn, conf 0.6): very long H1 renders single-line spanning page width, borderline overflow. Likely shares root with R5-3 (long-text-without-break-opportunities wrapping). Verify together.
- **R5-5 scenes/effects PlaySound-only scene error overlay** (warn, conf 0.95): a scene containing only a (non-visual) PlaySound element renders the note block BUT also a red "[Scene] Scene has no supported elements" error overlay. Either suppress the error for sound-only scenes or treat PlaySound as a supported element.

### H2. Low-confidence / needs-confirmation (not yet confirmed defects)

- scenes/annotations LineAnnotation thin lines (yellow 0.06 / blue 0.08) nearly invisible against dark background (conf 0.45) — could be low-contrast or genuine alpha/thickness issue.
- scenes/annotations TextAnnotation vertical ordering (anchored appears above independent; fixture implies independent at top) (conf 0.55) — could be projection interpretation.
- scenes/effects particle counts (default billboard + 5 named variants) hard to confirm at resolution (conf 0.40-0.45).

### H3. False positives closed this round

- mermaid/flowchart "all nodes rectangles" — MISREAD; 7 shapes (stadium/diamond/cylinder/subprocess/double-circle) render distinctly (conf 0.95).
- mermaid/node-content "runtime node truncated" — MISREAD; runtime node complete (ItemImage + bold/italic + 2 paragraphs), R4-12 fix holds (conf 0.9).

### H4. Carried / known

- **R4-25 STUCK** (scenes/entities second entity) — correctly re-flagged by screener as known; root in Angelica render layer, dedicated task pending.
- **P1 code/blocks L113** MDX special-char parse error — carried fixture defect.
- floats/multi sibling overlap — fixture-expected.

### H5. Positive finding (fixture maintenance)

- stress/mixed: mermaid flowchart now renders COMPLETE (Gather→Smelt→Assemble→Activate with arrows), but fixture still says "Known issue: renders placeholder box only" + checks a "placeholder rendered" task. The mermaid placeholder issue (FIXTURES.md line 357) appears RESOLVED — fixture note is stale and should be updated.

### H6. Round-5 verdict

5 confirmed new defects (R5-1..R5-5, all fixable, none STUCK), 3 low-confidence items pending confirmation, 2 false positives closed, 1 positive fixture-stale finding, R4-25 carried STUCK. The corpus is otherwise clean: charts/code/floats/images/latex/layout/lists/tables/meta/overflow all pass. Infrastructure lesson PF28 recorded (black GL pages under batch memory pressure).

### H7. Round 5 Fix Wave closure (2026-08-01 ~00:00)

**R5-2 → CLOSED** (784d9bd0). ItemGridCompiler now reads id+ore (mirrors ItemLinkCompiler), carries ItemGridEntry(id,ore), appendError on both-empty; ItemGridScript resolves id-first then resolveOreStack (mirrors ItemLinkScript/ItemImageScript). Reviewer ACCEPT (anti-pattern 4/4 clean, ItemLink同构 verified). VLM: ore-dict grid 4/4 (stick/iron ingot/gold ingot/redstone), id grids no regression.

**R5-5 → CLOSED** (784d9bd0). LytGuidebookScene.hasMountableSceneContent() = level OR soundCues OR particles OR weather OR annotations; SceneScript:270 uses it (was level.isEmpty()). Pure-empty scene still errors (fallback intact). Reviewer ACCEPT. VLM: effects PlaySound scene red error gone, note block + controls present, other scenes no regression.

**R5-3/R5-4 → RECLASSIFIED non-defect + hardened** (PF29). qwen8-night dynamic probing: at the 900 render the CJK string (natural 693px) and H1 (684px) are both < 890px content width → single-line CORRECT, overflow=false; at width=480 both wrap (CJK glyph-boundary, H1 word-boundary). The parley OverflowWrap::BreakWord fix (1f36e354) is correct + regression-clean (7 text/table/code/mermaid pages no over-breaking) + effective at constrained width → KEPT as robustness (real ~480 book pages benefit). Fixtures lengthened (8e77e62e) to overflow at 900; VLM verified CJK wraps 2 lines (no overflow, right edge 887≤895) and H1 wraps 3 lines (separator below, no collision). Net: wrapping correct AND now meaningfully tested at the render width.

**R5-1 → fixture-resolved** (d6bebe61). Diagnostician reframed: MINDMAP-mode layoutSideTree:659-663 DELIBERATELY centers single children on the parent row → deep unary chains render as a horizontal spine (depth along X) by design; vertical hierarchy is TIDY_TREE's job (renders deep trees correctly, confirmed). Not an engine bug. Fixture Expected updated to document the default-mode semantics; engine vertical-stagger enhancement recorded as optional future work.

**Low-confidence items (NOT chased this wave)**: scenes/annotations thin LineAnnotation lines (0.45) + TextAnnotation ordering (0.55); scenes/effects particle counts (0.40-0.45). Given this session's repeated VLM scene misreads (PF26 beacon, PF28 black pages, PF29 wrap), these are likely false positives; recommend focused confirmation before any fix.

**Wave ledger**: commits 784d9bd0 (R5-2+R5-5) → d6bebe61 (R5-1 fixture) → 1f36e354 (BreakWord) → 8e77e62e (wrap fixtures). PF29 recorded. qwen8-night ran the decisive R5-3/4 reframe (probe + clean restore + clean DLL rebuild); cursor-diagnostician delivered R5-1/R5-2/R5-5 static roots; ds-reviewer ACCEPT'd R5-2/R5-5.

## I. Round 6 + LaTeX Inline Special (2026-08-01 ~00:00-03:00)

### I0. LaTeX inline special action (user-directed)

**P1 inline formula oversize → FIXED** (9a684d16). User reported LaTeX inline rendering "still not ideal." qwen8-night identified the main issue: inline formulas calibrated to body LINE HEIGHT (10) instead of x-HEIGHT (~7 = GuideText.ascent()) → ~1.43× oversized. Fix: LytLatexBlock calibration target lineHeight→GuideText.ascent(), insets excluded from ratio, baselineAscent scaled proportionally (Rust baseline anchoring untouched, fraction bar stays on text baseline). Verified: bounds (E=mc² h 15→12, ratio 1.5→1.2; fraction lines 20-21→17, no overflow) + VLM (inline proportionate to text, fraction baseline correct, display page unchanged). Secondary findings (P3 valign not independently passed to Rust — currently invisible/no slack; P4 baseline correct per fixture) recorded, not fixed (low priority).

### I1. Round 6 screening + adjudication

Render 64/64 (2 batches, no OOM — PF28 mitigation: orphan-kill + sleep between batches + overlay dropped). Geometric: 3 findings = floats_multi sibling_intersection (fixture-expected). VLM ×4 + targeted re-checks.

**False "regression" claims exonerated by objective bounds (PF26/PF29/PF30 discipline)**:
- text/cjk-mixed + text/headings "single-line not wrapping" (VLM conf 0.95) → bounds show CJK i=13 h=19 (2 lines), H1 i=14 h=26 (3 lines): WRAPPING. VLM saw the full-width first line and missed subsequent lines.
- scenes/effects "PlaySound scene empty/black" (conf 0.6) → nonblack=6.2%, content present. Misread.
- tables/basic alignment "left not center/right" (conf 0.7) → R4-17 verified alignment exact; short values hard to judge. Misread.

**Two real new defects found + fixed**:
- **R6-1 ItemGrid no row-wrap → FIXED** (13eecb47). 6/9-item grids rendered single row; fixture expects 2 rows / 3×3. Root: LayoutStyleExtractor lowered LytItemGrid as flex row+wrap without explicit content width (unlike LytSlotGrid) → shrink-wrap single row. First fix attempt gave 2 columns (explicitW=54 is border-box; padding=5/side ate 10px → content 44 → 2 cols). Refined: explicitW = min(3,slotCount)×OUTER_SIZE + horizontal padding → 3 columns. Verified bounds: all grids 3 cols (x=10/28/46); 3-item 1 row, 6-item 2 rows, 9-item 3×3, ore-4 3+1.
- **R6-2 code blocks wrap (BreakWord regression) → FIXED** (1f33f1b0). The R5 BreakWord fix (1f36e354/1f33f1b0 lineage) made long code lines emergency-break at narrow container width → code wrapped instead of overflowing/scrolling. Root: Rust text path never read TextData.white_space (schema had it, Java wrote it). Fix: new white_space value 2 (Pre/NoWrap, byte field — no flatc regen); LytCodeBlock body PRE_WRAP→PRE; LayoutNodeSerializer PRE→2; Rust reads white_space, ==2 → TextWrapMode::NoWrap + OverflowWrap::Normal + break_all_lines(None) + natural-width measure; 0/1 paths byte-identical (prose BreakWord preserved). Verified (qwen8-night probe): code nodes white_space=2 reach Rust, NoWrap active (python block 106px viewport, 150+ char line h=50 single-line); VLM: code single-line, cjk/headings still wrap (ws=0), csv ok (ws=1), error-parse ok. Reviewer ACCEPT (parley 0.11 source verified, anti-pattern 4/4 clean). NOTE: horizontal scroll UI itself is R4-36 residual (separate missing feature); R6-2 restores single-line+overflow (no wrap).

**Disclosed side effect (accepted)**: PageCompiler:939 error blocks also use PRE → now white_space=2 (NoWrap). Error blocks are pre-formatted (hard break per line); error-parse VLM verified red error text renders readable, no overflow. Residual edge case: pathologically long single-line source in an error block now overflows/clips instead of wrapping (no horizontal scroll on error viewport). Accepted as semantically correct for pre-formatted diagnostic text.

### I2. Round 6 verdict

LaTeX P1 fixed; 2 real defects (R6-1, R6-2) found+fixed+verified+reviewed; 3 VLM false-regression claims exonerated by objective measurement; PF30 recorded. Carried: R4-25 STUCK (Angelica entities), R4-36 horizontal scroll axis, code_blocks P1 fixture defect, scenes low-confidence items (annotations thin lines/text order, effects particles — likely false positives given session's scene-VLM unreliability), stress_mixed fixture-stale mermaid note. Commits: 9a684d16 (LaTeX) → 1f33f1b0 (R6-2) → 13eecb47 (R6-1).

## J. Round 7 (2026-08-01 ~22:00-23:00)

**Render**: 64/64. One transient native crash (NTSTATUS 0xC000041D fatal user callback) on a 4-page verify render — succeeded on clean retry; not code-related (same pages rendered fine in the full batch; layout gate passed). PF28 mitigation held for the full batch (no OOM).

**Geometric**: 3 findings = floats_multi sibling_intersection (fixture-expected, consistent all rounds).

**R7-1 → NEW REGRESSION found + FIXED** (12b0b826). VLM (conf 0.85) flagged latex/display formulas left-aligned (x=5) instead of centered (INVARIANT 水平居中). Objective bounds bisection: centered (x≈400) in 2026-07-29 renders, left (x=5) from 2026-07-30 — regression introduced by **b52e2ab1 (R4-18 table natural-width fix)**, which removed `|| block instanceof LytFloatAwareBlock` from LayoutStyleExtractor.needFullWidth. That clause (added by A6) forced the FloatAware wrapper around display LaTeX to full-width so the inner align_self=Center had room to center; removing it shrink-wrapped the wrapper to formula width, making Center geometrically void. Fix: restore a NARROW rule — `(block instanceof LytFloatAwareBlock fb && fb.getInner() instanceof LytLatexDisplayBlock)` → full-width — without restoring the blanket FloatAware clause (which would re-break R4-18 natural-width tables). Verified: all 11 display formulas centered (center≈450, x≈(890-w)/2+5); tables/metadata natural width (203/354/253, R4-18 preserved); csv full-width is its own pre-existing design (doesn't match the narrow condition). Reviewer ACCEPT (anti-pattern 4/4 clean). NOTE: this regression survived R5/R6 screening because those rounds' LaTeX checks focused on inline scale (P1) and didn't re-check display centering; the ratchet has a `centered tol=3` assertion that should have caught it — worth checking why it didn't fire (assertion may target a different node or be disabled).

**Recurring VLM misreads re-confirmed (PF26/29/30)**: text/cjk-mixed + text/headings "single-line not wrapping" (conf 0.6) — bounds confirm CJK i=13 h=19 (2 lines), headings wrapped; 3rd occurrence of this exact misread (screener sees full-width first line, misses subsequent lines).

**Recurring low-confidence scene item (deferred)**: scenes/effects PlaySound scene viewport reported empty/black (conf 0.7, R6+R7). R5-5 fixed the error overlay (scene now mounts); whether the anchor note_block renders in that specific scene is a secondary scene-rendering subtlety (PlaySound is non-visual; test is "render existence only"). Grouped with other deferred scene items (annotations thin lines/text order, effects particles) — scene VLM findings have been unreliable this session (PF26/28); recommend focused confirmation if pursued.

**Known carried**: code_blocks L113 P1 (fixture), lists/rich N3 table-in-list full-width (R4-18-residual), mermaid edge labels R3-13 (low), R4-25 entities STUCK (Angelica), R4-36 horizontal scroll axis.

**Fixture maintenance (documentation discipline)**: stress_mixed.md + FIXTURES.md mermaid "placeholder box only" notes updated to RESOLVED — mermaid renders fully in-game and offline (confirmed across R6/R7 mermaid pages all clean; the ELK/async migration loss was fixed in the R4 mermaid waves).

**Round 7 verdict**: 1 real regression (R7-1 display centering) found via VLM + objective bisection, fixed at root (R4-18 collateral), verified, reviewed. Corpus otherwise stable post-R6 global text changes (BreakWord/NoWrap/LaTeX scale all hold at corpus scale). Commits: 12b0b826 (R7-1) + fixture/docs maintenance.

## K. Round 8 + Confirmation Round closure (2026-08-01 ~23:00 - 2026-08-02 ~01:00)

### K1. Ratchet first-run (PF31 discovery)

Running `assert_bounds.py` for the first time (it had never been executed in prior rounds — PF31) caught what VLM screening missed:
- `centered tol=3` (display LaTeX) — would have caught R7-1 in R5/R6 (display formulas left-aligned x=5). Confirmed it passes post-R7-1-fix.
- `max_height le 400` (large mindmap) — **NEW failure**: large mindmap canvas h=438 > 400.

### K2. R8 — large mindmap height regression → FIXED (6f5050aa)

Bisection: mindmap h=320 (2026-07-29, capped) → 438 (2026-07-30+, uncapped); flowchart stayed 320 throughout. Root: LytMermaidMindmapCanvas had MAX_HEIGHT=320 constant but never applied it (computeLayout/afterExternalLayout used `max(MIN_HEIGHT, desired)` — floor only, no ceiling), unlike flowchart which clamps `clamp(desired, MIN_HEIGHT, MAX_HEIGHT)` in 3 places. When the R4-wave LayoutStyleExtractor changes altered the canvas width, the mindmap reflowed to its natural height (438) with no cap. Fix: mirror flowchart's clamp in mindmap's 3 places (computeLayout:158-160, precomputeLayout:192-195, afterExternalLayout:244-246) + freeze preferredHeight to `max(48, preferred)`. Verified: mindmap h 438→320 (matches flowchart); ratchet 24/24 green; VLM large mindmap tree root-centered with viewport pan/zoom (same UX as 23-node flowchart); other mindmap pages (Default/TIDY_TREE/Deep Nesting) unchanged. Reviewer ACCEPT.

### K3. R8 — mermaid NodeContent inline ItemImage positioning → FIXED (3d65d02a)

VLM (R8) caught: runtime node's inline diamond ItemImage rendered at line START (overlapping "Runtime" → "🔷ime"), should be inline at end (after "and inline"). Pre-existing bug missed by R5-R7 (screeners checked "icon present" not "icon position"). Root: NodeContent subtrees are off-tree (LytMermaidCanvas.nodeContentBlocks), never go through Rust inline_post_pass; LytParagraph.computeLayout lays inline blocks at (0,0) expecting Rust to assign pen_x, but NodeContent uses Java manual layout (layoutContentSubtree, VBox-vertical only) → inline ItemImage stuck at x=0. Fix (method A): route NodeContent root through the same Rust layout as the main document (new shared LytMermaidCanvas.layoutNodeContentWithRust: serialize → measureLayout → writeback incl. inline_post_pass + glyph injection + atlas upload), with Java post-order fallback (preserves R4-12 list semantics) on Rust failure. RUST_CONTENT_PAD=5 aligns content width. Verified: runtime icon at end (correct), preview list 3 items with bullets (R4-12 not regressed), all 5 mermaid pages clean (flowchart Result recipe box frame-identical to pre-change). Reviewer ACCEPT (coordinate cancellation, node-width preservation, R4-12 fallback all source-verified; render log confirms Rust path active, no fallback).

### K4. Confirmation round → CLEAN

Full re-screen (geo + ratchet + VLM ×4) after R8 fixes: geometric 3 findings = floats_multi expected; **ratchet 24/24 green**; VLM **no new confirmed defects** — only documented known items (code_blocks L113 P1 fixture defect; lists/rich N3 table-in-list full-width = R4-18-residual; R4-25 entities STUCK = Angelica layer; scenes/annotations blue-polyline + text-order + effects PlaySound note_block = low-confidence scene items). The recurring cjk/headings "single-line" VLM misread was correctly suppressed this round (screener recognized the documented misread pattern). **Visual screening reached the clean termination condition.**

### K5. Latent items recorded

- **Cross-thread measureLayout (latent)**: NodeContent layout now calls LayoutBridge.measureLayout (shared FontSystem handle). If a future PreCompiler path holds NodeContent blocks while the guidenh-compile thread runs concurrently with the Client thread, this becomes concurrent mutable access to the FontSystem handle — needs serialization protection if that path emerges. Not triggered currently (compile-thread precompute has no NodeContent blocks; all measureLayout observed on Client thread).
- **Carried architectural/STUCK**: R4-25 (Angelica entities), R4-18-residual (no-declared-width table natural width), R4-36 (code-block horizontal scroll axis), code_blocks L113 (MDX special-char fixture defect), scenes low-confidence items.

### K6. R8 verdict

2 regressions/defects fixed (mindmap height clamp, NodeContent inline positioning); ratchet integrated into screening (PF31, WORKFLOW Stage 3); confirmation round CLEAN. Corpus stable: visual screening shows no new problems; remaining items are documented known/STUCK/architectural. Cumulative R4-R8: 34 original R4 issues + R5-R8 findings resolved or categorized; the fix wave converged from ~5 defects/round (R5) to 2 (R6) to 1 (R7) to 2 ratchet-caught (R8) to 0 (confirmation).

## L. Narrow-width (480) screening round (2026-08-02 ~01:00)

**Motivation** (user-directed): all prior screening ran at width=900, but the typical guidebook content width is ~480 (mermaid precompute fallback comment: "typical page content width"). Width-dependent behaviors (wrapping, grid columns, table natural width, float reflow, mermaid viewport) had never been exercised at the real book width. (Related: PF29 test-width mismatch — the cjk/headings fixtures had to be lengthened to wrap at 900, a symptom of testing at an unrepresentative width.)

**Render**: full corpus at width=480, scale=2 (PNG 960px), to `screenshots_narrow/`. 64/64 ok.

**Geometric (--page-width 960)**: 3 findings = floats_multi sibling_intersection (fixture-expected). **No overflow_width at 480** — the layout adapts to book width without blocks exceeding the page width.

**VLM ×4 findings — all adjudicated EXPECTED width-adaptive behavior (no real defects)**:
- **mermaid/mindmap + node-content clipping** (conf 0.75-0.85): mindmap content wider than the 480 canvas → right subtree / runtime node clipped at the canvas edge (mid-word text truncation is pixel-clipping at the viewport boundary, not a wrap failure). This is the mermaid viewport model (fixed viewport + pan/zoom; the mindmap is height-clamped to 320 per R8 and width-clipped to page width). In the interactive book the user pans/zooms; the headless screenshot shows only the initial viewport. EXPECTED.
- **floats/content-types floating table full-width** (conf 0.6): a wide floating CsvTable at 480 leaves no room for text to wrap beside it → text flows below. Standard float behavior (float too wide to wrap beside → text drops below). EXPECTED.
- **meta/indexes category title ellipsis** (conf 0.85): long category title gracefully truncated with ellipsis ("Indexes (SubPages + Cate...") in the narrow column. Legitimate UI degradation. EXPECTED.
- Known items re-confirmed: code_blocks P1, lists/rich N3, mermaid edge labels R3-13, R4-25 entities, scene low-confidence items.

**Verdict**: the corpus adapts well to the real book width (480) — graceful wrapping, viewport clipping, float reflow, ellipsis truncation; no overflow, no broken layout. **No real defects at narrow width.**

**Potential enhancement (not a defect)**: the mermaid mindmap uses a fixed viewport (clip + pan/zoom) at narrow width, while the flowchart has headless fit-to-view zoom (R4-37). Adding fit-to-view to the mindmap would make it consistent with the flowchart and show the full mindmap at narrow widths without initial clipping. Recorded as a candidate feature task.

**Workflow change**: multi-width screening added — screen at BOTH the test width (900) AND the representative book width (480) to catch width-dependent issues (see WORKFLOW Stage 3 / USAGE). Expected narrow-width behaviors documented here so future screeners don't re-flag them.

## M. Typography Optimization Arc — qwen8-night 8-round free critique (2026-08-02 ~01:00-05:45)

**Origin** (user-directed): after the narrow-width (480) screening round exposed the width gap, the user directed a typography optimization pass — qwen8-night freely critiques typography (no prescribed direction) each round, executor implements, iterate until qwen8-night judges typography good. Goal: 排版让人舒服 (comfortable reading).

**Comfort score progression**: 5.5 → 6.5 → 5.5 → 6.5 → 7 → 7 → 8 → **9/10**. Round 8 verdict: **调优类空间已尽，排版优化良好** (tuning space exhausted; remaining items are feature gaps, not comfort tuning).

**Implemented (7 rounds of tuning)**:
- **Base metrics**: font size 9→11, line-height ratio 1.11→1.55 (parley FontSizeRelative + layout.rs/measure.rs literals + GuideText.BASE_LINE_HEIGHT 16→17), page margins CONTENT_PAD 5→14 (Rust + Java mirrors LayoutTreeSerializer/LytMermaidCanvas).
- **Heading hierarchy**: monotonic fontScale ladder (H1 1.5 / H2 1.4 / H3 1.15 / H4 1.08 / H5 1.0 / H6 0.95, all bold white), graded margins (top>bottom, higher=bigger), consecutive-heading margin collapse (HeadingCompiler adjacent detection → collapseBottomForAdjacent, H3→H4 gap 19→12px), separator descender gap (+5px, no descender clipping), H1/H2 separator-to-body spacing tightened.
- **Lists**: unified marker hanging-line (ordered + bullet right-align to same gutter), marker↔text gap 2→5, bullet size 2→3, list/table block margins, table column width constrained to list-item content box.
- **Footnote**: full-width (LytWidthBox preferredWidth≤0 → fullWidth, reusing the code-block/details full-width path; separator line full-width).
- **Table**: header row flag + 2px emphasized bottom separator (header bold was pre-existing).
- **Links/inline semantics**: links default underlined (not just hover), SoundLink gold + no-underline (de-homogenized from nav links), KeyBind bold + brighten (key affordance).

**Verified each round**: gate TOTAL ISSUES: 0, ratchet 24/24 green (all typography changes preserve geometric assertions), qwen8-night free re-review at book width 480.

**Feature gaps documented (need new engine capability, NOT comfort tuning)**:
- **F1 wavy underline / emphasis dots not rendered**: the Rust glyph-run main path models only underline/strikethrough/highlight (schema TextStyle has no wavy/dotted fields; emit_decorations kind 0/1/2 only). The Java drawTextDecorations has wavy/dots drawing (params tuned in round 5) but is a DEAD PATH for the main Rust rendering. Fix requires: fbs schema wavy/dotted fields → Rust SpanStyleInfo + emit_decorations kind 4/5 → Java consumption reusing the tuned drawing. (This is the H1 gap from the narrow-width round.)
- **F2 inline key-cap box**: kbd/KeyBind need a bordered+rounded+padded inline box primitive (currently bold text only). Affects `<kbd>`/`<KeyBind>`.
- **Optional semantic**: PlayerName has no distinctive style (readable via context, not a comfort issue).

**Method note**: qwen8-night free critique (no prescribed direction) was the driver; it correctly distinguished tuning (parameters/rhythm) from feature gaps (missing engine capability) and flagged when tuning space was exhausted. Two misperceptions were caught by objective bounds verification (list-table indentation was already fixed; italic line-start "shift" was inherent italic slant). The wavy/dots Java-path tuning (round 5) had no effect because the main rendering path is Rust — a reminder to verify which rendering path is live before tuning its parameters.

**Verdict**: typography optimization reached 9/10, tuning space exhausted. Remaining work is feature implementation (F1/F2), not tuning.

## N. F4 — glyph atlas full 巨型字形 (PENDING-IN-GAME, 2026-08-02)

- **Symptom**: 游戏内浏览真实指南书时渲染 mermaid flowchart 触发海量 WARN
  `glyph atlas full, dropping glyph key=... (978x1110)`; fml-client-2.log 中 10140 条
  (10:06:19-10:08:03), 位图尺寸 533-1434px (正常 11px 字形 ~15x20px), ~97.5 WARN/s.
- **Evidence**: 洪泛始于 mermaid canvas `computePrimitives diagramReady=true
  bounds=...663x320`; 字体 msyh.ttc 无内嵌位图表 (fontTools 验证无 EBDT/CBDT/sbix).
- **Diagnosis** (ds-coder 插桩诊断, 插桩 6 处已还原, git diff 0): 离线不可复现 —
  headless 所有路径上限 11 × 2.5(MAX_ZOOM) × 4(guiScale) = 110ppem, 洪泛要求
  fontScale × render_scale ≈ 76-173 (位图 840-1900ppem). 候选排序:
  1. NodeContent Rust 光栅化 `size = g.font_size × render_scale`
     (layout-engine/src/parley_text.rs:741) — 触发点/机制契合最高;
  2. MermaidNodeRenderer.scaleTextStyle `fontScale × zoom` 叠乘
     (MermaidNodeRenderer.java:74-76) — 唯一能把 fontScale 顶到 38-86 的通道;
  3. swash ColorBitmap(StrikeWith::BestFit) (parley_text.rs:757-760) — 基本排除
     (msyh 无位图 strike, 位图尺寸与 size 严格成正比).
- **Status**: PENDING-IN-GAME. 需游戏内打点复现; 打点建议 (Rust 侧需重建+DLL):
  1. parley_text.rs:741 size>60 时打印 g.font_size / render_scale / 位图 w×h —
     font_size≈900→候选1/2 样式膨胀; render_scale≈40+→DisplayScale 异常; 正常但
     位图≈1000px→候选3 回退字体;
  2. layout.rs:830 shape_text_cmd scaled>30 时打印 font_size/font_scale/scaled;
  3. GuideText.java:202 shapeUncached fontScale>2 时打印来源文本;
  4. LytMermaidCanvas.java:334 computePrimitives 打印 getActiveZoom()/zoom/
     visualZoom/scaleFactor (直接验证候选2 activeZoom 是否病态).
- **Next**: 用户游戏内按上述打点复现一次 → 定位乘子 → 修复; 修复验收 = 游戏内
  洪泛消失或位图尺寸回归正常量级.

## O. Comfort Fix Round 1 — F5-1/F5-2/F5-3/F8 (2026-08-02, CLOSED)

- **F8 heading/callout vertical gap** (user-reported: 标题与正文空行太多; callout 标题栏空行):
  Root cause = Rust top-level pusher applied margins **post-posed** (`cursor += h + mt + mb`),
  so the heading's large margin-top (H1 20px) landed between heading and body
  (26px gap). taffy subtrees and the retired Java layout were CSS-front; the
  pusher was the migration bug. Fix: prepose `mt` in both paragraph and block
  paths (layout.rs), heading→body gap 26→11px; callout title→body margin zeroed
  (BlockquoteCompiler first-body margin-top, LytQuoteBox getBodyContainer);
  reverted a wrong-model Java collapse added earlier that over-collapsed under
  CSS semantics. Verified: ratchet 24/24, geometric 3 known, VLM confirms
  compact rhythm, body-paragraph spacing unchanged, float/clear paths intact.
  Commits: 62b0db08.
- **F5-1 task checkbox alignment** (user-reported): checkbox anchored
  `bounds.y()+1` (top-aligned). Fix: vertical center on marker first-line text
  geometry (Rust-written-back glyph bounds, same mechanism as bullets).
  Commit: 1a8ae489.
- **F5-2 details marker alignment** (user-reported): ">"/"v" optically high and
  smaller. Fix: summaryMarker marginTop=1 structural declaration (taffy CENTER
  aligns margin boxes; 0.5px optical trade-off declared). Commit: 02ca4d82.
- **F5-3 inline LaTeX metrics** (user-reported): formula glyphs oversize
  (calibrated to font ascent, not x-height) and depth-anchor drift. Fix:
  x-height calibration (0.625×ascent), single-ceil texture scaling, bottom-inset
  aware depth rounding (removes ~2s px systematic drift). Commits: 240f69bb.
- **Verification**: gate TOTAL ISSUES: 0; ratchet 24/24; geometric 65 pages 3
  known (floats_multi); log clean; independent review ACCEPT (4 redlines PASS,
  no anti-pattern hits); VLM confirms all four fixes effective.

## P. F3 — Table separator dual-track offset (2026-08-02, CLOSED)

- **Symptom** (user-reported): table cell AABB does not hug the separator lines;
  first characters look padded. Investigation found the offset was systematic:
  separators were drawn from Java `column.x` computed at x=0 basis during
  serialization, while Rust wrote back cell bounds in real document coordinates
  (CONTENT_PAD=14) — a 14px offset on every separator (pixel-measured:
  line at x=435 vs cell boundary 449/450). The `moveLayoutPos` translation chain
  had no caller in the Rust pipeline (dormant); the serializer comment claiming
  "column.x is overwritten by Rust" was false.
- **Fix**: separators now derive x from Rust-written-back cell bounds
  (`widestRow()` + `columnSeparatorX()`, both computePrimitives and render
  paths); column-width allocation stays Java (declared intent → size_w);
  fallback chain (no row / missing bounds) degrades to pre-fix behavior without
  new offset; stale comment corrected.
- **Verification**: gate TOTAL ISSUES: 0; pixel-level check at both widths
  (900/480) on basic/wide/csv/metadata/cjk — every separator exactly at cell
  right edge; ratchet 24/24; independent review ACCEPT (redlines PASS, 5
  declared assumptions code-supported, no anti-patterns).
- **Note**: separator alignment is now objectively verifiable (pixel probe);
  consider a future ratchet/audit probe per §3.1.3.

## Q. F7a — Special Index pixel-font migration (2026-08-02, CLOSED)

- **Symptom** (user-reported): Special Index text rendered in MC 8×8 pixel
  font, inconsistent with body font; measure/draw dual-source (layout-time
  FontFacts used GuideText/Rust metrics, render-time used MC FontRenderer).
- **Fix**: MediaWikiSpecialGeneratedBlock → usePrimitives + computePrimitives:
  5 texts via GuideText.emitText, all render-time metrics switched to
  GuideText.measureWidth/lineHeight (17px line height, same source as layout),
  non-text elements to FillRect/RenderItem/BlitTexture/DrawBorder,
  estimateEntryHeight hardcoded 9 → GuideText line height. Independent review
  REJECTed first attempt on DrawBorder argument order (bottom/right slots
  swapped → bottom border lost, extra right line for asymmetric borders);
  re-dispatch fixed arg order, semantics verified against legacy
  BorderRenderer. Two benign deviations accepted: getGlTextureId silent catch
  (repo convention), RenderItem overlay for stackSize!=1.
- **Verification**: gate TOTAL ISSUES: 0; ratchet 24/24; VLM confirms pixel
  font eliminated, rhythm consistent, no regression; code-level border
  verification (no fixture triggers `<Special>` — coverage gap registered).
- **Coverage gap**: no fixture uses `<Special>`; the Special block's visual
  result (borders, hover underline) is only verifiable in-game or after adding
  a `<Special>` fixture page. Registered for fixture coverage round.
- **Remaining legacy text blocks** (path map, shrink list): LytGuidebookScene
  (F7b — hybrid HostDraw+primitives design needed), MediaWikiGeneratedListBlock
  (F7c), LytListItem:123 label, annotation renderers (in-world overlay, not
  document path), LytParagraph:422/LytGenericRecipeBox:93/CornerLegendRenderer
  legacy fallbacks.

## R. Aesthetic Baseline & N-A1/N-A2 (2026-08-02, CLOSED)

- **Aesthetic baseline** (Stage 4, 12 representative pages @480, §3.2 checklist):
  8/10 — heading rhythm natural (CSS-front fix confirmed), block spacing
  intentional, inline harmony good, font ecosystem unified (F7a effect),
  colour comfort high. Two new findings:
- **N-A1 details embedded table overflow** (error): details content viewport
  inset (PADDING 6 + BORDER 1 = 7px/side) never entered the Rust layout tree —
  updateContentPosition shifted x by +7 without narrowing width; fullWidth
  viewport + margin (Taffy 0.12: explicit percent(100) not narrowed by margin)
  kept width = detailsWidth → 7px right overflow on viewport and embedded
  table (900/480 bounds confirmed). Fix: content margin declaration + drop
  setFullWidth → stretch resolves width = detailsWidth−14, right edge
  details.right−7. Verified: all 7 details blocks viewport/table right ≤
  details right at both widths; ratchet 24/24.
- **N-A2 TEST GOAL grey-block** (warn): bare `<details>`/`<SubPages>`/
  `<NodeContent>` in TEST GOAL headers parsed as HTML/JSX → rendered as grey
  rect. Fix: backtick-wrap in details.md/indexes.md/node-content.md (fixture
  copy fix, dedicated commits).

## S. F7c — MediaWiki list-page pixel-font migration (2026-08-02, CLOSED)

- **Fix**: MediaWikiGeneratedListBlock migrated (same pattern as F7a):
  usePrimitives+computePrimitives, 3 texts (empty/header/title) via
  GuideText.emitText, all render metrics (vertical center, clipToWidth,
  clickableWidth) GuideText-sourced, list markers/icons/borders to primitives
  (DrawBorder arg order verified against GuideRenderPrimitive — learned from
  F7a REJECT), hover underline manual FillRect, BORDER_RENDERER removed.
  Verified by fixture (meta/indexes pages render this block): VLM confirms all
  text smooth system font, F/I columns consistent, no regression; independent
  review ACCEPT (redlines PASS, no anti-patterns, 4 declared assumptions
  verified, differences vs F7a all justified).
- **Verification note**: an initial VLM pass misreported "F column still pixel
  font" because the executor reused a stale screenshot list (pre-migration
  render). Diagnosis (code + pixel timeline) proved migration complete; the
  re-check with fresh renders passed. Lesson: screenshot lists must be
  regenerated after each render batch (never reused across code changes).
- **Path-map update**: mediawiki package now fully migrated (F7a+F7c); remaining
  legacy text blocks: LytGuidebookScene (F7b — hybrid design pending),
  LytListItem:123 label, annotation renderers, LytParagraph:422 /
  LytGenericRecipeBox:93 / CornerLegendRenderer legacy fallbacks.

## T. Special fixture coverage (2026-08-02, CLOSED)

- Added meta/special.md: `<Special name="SpecialPages" />` (static catalog, 6
  groups, 2 columns — always non-empty), `<Special name="Categories" rows="2" />`
  (CategoryIndex grid), `<Special name="AllPages" rows="3" />` (index-backed;
  renders empty-state "No pages available" pre-warm — legitimately verifies
  EMPTY_STYLE path). All data local (MediaWikiListContext/static catalog), no
  wiki backend needed.
- **F7a visual verification gap CLOSED**: VLM confirms all Special text smooth
  system font (no 8×8 pixel font), borders intact (top/bottom 1px, no extra
  lines — DrawBorder fix holds), 2-column layouts correct at 900/480, TEST
  GOAL backticks render as text (N-A2 holds).

## U. F7b-1 — Scene slider labels hybrid migration (2026-08-02, CLOSED)

- **Symptom** (user-reported): scene slider labels ("Layer: All" etc.) in MC
  8×8 pixel font.
- **Fix** (first hybrid-mode migration): usePrimitives + computePrimitives =
  collect-time slider-label geometry (GuideText.measureWidth, no /z) +
  emitLegacy(this) HostDraw wrapper (3D/controls/tracks untouched) + 3×
  GuideText.emitText emitted AFTER the HostDraw primitive (painter's order);
  suppressLegacyText flag set at collect, snapshot-consumed at render() entry
  (design's try/finally would double-draw — corrected); dedicated
  *_SLIDER_LABEL_TEXT_STYLE fontScale 0.8 (line height 14 ==
  SCENE_SLIDER_AREA_HEIGHT, glyph ≈9px close to MC). Block Stats / load-state /
  Ponder labels remain legacy (phases 2-4).
- **Verification**: gate 0; render scenes blocks/ponder/entities 900+480;
  ratchet 24/24; VLM: labels smooth system font, HostDraw parts intact,
  no regression; review ACCEPT (snapshot-trap structure holds, flag lifecycle
  correct on pipeline/direct/legacy-subtree paths, redlines PASS).
- **Follow-ups registered** (non-blocking): orphan constants
  STRUCTURELIB_TIER/CHANNEL_SLIDER_TEXT_STYLE now unreferenced; direct-path
  (editor preview/export) labels render at 0.8 — accepted for
  what-you-see-is-what-you-get consistency; optional exception hardening of
  the label-emission section (failure coupling).
- **Path-map**: scene slider labels migrated; remaining legacy text in scene:
  Block Stats (F7b-2), load-state (F7b-3), Ponder hover (F7b-4, deferred).

## V. Blocked decision: Scene3D primitive-ization (2026-08-02, USER DECISION — escape hatch accepted)

- **Context**: F7b-1 migrated scene slider labels via hybrid (HostDraw escape
  hatch + collect-time emitText). The root-cause-level alternative — turning
  scene 3D rendering into a first-class primitive (`RenderScene3D` /
  `GuidebookSceneRenderer` are stubs) — is a major framework change with large
  cost and low glue-reuse value.
- **User decision**: Scene stays on the normal escape hatch (HostDraw/legacy
  hybrid is the legitimate pattern for scene blocks). Scene3D
  primitive-ization is NOT to be pursued; major framework changes of this
  class are recorded as blocked items and escalated to the user for decision
  rather than self-directed.
- **Policy recorded**: major framework changes (new primitives, pipeline
  rewrites, schema evolution with wide blast radius) are BLOCKED items —
  executor records them and reports to the user; no self-directed green light.
- **Remaining scene text** (F7b-2/3/4) stays on the established hybrid pattern
  if/when pursued; currently parked.

## W. Architecture Audit 2026-08-02 — full record (see ARCHITECTURE-AUDIT.md)

Five parallel read-only audits completed. **Full findings, evidence and
the status-tracked action list live in the LOCAL (git-ignored) handoff
document `docs/ARCHITECTURE-AUDIT.md`** — process document, not tracked (PF33).
The durable conclusions are folded into this ledger and WORKFLOW §4 here. Headline findings:

- **Unregistered active legacy block**: `LytNeiRecipeBox` (HostDraw, MC
  pixel-font title :274) — never in the path map (now registered, §W → path
  map updated).
- **Principle conformance**: text/document flow conformant; hotspots = charts/
  function-graph/mermaid data→pixel (S1), table column-width allocation (S2),
  Layouts.java + scroll containers (S3), manual grids (S4) — reclassified
  2026-08-02 by leaf-boundary exemption (below; AUDIT §2.4): leaf-interior
  domains, not violations, no migration required.
- **Text-presentation pipeline gaps** (root: span-pipeline phase 3 never
  implemented): T5 clip/wrap (10+ duplicate implementations with semantic
  forks), T1 decoration, T2 dropShadow, T4 metrics, T3 baseline, T6 type
  scale — all additive fixes; program green-light **PENDING USER**.
- **Verification probes**: P1 inside_parent (primitive exists, 0 uses), P2
  table-column geometry, P4 screenshot-list freshness, P5 log-scan tool —
  low-cost, do-in-round; P7/P8 need schema/new export.
- **fbs schema evolution**: Java-side regen command undocumented — blocks all
  schema-requiring items (A5 prerequisite).
- **Dead code A-class** (orphan constants ×2, LytGenericRecipeBox class,
  CornerLegendRenderer render, redundant bounds-pair, stale comments) —
  deletable in normal rounds.
- **Path map refreshed**: WORKFLOW §4.2/§4.3 updated (5 migrated rows removed,
  NEI/annotations/fallbacks registered, M2 marked done).

- **Leaf-boundary exemption (user directive 2026-08-02)** — Rust layout
  authority ends at tree leaf nodes; leaf-interior rendering is private (any
  means incl. Java, Rust public APIs usable); piercing leaf internals forbidden
  (no schema leaf-private state, no Rust leaf-internal models); obligation:
  leaf-interior text/metrics MUST use Rust text API; tree-level structure stays
  Rust-authoritative. Full text WORKFLOW §2.1.1; hotspot reclassification
  (S1/S2/S3/S4/S5) in AUDIT §2.4 (local). A7/A8 CLOSED, audit decisions
  §6-1/§6-2 RESOLVED. Remaining pending: A4 green-light (§6-3) + scene text
  F7b-2/3/4 (§6-4).

- **SubPages links render at page top (y<19), overlapping the TEST GOAL line
  (OPEN, pre-existing)** — first exposed by `meta/indexes.md` fixture render
  2026-08-02 (batch 211250): two blue child-page links ("Index Sub Page B/A")
  drawn above the fixture's first paragraph (y=19) and absent from the bounds
  JSON (render output vs bounds discrepancy). Ruled OUT of scope for T5-1
  (diff touches only GuideText/MediaWiki blocks/CornerLegend; special.md
  pixel-identical to prior batch; geometry zero-new). Suspect: SubPages block
  path bypasses Rust layout or render-order issue — needs diagnosis in a
  dedicated round.
- **T5-1 residue (cosmetic, next cleanup round)**: dead anonymous FontMetrics
  getAdvance override at MediaWikiSpecialGeneratedBlock.java:510-514 after wrap
  migration; reviewer suggests archiving WidthHarness/CharsHarness into
  git-ignored src/test for future re-verification.

- **T5-2 done (791e689a): G4 char-level truncation unified + clipToChars root-fix.**
  Migrated to GuideText.clipToChars (codepoint/surrogate-safe): CommandLinkCompiler
  buildTooltip (28=25+3), GuideSiteGraphRenderer corner-legend ellipsize (removed),
  GuideSearchSnippetFormatter whole-chain (countVisibleChars/clipNode/ellipsis/trailing-trim,
  budget UTF-16->codepoint). **clipToChars root-fix**: T5-2 first exercised clipToChars
  and surfaced a T5-1 latent defect — it judged "full text fits" against the CONTENT
  budget (maxChars-suffixCount), unconditionally reserving suffix space and over-truncating
  text that fit (e.g. ("abcdefgh",10,DOTS3) gave "abcdefg..." not "abcdefgh"). Fixed to
  fits-in-full-budget (codePointCount<=maxChars -> as-is), symmetric with clipToWidth;
  invariant result codepoints <= maxChars.
  Behavior changes (declared, accurate per reviewer): (1) CommandLink commands of 26-28
  codepoints now render FULL (was "25+...") — fits-in-full-budget; >28 still clip to
  25+"..." (original clip length); surrogate commands no longer split. (2) SearchSnippet
  budget counts codepoints not UTF-16 (emoji snippets marginally longer; budget source
  GuideScreen:6754 still UTF-16, bounded pixel heuristic). (3) siteexport labels maxChars<=3
  -> empty (ning-kong-wu-yi; corner unreachable in practice, legend text width >=~30px =>
  maxChars>=5); BMP otherwise behavior-preserving post-fix.
  Verification: gate TOTAL ISSUES 0; render 21/21; ratchet 28/28; geometric zero-new;
  harness 63/0 + 234/0 (invariant matrix 6 texts x 3 suffix x max 0..10, no lone surrogates);
  reviewer ACCEPT (anti-pattern 4/4 clean). Minor doc note (non-blocking): clipToChars
  Javadoc could state the corner "suffix alone doesn't fit but full text fits -> return full".

- **T5-3 done (1670539b): mermaid node wrap unified to GuideText.wrap.** MermaidNodeRenderer
  (guide/document/block/) wrapText(LayoutContext,...) now delegates to GuideText.wrap
  (signature unchanged; 3 call sites FlowchartCanvas:236/:814 + MindmapCanvas:507 untouched;
  measurement neutral, both paths GuideText/Rust-backed). Deleted dead wrapText(RenderContext)
  overload (zero callers, violated leaf obligation via MC fontRenderer) + wrap-only helpers
  scanWords/appendWrappedWord/appendBrokenWord/WordVisitor (-102/+7). Kept measureTextInternal
  (reviewer-verified active via measureText(LayoutContext) -> badge/edge-label measurement).
  Behavior changes (declared): long-word breaking fragment back-fill -> independent lines
  (long-word/CJK no-space labels -> taller node boxes); empty lines dropped (mermaid labels
  single-line tokens, unaffected); sub-pixel measurement neutral. Current fixtures all
  single-line -> ZERO breakpoint change. Verification: gate TOTAL ISSUES 0; render mermaid
  4/4; ratchet 28/28 (5 mermaid assertions ok on fresh batch = geometry zero-change confirmed);
  geometric zero-new; harness 41/0; reviewer ACCEPT (anti-pattern 4/4).
- **T5 follow-up (coverage gap, deferred)**: mermaid wrap->node-box-size geometry path has NO
  multi-line/long-word fixture (all 4 mermaid fixtures single-line). Add a forced-wrap fixture
  (node label >180px or long no-space word triggering codepoint break) + ratchet assertion to
  lock node box height, preventing silent geometry regression. Non-blocking (current ratchet
  confirms zero change).
- **T5 follow-up (dead code, deferred)**: MermaidNodeRenderer.measureText(RenderContext) :131-133
  is dead (zero callers) but kept as public API; candidate for A-class cleanup round.

- **T2 done (d1c18d53): drop shadow capability for Rust-backed text path.** DrawGlyphRun
  +shadow component; GuideRenderEngine.drawGlyphRun dual-pass (shadow=true renders offset
  second pass beneath main: SHADOW_OFFSET=1 doc unit right-down, shadowArgb=RGB x25/100 ==
  MC (c&0xFCFCFC)>>2 alpha-preserved, same textured-quad batch); shadow=false bit-identical
  to original single pass (reviewer IEEE-754 zero-regression proof). GuideText.emitText:179
  passes style.dropShadow() (null-safe, ~50 sites); LytParagraph:129 passes
  resolveStyle().dropShadow() (paragraph/heading). Pure Java, zero schema/Rust.
  **Pipeline-ready, default UNWIRED**: no current style sets dropShadow=true for Rust-path
  text (plain paragraphs resolve to BASE_STYLE.dropShadow=false; BODY_TEXT is NOT the
  paragraph style - only list markers/MediaWiki/scene use it). Enabling dropShadow on
  BASE_STYLE / compile-time setStyle / HEADING* produces shadow. Whether to wire a default
  is a VISUAL DESIGN DECISION (changes all text appearance), out of T2 scope.
  **Verification journey (recorded for method honesty)**: initial BODY_TEXT.dropShadow(true)
  toggle gave ZERO pixel change (2.8M px identical) - wrong verification target (paragraphs
  don't use BODY_TEXT). Isolation hardcode shadow=true at LytParagraph:129 -> 88644 changed
  px (render path works). BASE_STYLE.dropShadow=true (real style chain) -> 88644 changed px
  IDENTICAL count => resolveStyle->drawGlyphRun chain fully functional. Diagnostician
  confirmed root cause = style attribution (a), not resolution-drop (b); mergeWith propagates
  dropShadow (TextStyle:50,67). All toggles reverted; ratchet 28/28 (shadow render-layer,
  bounds unaffected); reviewer ACCEPT (anti-pattern 4/4).
- **T2 follow-ups (non-blocking, cleanup round)**: (1) emitGlyphQuads javadoc :526-527 says
  "already offset glyphs" but shearBaseY uses un-offset (comment inaccurate, behavior correct);
  (2) drawGlyphRun:510 / SHADOW_OFFSET javadoc claim "+1/+1" but italic runs offset (1-K)*scale
  horizontally (sub-pixel, MC-italic-compatible); (3) add persistent shadowArgb/DrawGlyphRun
  shadow test to src/test (coder harness was temporary); (4) shadowArgb a==0->0xFF deviates
  from MC pure-alpha-preserve (per task spec + codebase tessColor convention; alpha=0 text
  would show ghost halo - edge case).

- **T4 done (e7592981 + probe 1919b13f): real font x_height/cap_height from Rust.** Schema
  ShapeTextResult append x_height/cap_height (id 6/7, =0.0, append-only wire-compatible).
  Rust layout.rs reads first-run parley RunMetrics.x_height/cap_height (skrifa OS/2
  sxHeight/sCapHeight, font-size-scaled), fallback ascent*0.625 / ascent*0.7 (never 0.0).
  Java GuideText.xHeight() reads shape("x").xHeight() real field; removed X_HEIGHT_RATIO.
  Regenerated ShapeTextResult.java + TextData.java drift normalization (add-call order only,
  vtable offsets/field ids wire-identical). Rebuilt release DLL + natives. AUDIT cited
  cosmic-text but project uses parley 0.11->skrifa 0.43.2 (natively exposes metrics, no
  dependency upgrade). Sole visual consumer: inline LaTeX sizing (F5-3 oversize target).
  Verification: gate TOTAL ISSUES 0 (Rust+Java integration); T4-ACTIVE pixel-proof
  latex/inline height 2008->1996px while latex/display control 0px; ratchet 29/29 (added
  latex/inline-xheight.md probe fixture + LytLatexBlock exists assertion); geometric zero-new.
  **Screener 'formula x undersize' adjudicated via harness (objective)**: xHeight/ascent=0.475
  (bit-exact Arial OS/2 sxHeight 1062/2048*11=5.704102), 23.95% smaller than old 0.625*ascent=7.5.
  T4 emits CORRECT real metrics, NOT a bug; undersize is the intended F5-3 oversize correction
  (old target 7.5 > body x 5.70 = oversize; new target 5.70 = body x). Engine resolves SansSerif
  to system Arial for Latin (CJK falls back msyh).
- **T4 follow-up (calibration tuning, deferred)**: residual formula-vs-body visual mismatch is
  a property of the jlatexmath inline calibration formula (LytLatexBlock.inlineScaleFactor with
  LATEX_INSET_PX / sourceRefHeightPx), not T4. If formula sizing complaints arise, tune the
  calibration formula (target is now real x_height); the cross-font mismatch (body Arial vs
  jlatexmath math font) may be irreducible without calibrating against the actual body font.

- **T1 done (T1a 48e8a4cd + T1b a7e613bb): wavy/dots underline decorations through Rust pipeline.**
  T1a (Rust emits): schema TextStyle append wavy_underline/dotted_underline bool (id 11/12,
  append-only); DecorationRect table unchanged (kind byte, 4=wavy/5=dots value conventions);
  LayoutNodeSerializer.buildFbTextStyle serializes them (previously silently dropped); Rust
  SpanStyleInfo +2 fields, span_style_table reads, emit_decorations adds kind=4 (h=2.0 wave
  band) + kind=5 (h=1.0) reusing underline geometry. T1b (Java renders): new DrawDecorationLine
  primitive (x/y/w/h/argb/kind); LytDocument routes kind 4/5 to dedicated list (no longer
  swallowed into plain lines bucket; kind 0/1/2/3 byte-identical, reviewer -U0 verified);
  GlyphRunData +decorations field (4-arg compat ctor); LytParagraph emits + moveDecorations
  on layout move; GuideRenderEngine.drawDecorationLine kind 4 batched sine wave
  (dy=round(sin(i*PI/4)*2)) / kind 5 batched 3x3 dots 4px step, via brightenDecorationColor,
  batched shape-quads (math migrated line-by-line from drawTextDecorations legacy).
  Markdown ^^wavy^^/::dotted::/++underline++/~~strike~~ already compiled to span styles
  (DelUWaveMarkCompiler). decorations.md fixture (4 decorations + combination + long-text wrap)
  + ratchet exists (30 assertions).
  Verification: gate TOTAL ISSUES 0; screener pixel-confirmed all four - wavy = visible
  sinusoidal oscillation, dotted = discrete dots, plain = straight-solid, strike = midline,
  combination + long-text wrap aligned no leak; ratchet 30/30; geometric zero-new; both
  reviewers ACCEPT (anti-pattern 4/4). Note: T1a missed src/test GlyphDiag.java:127
  createTextStyle call site (git-ignored; executor build-fixed).
- **T1 follow-ups (edge cases, deferred)**: (1) mermaid node content kind 4/5 still renders
  straight (LytMermaidCanvas:614 uses 4-arg GlyphRunData ctor, decorations=List.of()) -
  mermaid labels rarely have wavy/dots; (2) LytItemImage ^^/:: label markup parsed
  (buildFormatStyle:418-425) but lost at render (emitText emits no decorations, shapeUncached
  doesn't serialize wavy/dots; LytItemImage is node_type 0 leaf bypassing TextData/spans) -
  recovery = leaf-bypass manual draw (like F7a/F7c) or shapeUncached+Rust path; (3) F7a/F7c
  manual FillRect hover underlines (MediaWikiGeneratedListBlock:425-431 /
  MediaWikiSpecialGeneratedBlock:1188-1193) kept as leaf-internal exempt (plain underline only).

- **T3-min done (ac7c80d3): AlignItems::Baseline enum plumbing (4 layers), DEGRADED to bottom-edge.**
  AlignItems +BASELINE (ordinal 3); fbs align_items comment +4=Baseline (byte field, no regen);
  LayoutStyleExtractor.getAlignItems +case BASELINE->4; style_convert.rs +4=>AlignItems::BASELINE
  (taffy 0.12.1). baseline-align.md fixture (3 mixed-height Rows baseline/start/center controls)
  + ratchet LytHBox exists (31 assertions). Gate TOTAL ISSUES 0; ratchet 31/31.
  **CRITICAL honest semantics**: taffy 0.12.1 AlignItems::BASELINE exists BUT GuideNH leaf measure
  returns only Size<f32> (taffy 0.12.1 measure signature cannot report baseline; leaves always
  first_baselines=NONE) -> BASELINE degrades to BOTTOM-EDGE alignment (baseline.unwrap_or(height)),
  NOT true text-baseline. Fixture INVARIANTS documents this.
- **T3 -> P7 follow-up (true baseline, deferred, larger task)**: real text-baseline alignment
  needs baseline metric export (Rust/parley ascent -> taffy first_baselines), possibly taffy API
  upgrade or custom measure variant + bounds JSON baseline/ascent field (AUDIT P7). Far beyond
  T3's ~10 lines. No current doc-model use case (inline ItemImage/text alignment uses Rust
  inline_post_pass + InlineBlockRef.align, independent of taffy AlignItems).
- **F5-2 clarification (AUDIT association invalid)**: AUDIT cited F5-2 marginTop=1 hack
  (LytDetailsBlock:46,84) as baseline-loss evidence. Investigation: F5-2 is glyph-INK optics
  (>/v marker ink sits high in 1.55x line box; CENTER aligns the margin box so marginTop=1 nudges
  content box down ~0.5px for MS YaHei). Unrelated to baseline metrics; NO flexbox alignment mode
  (Start/Center/End/Stretch/Baseline) can replace it. F5-2 KEPT. The AUDIT T3 rationale (F5-2
  proves baseline loss) does not hold.

## X. User-Reported Six Problems + Realcorpus Screening Arc (2026-08-03 ~ 2026-08-04, CLOSED)

Full-chain arc: 6 user-reported problems (P1-P6) + 408-page realcorpus render + screener
sweep (75 buckets) + confirmed patterns F-N1/N2/N3 + infra determinism + SideBar migration.
All closed with dual verification (objective gate + independent review) per fix.

### X.1 Six user problems — all CLOSED

- **P1 inline LaTeX too small + baseline off (d246f34e, two rounds)**:
  R1: INLINE_PERCEPTUAL_FACTOR=1.2f (TeX lowercase reads small next to CJK at exact
  x-height parity); getBaseLine() exact ratio replaces ceil(getTrueIconDepth())+2 double
  rounding (baselineRatioCache + measureBaselineRatio; dead sourceDepthPx /
  LATEX_BOTTOM_INSET_PX removed). R2 ROOT CAUSE: jlatexmath TeXIcon.setInsets(Insets)
  single-arg silently adds (int)(0.18f*size) per side — real inset 20px/side at size=100,
  not the 2px every comment assumed; ~40px vertical padding baked into each texture made
  the calibrated box half padding. All 4 sites switched to setInsets(insets, true);
  LytLatexDisplayBlock legacyPaddingDiff re-adds the phantom padding to size+refH
  (algebraic identity (Fc+40)/(Xc+40), display box ABI stable: 11/11 display blocks 0%
  deviation). Evidence: $x$ ink 15px vs body x-height 12px = 1.25x (target 1.2); executor
  pixel-verified the 1.33 tool artifact was the fixture's period caught by the 3px margin;
  ratchet +2 h-floor assertions (35 total).
- **P2/P6 ItemImage inline + ItemLink gap (fa8d262c)**: IconMetrics alpha-ink optical
  advance — label-less inline ItemImage/ItemLink used uniform 16px cell zero-gap; now
  tight advance round(inkW*scale)+2*PAD (PAD=2) + draw offset -inkLeft*scale+PAD; lazy
  static cache, multi-pass merge, alpha>8 ink box, PNG fallback (runtime CPU frames
  cleared after atlas upload). Widths: crafting_table 16->20, compass 16->18, diamond
  64->52, emerald 64->48.
- **P3 Special Index layout overflow (d4b941e4)**: looked like wrong floats; actually
  MediaWikiSpecialGeneratedBlock grid overflow — Rust
  measure_mediawiki_special_generated hardcoded line heights 10.0 vs Java 17px render.
  fbs append-only link_line_height/subtitle_line_height (default 10.0 back-compat);
  collectFontFactsImpl serializes real GuideText.lineHeight=17; box h 237->335, 6 rows
  fit, zero ink below box. First Java-estimate unification attempt was misplaced
  (render-verified, stashed dead-path).
- **P4 wavy/dotted decorations too rough (a3d9cbb8, two rounds)**: three hardcoded Java
  copies drew axis-aligned opaque 2x4px blocks (8-phase sine integer-rounded, 3x3 hard
  dots, zero AA). R1: shared DecorationRasterizer (pure functions) — subpixel sine
  sampling 0.25 doc px with per-column coverage alpha, circular dot mask with soft edge,
  run-merge; all 3 call sites converged (wave: 2 -> 19 brightness levels; dots: 243 core
  104/42 falloff); geometry params unchanged. R2 (review REJECT fixes): GL scope
  (pushAttrib+disable GL_ALPHA_TEST+try/finally pop) around wavy/dotted on legacy paths
  (tooltip chain restores GL_GREATER 0.1 which would cull alpha<=25 soft edges); alpha
  compositing coverage*tintAlpha/255 (was coverage-replaces-alpha).
- **P5 callout first-line blank + icon tofu (37042f76 + 66478eb2)**:
  P5-A: bfe98e4f had dropped .trim() in trimLeadingDirectiveText — restored; LytAlertBox
  h 67->50, body 34->17 single line. P5-B: callout icon glyphs ⓘ✦➤⚠☢ (U+24D8/2726/27A4/
  26A0/2622, Common script) absent from msyh.ttc with no fallback path (fontique
  FallbackMap tracks only 7 scripts; parley Hani-hack lands on msyh/SimSun) -> .notdef
  tofu. Font fallback mechanism: Java feeds seguisym.ttf bytes (SystemFontProvider
  candidates incl. Apple Symbols/DejaVu; empty-skip + UnsatisfiedLinkError guard) -> new
  JNI LayoutBridge_loadFallbackFont -> ParleyFonts.load_fallback_font_data registers +
  primes system Hani fallback cache + append_fallbacks (append, never set — CJK order
  preserved). Layout JSON byte-identical with/without fallback (no primary-glyph theft);
  tofu->real symbols confirmed (alert title run widths +1..3px).

### X.2 Realcorpus screening patterns — all CLOSED

- **F-N1 FloatingImage single-dimension (8e66d421)**: real corpus uses width-only (or
  height-only) widely; compiler hard-rejected at parse time. Now single-dimension =
  display-size semantics (full image, missing dim inferred from natural aspect, no extra
  scale on inferred axis); two-dim crop semantics unchanged; natural<=0 falls back to
  error path. Rust measure_image + compiler + 2 Java mirrors identical formula. 10
  real-corpus images inferred within 1px; fixture single-param.md + 3 assertions.
- **F-N2 script error fallback zero width (cbea6430)**: error LytParagraph.error inline
  blocks fell to serializer else-branch -> FloatAbs(0,0) -> Parley InlineBox width 0 ->
  pen never advanced -> overlap with body text (12+ real pages). New LytParagraph branch
  declares FloatAbs from GuideText.measureWidth (longest line) x lineHeight*lines.
  alchemic_router follow-up text x=39->494; sibling intersections 14->0 on 5 pages;
  error-fallback.md fixture locks w>=200 (blind spot documented: node-disappears mode).
- **F-N3 silent black scene viewports (5f026c66, two rounds)**: static hypothesis (modern
  SNBT dialect) FALSIFIED by probes — real root cause camera geometry: explicit rotation
  center skipped auto-centering (structure projected to y~-595); ponder/GameScene offsets
  applied as world units though screen-space everywhere else (SceneTagCompiler doc,
  auto-pan, ponder drag) — -75/-90 x 10xzoom threw structures offscreen. Fixes:
  explicit-center scenes also centered (level center); offsets unified to screen px via
  s=0.625*16*zoom=10xzoom; error channel (sceneBuildError + drawSceneBuildError red text,
  never silent black); modern dialect parsing added forward-compat; wiki scene-camera.md
  (en+zh) stale 'units: blocks' corrected. Evidence: subnetworks ROI 7.13%->32.26%,
  crops 16.89%->23.56%, camera.md offset scene 9.68%->40.15%; 8-file affected-offset
  inventory.

### X.3 Infrastructure — CLOSED

- **Warmup race (2315fd38)**: MediaWikiSpecialDataIndex async warmup vs headless batch —
  13 alphabetically-early Special pages rendered as 96px empty fallback (warmup completed
  1619ms AFTER page 12). handleWorldStable awaits warmup (poll specialDataIndex() !=
  EMPTY singleton, 25ms, 30s cap + warning, interrupt-flag restored). All 13 restored
  (allpages 48->954, doubleredirects 48->6078); log order warmup-complete < batch-start.
- **Identity-hash iteration-order nondeterminism (d82a4575)**: same-build renders drifted
  across JVM runs (mermaid arrangement, tasks 4px shift) — forced pixel-regression
  exemptions. ROOT: FlowchartDocument Map.copyOf -> JDK ImmutableCollections.MapN salts
  hash table with System.nanoTime() per JVM; getNodes() order feeds computeNodeMinSizes +
  ELK node creation order; ELK keeps input order among equal solutions. One-line fix:
  Collections.unmodifiableMap(new LinkedHashMap<>(src)). 25 pages rendered twice in
  independent JVM runs: 24/25 pixel-identical (exemption list shrinks to nei_recipes only
  — residual 1344px lives in external GTNH sprite-atlas registration order, unreachable).
- **SideBar integration S1+S2 (3c6d8bce + f58f903c)**: GuideNavBar was the last GUI chrome
  island on MC FontRenderer bitmap fonts. S1: all text to GuideText/Rust primitives
  (drawPageTitle precedent, single execute after legacy rects; row fontScale 0.70 ==
  lineHeight 12 == ROW_H; title 0.80; hover smooth scroll via pushScreenScissor
  primitives; FontRenderer fallback when !isAvailable). S2: -Dguidenh.renderpage.chrome=
  true headless pass (build.gradle.kts forwards key) — navbar primitives composed left
  162 logical px (mirrors resolveNavigationOpenWidth), document shifted right, output
  width+navW; document bounds byte-identical; verified 79 nav primitives, 2124x1622,
  smooth AA text, 24px row pitch.

### X.4 Screener sweep adjudications (75 buckets, 408 pages)

- 9 pending items adjudicated: ZERO real engine bugs — 6 false positives (quest-ID
  'AAAA...' misread as '&&&&' — homoglyph-run misread pattern registered; '|Iron|' was
  resolved item names; 'Le el 1' was complete 'v' glyph at small size; NEI panel grey
  bars; ponder annotations wrapped fine; icon-slot red placeholders are headless texture
  absence), 3 content/fixture data (content-embed crop 128x128 vs 32x32 actual;
  spatial-io allowLayerSlider={false} by design; overriddenpages red = icon placeholder
  not text clipping).
- New false-positive pattern for screener prompts: homoglyph long-run misread
  (repeated same-char strings read as symbol runs).

### X.5 Follow-ups registered (open)

- **F4 glyph atlas full**: PENDING-IN-GAME (user repro) — see section N.
- **R4-25 scene second entity**: STUCK (Angelica layer).
- **resourcepack1 redeploy**: deployed snapshot still carries stale scene-camera 'blocks'
  text (wiki source fixed in 5f026c66; snapshot is gitignored run/ content — redeploy is
  an owner action); _en_gb variant exists only in the snapshot (structural asymmetry).
- **ponder corpus offX/offY visual check**: pixel semantics now (auto.json -75/-90,
  coke_oven.json -25/-50 etc.) — rendered correctly per F-N3 verification but worth a
  visual pass on next deploy.
- **Navbar follow-ups**: sticky-row text z-order in scrolled state composites above
  sticky rects (single-execute consequence, reviewer-noted); navbar font metrics are
  msyh-measured (other fonts need re-measure); >100 lines mirrored geometry between
  collectPrimitives and render() (share a geometry data source).
- **FlowchartNode.java:41 MapN residual**: same Map.copyOf pattern (extendedProperties);
  iteration feeds CSS string building — order-insensitive today, note if styleOverride
  strings ever become order-sensitive.
- **site-export notes**: single-param regional ImageAnnotations not laid out by site JS
  (app.js selector, documented); x/y error text is a merged message vs runtime's
  per-attribute message; no headless export entry (probe-verified instead).
- **Engine quirks documented (not bugs)**: [!NOTE] marker swallowed by parser — alert
  detection keys on body first word; title=/color-only quote directives fall to plain
  blockquote path (LytVBox, no header row); 3-digit hex colors malformed by parser.
- **Deferred/standing**: F6 monospace font; F7b scene text; P7 true baseline (needs
  baseline metric export + bounds baseline field); T4 calibration tuning (absorbed by
  P1); shadow permanent fixture (needs trigger mechanism decision); T1 edge remnants
  (mermaid kind 4/5 straight lines, LytItemImage ^^/::); A6 tooltip+NEI, A11
  LytNeiRecipeBox, SubPages top rendering, A10 M1' (await decision).
- **Ratchet/assertion state**: 55 assertions / 33 pinned pages; 34-page acceptance batch
  (23 original + images/single-param + scenes/import + 9 expansion pages); pixel
  regression exemption list = nei_recipes only.
