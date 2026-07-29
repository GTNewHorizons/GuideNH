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
