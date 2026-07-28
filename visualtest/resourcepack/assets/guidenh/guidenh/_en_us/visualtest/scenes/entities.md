---
navigation:
  title: Entity Scene Primitives
  position: 7890
---

TEST GOAL / 测试目标：`<Entity>` NBT 变体 + `<RemoveEntity>` + 实体 Y 偏移复现（backlog）

INVARIANTS / 不变式：实体贴地（y=1 在 grass 上）；Y 偏移无悬浮；RemoveEntity 后实体消失

## Ground-Touch Verification

Expected: Sheep (red, baby) and zombie (baby) placed at y=1 on grass block — entity feet visibly contact the block surface, no floating gap.

<GameScene width={240} zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Block id="minecraft:grass" x="1" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>

## Entity With NBT Data

Expected: Creeper with `{powered:1b}` shows charged creeper overlay; skeleton with `{Equipment:[{id:"minecraft:bow",Count:1b}]}` holds a bow. Both at y=1 on grass.

<GameScene width={240} zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:creeper" y="1" data="{powered:1b}" />
  <Block id="minecraft:grass" x="1" />
  <Entity id="minecraft:skeleton" x="1.5" y="1" data='{Equipment:[{id:"minecraft:bow",Count:1b}]}' />
</GameScene>

## Player With Rotation Attributes

Expected: Player at y=1 with custom arm and head rotations; shows name "GuideNH".

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="GuideNH" showName={true} headRotation="0 20 0" rightArmRotation="-35 0 0" leftArmRotation="10 0 -12" />
</GameScene>

## RemoveEntity By sceneEntityId

Expected: A horse at (1.5, 1) with a rider mounted, then `<RemoveEntity>` clears both — only grass block remains visible.

<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
  <RemoveEntity sceneEntityId="rider" />
</GameScene>
