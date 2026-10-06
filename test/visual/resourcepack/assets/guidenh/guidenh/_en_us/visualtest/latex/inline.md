---
navigation:
  title: Inline LaTeX Formulas
  position: 8600
---

TEST GOAL: inline LaTeX formula baseline alignment and line-height expansion (backlog: inline dropped-line reproduction)

INVARIANTS: the baseline is aligned and the horizontal position is correct within the text flow; fractions that expand the line do not overlap adjacent text

## Dollar Shorthand

Expected: Inline formulas rendered at text baseline with correct horizontal position.

Text with $E=mc^2$ inserted in the middle of a sentence, and also with $\frac{1}{2}$ and $\sqrt{x}$ in regular paragraph text.

## Fraction Line Height Expansion

Expected: Line height expands to accommodate tall formulas; text before and after is not crushed.

A sentence that contains $\frac{1}{2}$ and also $\frac{a+b}{c-d}$ in the same line must not drop or collapse either formula.

## Tag Syntax (Same-Line Minimal Reproduction)

Expected: Same-line placement with `<Latex>` tag form; identical behavior to dollar shorthand.

A sentence that contains <Latex formula="\frac{1}{2}" /> and also <Latex formula="\frac{a+b}{c-d}" /> in the same line must render both without line break or overlap.

## Color

Expected: Formula rendered in non-default color (#RRGGBB).

This is <Latex formula="E=mc^2" color="#FFD700" /> rendered in gold color.

## Scale

Expected: Formula scaled up relative to text.

Scaled up (scale=1.5): <Latex formula="\pi" scale="1.5" /> in the middle of text.

## Vertical Alignment Variants

Expected: Each valign mode positions the formula correctly within the text line.

Default (baseline): <Latex formula="\frac{a}{b}" /> aligns math baseline with text baseline.

Top: <Latex formula="x^2" valign="top" /> is flush with the line top.

Center: <Latex formula="E=mc^2" valign="center" /> centered on text line.

Bottom: <Latex formula="\frac{a}{b}" valign="bottom" /> sits on the text line bottom.

## Tooltip

Expected: Hovering over the formula shows tooltip text.

Hover for source: <Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}" showTooltip={true} />

Custom tooltip: <Latex formula="E=mc^2" tooltip="Energy equals mass times the speed of light squared." />

## Offset

Expected: Formula shifted from default position by offsetX/offsetY pixels.

Nudged: <Latex formula="E=mc^2" offsetX="2" offsetY="-1" /> right 2px and up 1px.
