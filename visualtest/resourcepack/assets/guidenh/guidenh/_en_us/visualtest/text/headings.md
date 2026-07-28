---
navigation:
  title: Heading Levels & Separators (K2)
  position: 9000
---

TEST GOAL / 测试目标：H1-H6 全级 + 标题紧跟正文 + 标题紧跟标题 + 长标题折行 + 分隔线穿字复现

INVARIANTS / 不变式：分隔线不穿字（K2）；连续标题间距一致；长标题不溢出右边距

## Heading 2 Followed by Body Text

This paragraph immediately follows an H2 heading. The engine should render normal paragraph spacing below the heading decorative separator line, not overlapping.

Expected: H2 heading separator line does not intersect the body text below; normal vertical gap between heading and paragraph.

### Heading 3 Immediately Followed by Another Heading

#### Heading 4 Immediately Below H3

There is no body text between the H3 and H4 above. This paragraph follows the H4. The heading decorative separators for H3 and H4 should not overlap each other, and the spacing between consecutive headings should be consistent.

Expected: Two consecutive headings (H3 then H4) with no body text between them; their separator lines do not overlap; spacing is uniform.

##### Heading 5

This body text follows an H5 heading.

###### Heading 6

This body text follows an H6 heading. All six heading levels have been demonstrated.

Expected: H5 and H6 render at progressively smaller sizes; separator lines remain below text; no overlap.

# H1: Very Long Heading That Should Wrap Across Multiple Lines to Test Overflow and Line Break Behavior Within Heading Text Itself

Expected: Long heading text wraps at word boundaries; text does not overflow the right page margin; the heading separator line stays below the wrapped block, not cutting through any line of text.

## Separator Line vs Text Collision (K2 Replication)

The heading above this paragraph and the one below both render decorative separator lines. The engine must not let those lines intersect any character descenders.

Expected: No part of any heading separator line visually overlaps or cuts through heading text characters on any line.

--- a horizontal rule separator

Expected: The `---` thematic break renders as a standalone horizontal line; it does not collide with the preceding heading separator or the following heading text.
