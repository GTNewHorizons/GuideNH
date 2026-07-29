# Contribution Principles — GuideNH Visual Test System

These principles bind every change to the visual test system: fixtures, tools,
assertions, documentation, and any engine fix that the system observes. They apply
equally to human contributors and AI agents.

## §1 Audience and Language

Tracked documentation is written for two audiences: human contributors and AI
agents. It must use formal English, present tense, and declarative statements.
Conversational tone, session references ("as discussed above"), and speculative
claims are not permitted. Every factual claim (command, path, class name, behaviour)
must be verifiable against the repository.

## §2 Public vs Private Material

| Classification | Location | Tracked |
|---|---|---|
| Public documentation, fixtures, assertions | `visualtest/`, `tools/visual-inspection/` | Yes |
| Private working notes | `visualtest/local/` | No (git-ignored) |
| Render output, logs | `run/` | No (git-ignored) |
| Credentials, machine configuration | `.env*` | No (git-ignored) |

Public material must not contain machine-local absolute paths, credentials,
internal URLs, or personal data. Use repository-relative paths or `<...>`
placeholders. When in doubt, keep the material private.

## §3 Test Assets Are Append-Only

Regression test code (`src/test/`) and fixture acceptance sections (the
TEST GOAL / INVARIANTS headers in fixture pages, and `ratchet/assertions.json`)
must not be modified, weakened, or deleted in the normal course of work. They may
only be appended to.

If a test asset is genuinely wrong (the test, not the code, is at fault), it may be
modified **only in a dedicated commit** whose message states:

1. which test asset changed,
2. why it was wrong,
3. why the new form is the correct expectation.

Weakening or deleting an assertion to make a failing run pass is strictly
forbidden; a failing assertion is evidence about the code, not about the test.

## §4 Fixing a Layout Issue Requires Updating the Documentation

Every rendering or layout fix must leave the documentation at least as complete as
the code:

1. The corresponding entry in `docs/ISSUES.md` must be updated (status, fix commit,
   root-cause summary) in the same commit or the immediately following one.
2. If the fix surfaced a reusable lesson (a trap future contributors could
   re-trigger), an entry must be added to `docs/PITFALLS.md`.

Experience gained during fixes is shared through the documentation, not left in
chat logs or commit messages alone.

## §5 Issue Records Are Permanent

Entries in `docs/ISSUES.md` are never deleted. Status transitions
(`OPEN → FIX-PENDING-VERIFY → VERIFIED → CLOSED`, or `REGISTERED-PREEXISTING`,
`PENDING-IN-GAME`, `DEFERRED`) are recorded by updating the entry. Corrections are
appended as dated notes, not rewritten.

## §6 Cross-Referencing

- A fix commit message references the issue ID(s) it addresses (e.g. `A5`, `N1`).
- An issue entry references its fix commit hash(es) and its verification evidence
  (gate command, render timestamp, or assertion id).
- Unreferenced fixes and unverified closures are both considered incomplete work.

## §7 Fixture Invariants Are the Acceptance Authority

A fix is accepted only when the relevant fixture pages render in conformance with
their documented INVARIANTS. If implementation convenience conflicts with an
invariant, the invariant prevails. Changing an invariant is a test-asset change
and follows §3's dedicated-commit rule.

## §8 Objective Evidence over Self-Report

A task or fix is complete only when demonstrated by objective evidence: a gate
command and its output, a render artifact, or an assertion run. Statements of the
form "should work" or "verified" without evidence carry no weight. This rule
applies to human contributors and is mandatory for AI sub-agents, whose
self-reports must always be re-verified independently.

## §9 Ratchet Discipline

When a fix is observable in the render bounds JSON (node classes, geometry,
containment), a corresponding assertion must be appended to
`ratchet/assertions.json` in the same round. Assertions follow the append-only
rule of §3. Pixel-level features that bounds JSON cannot observe (colour, glyph
content, primitive-level drawing) are out of the ratchet's scope and remain under
visual-inspection acceptance until a suitable probe exists.

## §10 Fixture Changes Are Test Changes

Fixture pages are test inputs. Editing a fixture to accommodate broken engine
behaviour is forbidden (that is weakening the test to fit the code). Legitimate
fixture changes (new coverage, correcting a genuinely wrong expectation) follow
the dedicated-commit rule of §3.
