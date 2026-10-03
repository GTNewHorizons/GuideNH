---
navigation:
  title: Error Fallback Width (F-N2)
  position: 8935
---

TEST GOAL (F-N2 regression sentinel): when ItemLink/ItemImage points at a nonexistent item the error fallback segment must keep its real width, and following body text must not overlap the error segment

INVARIANTS: each error fallback segment (nested LytParagraph) keeps a real width of ≥200px in the bounds JSON (guarding against width collapse); the body text immediately following the error segment continues on the same line (trailing text start x = error segment x + error segment w > error segment x + 20); the error segment and the body text do not overlap each other (zero sibling_intersection hits). Assertion blind spot: the current ratchet (all LytParagraph w≥200) locks in the "error segment width collapses to zero width" regression mode but does not cover the "error node never emitted at all" mode (the harness has no overlap rule yet; the trailing-x quantity is verified manually) — it is suggested that the harness add sibling overlap assertions later and lock this down

This page intentionally renders red error-fallback text for non-existent items. Every case below is a single paragraph: [prefix text] + [error fallback] + [trailing prose]. The trailing prose must start strictly right of the error segment — if the fallback width ever collapses to near zero again, the trailing text lands on top of the error text and this page's ratchet assertion fails.

## Missing ItemImage With Trailing Prose

Here it should: render the error fallback for a non-existent item, then the trailing prose continues on the same line to the right of the error segment.

<ItemImage id="minecraft:example_nonexistent_item" /> after this the body prose continues on the same line, well to the right of the error text.

## Missing ItemImage Inside A Sentence

Here it should: render a missing-item error mid-sentence with prose both before and after — the error sits inline after the prefix and the suffix resumes right after it.

Before the missing <ItemImage id="minecraft:not_a_real_item" /> after it the rest of the sentence continues normally without overlapping the error.

## Missing ItemLink With Trailing Prose

Here it should: render an ItemLink error fallback whose following prose stays on the same line after the error segment.

<ItemLink id="minecraft:ghost_ingot" /> after this the body prose continues on the same line, well to the right of the error text.

## Missing ItemLink Inside A Sentence

Here it should: render an ItemLink error mid-sentence — the error reserves real width so the following clause stays clear of it.

A link to the fictional <ItemLink id="minecraft:fictional_block" /> is expected to fail, and the clause after it must still sit to the right.

## Two Consecutive Missing Items

Here it should: render two error fallbacks in a row — both reserve width and the second sits after the first, with trailing prose after both.

<ItemImage id="minecraft:missing_a" /> <ItemImage id="minecraft:missing_b" /> and the trailing prose follows both error segments on the same line.

## Missing ItemImage At Paragraph Start

Here it should: render an error fallback that opens the paragraph — the trailing prose must begin strictly to the right of the error's right edge.

<ItemImage id="minecraft:oops_no_item" /> opening error at the paragraph start, followed by prose that must not overlap it.
