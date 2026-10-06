---
navigation:
  title: Text Decorations (underline/wavy/dots/strike)
  position: 8981
---

TEST GOAL: rendering of four text decorations — plain underline ++, wavy underline ^^, dotted underline ::, strikethrough ~~; includes same-line combination comparisons and decoration extension over long text

INVARIANTS: all four decorations render with no compile errors; decoration lines stop at the boundary and do not leak into following text; decorations do not overlap the lines above and below

## Plain Underline (++)

++underlined text++ should render with a continuous straight line below the glyphs.

Normal text after the mark appears unformatted.

Expected: a solid underline spans exactly the decorated text and nothing after it.

## Wavy Underline (^^)

^^wavy underlined text^^ should render with a wavy line below the glyphs.

Normal text after the mark appears unformatted.

Expected: a wavy line spans exactly the decorated text and nothing after it.

## Dotted Underline (::)

::dotted underlined text:: should render with dots below the glyphs.

Normal text after the mark appears unformatted.

Expected: a dotted underline spans exactly the decorated text and nothing after it.

## Strikethrough (~~)

~~strikethrough text~~ should render with a line through the middle of the glyphs.

Normal text after the mark appears unformatted.

Expected: the strike line spans exactly the decorated text and nothing after it.

## Combined Decorations on One Line

Compare on the same line: ++underlined text++, ^^wavy underlined text^^, ::dotted underlined text:: and ~~strikethrough text~~.

Expected: each decoration is visually distinct and terminates at its own boundary within the line.

## Long Text Decoration

++This is a long paragraph wrapped entirely in the plain-underline decoration, spanning across multiple wrapped lines to verify that the underline extends through the whole paragraph and stays aligned on every line.++

Expected: the underline runs continuously across all wrapped lines; the decoration does not leak past the closing marker.
