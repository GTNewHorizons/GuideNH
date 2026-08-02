---
navigation:
  title: Text Decorations (underline/wavy/dots/strike)
  position: 8981
---

TEST GOAL / 测试目标：四种文本装饰渲染——普通下划线 ++、波浪下划线 ^^、点线下划线 ::、删除线 ~~；含同行组合对照与长文本装饰延伸

INVARIANTS / 不变式：四种装饰均渲染、无编译错误；装饰线终止于边界不泄漏后续文本；装饰不压上下行

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
