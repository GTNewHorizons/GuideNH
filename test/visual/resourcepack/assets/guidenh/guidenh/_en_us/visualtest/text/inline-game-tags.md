---
navigation:
  title: Inline Game Tags
  position: 8940
---

TEST GOAL: ItemImage (id/scale/label/format), ItemLink (showIcon/showText/linksTo), KeyBind, PlayerName, Tooltip (inline trigger), Spoiler

INVARIANTS: icons are baseline-aligned with text; line height is not abnormally enlarged by icons; tooltip hotspots are correct

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

## ItemImage (Inline Label)

TEST GOAL: inline (within a paragraph text flow, not block-level) ItemImage with a label — the label text baseline sits on the body baseline; the advance of the icon+label combination = icon + gap + label text width; following inline text sits flush against the label text.

INVARIANTS: the label text baseline lands on the body baseline (anchored by align=1 baseline ascent, replacing the retired align=2 centered setting); the icon does not intrude into the text before/after it; the combined block does not abnormally increase the line height.

Inline label icons embedded in prose (label="right", mirrors the real corpus pattern, e.g. AE2 getting-started):

Collect <ItemImage id="minecraft:redstone" label="right" />, <ItemImage id="minecraft:diamond" label="right"/>, and <ItemImage id="minecraft:gold_ingot" label="right"/> to craft the circuit.

A left-labeled icon before the prose continues after it: <ItemImage id="minecraft:compass" label="left"/> when exploring.

A scaled inline label icon: obtain <ItemImage id="minecraft:apple" scale="1.5" label="right"/> from trees.

Two inline label icons in one sentence: compare <ItemImage id="minecraft:iron_ingot" label="right"/> with <ItemImage id="minecraft:gold_ingot" label="right" />.
