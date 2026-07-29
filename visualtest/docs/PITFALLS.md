# Engine Pitfalls — GuideNH Visual Test System

## Purpose

Traps discovered during fix rounds, recorded so they are debugged once. Each entry
states the symptom, the root cause, the fix reference, and the lesson. Read the
relevant entries before modifying code in the affected area. New entries are
mandatory when a fix surfaces a reusable lesson (`docs/PRINCIPLES.md` §4).

---

## PF1 — Dead code paths look alive

- **Symptom**: Fixes to `MermaidScript.precompute` had no effect on fenced
  mermaid blocks (three fix rounds, no change in render).
- **Root cause**: Fenced mermaid blocks are created by
  `PreCompiler.compileMermaidFlowchart` / `compileMermaidMindmap`; the
  `MermaidScript` path is never taken for them. Similarly,
  `PageCompiler.compileParagraphBlock` is dead code; `$$` paragraphs are compiled
  by `ParagraphCompiler.compile`.
- **Lesson**: Before fixing behaviour, trace the actual construction site with
  runtime logging. The absence of an expected log line is itself the finding.
- **Reference**: fix round A1/A6 (`docs/ISSUES.md`).

## PF2 — Sentinel value inside the valid domain

- **Symptom**: `§f` (white) rendered as literal text and inherited the previous
  colour, while all fifteen other colour codes worked.
- **Root cause**: `mapSectionColor` returned `-1` for "not a colour code", but
  white `0xFFFFFFFF` equals `-1` as a Java `int`, so the valid white mapping was
  indistinguishable from the invalid sentinel.
- **Lesson**: Never use a value inside the function's valid range as its error
  sentinel. All mapped colours carry an `0xFF` alpha byte, so `0` is a safe
  sentinel here; prefer domain analysis over convention.
- **Reference**: fix A13 residual (`PageCompiler.mapSectionColor`).

## PF3 — Mutating a shared AST in place

- **Symptom**: Task-list checkboxes (`- [x]`) vanished depending on compile order.
- **Root cause**: `MarkdownListSemantics.extractTaskMarker` stripped the marker
  prefix from the shared AST text node. The page is compiled twice (precompile and
  render); the second pass no longer saw the marker.
- **Lesson**: Treat parsed ASTs as shared, read-only structures. Transformations
  must be pure (detect, do not mutate), or must save and restore state with
  `try/finally`.
- **Reference**: fix A12 (`MarkdownListSemantics`, `ListItemCompiler`).

## PF4 — Headless rendering is a single-pass capture

- **Symptom**: Mermaid diagrams rendered at zoom 0 (invisible) or offset only in
  headless screenshots.
- **Root cause**: Zoom and pan are smoothed animations starting from 0 that
  converge over several frames. The headless pipeline captures exactly one pass;
  there is no "next frame".
- **Lesson**: Any mechanism that depends on animation convergence or a second
  layout pass is unreliable under headless capture. Provide a headless bypass
  that returns the final logical value directly.
- **Reference**: fix A1 (`LytMermaidCanvas` HEADLESS constant).

## PF5 — Shrink-wrap defeats alignment

- **Symptom**: `align_self` had no visible effect on full-width blocks (code
  blocks, display LaTeX, tabs) — everything stayed left-aligned.
- **Root cause**: With the taffy root at `size.width=Auto`, the container
  shrink-wraps to its content width; alignment then operates inside a container
  that is exactly as wide as the content, making it a no-op.
- **Lesson**: Fix width at the size level (full-width blocks get
  `size_width=100%`), not at the alignment level. When alignment "does nothing",
  check the container's sizing mode first.
- **Reference**: fix K4/A11 (`LayoutStyleExtractor`).

## PF6 — Layout metadata must survive pipeline migration

- **Symptom**: Inline LaTeX formulas dropped one line below the text baseline.
- **Root cause**: `LytLatexBlock.baselineAscent` was only set during the legacy
  Java layout pre-scan. After layout moved to the Rust engine, nothing set it;
  the Rust inline post-pass placed the formula top at the baseline (ascent 0).
- **Lesson**: When a pipeline stage is removed, enumerate every side effect it
  produced — not only its primary output. Geometry metadata (ascent, baselines,
  insets) is the easiest to lose silently.
- **Reference**: fix A7 (`LytLatexBlock.computeFormulaDisplay`).

## PF7 — A real bug is not necessarily the bug

- **Symptom**: Inline formulas rendered at wrong scale and dropped a line.
- **Root cause (twofold)**: A genuine double-application of `user_scale` existed
  in `measure.rs`, but with `userScale=1` it is an identity operation — fixing it
  did not change the symptom. The drop was PF6.
- **Lesson**: Before attributing a symptom to a found defect, check whether the
  defect is on the causal path at the failing configuration. Fix confirmed bugs
  anyway, but keep searching for the actual cause.
- **Reference**: fix A7 rounds 1–2.

## PF8 — Reviewers are fooled by git-ignored files

- **Symptom**: An independent reviewer rejected a correct change because "the test
  file is missing".
- **Root cause**: `src/test/` is git-ignored; a git-only review (`git status` /
  `git diff`) cannot see the new test file.
