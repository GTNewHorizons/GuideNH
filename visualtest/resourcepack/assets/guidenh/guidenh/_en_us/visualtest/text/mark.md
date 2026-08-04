---
navigation:
  title: Mark Tag (Text Highlight)
  position: 8995
---

TEST GOAL / 测试目标：`<mark>` 文本高亮标签（默认色 + `color=` 自定义色）渲染

INVARIANTS / 不变式：mark 高亮背景装饰存在；颜色不泄漏到后续文本；与其它行内标记混合排版正常

Syntax reference: `MarkTagCompiler` — `<mark>text</mark>` applies the default mark background; `<mark color="#RRGGBB">text</mark>` overrides it.

## Default Mark Background

Here it should: render the highlighted span with the engine's default mark background colour (dark golden), inline with surrounding prose.

This sentence contains a <mark>highlighted term</mark> that must stand out from the body text.

A longer highlighted run: <mark>engineers mark important references like this entire phrase so the reader can scan for them quickly</mark> and the wrapping stays clean.

## Custom Mark Colour

Here it should: render the highlighted span with the custom background colour given by `color="#"` — distinct from the default highlight.

<mark color="#4C7AFF">blue mark highlight</mark> and <mark color="#4CAF50">green mark highlight</mark> side by side.

Default <mark>amber mark</mark> vs custom <mark color="#FF8A80">red mark</mark> on the same line.

## Mark Mixed With Other Inline Marks

Here it should: compose `<mark>` with bold, italic, code and `<Color>` inside the same paragraph — decorations apply to their own runs without leaking.

This paragraph mixes <mark>**bold inside mark**</mark>, <mark>*italic inside mark*</mark>, plain text, and a <mark>highlight that ends before</mark> the <Color id="red">colored span</Color>.

## Mark In A List

Here it should: render marks inside list items with the list indentation intact.

- First item with a <mark>highlighted word</mark>.
- Second item with <mark color="#61B75D">green highlight</mark>.
