---
navigation:
  title: Display LaTeX Formulas
  position: 8590
---

TEST GOAL / 测试目标：展示 LaTeX 公式居中渲染、缩放、颜色与 tooltip

INVARIANTS / 不变式：水平居中；缩放不溢出；段距一致

## Dollar Double-Dollar Shorthand

Expected: Formula rendered as centered block with vertical margins above and below.

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}$$

## Tag Syntax

Expected: Block-level `<Latex>` tag renders centered formula with vertical spacing.

<Latex formula="\oint_C \mathbf{E} \cdot d\mathbf{l} = -\frac{d}{dt}\iint_S \mathbf{B} \cdot d\mathbf{S}" />

<Latex formula="\Delta G = \Delta H - T\Delta S" />

## Scale

Expected: Formula scaled up by userScale=1.5 renders larger but still centered; does not overflow container.

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" scale="1.5" />

## Color

Expected: Formula glyph colour overridden by color attribute.

<Latex formula="E=mc^2" color="#FFD700" />

<Latex formula="\sqrt{x^2+y^2}" color="#55CCFF" />

## Tooltip

Expected: Hovering over display formula shows tooltip.

<Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}" showTooltip={true} />

<Latex formula="E=mc^2" tooltip="Mass-energy equivalence in display context." />

## Vertical Alignment (display context)

Expected: valign attribute is silently ignored in display context; formula renders at default centered position.

<Latex formula="\frac{a}{b}" valign="top" />

<Latex formula="x^2 + y^2 = z^2" valign="bottom" />
