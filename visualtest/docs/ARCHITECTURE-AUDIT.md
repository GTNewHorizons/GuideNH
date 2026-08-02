# Architecture Audit 2026-08-02 — Capability Gaps, Implementation Audit, Principle Conformance

## Purpose and Handoff Contract

Five parallel read-only audits (2026-08-02) assessed: (A) text-presentation
pipeline capability gaps, (B) legacy-path inventory, (C) conformance to the
"Rust computes, Java declares" principle (§2.1), (D) verification infrastructure
coverage, (E) architectural residue and hidden debt. This document is the
handoff record: the next round's executor works from the action list (§5),
checks decisions pending (§6), and updates item statuses here as work lands.
Cross-references: `ISSUES.md` (issue ledger), `WORKFLOW.md` §4 (path map),
`PITFALLS.md` (lessons).

## 1. Executive Summary

| Goal | Verdict |
|---|---|
| Principle conformance (§2.1) | Text/document flow highly conformant; **content self-rendering geometry is the gap**: charts/function-graph/mermaid layouts, scroll containers, table column-width allocation, manual grids — see §2 |
| Capability gaps | Confirmed and family-shaped: **text-presentation pipeline** (decoration/shadow/clip/metrics/baseline/scale — 6 items, all verified) + **verification probes** (alignment/spacing/nesting/log — 4 classes without machine probes) + **fbs schema evolution** (Java-side regen command missing) — see §3 |
| Implementation audit | Found unregistered active legacy block (`LytNeiRecipeBox`), a batch of safely-deletable dead code, dormant/residual mechanisms, and semantic forks — see §4 |
| Legacy shrink redline (§2.2) | Compliant since 62b0db08 (only addition is the user-accepted F7b-1 escape hatch) |

## 2. Principle Conformance (§2.1) — Findings

### 2.1 Conformant (high)

Main document pipeline (`LytDocument` serialize→Rust→write-back), paragraphs
(`LytParagraph` glyphData consumption), lists/markers, heading separators,
F3 table separators (Rust cell bounds), F5-1 checkbox (Rust marker bounds),
file tree, quote/alert/details panels, margin/padding declarations.

### 2.2 Violation hotspots (Java computes geometry)

| Severity | Area | Evidence | Fix direction | Decision |
|---|---|---|---|---|
| S1 | Charts/function-graph/mermaid layout: data→pixel fully Java-computed (mapX/mapY, bar widths, pie angles, ticks, ELK/mindmap layout) | `chart/*.java`, `functiongraph/LytFunctionGraph.java:502-708`, `LytMermaidFlowchartCanvas.java`, `LytMermaidMindmapCanvas.java`, `ElkLayoutStrategy.java` | Step 1: consume Rust plotRect, declarative ticks (no schema). Step 2: schema-extend ChartData/FunctionGraphData, Rust maps data→pixel | **PENDING USER** (§6-1) |
| S2 | Table column-width allocation in Java (`layoutColumns`, pinned widths into Rust) | `LytTable.java:201-249`, `LayoutTreeSerializer.java:344-385` | Rust Grid column model (schema) — Java declares preferredWidth/colspan only | **PENDING USER** (§6-2) |
| S3 | `Layouts.java` manual layout engine + scroll containers (SizeBox/CodeBlock/Details) | `Layouts.java:14-164`, `LytMermaidCanvas.java:656-673`, `LytHBox.java:20-36`, scroll containers | M1' path: force font init (kill fallback), declarative overflow intent, Rust scrollRange for thumb | M1' milestone |
| S4 | Manual grids/recipe boxes/MW lists | `LytItemGrid.java:21-37`, `LytSlotGrid.java:105-125`, `LytStandardRecipeBox.java:88-104`, MW list `recomputeRowLayouts` | ItemGrid/SlotGrid: Rust flex-wrap equivalent exists — delete Java loops; NEI relx/rely = content declaration (exempt) | Partial — delete loops |
| S5 | Inline image/icon size math + hardcoded constants (`resolveLineHeight`=10) | `LytImage.java:105-136`, `LytItemImage.java`, `LytGuiSprite.java`, `LytCodeBlock.java:419-421` | Narrow to serialization-time declarations; line-height roundtrip needs schema (optional) | Low |

### 2.3 Exemptions (document, do not migrate)

jlatexmath LaTeX metrics (Java side unavoidable), ELK graph layout, NEI recipe
relx/rely content declaration, siteexport HTML/SVG path, editor/screen chrome
(GuideScreen/SceneEditor), 3D scene via escape hatch (user decision, ISSUES §V).

## 3. Capability Gaps

### 3.1 Text-presentation pipeline (root: "span pipeline phase 3" never implemented)

