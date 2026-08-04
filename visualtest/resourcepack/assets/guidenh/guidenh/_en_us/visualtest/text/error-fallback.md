---
navigation:
  title: Error Fallback Width (F-N2)
  position: 8935
---

TEST GOAL / 测试目标（F-N2 回归哨兵）：ItemLink/ItemImage 指向不存在物品时的错误回退段必须保留真实宽度，后续正文不得压叠到错误段上

INVARIANTS / 不变式：每个错误回退段（嵌套 LytParagraph）在 bounds JSON 中保留 ≥200px 真实宽度（防宽度塌缩）；错误段之后紧跟的正文在同一行继续排版（后随文本起始 x = 错误段 x + 错误段 w > 错误段 x + 20）；错误段与正文互不重叠（sibling_intersection 零命中）。断言盲区：当前 ratchet（全部 LytParagraph w≥200）锁定"错误段宽度塌缩为零宽"回归模式，但不覆盖"错误节点整体不再发射"模式（harness 暂无 overlap 规则；trailing-x 量化为人工验证）——建议未来 harness 支持 sibling overlap 断言后补锁

This page intentionally renders red error-fallback text for non-existent items. Every case below is a single paragraph: [prefix text] + [error fallback] + [trailing prose]. The trailing prose must start strictly right of the error segment — if the fallback width ever collapses to near zero again, the trailing text lands on top of the error text and this page's ratchet assertion fails.

## Missing ItemImage With Trailing Prose

Here it should: render the error fallback for a non-existent item, then the trailing prose continues on the same line to the right of the error segment.

<ItemImage id="minecraft:example_nonexistent_item" /> after this the body prose continues on the same line, well to the right of the error text.

## Missing ItemImage Inside A Sentence

Here it should: render a missing-item error mid-sentence with prose both before and after — the error sits inline after the prefix and the suffix resumes right after it.

Before the missing <ItemImage id="minecraft:not_a_real_item" /> after it the rest of the sentence continues normally without overlapping the error.

## Missing ItemLink With Trailing Prose

Here it should: render an ItemLink error fallback whose following prose stays on the same line after the error segment.

<ItemLink id="minecraft:ghost_ingot" /> after this the body prose continues on the same line, well to the right of the error text.

## Missing ItemLink Inside A Sentence

Here it should: render an ItemLink error mid-sentence — the error reserves real width so the following clause stays clear of it.

A link to the fictional <ItemLink id="minecraft:fictional_block" /> is expected to fail, and the clause after it must still sit to the right.

## Two Consecutive Missing Items

Here it should: render two error fallbacks in a row — both reserve width and the second sits after the first, with trailing prose after both.

<ItemImage id="minecraft:missing_a" /> <ItemImage id="minecraft:missing_b" /> and the trailing prose follows both error segments on the same line.

## Missing ItemImage At Paragraph Start

Here it should: render an error fallback that opens the paragraph — the trailing prose must begin strictly to the right of the error's right edge.

<ItemImage id="minecraft:oops_no_item" /> opening error at the paragraph start, followed by prose that must not overlap it.
