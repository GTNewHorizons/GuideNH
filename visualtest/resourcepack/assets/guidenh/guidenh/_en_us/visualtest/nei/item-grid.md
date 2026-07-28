---
navigation:
  title: ItemGrid Multi-Item Grid
  position: 7990
---

TEST GOAL / 测试目标：`<ItemGrid>` 多物品网格渲染（ItemIcon 子项行/列排列）

INVARIANTS / 不变式：所有 ItemIcon 子项在网格中正确布局，无重叠或溢出

## Basic Grid — 3 Items

Expected: Three item icons displayed in a single row.

<ItemGrid>
  <ItemIcon id="minecraft:stick" />
  <ItemIcon id="minecraft:stone" />
  <ItemIcon id="minecraft:iron_ingot" />
</ItemGrid>

## Grid — 6 Items (Two Rows)

Expected: Six item icons arranged in two rows forming a compact grid.

<ItemGrid>
  <ItemIcon id="minecraft:stick" />
  <ItemIcon id="minecraft:stone" />
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:diamond" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>

## Grid — 9 Items (Three Rows)

Expected: Nine item icons forming a 3×3 grid layout.

<ItemGrid>
  <ItemIcon id="minecraft:stick" />
  <ItemIcon id="minecraft:stone" />
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:diamond" />
  <ItemIcon id="minecraft:redstone" />
  <ItemIcon id="minecraft:coal" />
  <ItemIcon id="minecraft:apple" />
  <ItemIcon id="minecraft:arrow" />
</ItemGrid>

## Grid with Ore Dictionary Entries

Expected: Items resolved by ore dictionary name alongside id-based icons.

<ItemGrid>
  <ItemIcon id="minecraft:stick" />
  <ItemIcon ore="ingotIron" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