| # | Gap | Current workarounds (evidence) | Minimal fix | Priority |
|---|---|---|---|---|
| T5 | **Clip/wrap primitive** — worst duplication: 10+ independent implementations with semantic forks (F7a returns "", F7c returns "...", NavBar uses U+2026) | `clipToWidth` ×2 (`MediaWikiGeneratedListBlock:414-428` vs `MediaWikiSpecialGeneratedBlock:1307-1322`), `wrapLines` ×2 pairs, `ellipsize` ×2, GuideScreen ×2, ScrollbarOutline, HomePage, NavBar, SearchSnippetFormatter (char-based!) | `GuideText.wrap(text,maxW,style)` + `clipToWidth(text,maxW,style)` unified semantics; also `emitTextWrapped` (emitText shapes with max_width=-1.0 — never wraps) | **1** |
| T1 | **Decoration primitive** (underline/strike/wavy/dots): wavy/dots absent in schema (63-75), serializer (381-406 silently drops), Rust span_style_table (712-728); F7a/F7c manual FillRect underlines; LytItemImage ^^/:: silently lost | F7a `:441-448`, F7c `:1341-1348` | schema +2 bools → Rust emit kind 4/5 (reuse GuideRenderEngine:739-748 sine) → serializer +2 fields (~40 lines) | **2** |
| T2 | **dropShadow**: emitText none; count style pinned to legacy (`BLOCK_STATS_COUNT_TEXT_STYLE`) | `GuideRenderEngine.drawText:701-710` (legacy only) | DrawGlyphRun second-pass offset shadow (Java render layer, no Rust) | 3 |
| T4 | **Metrics completeness**: Rust outputs width/height/ascent/line_height only; X_HEIGHT_RATIO=0.625 approximation | `GuideText.java:59`, schema ShapeTextResult (532-539) | Rust emits x_height/cap_height (+2 schema floats), Java reads real values | 4 |
| T3 | **Baseline alignment**: AlignItems::Baseline lost at 4 layers (AlignItems.java:10-14 no BASELINE; serializer; schema:20; style_convert.rs:74-79) | F5-2 marginTop=1 optical hack | Add BASELINE=4 through 4 layers (~10 lines); small usage surface | 5 |
| T6 | **Type-scale system**: ad-hoc ladder exists (DefaultStyles.java:59-89) but magic values (0.8, 0.85) + LINE_HEIGHT_RATIO 1.55 scattered 7× in Rust + stale comment 1.45 (LayoutNodeSerializer.java:163-167) | `LytGuidebookScene:172-176-181`, `Sup/SubscriptTagCompiler:21` | FontScaleLevels + single LINE_HEIGHT_RATIO constant injected to Rust | 6 (debt) |

All fixes are additive (fields/methods); no behavior breakage. Also noted:
§-format-code duality (GuideFontCompat legacy vs Rust glyphs).

### 3.2 Verification probes (WORKFLOW §3.1 — 6 objective classes, only 3 machine-covered)

| Probe | Catches (historical) | Cost | Data |
|---|---|---|---|
| P1 `inside_parent` assertion family (pad-aware) — primitive exists, **0 uses** | N-A1 (7px overflow), R3-8/9 | very low | bounds |
| P2 Table column-geometry probe (cell-right grid consistency, declared-width match, content-in-cell) | F3 (14px offset), R4-4 | low | bounds |
| P3 Vertical-rhythm probe (sibling y-gap histogram) | F8 (26px gap) | low | bounds |
| P4 Screenshot-list freshness validator (stem→latest batch, timestamp reconciliation) | F7c false positive (stale list) | low | fs |
| P5 Log-scan tool (`glyph atlas full`/`OutOfMemory` → fail) — **§3.1.5 mandate, no tool exists** | F4 (10140 WARN flood) | low | logs |
| P6 Cross-width automation (900+480 one-command diff) | PF29, §L | med | bounds ×2 |
| P7 Baseline probe — needs bounds JSON baseline/ascent export (Rust inline_post_pass output, schema) | A7/PF6, R4-21, F5-1 | med-high | new fields |
| P8 Font-family probe — glyph-run font metadata export or static grep vs §4.2 shrink list | R2-1, F7a/F7b/F7c | med/low | new fields/source |

Also: "visual-only" assertion annotation promised in USAGE §7.2 is dead
documentation (never implemented in assertions.json).

### 3.3 fbs schema evolution

Rust regen automated (build.rs); **Java regen manual with NO documented
command** (37 checked-in generated classes, FLATBUFFERS_23_5_26). No version
field, no wire-compat strategy; only "keep field, write zero" convention
(4 DEPRECATED precedents). **This blocks every schema-requiring item
(S1/S2/P7/T1/T4).** Fix: document/script Java-side flatc regen + versioning
policy.

## 4. Implementation Audit — Residue and Debt

### 4.1 Safely deletable (A-class, zero risk — do in normal rounds)

