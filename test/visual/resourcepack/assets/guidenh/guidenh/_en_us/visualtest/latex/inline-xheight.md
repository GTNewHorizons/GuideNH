---
navigation:
  title: Inline LaTeX x-height Calibration
  position: 8610
---

TEST GOAL: inline formula x-height matches the x-height of lowercase body text (T4 real x_height measurement, replacing the 0.625 approximation; under 0.625 inline formulas were oversized, F5-3)

INVARIANTS: the `$x$` formula renders at about the height of a body-text x; fractions/radicals/summations render normally; no compile errors

## Control: Formula x vs Body x

Expected: The inline formula x renders at the same height as the lowercase body x, because both derive from the same real x_height metric (T4).

Body x x x followed by the inline formula: x x x $x$.

## Magnification: Fractions, Radicals, Sums

Expected: Tall inline formulas magnify any x_height ratio error into a visible pixel mismatch while rendering inline without breaking or overlapping.

Mixing $\frac{1}{2}$, $\sqrt{x}$, and $\sum_{i=1}^{n} i$ into the same line as regular body text amplifies a mis-scaled inline factor into a visible size difference; the fraction must expand the line without crushing adjacent text.
