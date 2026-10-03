---
navigation:
  title: Mark Tag (Text Highlight)
  position: 8995
---

TEST GOAL: rendering of the `<mark>` text-highlight tag (default color + `color=` custom color)

INVARIANTS: the mark highlight background decoration is present; the color does not leak into following text; mixed layout with other inline marks works normally

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