- **Lesson**: Review instructions must state which paths are git-ignored and
  require on-disk verification for them. Version-control invisibility is not
  absence.
- **Reference**: fix-round review protocol (see `docs/WORKFLOW.md` stage 5).

## PF9 — Concurrent build processes produce phantom "environment issues"

- **Symptom**: Agents reported "pre-existing environment problems" during
  verification.
- **Root cause**: Multiple gradle invocations racing (render vs test runs) create
  transient lock/daemon noise that looks like a broken checkout.
- **Lesson**: Serialize gradle-using tasks; re-run the gate cleanly before
  accepting any "environment is broken" claim.

## PF10 — Native library deployment has two homes

- **Symptom (risk)**: Rust layout-engine fixes silently absent at runtime.
- **Root cause**: `guide_layout_engine.dll` must be copied to both
  `src/main/resources/natives/` and `bin/main/natives/`; the runtime loads from
  the jar resource (no stale cache risk once both are updated).
- **Lesson**: After every `cargo build --release`, deploy to both directories and
  verify with a layout-dependent command before testing behaviour.

## PF11 — Escapes must survive the mask/restore boundary

- **Symptom (registered residual N2)**: Source `\$x$` (escaped dollar) is masked
  as a literal, restored to `$x$`, and can then be matched as a formula by the
  split stage.
- **Root cause**: The mask stage sees escape information (backslash) that the
  split stage no longer sees after restoration.
- **Lesson**: In mask→parse→restore→split pipelines, every stage sees a different
  text. Decide explicitly which stage owns which rule, and test the full
  round-trip, not each stage in isolation.
- **Reference**: issue N2 (`docs/ISSUES.md`).

## PF12 — Metrics cached only in computeLayout() are load-bearing bugs waiting to happen

- **Symptom (R2-2)**: Inline and standalone `LytItemImage` with `label="left"` drew
  the icon on top of the label text; inline item images reserved zero space.
- **Root cause**: `labelTextW`/`labelTextH` and the element bounds were only
  populated when `computeLayout()` ran. After the Java layout pre-pass was removed
  (see `LytDocument.java:261-262`), those caches stayed at their zero defaults —
  but the drawing code still trusted them (`iconX = baseX + labelTextW + gap` with
  `labelTextW == 0`).
- **Lesson**: Any metric that is cached during layout and consumed during drawing
  must have a draw-time fallback (recompute on demand via static measurement, e.g.
  `GuideText.measureWidth`) or be produced at serialization time. When an
  architectural pass is removed, grep for every field "cached from the last layout
  pass" — each one is a latent zero.
- **Reference**: issue R2-2/R2-3 (`docs/ISSUES.md`).

## PF13 — Single-style fast paths silently drop span styles

- **Symptom (R2-5)**: Error text created by `createErrorFlowContent()` rendered
  gray instead of red — but only when the error was alone in its paragraph.
- **Root cause**: `LayoutNodeSerializer.buildTextData()` serializes rich spans
  only when `needsRichSpans()` finds >= 2 distinct resolved styles. A paragraph
  whose entire content is one uniformly-styled span (e.g. all-red error text)
  falls into the single-style legacy path, whose base color comes from the
  *paragraph* style — the span's style is discarded. Mixed paragraphs (body text +
  red error inline) worked, which masked the bug: the same error span renders red
  inline but gray standalone.
- **Lesson**: Every "optimization" that selects between a rich path and a fast
  path based on uniformity must define what happens to the *single* non-default
  style. Test style features both inline (mixed) and standalone (uniform) — the
  two serialization paths differ.
- **Reference**: issue R2-5 (`docs/ISSUES.md`).

## PF14 — Measure with one font, draw with another

- **Symptom (R2-1)**: Mermaid node/edge labels rendered in the pixelated vanilla
  bitmap font and overflowed node borders.
- **Root cause**: Layout measured text width with the engine font
  (`GuideText.measureWidth`, cosmic-text) but the canvas emitted
  `GuideRenderPrimitive.DrawText`, which draws with the vanilla bitmap font.
  Different glyph widths → text wider than the box it was measured for.
- **Lesson**: For every text-drawing call site, ask "which font shaped the
  measurement for this text?" Measurement and rasterization must use the same
  font pipeline (`GuideText.emitText` end-to-end). A render that mixes smooth
  body text with pixelated labels on one page is the visible signature.
- **Reference**: issue R2-1 (`docs/ISSUES.md`).

## PF15 — "Benign" classifications in screeners need re-evaluation triggers

- **Symptom (infra D3)**: The geometric screener reported 0 findings on pages
  with completely broken inline item images.
- **Root cause**: `ZERO_SIZE_BENIGN_CLASSES` in `screen.py` listed `LytItemImage`
  as known-benign based on round-1 measurements. When zero-size later became the
  actual defect (R2-2/R2-3), the whitelist suppressed exactly the signal that
  would have caught it.
- **Lesson**: Every screener whitelist entry must carry its justification and a
  re-evaluation trigger (e.g. "re-check after any change to X's layout path").
  A whitelist calibrated against a buggy baseline encodes the bug as the norm.
- **Reference**: infra issue D3 (`docs/ISSUES.md`).
