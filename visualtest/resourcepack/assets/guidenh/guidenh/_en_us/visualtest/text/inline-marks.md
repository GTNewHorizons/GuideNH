---
navigation:
  title: Inline Marks & Formatting
  position: 8980
---

TEST GOAL / 测试目标：全部行内标记 + 互相嵌套 + 样式不泄漏

INVARIANTS / 不变式：样式终止于边界不泄漏后续文本；嵌套渲染正确；装饰线不压上下行

## Bold, Italic, Bold-Italic

**Bold text** should render with heavier weight. *Italic text* should render with a slant. ***Bold-italic text*** combines both.

Normal text after each mark should appear unformatted.

Expected: Bold, italic, and bold-italic render correctly; unaffected text after each mark has no residual formatting; styles terminate at their boundaries.

## Strikethrough

~~Strikethrough text~~ has a line through the middle. ~Single tilde strikethrough~ also works. Normal text after must not be struck through.

Expected: Both `~~` and `~` produce a strikethrough line at consistent vertical position; the line does not extend into surrounding text or overlap adjacent lines.

## Underline, Wavy Underline, Emphasis Dots, Highlight

++Underlined text++ renders with a continuous line below. ^^Wavy underline^^ renders with a wavy line below. ::Emphasis dots:: renders with dots below. ==Highlight== renders with a background color.

Normal text resumes without any decoration.

Expected: Each decoration type is visually distinct; underline is solid, wavy is wavy, emphasis uses dots, highlight has a background fill; all decorations terminate cleanly; decorative lines do not overlap characters on the line below.

## Inline Code

Use `var x = 42;` inline code within a sentence. The code fragment `if (x > 0) { return true; }` contains special characters. Normal text continues after.

Expected: Inline code renders in a monospace font with a distinct background or border; special characters `<>&"` render correctly; formatting does not leak beyond the backticks.

## kbd, sub, sup

Press <kbd>Ctrl</kbd> + <kbd>C</kbd> to copy. The kbd tag should render with key-cap styling.

Subscript: H<sub>2</sub>O and Superscript: E=mc<sup>2</sup> render at reduced size with vertical offset.

Expected: `<kbd>` renders with key-cap appearance; `<sub>` renders below baseline; `<sup>` renders above baseline; both are visibly smaller but readable; they do not disrupt the line height.

## span (No-Op Wrapper)

The word <span>wrapped</span> in a span tag should render identically to unwrapped text.

Expected: `<span>` is a transparent wrapper; its content renders with no additional styling; text before and after is indistinguishable.

## Mixed Nested Marks

**Bold with *italic inside*** should render bold text that transitions to bold-italic for the inner segment. ~~Strikethrough with ++underline inside++~~ combines two decorations. ==Highlight with `code` inside== nests inline code within a highlight.

***Bold-italic with ~~strikethrough~~ and ==highlight== mixed*** demonstrates triple nesting.

Expected: Nested marks apply correctly at each level; inner formatting is properly terminated before outer formatting ends; no visual artifacts at nesting boundaries.