1. Orphan constants `STRUCTURELIB_TIER_SLIDER_TEXT_STYLE` /
   `STRUCTURELIB_CHANNEL_SLIDER_TEXT_STYLE` (`LytGuidebookScene.java:159,161`)
2. `LytGenericRecipeBox` class + dead `isRecipeBox` branch (`LytRecipeGalleryRow.java:28`)
3. `CornerLegendRenderer.render()` + `ellipsize(RenderContext,…)` (no callers)
4. Redundant pair `getFirstTextRunBounds`/`getFirstLineBounds` (identical impl; delete one, update `LytListItem:150,154,170,174`)
5. 8 empty `render()` stub misleading comments (LytChartBase:324, LytFunctionGraph:348, LytStructureView:141, LytContentTabsHeader:207, LytContentTabsBlock:190, LytDetailsBlock:351, LytSizeBox:174, LytItemImage:308)
6. Stale comments: `LytParagraph:412` (pageTitle claim outdated), `LytDocument:261` (omits tooltip/annotation chains)

### 4.2 Needs prerequisites (B-class)

- `Layouts.java` removal — M1' prerequisites (force font init; decide tooltip/annotation/editor chain destination)
- `LytParagraph.render` legacy path — confirm no direct callers remain (pageTitle migrated)
- `LytTableColumn.x` — after removing `LytTableRow:68` Java fallback + `columnSeparatorX:176` fallback

### 4.3 Keep (C-class, live)

Scroll-adaptation post-adjustments (3 scroll containers), moveLayoutPos
propagation for scroll/M1 chain, fbs DEPRECATED zero-write fields,
`LytNeiRecipeBox` legacy (until migrated), scene F7b escape hatch (user
decision).

### 4.4 Transitional (D-class, has elimination path)

`LytHBox` full computeBoxLayout vs `LytVBox` stub asymmetry; `estimateEntryHeight`
packing heuristic (Rust column packing replaces); clipToWidth boundary fork;
scene remaining text F7b-2/3/4 (parked); `LytListItem:123` ordered-label;
`LytFunctionGraph.dragButton` dead field (future-use decision).

### 4.5 New findings (not previously registered)

- **`LytNeiRecipeBox` — ACTIVE legacy HostDraw block, MC pixel-font title
  (:274), never registered** (B-audit §1.A1). Fixture `nei/recipes.md`
  captures it. Migrate via F7b-1 hybrid template or register.
- `MediaWikiSpecialGeneratedBlock:1079-1080` dual-source measurement residue
  (computeEntryHeight context.getLineHeight) — switch to GuideText.lineHeight.

## 5. Action List (status-tracked)

| # | Action | Evidence | Status | Priority |
|---|---|---|---|---|
| A1 | Delete A-class dead code (4.1 items) | §4.1 | PENDING | do-in-round |
| A2 | Probes P1+P4+P5 (inside_parent family, freshness validator, log scan) | §3.2 | PENDING | do-in-round |
| A3 | Update path map — DONE (WORKFLOW §4.2/§4.3 refreshed) | this doc | DONE | — |
| A4 | Text-presentation pipeline program (T5+T1+T2+T4, then T3/T6) | §3.1 | **PENDING USER green-light** (§6-3) | program |
| A5 | fbs schema tooling (Java regen command + version policy) | §3.3 | PENDING | prerequisite |
| A6 | Tooltip/annotation collector-path fix (kills 3 fallback roots) + NEI recipe box migration | B-audit §5.P2/P1 | PENDING | after A5 |
| A7 | S1 charts/function-graph → Rust (schema) | §2.2 | **PENDING USER** (§6-1) | after A5 |
| A8 | S2 table column widths → Rust Grid (schema) | §2.2 | **PENDING USER** (§6-2) | after A5 |
| A9 | Scene remaining text F7b-2/3/4 | ISSUES §U | parked | user |
| A10 | M1' (Layouts.java removal prerequisites) | §4.2 B | PENDING | milestone |
| A11 | LytNeiRecipeBox migration or registration | §4.5 | PENDING | P1 |

## 6. Decisions Pending (user)

1. **S1**: migrate chart/function-graph data→pixel mapping into Rust (major
   framework change, schema) — accept as far-term program, or keep current
   "Rust boxes only" status with documented exemption?
2. **S2**: table column-width allocation into Rust Grid (schema) — same choice.
3. **A4**: green-light the text-presentation pipeline program (T5+T1+T2+T4)
   as a dedicated program parallel to the font-library program?
4. Scene remaining text (F7b-2/3/4): keep parked, or extend hybrid pattern?

## 7. Handoff Notes

Next-round executor: (1) read this doc + ISSUES §N–§V; (2) run A1+A2+A3
(already partially done) as normal-round work; (3) before any schema-touching
task, do A5; (4) escalate §6 items to user before starting A4/A7/A8; (5) update
statuses in §5 as items land; (6) append new audit findings to §4/§5 rather
than rewriting history.
