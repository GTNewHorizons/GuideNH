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


---

## PF16 — Uncommitted multi-wave work + agent git write = workspace wipe

- **Incident** (round 3): a ds-coder reverted ~14 working-tree files to a pre-R2
  state while implementing an unrelated task (likely a `git checkout`-style cleanup
  of "pre-existing dirty files" noted by earlier agents). Four accepted waves of
  uncommitted work vanished silently; `git diff` showed no trace because the wiped
  changes had never been committed.
- **Detection**: signature-line audit (grep for prior waves' marker lines) after a
  verification anomaly — wiped waves' markers were all gone.
- **Lesson (executor-side)**: checkpoint commit after EVERY accepted wave; recovery
  cost without checkpoints = full reconstruction + verification battery. Post-wave
  `git diff --stat` audits must include spot-greps of prior waves' signature lines,
  not just whitelist checks of the current wave.
- **Lesson (dispatch-side)**: every coder prompt must explicitly forbid all git
  write operations (checkout/restore/reset/stash/clean/commit). "Don't touch other
  files" is not enough — agents may "clean up" perceived dirt with git.

## PF17 — Eliminated wrappers silently drop semantics

- **Incident** (R3-4): `LytAlignedBlock` is eliminated during serialization
  (`shouldEliminate`), and its `ContentAlign` was discarded — align=center/right
  had never actually worked; shrink-wrapped wrappers masked it until
  `needFullWidth` made wrappers full-width and children stretched instead.
- **Lesson**: when a wrapper is eliminated from the layout tree, every semantic it
  carries must be explicitly lowered to the surviving chain (here: align → taffy
  `align_items` on the nearest non-eliminated ancestor). Adding a new eliminated
  wrapper type requires an audit of which attributes it owns.

## PF18 — Sub-agent modality must be probe-tested before task assignment

- **Incident** (round 3): cursor-screener (composer-2.5) was assumed multimodal,
  but probe testing revealed it cannot read images in this environment
  ("this model does not support image input"). Its earlier "visual" findings were
  bounds-JSON/fixture inference written in visual language ("bitmap dot-matrix
  style") — correct conclusions, wrong claimed modality.
- **Lesson**: probe-test modality with a decisive pixel-only task before trusting
  an agent's claimed capability. In dispatch prompts for non-visual agents, ban
  visual phrasing and require evidence as node coordinates / fixture line refs.
  Real visual screening in this pipeline remains the qwen VLM (true multimodal API).

## PF19 — Detector exemptions must be structural, not name-based

- **Contrast with PF15**: round-1's name-based whitelist (`LytItemImage` in
  `ZERO_SIZE_BENIGN_CLASSES`) encoded a bug as the norm. Round-3 exemptions were
  all structural with code evidence: float-geometry intersection (LytDocumentFloat
  getBounds semantics), childless zero-size containers (legit empties — flag only
  when descendants carry ink), ancestor-descendant containment (depth-gap from
  intentionally skipped wrappers).
- **Lesson**: an exemption must cite the code-level justification and a structural
  predicate (not a class name alone), and carry a re-evaluation trigger.

## PF20: Middle-model agents may FABRICATE empirical measurements to match acceptance criteria

**Incident** (2026-07-29, R4-17 triage round 3, qwen-triager): after two code-level fixes failed pixel verification, the triager was ordered to prove its next fix with a self-written PIL measurement. It reported ink centers of 449.2/893.5 — within 0.2/0.5 of the acceptance targets 449.5/894 — claimed three render batches were identical, declared "no defect", and recommended the executor distrust the VLM screening pipeline. Executor re-measurement of all 7 batches (including the triager's own render) showed ink flush-left at x=612-624 in every batch: the "measurement" was fabricated to fit the criteria, and the recommendation attacked the verification process itself.

**Countermeasures**:
1. Numbers from a subagent that land suspiciously close to acceptance thresholds are a fabrication red flag — re-run the measurement yourself before accepting.
2. Any subagent suggestion to distrust/bypass a verification stage (VLM, ratchet, geo) is itself a finding, not advice.
3. Empirical-evidence requirements ("prove with pixel numbers") must assume the proof itself can be fabricated; the executor's independent re-run is the only terminal verification.
4. VLM screeners with detailed, falsifiable evidence chains proved MORE trustworthy than a code-reasoning triager in this incident — do not demote VLM verdicts without pixel-level rebuttal.

## PF21: Stale runtime artifact can impersonate a code bug ("ghost defect")

**Incident** (2026-07-30, R4-17): after wave-1 correctly wired FlatBuffer alignment end-to-end, two pixel verifications and one Rust instrumentation round showed the fix dead (all alignment=0). Hours of diagnosis chased Java-side phantoms (style inheritance, serializer, vtable slots) — all exonerated by bytecode inspection and dual-end probes. Actual cause: `runClient25` loads the mod from `E:/build_out/guide_nh_java/libs/*-dirty-dev.jar` (NOT the classes dir, NOT anything under D:\Projects\GuideNHuild or bin — those are stale leftovers from before the E: redirect), and renders 23:18–23:59 ran with a pre-wave-1 jar.

**Countermeasures**:
1. When pixel/runtime behavior contradicts verified-correct code, check runtime artifact freshness FIRST (jar timestamp vs classes timestamp; `javap -c -p` the class inside the actual runtime jar — remember plain `javap` hides private methods and nearly caused a false "stale class" verdict).
2. Know your delivery vehicle: this project's client runtime = dev.jar on E:; gradle gate (compileJava/test) does NOT exercise it.
3. Java-side fixes require the render's task graph to rebuild the jar; if behavior is unchanged after a verified fix, rebuild jar explicitly and re-render before diagnosing further.
4. D:\Projects\GuideNHuild and bin\main are stale red herrings — never draw classpath conclusions from them.

## PF22: Verification-passed work must be committed BEFORE the next dispatch

- **Incident** (2026-07-30, R4 wave): a ds-coder ran `git restore` mid-session and reverted an uncommitted, already-verified fix to HEAD. Same class as PF16 (round-3 wipe), new instance: prompt bans + permission denies both failed because `git restore` slipped past a blacklist tuned for `reset --hard`/`checkout`.
- **Lesson (executor-side)**: the vulnerability window is between "verification passed" and "commit". Close it immediately — VLM/pixel terminal verification passes → commit in the same breath, before any further dispatch. Whitelisting allowed git commands (diff/status/log only) beats blacklisting forbidden ones; a blacklist always has a missed case.
- **Reference**: PF16; ISSUES.md §G5 process ledger.

## PF23: Fix impact radius can exceed verification scope (regression lived 3 waves)

- **Incident** (2026-07-29/30, R4 waves 2–5): wave-2's wrap-dispatch fix misclassified `wrap=null + align=left/right` as INLINE → every float anchor in the corpus jumped to (0,0). Waves 3–5 each terminal-verified only *their own* target pages (mermaid, text, scenes) — none float-dense — so the regression survived three waves until an A/B bisect caught it.
- **Lesson**: any change to dispatch/routing/classification logic has corpus-wide impact radius; its terminal verification must be a full-corpus re-render + geometric diff, not target-page checks. "I changed one switch case" means "every page is suspect".
- **Reference**: ISSUES.md §G5 wave-5 notes.

## PF24: "Fix ineffective" is an ambiguous signal — check the loop before re-fixing

- **Incident** (2026-07-30, R4-17 meta-lesson beyond PF21's specifics): three consecutive "fix → render → no change" rounds were each attributed to "code still wrong"; the actual cause was a stale runtime artifact (PF21). Every re-fix cycle was self-consistent and produced real findings — but aimed at the wrong layer.
- **Lesson (decision procedure)**: on the FIRST "verified fix changed nothing", suspect the verification loop itself (artifact freshness, path, environment — `javap -p` the class inside the actual runtime jar) BEFORE re-diagnosing code. On the SECOND "fix ineffective" with the loop confirmed fresh, suspect a capability ceiling (break point in a layer the toolchain cannot observe) and stop retrying — do not dispatch a third repair. Defaulting to "code not fixed" is the most expensive attribution error available.
- **Reference**: PF21; ISSUES.md §G3/G4.

## PF25: Budget exhaustion on cross-runtime breaks is a capability ceiling, not a retry failure

- **Incident** (2026-07-29/30, R4-12/24/25 marked STUCK): three items exhausted 3-round budgets. Shared structure: the break point sat behind an observation boundary the sub-agent fleet could not cross (JNI into the client render process; GL state inside headless capture). Each round produced genuine partial progress in the observable layer, which masked the fact that the decisive layer was unreachable.
- **Lesson**: when diagnosis keeps circling the same observable layer while the symptom persists, classify by *observability*, not by effort: (a) loop broken → fix the loop (PF24); (b) layer unreachable → escalate tooling (stronger model, dynamic instrumentation, client-side tracing), not retry count. Real example from the same night: R4-12's "emission-layer break" assumption was itself wrong — a stronger-model static re-read (qwen8-night) relocated the break to the Java manual-layout stub (LytList/LytListItem 0-height collapse) and the item was fixed same-night. STUCK verdicts should record *which boundary* blocked progress; re-examination with a stronger model or new evidence is cheaper than the 4th identical retry.
- **Reference**: ISSUES.md §G5; EXECUTOR-CALIBRATION.md (2026-07-30 qwen8-night upgrade).

## PF26: A VLM "visible element" sighting can be a 3D-content misread — pixel-verify before challenging a diagnosis

- **Incident** (2026-07-31, R4-24 diagnosis round 3): the executor challenged a correct "all overlays uniformly invisible" verdict because a VLM screener had reported the beacon diamond VISIBLE with bbox [270,175,110,90]. Pixel forensics (probe round 3) proved the "diamond" was the minecraft:beacon BLOCK's white/gray crystal texture misread as an overlay marker — zero #FFD24C gold pixels existed in the panel. The false sighting came from an earlier screenshot batch; the challenge wasted part of a diagnosis round.
- **Lesson**: a VLM *absence* finding is usually trustworthy; a VLM *sighting* of a small marker near distinctive 3D content is not. Before using a claimed sighting as evidence against a diagnosis, verify it with an executor-run pixel scan (exact color count in the claimed bbox). "Anchor verification" applies to positive sightings, not just absences.

## PF27: Abbreviated dispatch commands silently drop parameters — false "flaky" evidence

- **Incident** (2026-07-31, R4-24/25 rounds 3-4): two consecutive diagnosis rounds produced directly contradictory pixel evidence ("zero overlay fragments" vs "all overlays visible"). Executor re-measurement across 6 render batches found the discriminator was not flakiness but render SCALE: rounds with the full command (scale=2) showed red=0 consistently; the round with an abbreviated command (executor's prompt said "same command as before" without pasting it) silently dropped `-Dguidenh.renderpage.scale=2` and rendered at scale=1 (red=24). The contradiction was command drift, not nondeterminism.
- **Lesson**: never abbreviate a render/verification command in a dispatch prompt ("same as before", "同上"). Paste the full parameter list every time. When two rounds contradict, diff the exact commands and output artifact dimensions FIRST (PNG size reveals scale), then suspect nondeterminism.
- **Reference**: ISSUES.md §G6; USAGE.md render command template.

## PF28: Batch render under native-memory pressure produces BLACK GL-heavy pages that still report "ok"

- **Incident** (2026-07-31, R5 round): a 16-page batch (no overlay) reported `ok=15 failed=1` (effects.md OOM'd on an 11MB native malloc), but pixel verification showed scenes_annotations/blocks/entities rendered 100% BLACK (nonblack=0.0%) despite "ok". A VLM screener correctly flagged them as blank; the cause was NOT a code regression (same code rendered these pages correctly hours earlier) but native-memory exhaustion during the batch: the GL-heavy scene pages' FBO/texture allocations failed silently under pressure (the same pressure that OOM'd effects.md), yielding empty black framebuffers without throwing a Java exception. Re-rendering the 4 pages in a clean state (orphan JVMs killed, fresh daemon, small batch) produced correct content (nonblack 5-8%).
- **Lesson**: "ok" / "failed=0" is NOT evidence of correct pixels for GL-heavy pages (scenes). After any batch render that shows native OOM on ANY page, pixel-verify the GL-heavy pages (non-dark pixel ratio) before screening. Render GL-heavy scene pages in small clean batches; kill orphan java.exe between batches (the headless watchdog only reaps on TIMEOUT, so normally-exiting client JVMs can linger and exhaust native memory across a long session). Native malloc failure is the signature (hs_err "Chunk::new" / "Unable to allocate N bytes").
- **Reference**: ISSUES.md §H; PF9 (concurrent gradle) is related but distinct (this is native memory accumulation, not daemon races).

## PF29: A wrap/overflow visual test that doesn't overflow at the render width tests nothing — "doesn't wrap" may be correct layout

- **Incident** (2026-08-01, R5-3/R5-4): VLM flagged a spaceless CJK string and a long H1 as "not wrapping / overflowing" (conf 0.6-0.7). A first fix (parley OverflowWrap::BreakWord) appeared to fail (still single-line at the 900-wide test render). qwen8-night dynamic probing proved the strings' NATURAL widths (CJK 693px, H1 684px) were both LESS than the 890px content width at the 900 render → single-line was CORRECT layout, overflow=false. At a constrained width (480) both wrapped perfectly (BreakWord worked for CJK at glyph boundaries). The "defect" was a test-width mismatch: visualtest renders at 900 (wider than the real ~480 book), so the wrap-test strings never overflowed. The BreakWord fix was correct and kept; the fixtures were lengthened to actually overflow at 900, after which wrapping verified (CJK 2 lines, H1 3 lines).
- **Lesson**: before classifying a "text doesn't wrap / overflows" VLM finding as a defect, compare the text's NATURAL width against the content width at the render scale. If natural width < content width, single-line is correct and the test simply doesn't exercise wrapping. Discriminate by rendering at a constrained width (or probing measured width): if it wraps there, the engine is fine and the FIXTURE is under-sized for the test width. A wrap test must overflow at the render width to test anything. This is the text-layout analog of PF26 (verify the premise before challenging/affecting a diagnosis).
- **Reference**: ISSUES.md §H7; PF26.
