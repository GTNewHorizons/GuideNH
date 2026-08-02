# GuideNH Visual Test System

A fixture-driven visual regression system for the GuideNH guide engine: a curated
page corpus is rendered headlessly, screened, adjudicated, fixed, and protected by
an executable assertion ratchet.

## Audience

This documentation set serves both human contributors and AI agents. All tracked
documents are written in formal English and must remain free of machine-local
paths, credentials, and personal data (see `docs/WORKFLOW.md` §2.9).

## Document Map

| Document | Responsibility |
|---|---|
| `docs/WORKFLOW.md` | The visual feedback loop: stages, roles, status lifecycle, quality gates, commit discipline. |
| `docs/USAGE.md` | How to run renders, the watchdog, list files, the verification gate, and the assertion ratchet. |
| `docs/RESOURCES.md` | Resource index: fixture folder map, tool inventory, source-code map, corpus assets. |
| `docs/FIXTURES.md` | Per-file fixture specifications and fixture authoring conventions. |
| `docs/ISSUES.md` | Issue registry: adjudication ledgers, fix closure ledger, new and pre-existing issues. |
| `docs/PITFALLS.md` | Engine traps and lessons learned during fix rounds. Read before touching related code. |
| `docs/PRINCIPLES.md` | Historic stub — principles integrated into `docs/WORKFLOW.md` §2/§3. |
| `ratchet/assertions.json` | Executable regression assertions over render bounds JSON (see `docs/WORKFLOW.md` §6). |

## Quick Start

1. Render the corpus headlessly (commands in `docs/USAGE.md`).
2. Run the verification gate: `./gradlew compileJava compileTestJava test runLayoutDump`
   must end with `==== TOTAL ISSUES: 0 ====` (harness lives in git-ignored `src/test/`).
3. Run the ratchet:
   `py -3 tools/visual-inspection/assert_bounds.py --shots <screenshots-dir> --assertions visualtest/ratchet/assertions.json`
   must exit 0.

## Public vs Private

Tracked (public): this directory's documentation, the fixture corpus, and the
ratchet assertions. Untracked (private): render output under `run/`, local scratch
under `visualtest/local/`, API credentials, and machine-specific notes. Details in
`docs/WORKFLOW.md` §2.9.
