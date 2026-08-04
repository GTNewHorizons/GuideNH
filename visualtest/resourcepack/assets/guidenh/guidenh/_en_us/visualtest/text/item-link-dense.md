---
navigation:
  title: ItemLink Dense Usage
  position: 8985
---

TEST GOAL / 测试目标：密集 ItemLink 混排（多行多 ItemLink + 图标）排版稳定性

INVARIANTS / 不变式：每个带图标 ItemLink 渲染真实尺寸图标（w>0 / h>0）；多链接长行正常换行；图标不与相邻文本重叠

Real-corpus note: production guides use ItemLink 827 times vs 1 fixture occurrence; this page reproduces dense multi-line ItemLink flow.

## Equipment Set (5 Links, Icons Left)

Here it should: render five item links in one flowing sentence, each with a leading icon and display text, wrapping naturally.

<ItemLink id="minecraft:diamond_sword" showIcon="left" showText="true" />, <ItemLink id="minecraft:diamond_pickaxe" showIcon="left" showText="true" />, <ItemLink id="minecraft:diamond_axe" showIcon="left" showText="true" />, <ItemLink id="minecraft:diamond_shovel" showIcon="left" showText="true" /> and <ItemLink id="minecraft:diamond_hoe" showIcon="left" showText="true" /> form a full tool set.

## Tool Chain (5 Links, Icons Left)

Here it should: render five tiered tool links in a second dense sentence — icon and text pairs stay aligned across the line wrap.

Start with <ItemLink id="minecraft:wooden_pickaxe" showIcon="left" showText="true" />, upgrade to <ItemLink id="minecraft:stone_pickaxe" showIcon="left" showText="true" />, then <ItemLink id="minecraft:iron_pickaxe" showIcon="left" showText="true" />, then <ItemLink id="minecraft:golden_pickaxe" showIcon="left" showText="true" />, and finally <ItemLink id="minecraft:diamond_pickaxe" showIcon="left" showText="true" />.

## Food Group (6 Links, Icons Left)

Here it should: render six food item links densely — every link carries its icon before its name, text stays readable.

Hungry? Grab <ItemLink id="minecraft:apple" showIcon="left" showText="true" />, <ItemLink id="minecraft:bread" showIcon="left" showText="true" />, <ItemLink id="minecraft:cooked_beef" showIcon="left" showText="true" />, <ItemLink id="minecraft:cooked_porkchop" showIcon="left" showText="true" />, <ItemLink id="minecraft:cookie" showIcon="left" showText="true" /> or <ItemLink id="minecraft:cake" showIcon="left" showText="true" />.

## Mixed Plain Links (Icons Right)

Here it should: render item links with `showIcon="right"` so the icon trails the display text — icon placement varies without breaking the flow.

Resources: <ItemLink id="minecraft:stone" showIcon="right" showText="true" />, <ItemLink id="minecraft:iron_ingot" showIcon="right" showText="true" />, <ItemLink id="minecraft:gold_ingot" showIcon="right" showText="true" /> and <ItemLink id="minecraft:redstone" showIcon="right" showText="true" />.

## Text-Only Links (No Icon)

Here it should: render text-only item links with no icon (default) interleaved with icon links — density with mixed forms stays stable.

Compare <ItemLink id="minecraft:stone" /> and <ItemLink id="minecraft:iron_ingot" showIcon="left" showText="true" /> and <ItemLink id="minecraft:gold_ingot" /> and <ItemLink id="minecraft:diamond" showIcon="left" showText="true" /> in the same sentence.

## Two-Column Mixture

Here it should: render a paragraph that mixes ItemLink inside bold and a trailing plain text tail.

Build with <ItemLink id="minecraft:crafting_table" showIcon="left" showText="true" />, smelt in <ItemLink id="minecraft:furnace" showIcon="left" showText="true" /> and then use <ItemLink id="minecraft:anvil" showIcon="left" showText="true" /> for repairs — the trailing prose continues on the same lines.
