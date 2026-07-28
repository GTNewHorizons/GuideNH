---
navigation:
  title: Row and Column
  position: 8300
---

TEST GOAL / 测试目标：Row/Column 的 gap 变体、alignItems 值、fullWidth、width、嵌套

INVARIANTS / 不变式：flex 间距正确；alignItems 对齐方向正确；fullWidth 占满可用宽度；嵌套内层不溢出外层

## Gap Variants

Expected: Three rows with gap 2, 10, and 20. Spacing between child items matches the gap value.

<Row gap="2">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="1" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

<Row gap="10">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="1" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

<Row gap="20">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="1" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

## alignItems Values

Expected: Three rows demonstrating alignItems start, center, end. Items align at the top, middle, and bottom of the row respectively.

<Row alignItems="start" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

<Row alignItems="center" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

<Row alignItems="end" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

## fullWidth vs Natural Width

Expected: The first Column has no fullWidth and shrinks to content width. The second Column uses fullWidth and spans the full available width.

<Column gap="4" alignItems="start">
**Without fullWidth (natural width)**

<BlockImage id="minecraft:diamond_block" scale="1" />
</Column>

<Column gap="4" alignItems="start" fullWidth>
**With fullWidth**

<BlockImage id="minecraft:diamond_block" scale="1" />
</Column>

## Width Constraint

Expected: The Column restricted to width=120 renders narrower than the unrestricted Column below.

<Column gap="4" width="120" alignItems="start">
Red text and a constrained box.

![](../assets/red-64.png)
</Column>

<Column gap="4" alignItems="start">
Unconstrained Column for comparison.

![](../assets/checker-128.png)
</Column>

## Nesting (Row inside Column)

Expected: A Column containing two Rows. Each Row contains three BlockImages. The inner Rows lay out horizontally within the vertical Column.

<Column gap="8" fullWidth alignItems="start">
<Row gap="4">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="1" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>
<Row gap="4">
<BlockImage id="minecraft:iron_block" scale="1" />
<BlockImage id="minecraft:emerald_block" scale="1" />
<BlockImage id="minecraft:redstone_block" scale="1" />
</Row>
</Column>
