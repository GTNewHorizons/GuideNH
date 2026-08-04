---
navigation:
  title: Callout Combinations
  position: 8265
---

TEST GOAL / 测试目标：多类型 callout 同页组合（info/warn/danger 等 alert + 带标题/不带标题自定义 quote + 多行内容）

INVARIANTS / 不变式：alert 框带类型色左边框与标题行；自定义 quote（含 title= 与 color-only 指令）落入朴素引用块路径：灰底、2px 灰左边框、无标题行；多行内容全部容纳在框内；无红色错误文本

Syntax note: in the current engine the `[!NOTE]`-style marker is consumed by the markdown parser, so alert detection keys on the first body text starting with the alert keyword (`NOTE`/`TIP`/`IMPORTANT`/`WARNING`/`CAUTION`). The blocks below therefore open their body with the keyword.

## Info Alert (NOTE)

Here it should: render a Note alert box with a single-line body.

> [!NOTE]
> Note: a short info callout with one body line.

## Info Alert With Multi-Line Body

Here it should: render a Note alert box whose body spans three paragraphs — all inside one alert box with the title row.

> [!NOTE]
> Note: first paragraph of a multi-paragraph info callout.
>
> Note: second paragraph separated by a `>`-marked blank line, still inside the same alert box.
>
> Note: third paragraph closes the multi-line body.

## Warn Alert (WARNING)

Here it should: render a Warning alert box with a two-line body.

> [!WARNING]
> Warning: first line of the warning callout.
> Warning: second line stays in the same alert body.

## Danger Alert (CAUTION)

Here it should: render a Caution alert box immediately after the warning — different alert types stack on the same page without visual bleed.

> [!CAUTION]
> Caution: a danger callout that follows the warning directly.

## Tip And Important Stack

Here it should: render Tip and Important alerts back to back to exercise adjacent-type transitions.

> [!TIP]
> Tip: a single-line tip callout.

> [!IMPORTANT]
> Important: a single-line important callout.

## Custom Quote With Title

Here it should: render a quote directive WITH `title=` — per `BlockquoteCompiler` the `title` attribute produces no header row: the directive falls through to the plain-blockquote path exactly like the colour-only directive below (grey 2px left border, no header row), and only the body line renders.

> {: title="Custom Info" color="#638ef1" }
> Custom quote body with a header title row.

## Custom Quote Without Title

Here it should: render a quote directive WITHOUT `title=` or `icon=` — per `BlockquoteCompiler` such a directive falls through to the plain-blockquote path: grey 2px left border, no header row, and the body below.

> {: color="#e46150" }
> A bare colour-only directive renders as a plain blockquote without a title row.

## Custom Quote With Multi-Line Body

Here it should: render the same `title=` directive with a multi-line body — as above, `title=` produces no header row and the directive falls through to the plain-blockquote path (grey 2px left border, no header row); the two paragraphs stack inside the same grey quote box.

> {: title="Long Custom Quote" color="#61b75d" }
> First paragraph of the custom quote body.
>
> Second paragraph of the custom quote body, still inside the same box.
