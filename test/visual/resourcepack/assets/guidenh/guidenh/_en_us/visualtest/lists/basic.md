---
navigation:
  title: Lists Basic
  position: 8900
---

TEST GOAL: three levels of unordered lists, three levels of ordered lists including start, mixed nesting

INVARIANTS: indentation and spacing are consistent across levels

## Unordered Three Levels

Expected: Three levels of unordered list rendered with `*`, `-`, `+` markers; indentation and spacing consistent per level.

* Level one item A
* Level one item B
  - Level two item A
  - Level two item B
    + Level three item A
    + Level three item B
* Level one item C

## Ordered Three Levels with Start

Expected: Ordered list starting at 3 with three levels of nesting; numbering restarts per nested level.

3. First ordered item (start=3)
4. Second ordered item
   1. Nested level two A
   2. Nested level two B
      1. Nested level three A
      2. Nested level three B
5. Third ordered item

## Mixed Nesting

Expected: Unordered and ordered lists intermix within a single tree; indentation clearly shows hierarchy.

* Root category A
  1. Step one
  2. Step two
     - Sub detail alpha
     - Sub detail beta
* Root category B
  - Item one
    1. Numbered sub-step
    2. Numbered sub-step
  - Item two

## Ordered Multi-Digit Markers (start=8)

Expected: Ordered list starting at 8; the 1→2 digit transition (9. → 10.) keeps every marker right-aligned to the same gutter — 10., 11., 12. extend further left than the single-digit markers, and every marker shares the item's first-line baseline.

8. Item eight
9. Item nine
10. Item ten
11. Item eleven
12. Item twelve
