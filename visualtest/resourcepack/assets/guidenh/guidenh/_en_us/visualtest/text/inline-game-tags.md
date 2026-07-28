---
navigation:
  title: Inline Game Tags
  position: 8940
---

TEST GOAL / 测试目标：ItemImage（id/scale/label/format）、ItemLink（showIcon/showText/linksTo）、KeyBind、PlayerName、Tooltip（行内触发）、Spoiler

INVARIANTS / 不变式：图标与文本基线对齐；行高不被图标异常撑大；tooltip 触发区正确

## ItemImage

Expected: Item icon rendered at specified scale; label position left/right places text correctly; format applies to label text.

<ItemImage id="minecraft:apple" scale="2" />

<ItemImage id="minecraft:diamond" scale="1" label="right" />

<ItemImage id="minecraft:iron_ingot" scale="1.5" label="left" />

<ItemImage id="minecraft:golden_apple" scale="2" label="right" format="**%s**" />

<ItemImage id="minecraft:diamond_sword" scale="1" label="right" showTooltip="true" />

<ItemImage id="minecraft:compass" scale="1" label="left" noTooltip="true" />

## ItemLink

Expected: Item name rendered with icon; showIcon and showText control visibility; linksTo navigates to specified page.

<ItemLink id="minecraft:apple" />

<ItemLink id="minecraft:diamond_sword" showTooltip="true" />

<ItemLink id="minecraft:crafting_table" showIcon="left" showText="true" />

<ItemLink id="minecraft:compass" showIcon="left" showText="true" showTooltip="true" />

<ItemLink id="minecraft:stone" linksTo="./links.md" />

## KeyBind

Expected: Current key mapping displayed in styled text; fallback shown when id is unknown.

Press <KeyBind id="key.forward" /> to move forward.

<KeyBind id="key.jump" /> to jump — <KeyBind id="key.sneak" /> to sneak.

Inventory: <KeyBind id="key.inventory" />

## PlayerName

Expected: Current player username displayed inline; styled distinctively.

Welcome, <PlayerName />!

The player <PlayerName /> is currently online.

## Tooltip (Inline Trigger)

Expected: Hovering the trigger text shows rich content box; content tooltip renders Markdown and inline tags.

<Tooltip label="Hover for details">
  ## Rich Tooltip
  Contains **bold**, *italic*, and `code`.

  * List item
  * List item
</Tooltip>

<Tooltip label="Hover for item">
  Contains <ItemImage id="minecraft:diamond" scale="2" /> diamond
  and <ItemImage id="minecraft:apple" scale="2" /> apple.
</Tooltip>

## Spoiler

Expected: Text hidden behind blur/reveal overlay; clicking reveals the content; reveals persist.

<Spoiler>Hidden spoiler text with **bold** and *italic* formatting.</Spoiler>

<Spoiler>Spoiler containing a [link](./links.md) that remains clickable after reveal.</Spoiler>
