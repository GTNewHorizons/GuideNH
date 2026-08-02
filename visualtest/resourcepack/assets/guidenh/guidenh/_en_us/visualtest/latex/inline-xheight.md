---
navigation:
  title: Inline LaTeX x-height Calibration
  position: 8610
---

TEST GOAL / 测试目标：内联公式 x-height 与正文小写 x 高度一致（T4 真实 x_height 度量，替换 0.625 近似；0.625 时代内联公式尺寸偏大 F5-3）

INVARIANTS / 不变式：公式 `$x$` 渲染高度≈正文 x；分数/根式/求和正常渲染；无编译错误

## Control: Formula x vs Body x

Expected: The inline formula x renders at the same height as the lowercase body x, because both derive from the same real x_height metric (T4).

Body x x x followed by the inline formula: x x x $x$.

## Magnification: Fractions, Radicals, Sums

Expected: Tall inline formulas magnify any x_height ratio error into a visible pixel mismatch while rendering inline without breaking or overlapping.

Mixing $\frac{1}{2}$, $\sqrt{x}$, and $\sum_{i=1}^{n} i$ into the same line as regular body text amplifies a mis-scaled inline factor into a visible size difference; the fraction must expand the line without crushing adjacent text.
