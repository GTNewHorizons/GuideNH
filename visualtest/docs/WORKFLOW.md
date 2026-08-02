# Workflow — GuideNH Guide Rendering & Visual Feedback Loop

## 1. Purpose and Document Map

This document is the single authoritative operating procedure of the GuideNH
guide-rendering system: the binding principles, the comfort baseline, the
rendering-path map, the pipeline stages, the roles, the issue lifecycle, the
quality gates, and the commit discipline. It supersedes the former split between
`WORKFLOW.md` and `PRINCIPLES.md`; all principles are integrated in §2.

| Document | Responsibility |
|---|---|
| `docs/WORKFLOW.md` (this file) | Binding principles, comfort baseline, rendering-path map, pipeline, gates, discipline. |
| `docs/USAGE.md` | How to run renders, the watchdog, list files, the verification gate, and the ratchet. |
| `docs/RESOURCES.md` | Resource index: fixture folder map, tool inventory, source-code map. |
| `docs/FIXTURES.md` | Per-file fixture specifications and fixture authoring conventions. |
| `docs/ISSUES.md` | Issue registry: adjudication ledgers, fix closure ledger, residuals. |
| `docs/PITFALLS.md` | Engine traps and lessons learned during fix rounds. Read before touching related code. |
| `ratchet/assertions.json` | Executable regression assertions over render bounds JSON (§2.6, §5 Stage 3). |

## 2. Core Principles (binding on every change)

### 2.1 Architecture: Rust computes, Java declares

