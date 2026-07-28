---
navigation:
  title: Footnotes
  position: 8930
---

TEST GOAL / 测试目标：脚注引用 [^a] 多个 + FootnoteList

INVARIANTS / 不变式：引用渲染为上标链接；列表收集完整；脚注编号连续

## Single Footnote

Expected: The footnote reference [^first] renders as a superscript link; the footnote list at the bottom contains the definition.

This sentence has a footnote reference[^first] inline.

[^first]: This is the first footnote content.

## Multiple Footnotes

Expected: Each reference gets a unique numbered superscript; all definitions appear in the FootnoteList; numbering is sequential across the page.

Multiple footnotes[^second] can appear in the same[^third] paragraph or across[^fourth] different paragraphs.

[^second]: Second footnote with more detailed explanation.
[^third]: Third footnote — short text.
[^fourth]: Fourth footnote that contains slightly longer content for multiline testing.

## Repeated Reference

Expected: Referencing the same footnote label[^first] multiple times shows the same number each time; the list does not duplicate the definition.

The same footnote used again here[^first] and here[^second] to verify deduplication.

## Footnotes with Rich Content

Expected: Footnote content with inline formatting renders correctly; bold, italic, and code inside footnotes are styled.

Rich content footnote[^rich] demonstrates inline formatting in definition.

[^rich]: Contains **bold text**, *italic text*, and `inline code` together.

## Mix of Footnotes and Regular Text

Expected: Normal text and footnote references share the same line without layout disruption.

Before[^third] and after[^first] — footnotes woven into the narrative flow of the page. This paragraph also contains a reference[^fourth] to verify inline behavior with surrounding regular text.

[^third]: This definition was used earlier but the reference is fresh here.
[^first]: Repeated definition for the same label — list should only show it once.
[^fourth]: Another occurrence of the fourth footnote content.
