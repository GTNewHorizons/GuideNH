---
navigation:
  title: LaTeX Industrial Audit
  position: 8580
---

TEST GOAL: LaTeX industry-standard audit (quantitative diagnosis of font size/baseline/spacing/texture)

## A. x-height parity

Expected: formula body font size = body text font size (unified standard from task A; x-height follows each font's ratio, measured formula ≈ 0.92× body text, not the old inline target of 1.2×); baselines coincide exactly.

Calib text: x xg E Eg H qi baseline reference.

A $x$ B $xg$ C $E$ D $Eg$ E $H$ F $qi$ G end.

## B. Baseline precision

Expected: each formula baseline coincides with the text baseline at pixel level; the fraction bar sits on the math axis (slightly above the middle of the x-height).

Base $x$ $a$ $g$ $y$ $E$ $f$ $\frac{x}{x}$ $\sqrt{x}$ $\frac{g}{y}$ end.

## C. Scripts and fraction sizing

Expected: superscripts and subscripts are about 0.7× the body size; textstyle fractions shrink internally.

Text $x^2$ $x_i$ $e^{-x^2}$ $\frac{a+b}{c-d}$ $2^{2^2}$ done.

## D. Display body-size parity

Expected: display formula body glyph size = body text size (browser standard: display math at body font size).

Body ref: E x H mc.

$$E=mc^2$$

$$x^2+y^2=z^2$$

## E. Display vertical spacing

Expected: vertical spacing above and below display blocks is consistent and generous; adjacent display blocks are clearly separated.

$$\Delta G = \Delta H - T\Delta S$$

$$\oint_C \mathbf{E} \cdot d\mathbf{l} = -\frac{d}{dt}\iint_S \mathbf{B} \cdot d\mathbf{S}$$

Following paragraph after displays.

## F. Display fraction interior

Expected: displaystyle fraction numerators and denominators are close to the body size (the displaystyle convention), clearly larger than the interior of textstyle fractions.

$$\frac{a+b}{c-d}$$

Inline version for contrast: $\frac{a+b}{c-d}$.

## G. Big operators and limits

Expected: display uses large operators with limits above and below; inline uses small operators with limits at the side.

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2} \qquad \sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}$$

Inline: $\int_0^\infty e^{-x^2}\,dx$ and $\sum_{n=1}^{\infty} \frac{1}{n^2}$.

## H. Matrix and delimiters

$$\begin{pmatrix} a & b \\ c & d \\ \end{pmatrix} \begin{pmatrix} x \\ y \\ \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \\ \end{pmatrix}$$

## I. Stroke sharpness

Expected: formula strokes are as sharp as text strokes (scaling-blur check).

Text strokes: I I I l l l H |.

$I$ $l$ $H$ $\int$ $\sum$ $\pi$

## J. Mixed-size line growth

Expected: when a line contains a tall formula the line height expands and the lines above and below are not intruded upon; horizontal spacing around the formula is natural.

Before line $\frac{a}{b}$ after $\sqrt{x^2+y^2}$ tail text continues here for wrap test and more words to fill the line so wrapping occurs naturally in this paragraph.