The Rust layout engine is the **sole geometric authority** — every measurement,
position, baseline, and vertical rhythm is computed in Rust. Java declares
structure, style, and intent only: it builds the block tree, serializes style
intent (including explicit alignment semantics such as "align to first-line
baseline"), and consumes Rust-computed geometry. Java code that computes geometry
is an architectural violation:

- Table vertical separators must consume Rust-computed column boundaries; Java
  must not re-derive line positions from its own column bookkeeping.
- Inline elements (task checkboxes, details markers, inline LaTeX, inline images)
  must declare alignment intent; Rust anchors them to the text baseline. Manual
  pixel offsets (`bounds.y() + 1`) are forbidden.
- Geometry write-back must not require Java-side coordinate translation chains
  (`moveLayoutPos`); Rust outputs absolute coordinates.

The reviewer checks every fix for **new Java geometry computation** and rejects
it (see §5 Stage 6, review item 4).

### 2.2 Migration: legacy rendering shrinks only

The legacy HostDraw path (Java `RenderContext.drawText` → Minecraft
`FontRenderer` 8×8 pixel font) and the retired Java layout pre-pass exist only as
migration leftovers. The direction of travel is one-way: **legacy can only
shrink, never grow**.

- New code and new blocks MUST render through the primitives/Rust-glyph path.
- The legacy usage inventory (see §4) is reviewed each round; the count must not
  increase.
- Dead leftovers (e.g. `Layouts.java` pre-pass remnants) are removed as soon as a
  round touches their area — deletion is a zero-risk cleanup, not a project.
- The end state is the complete removal of the legacy path (§4 milestones).

### 2.3 Acceptance: comfort over mere usability

The goal of this engine is a guidebook that is **comfortable to read**, not just
usable. "No overflow, no overlap, no missing glyphs" is the floor, not the
ceiling. A change is accepted only when it satisfies the comfort baseline (§3).
"Functionally correct" verdicts from any agent are never a substitute for
comfort-level review. An aesthetic review that reports a high score without
walking the §3 checklist item by item is treated as an unverified claim (§2.5).

### 2.4 Test assets are append-only

Regression test code and fixture acceptance sections (fixture TEST GOAL /
INVARIANTS headers, `ratchet/assertions.json`) must not be modified, weakened, or
deleted in the normal course of work. They may only be appended to.

If a test asset is genuinely wrong (the test, not the code, is at fault), it may
be modified **only in a dedicated commit** whose message states which asset
changed, why it was wrong, and why the new form is the correct expectation.
Weakening or deleting an assertion to make a failing run pass is strictly
forbidden: a failing assertion is evidence about the code, not about the test.

### 2.5 Objective evidence over self-report

A task or fix is complete only when demonstrated by objective evidence: a gate
command and its output, a render artifact, or an assertion run. Statements of the
form "should work" or "verified" without evidence carry no weight. AI sub-agent
self-reports are always re-verified independently by the executor. Comfort-level
claims (scores, "looks good") are subject to the same rule — they are valid only
when backed by the §3 checklist walk.

### 2.6 Ratchet discipline

When a fix is observable in render bounds JSON (node classes, geometry,
containment), a corresponding assertion **must** be appended to
`ratchet/assertions.json` in the same round. Assertions are append-only (§2.4).

The ratchet is a **first-line gate, not a final ceremony**: it runs inside every
screening round (§5 Stage 3), because mechanical assertions catch layout
regressions that visual screening misses (PF31: two regressions survived multiple
VLM rounds and were caught the moment the ratchet ran). Assertion coverage grows
continuously: geometric (current), plus alignment assertions (inline elements vs
first-line baseline within tolerance) and log assertions (glyph-atlas-full /
OutOfMemory warnings fail the round — see §5 Stage 3, log scan). Pixel-level
features that bounds JSON cannot observe remain under visual-inspection
acceptance until a suitable probe exists. The planned direction is automatic
assertion extraction from accepted render snapshots (see §10).

### 2.7 Documentation discipline

Every rendering or layout fix must leave the documentation at least as complete
as the code: the corresponding `docs/ISSUES.md` entry is updated (status, fix
commit, root-cause summary) in the same commit or the immediately following one;
if the fix surfaced a reusable lesson, an entry is added to `docs/PITFALLS.md`.
Issue records are permanent: entries are never deleted, status transitions are
recorded by updating the entry, and corrections are appended as dated notes, not
rewritten. Cross-referencing is mandatory: a fix commit references its issue
ID(s); an issue entry references its fix commit hash(es) and verification
evidence. Unreferenced fixes and unverified closures are incomplete work.

### 2.8 Fixture discipline

A fix is accepted only when the relevant fixture pages render in conformance with
their documented INVARIANTS; if implementation convenience conflicts with an
invariant, the invariant prevails. Fixture pages are test inputs: editing a
fixture to accommodate broken engine behaviour is forbidden (weakening the test
to fit the code). Legitimate fixture changes (new coverage, correcting a
genuinely wrong expectation) follow the dedicated-commit rule of §2.4. The
fixture corpus is extended to cover representative real-guidebook content that
user-facing rendering actually hits (callout blocks, large flowcharts, nested
tables, wide tables, legacy-path pages) — the set of pages a human can visually
inspect must be a superset of the set the engine renders in production (§10).

### 2.9 Audience, language, public/private

Tracked documentation serves human contributors and AI agents, is written in
formal English, present tense, declarative statements, and every factual claim
(command, path, class name, behaviour) is verifiable against the repository.
Public material (this directory, `tools/visual-inspection/`) must not contain
machine-local absolute paths, credentials, internal URLs, or personal data;
render output and logs under `run/`, scratch under `visualtest/local/`, and
machine-specific notes are private and git-ignored.

## 3. Comfort Baseline

Comfort is judged in two layers. The **objective layer** is machine-checkable and
belongs in geometric audit / ratchet scope. The **subjective layer** is judged by
aesthetic review (VLM agent, human owner) against this checklist — never against
an unstated impression. Both layers are mandatory per round.

### 3.1 Objective comfort checks (machine)

1. **Baseline alignment**: inline elements (task checkboxes, details markers,
   inline LaTeX, inline images, code spans) align to the first-line text baseline
   of their containing line within ≤ 2 px (extendable to an executable
   alignment-audit script over bounds JSON).
2. **Vertical rhythm**: block spacing is uniform relative to line height; no
   block gap reads as a missing line (heading-to-body spacing, callout inner
   gaps, list item spacing). The 1.55× line-height box means glyphs occupy only
   ~65% of the paragraph box — spacing computations must account for the inner
   box slack so visual gaps never stack into "blank lines" (§2.1: rhythm is
   computed in Rust, Java only declares intent).
3. **Table geometry**: cell content boxes align to column separators within the
   declared cell padding; no first-character gap, no separator crossing text.
4. **Font-family consistency**: within one page, the same semantic role renders
   in the same font family; no pixel-font/vector-font mixing (legacy-path pages
   are the known offenders, §4).
5. **Log hygiene**: a render round fails if the client log contains
   `glyph atlas full` or `OutOfMemory` warnings (§5 Stage 3 log scan).

### 3.2 Subjective comfort checks (aesthetic review checklist)

1. **Heading hierarchy rhythm**: monotonic size ladder, graded margins, no
   orphan gaps above/below headings.
2. **Block spacing**: spacing reads as intentional (not too airy, not cramped);
   callouts, tables, code blocks, images sit in the same rhythm as prose.
3. **Inline harmony**: links, code spans, LaTeX, tags, and markers are visually
   distinct from body text and from each other without shouting (colour, weight,
   decoration used sparingly and consistently).
4. **Typography coherence**: one font ecosystem per page; no element looks like
   it was rendered by a different pipeline.
5. **Colour comfort**: sufficient contrast on dark background, no garish
   combinations, semantic colours consistent across pages.

### 3.3 Scoring and the tuning/ratchet conflict

An aesthetic score is only meaningful against the checklist: report per-item
status, not a bare number. Because comfort tuning changes geometry, it conflicts
with frozen ratchet assertions by design. The conflict is managed, not ignored:
during a tuning round the executor registers the expected geometry deltas in
`docs/ISSUES.md` and temporarily exempts the affected assertions; at round end the
new geometry is verified and the assertions updated **append-only** (an assertion
whose semantic still holds is never deleted — §2.4, §2.6). Tuning that breaks
the ratchet without a registered expectation is a regression, not a tuning.

## 4. Rendering-Path Map

The map answers "which path renders this block, and who computes its geometry?".
It is reviewed and updated each round (§2.2: the legacy count must not grow).

### 4.1 Rust-authority path (compliant with §2.1)

Body paragraphs, headings, lists (content and marker hanging lines), code blocks,
table cell content, mermaid node content, floats, details — geometry computed by
the Rust engine, Java declares structure/style and consumes written-back bounds.

### 4.2 Legacy-path blocks (shrink list)

| Block | Path | Reason it violates §2.1/§2.2 |
|---|---|---|
| `LytGuidebookScene` slider labels | HostDraw → MC `FontRenderer` | Pixel font, Java geometry |
| `MediaWikiSpecialGeneratedBlock` (Special Index) | HostDraw → MC `FontRenderer` | Pixel font, Java geometry |
| Table vertical separators | Java `column.x` bookkeeping | Java re-derives line positions; must consume Rust column boundaries |
| Task-list checkbox | `bounds.y() + 1` manual offset | Java pixel math; must declare baseline intent |
| Inline LaTeX anchor | Java-computed `baselineAscent` | Java computes geometry; Rust must anchor |
| Geometry write-back | Java `moveLayoutPos` translation chains | Java moves coordinates; Rust should output absolute coords |
| `Layouts.java` pre-pass | **Live fallback** (Mermaid NodeContent fontHandle==0 path, tooltip/editor/annotation chains) | Not dead code — see §4.3 M1 |

### 4.3 Migration milestones

1. **M1 — Correct the legacy-inventory record, delete only true dead leftovers**:
   investigation (2026-08-02) found `Layouts.java` is NOT dead — it backs the
   Mermaid NodeContent Java fallback (fontHandle==0 startup window) and the
   tooltip/editor/annotation/page-title/inline-block layout chains. M1 scope is
   therefore: delete obsolete comments now; deleting `Layouts.java` itself
   requires two prerequisites (eliminate the Mermaid fallback by forcing font
   initialization, migrate the non-document layout chains) and is a later
   milestone, not a zero-risk cleanup.
2. **M2 — Converge blocks to primitives**: migrate the legacy-path blocks
   (slider labels, Special Index) to the Rust-glyph primitives path.
3. **M3 — Remove legacy infrastructure**: delete the `RenderContext.drawText`
   family and the `VanillaRenderContext` FontRenderer delegation.
4. **M4 — Clean FlatBuffer legacy fields**: remove schema fields left over from
   the legacy path.

Each milestone is a stage boundary in its own right; the path map marks
migration state.

## 5. Pipeline

```
fixtures ─▶ headless render ─▶ objective screening ─▶ aesthetic screening
   ▲            │                   │                        │
   └──────────── ratchet + ledger closeout ◀── adjudication ◀┘
```

### Stage 1 — Fixture Authoring

Fixtures are the test inputs. Each fixture page declares its own acceptance
criteria in a TEST GOAL / INVARIANTS header (conventions in `docs/FIXTURES.md`).
Fixture changes are test changes (§2.4, §2.8). Coverage includes
real-guidebook-representative pages (callouts, large flowcharts, nested/wide
tables, legacy-path pages) so that production-rendered content is always
inspectable (§2.8, §10).

### Stage 2 — Headless Batch Render

The corpus is rendered headlessly to PNG plus per-page bounds JSON. Batches are
limited to at most 40 pages (issue D1). **Multi-width screening is mandatory**:
render at both the test width (900) and the representative book width (~480);
width-dependent behaviours (wrapping, grid columns, table natural width, float
reflow, mermaid clipping) only manifest at the width you test (PF29, ISSUES §L).
Render narrow to a separate dir (`screenshots_narrow`) and run geometric with
`--page-width 960`. Commands in `docs/USAGE.md`.

### Stage 3 — Objective Screening

Four automated passes over the latest render — all run **every round**:

1. **Geometric rules** over bounds JSON (overflow, zero-size, sibling
   intersection; calibrated false-positive handling in ISSUES §E/PF19).
2. **Ratchet** (`assert_bounds.py` against the latest render): any `not ok` line
   is a confirmed regression to fix before proceeding (§2.6).
3. **Log scan**: the client log of the round must be free of `glyph atlas full`
   and `OutOfMemory` warnings (§3.1.5); a log-warning round is a failed round
   with the warning itself as the issue evidence.
4. **Alignment/geometry audit** (as probes exist): bounds-JSON checks for
   baseline alignment and table-cell/separator alignment (§3.1.1, §3.1.3). VLM
   sight is not a substitute for geometry here — pixel/geometry inspection is
   the adjudication layer for spacing and alignment claims.

### Stage 4 — Aesthetic Screening

Aesthetic review (VLM agent with image modality, or the human owner) judges the
comfort baseline §3.2 against the page set, **checklist item by item**, and
reports per-item status plus a score derived from the checklist. Reviewers with
no image modality may cross-check structure against bounds JSON but never claim
comfort. Known failure mode: an agent scores high because "no hard defects" —
that is usability, not comfort (§2.3). The checklist is the prompt contract;
direction-free critique is reserved for designated tuning programs (see §10).

### Stage 5 — Adjudication

A high-capability reviewer (human or K3-class agent) merges objective and
aesthetic findings into verdicts: intended behaviour, false positive, fixture
defect, real engine issue, or comfort gap. Verdicts enter `docs/ISSUES.md` with
page evidence. The adjudicator owns the ledger. Comfort gaps are first-class
issues with the same lifecycle as defects.

### Stage 6 — Unified Fix Round

Fixes are executed under executor orchestration:

1. **Decompose.** The issue ledger is decomposed into atomic tasks, each with a
   whitelist of modifiable files and objective acceptance criteria (which
   include the relevant §3 comfort items where the issue is comfort-class).
2. **Dispatch.** Each task is dispatched to an implementer with complete context;
   implementers never decide acceptance.
3. **Objective verification.** The executor runs the gate commands itself.
   Implementer self-reports are never trusted (§2.5).
4. **Independent review.** The reviewer checks the diff against the whitelist,
   the acceptance criteria, and the known failure modes (deleted tests,
   commented-out errors, empty catches, scope creep, silent downgrades,
   patch-style special-casing), plus four architecture-specific checks:
   (a) no new Java geometry computation (§2.1);
   (b) no new legacy-path usage (§2.2);
   (c) no test-asset weakening (§2.4);
   (d) no new unregistered assumptions about the rendering path being live
   (PF1: verify the actual construction site before tuning parameters).
5. **Accept or return.** Rejections return with evidence; acceptance criteria may
   only change by owner decision, never unilaterally.

A task that fails three dispatched attempts is escalated to the owner with
evidence and options; the executor does not relax criteria to force closure.

### Stage 7 — Ratchet & Ledger Closeout

After verification, the executor updates `docs/ISSUES.md` (status transitions,
fix commits, residual registrations), `docs/PITFALLS.md` (reusable lessons), and
the rendering-path map (§4). The round ends with an integral acceptance pass:
full-corpus render at both widths, the gate, the ratchet, the log scan, and a
spot-check against fixture INVARIANTS and the §3 checklist — guarding against
"every part passes, the whole does not".

## 6. Issue Status Lifecycle

```
OPEN ─▶ FIX-PENDING-VERIFY ─▶ VERIFIED ─▶ CLOSED
 │              │
 │              └─▶ OPEN (rejected, returned with evidence)
 ├─▶ REGISTERED-PREEXISTING   (baseline-confirmed, predates the cycle)
 ├─▶ PENDING-IN-GAME          (not reproducible offline; in-game gold standard)
 └─▶ DEFERRED                 (owner decision, rationale recorded)
```

Comfort-gap issues use the same lifecycle; a fix is VERIFIED only when its §3
items pass.

## 7. Roles

| Role | Responsibility |
|---|---|
| Owner (human) | Adjudication of ambiguous verdicts, escalation decisions, acceptance-criteria changes, in-game gold-standard checks, comfort-level arbitration. |
| Executor | Decomposition, dispatch, objective verification, review orchestration, ledger upkeep, integral acceptance, path-map upkeep. Never writes production code. |
| Implementer | Executes one atomic task inside its file whitelist; reports completions, deviations, assumptions. |
| Reviewer | Independent verification only; never fixes. Evidence before claims; architecture checks of §5 Stage 6.4. |
| Screener (objective) | Geometric / ratchet / log / alignment passes; produces findings, not verdicts. |
| Aesthetic reviewer | Comfort-baseline checklist walk (§3.2); produces per-item findings, never verdicts. |

## 8. Quality Gates (all mandatory before a round closes)

1. `./gradlew compileJava compileTestJava test runLayoutDump` ends with
   `==== TOTAL ISSUES: 0 ====`.
2. The assertion ratchet exits 0 against the latest full-corpus render.
3. The round's client log is free of `glyph atlas full` / `OutOfMemory` warnings.
4. Objective comfort checks (§3.1) pass where probes exist.
5. Aesthetic review completed the §3.2 checklist on every page touched by the
   round.
6. `docs/ISSUES.md` reflects reality: every fix cross-referenced, every residual
   registered; the rendering-path map (§4) is current.

## 9. Commit Discipline

- Fix commits reference issue IDs (§2.7).
- Documentation updates land in the same commit as the fix or the immediately
  following one (§2.7).
- Test-asset modifications are dedicated commits with stated rationale (§2.4).
- Public/private classification is respected before every commit (§2.9).

## 10. Cross-Cutting Programs

- **Font library & reader settings (design program)**: a future navigation-bar
  settings panel will let readers choose fonts and other display options. Until
  then, font-family work (e.g. monospace for code) must be designed as
  **configurable** (schema field + Rust multi-family registration + fallback
  chain for CJK/Latin mixing), never hard-coded, so the settings system only
  feeds the injection point. This program owns the FlatBuffer `font_family`
  schema extension; it is a design-discussion deliverable, not an ad-hoc fix.
- **Real-guidebook corpus**: production guidebook chapters are rendered as
  screening corpus alongside fixtures so the human-inspectable set covers what
  the engine actually renders (§2.8).
- **Automatic assertion extraction**: derive ratchet assertions from accepted
  render snapshots to replace hand-written coverage gaps (§2.6).
- **Comfort tuning programs**: designated rounds where the aesthetic reviewer
  critiques freely (no prescribed direction) against a comfort target; the
  tuning/ratchet conflict is handled per §3.3.

### Stage 6 — Infrastructure Protocol (binding on all implementer dispatches)

Implementers never run shared or expensive infrastructure commands (gradle
builds, tests, gates, cargo builds, DLL copies, headless renders, ratchet runs).
Parallel implementers competing for the build daemon caused resource
contention; verification is the executor's sole duty. Instead, dispatches carry
two tripwire rules:

1. **Infrastructure tripwire**: the moment an implementer needs a shared
   command, it stops and files an infrastructure request (the
   `infrastructure-request` slot of its report) instead of running it.
2. **Boundary tripwire**: the moment implementation requires touching files
   outside the whitelist (Rust sources, dependencies, fixtures), the implementer
   stops and reports it — it never extends its own scope.

The executor serializes and batches all verification commands after implementer
reports return, and routes the results back (re-dispatch or acceptance input).
Implementer self-verification is limited to static self-checks: diff
completeness, whitelist compliance, ban-list compliance, logic review.
