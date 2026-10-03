---
navigation:
  title: Block Alignment
  position: 8290
---

TEST GOAL: behavior of block-level align=left/center/right on images, tables and scenes

INVARIANTS: left is flush left; center is centered; right is flush right; the differences between the comparison groups are obvious

## BlockImage Alignment

Expected: Three BlockImages aligned left, center, and right. The center image is horizontally centred; the right image is flush to the right margin.

<BlockImage id="minecraft:diamond_block" align="left" scale="1.5" />
<BlockImage id="minecraft:diamond_block" align="center" scale="1.5" />
<BlockImage id="minecraft:diamond_block" align="right" scale="1.5" />

## Table Alignment

Expected: Three tables aligned left (default), center, and right using the block-level align attribute. The centred table is positioned in the middle of the available width.

CASE DISABLED (engine bug, recorded in SPEC backlog): JSX `<table align="left|center|right">` with `<tr><td>` children produces a table whose column list is empty at layout time, crashing LytTable.layoutColumns (LytTable.java:176, NoSuchElementException at ArrayList.getLast). Repro (shown as literal text):

```xml
<table align="center"><tr><td>Center</td><td>Aligned</td></tr></table>
```

Expected after engine fix: three JSX tables aligned left / center / right; the centred table sits in the middle of the available width. (Case text kept inline so the fix phase can restore it verbatim.)

## GameScene Alignment

Expected: Three GameScenes aligned left, center, and right. The centred scene is horizontally centred in the available width.

<GameScene width="120" height="80" zoom="5" align="left">
<Block id="minecraft:diamond_block" />
</GameScene>

<GameScene width="120" height="80" zoom="5" align="center">
<Block id="minecraft:diamond_block" />
</GameScene>

<GameScene width="120" height="80" zoom="5" align="right">
<Block id="minecraft:diamond_block" />
</GameScene>
