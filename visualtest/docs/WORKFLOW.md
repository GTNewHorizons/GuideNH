# Workflow — GuideNH Visual Feedback Loop

## Purpose

This document defines the operating procedure of the visual test system: the
pipeline stages, the roles, the issue status lifecycle, the quality gates, and the
commit discipline. Tool commands live in `docs/USAGE.md`; binding rules live in
`docs/PRINCIPLES.md`.

## Pipeline Overview

```
fixtures ──▶ headless render ──▶ screening ──▶ adjudication ──▶ unified fix round
   ▲                                                                   │
   └──────────── ratchet + ledger closeout ◀───────────────────────────┘
```

### Stage 1 — Fixture Authoring

Fixtures are the test inputs. Each fixture page declares its own acceptance
criteria in a TEST GOAL / INVARIANTS header. Conventions and the per-file
specification are defined in `docs/FIXTURES.md`. Fixture changes are test changes
(`docs/PRINCIPLES.md` §10).

### Stage 2 — Headless Batch Render

The corpus is rendered headlessly to PNG screenshots plus per-page bounds JSON
(node class and geometry). Batches are limited to at most 40 pages (issue D1).
Render output is private (git-ignored `run/`). Commands in `docs/USAGE.md`.

### Stage 3 — Screening

Cheap automated passes reduce human review load: geometric rules over bounds JSON,
and VLM screenshot screening. Screeners produce findings, not verdicts. Known
screener failure modes (tile seams, overlay misreads, right-edge false positives)
are recorded in `docs/ISSUES.md` section E.

### Stage 4 — Adjudication

A high-capability reviewer (human or K3-class agent) merges findings into
verdicts: intended behaviour, false positive, fixture defect, or real engine
issue. Verdicts enter `docs/ISSUES.md` with page evidence. The adjudicator owns
the ledger.

### Stage 5 — Unified Fix Round

Fixes are executed under executor orchestration:

1. **Decompose.** The issue ledger is decomposed into atomic tasks, each with a
   whitelist of modifiable files and objective acceptance criteria.
2. **Dispatch.** Each task is dispatched to an implementer (human or coder agent)
   with complete context; implementers never decide acceptance.
3. **Objective verification.** The executor runs the gate commands itself.
   Implementer self-reports are never trusted (PRINCIPLES §8).
4. **Independent review.** A reviewer with no stake in the implementation checks
   the diff against the whitelist, the acceptance criteria, and the known failure
   modes (deleted tests, commented-out errors, empty catches, scope creep, silent
   downgrades, patch-style special-casing).
5. **Accept or return.** Rejections return with evidence; acceptance criteria may
   only change by owner decision, never unilaterally.

A task that fails three dispatched attempts is escalated to the owner with the
failure evidence and options; the executor does not relax criteria to force
closure.

### Stage 6 — Ratchet

Every fix that is observable in bounds JSON gains an assertion in
`ratchet/assertions.json` (PRINCIPLES §9). The ratchet command exits 0 when all
assertions hold and 1 otherwise, and can emit JUnit XML. It is CI-ready by design
but is not yet wired into a CI pipeline; the intended future pipeline is:

```
render (batch) ─▶ assert_bounds.py ─▶ exit code gate ─▶ JUnit report
```

### Stage 7 — Ledger Closeout

After verification, the executor updates `docs/ISSUES.md` (status transitions,
fix commits, residual registrations) and `docs/PITFALLS.md` (reusable lessons).
The round ends with an integral acceptance pass: a full-corpus render, the gate,
the ratchet, and a spot-check against fixture INVARIANTS — guarding against
"every part passes, the whole does not".

## Issue Status Lifecycle

```
OPEN ─▶ FIX-PENDING-VERIFY ─▶ VERIFIED ─▶ CLOSED
 │              │
 │              └─▶ OPEN (rejected, returned with evidence)
 ├─▶ REGISTERED-PREEXISTING   (baseline-confirmed, predates the cycle)
 ├─▶ PENDING-IN-GAME          (not reproducible offline; in-game gold standard)
 └─▶ DEFERRED                 (owner decision, rationale recorded)
```

## Roles

| Role | Responsibility |
|---|---|
| Owner (human) | Adjudication of ambiguous verdicts, escalation decisions, acceptance-criteria changes, in-game gold-standard checks. |
| Executor | Decomposition, dispatch, objective verification, review orchestration, ledger upkeep, integral acceptance. Never writes production code. |
| Implementer | Executes one atomic task inside its file whitelist; reports completions, deviations, and assumptions. |
| Reviewer | Independent verification only; never fixes. Evidence before claims. |
| Screener | Geometric/VLM first pass; produces findings, not verdicts. |

## Quality Gates (all mandatory before a round closes)

1. `./gradlew compileJava compileTestJava test runLayoutDump` ends with
   `==== TOTAL ISSUES: 0 ====` (emitted by `LayoutPipelineHarness` in `src/test/`,
   which is currently git-ignored; the string is not greppable in tracked sources).
2. The assertion ratchet exits 0 against the latest full-corpus render.
3. Visual inspection confirms the fixture INVARIANTS of every page touched by the
   round.
4. `docs/ISSUES.md` reflects reality: every fix cross-referenced, every residual
   registered.

## Commit Discipline

- Fix commits reference issue IDs (PRINCIPLES §6).
- Documentation updates land in the same commit as the fix or the immediately
  following one (PRINCIPLES §4).
- Test-asset modifications are dedicated commits with stated rationale
  (PRINCIPLES §3).
- Public/private classification is respected before every commit (PRINCIPLES §2).
