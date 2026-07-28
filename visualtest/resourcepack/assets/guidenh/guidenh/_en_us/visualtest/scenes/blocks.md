---
navigation:
  title: Block Scene Primitives
  position: 7900
---

TEST GOAL / 测试目标：`<Block>` 全属性 + `<PlaceBlock>` / `<ReplaceBlock>` / `<RemoveBlocks>` —— scale 修复回归哨兵

INVARIANTS / 不变式：场景内块正确排列、无偏移、不缺失、坐标对齐

## Single Block With Attributes

Expected: Five blocks in a row — stone at origin, oak_stairs with meta=1, furnace facing south, chest with NBT items, furnace with formed=false.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:oak_stairs" x="2" meta="1" />
  <Block id="minecraft:furnace" x="4" facing="south" />
  <Block id="minecraft:chest" x="6" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
  <Block id="minecraft:furnace" x="8" formed={false} />
</GameScene>

## PlaceBlock Fill Region

Expected: A 3x1x3 cobblestone platform with a glass center column 2 blocks high — `dx`/`dy`/`dz` fill works correctly.

<GameScene width="256" height="160" zoom={4} interactive={true}>
  <PlaceBlock id="minecraft:cobblestone" dx="3" dy="1" dz="3" />
  <PlaceBlock id="minecraft:glass" x="1" dx="1" dy="2" dz="1" />
</GameScene>

## ReplaceBlock With Bounds

Expected: Cobblestone at (0,0,0) replaced with brick_block; glass at (1,0,0) replaced with diamond_block. Unchanged blocks remain visible.

<GameScene width="256" height="128" zoom={5} interactive={true}>
  <PlaceBlock id="minecraft:cobblestone" dx="3" dy="1" dz="1" />
  <ReplaceBlock from="minecraft:cobblestone" to="minecraft:brick_block" x="0" y="0" z="0" dx="1" dy="1" dz="1" />
  <PlaceBlock id="minecraft:glass" x="1" />
  <ReplaceBlock from="minecraft:glass" to="minecraft:diamond_block" x="1" y="0" z="0" dx="1" dy="1" dz="1" />
</GameScene>

## RemoveBlocks By Id

Expected: A 5-block row of stone, iron_block, gold_block, diamond_block, obsidian. RemoveBlocks removes the iron_block and gold_block leaving gaps at their positions.

<GameScene width="320" height="128" zoom={5} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:iron_block" x="1" />
  <Block id="minecraft:gold_block" x="2" />
  <Block id="minecraft:diamond_block" x="3" />
  <Block id="minecraft:obsidian" x="4" />
  <RemoveBlocks id="minecraft:iron_block" />
  <RemoveBlocks id="minecraft:gold_block" />
</GameScene>
