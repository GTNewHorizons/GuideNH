---
navigation:
  title: Paragraphs & Line Breaks
  position: 8990
---

TEST GOAL / 测试目标：多段落间距 + 软换行 vs 空行段落 + 满行宽段落 + 单字段落 + br 与 br clear

INVARIANTS / 不变式：行距段距一致；无异常空白带

## Multiple Paragraphs

This is the first paragraph of a multi-paragraph block. It contains several sentences that form a normal paragraph. The next paragraph follows after an empty line in the source.

This is the second paragraph. There should be visible paragraph spacing (larger than line spacing) between this and the preceding paragraph.

This is the third paragraph. The gap between this and the second paragraph should be identical to the gap between the first and second.

Expected: All three paragraphs have equal inter-paragraph spacing; spacing between paragraphs is larger than line spacing within a paragraph.

## Soft Line Break vs Hard Paragraph Break

This line is the first sentence of the paragraph.  
This line is on a new line after a soft break (two trailing spaces in the source).  
This line is another soft break continuation. The soft breaks should render as a line feed within the same paragraph, not as a paragraph break.

This is a separate paragraph after an empty line. The gap above should be larger than the gap between soft-broken lines.

Expected: Soft line breaks produce smaller vertical gap than paragraph breaks; the visual distinction between soft break and paragraph break is clear.

## Exactly Full Line Width Paragraph

The quick brown fox jumps over the lazy dog near the bank of the river. The engine must handle text that fills the entire available line width without overflow. Every character should remain within the content bounds.

Expected: Text fills to the right margin without exceeding it; no horizontal overflow occurs; wrapping occurs naturally at word boundaries.

## Single-Word Paragraph

Pneumonoultramicroscopicsilicovolcanoconiosis

Expected: A single long word renders on its own line; it does not overflow the right margin; the paragraph height is a single line.

## br and br clear

This line contains inline content before a line break.<br>This text is after a `<br>` tag. The break should produce a line feed within the same paragraph, not a paragraph gap.

<br clear="all">

This text is after a `<br clear="all">` tag. The clear-all break should reset any active float context and start a fresh line.

Expected: `<br>` produces a line break with no extra vertical space; `<br clear="all">` clears any preceding floats and renders subsequent content on a new line below the float boundary.
