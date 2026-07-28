---
navigation:
  title: Camera & Viewport
  position: 7860
---

TEST GOAL / 测试目标：perspective 三值 + rotateX/Y/Z + offset/center + `<IsometricCamera>` + zoom

INVARIANTS / 不变式：视口大小与预设正确对应；缩放及旋转不导致场景裁剪异常；偏移值生效

## Perspective Presets

Expected: Three small scenes side by side — isometric-north-east (NE), isometric-north-west (NW), and top-down (up). NE furnace shows front-right face, NW shows front-left face, up shows top face.

<Row>
  <GameScene width="128" height="96" zoom={5} perspective="isometric-north-east" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
  </GameScene>
  <GameScene width="128" height="96" zoom={5} perspective="isometric-north-west" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
  </GameScene>
  <GameScene width="128" height="96" zoom={5} perspective="up" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
  </GameScene>
</Row>

## Explicit rotateX / rotateY / rotateZ

Expected: Left scene default; right scene rotated with rotateX=15 and rotateY=30 — blocks visibly tilted.

<Row>
  <GameScene width="128" height="96" zoom={5} interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
  <GameScene width="128" height="96" zoom={5} rotateX="15" rotateY="30" interactive={true}>
    <Block id="minecraft:stone" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
</Row>

## Offset Pan

Expected: Left scene shows centered diamond blocks; right scene with offsetX=2 and offsetY=1 shows blocks panned right and down.

<Row>
  <GameScene width="128" height="96" zoom={4} interactive={true}>
    <Block id="minecraft:diamond_block" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
  <GameScene width="128" height="96" zoom={4} offsetX="2" offsetY="1" interactive={true}>
    <Block id="minecraft:diamond_block" />
    <Block id="minecraft:diamond_block" x="1" />
  </GameScene>
</Row>

## IsometricCamera Yaw / Pitch / Roll

Expected: Left scene with NE preset; right scene with explicit yaw=45 pitch=30 roll=0 (should match); third scene with roll=15 (tilted).

<Row>
  <GameScene width="128" height="96" zoom={5} perspective="isometric-north-east" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <IsometricCamera />
  </GameScene>
  <GameScene width="128" height="96" zoom={5} interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <IsometricCamera yaw="45" pitch="30" roll="0" />
  </GameScene>
  <GameScene width="128" height="96" zoom={5} perspective="isometric-north-east" interactive={true}>
    <Block id="minecraft:furnace" facing="south" />
    <Block id="minecraft:cobblestone" x="1" />
    <IsometricCamera roll="15" />
  </GameScene>
</Row>

## Zoom Variant

Expected: Same block displayed at zoom=2, zoom=4, and zoom=6 — size increases proportionally.

<Row>
  <GameScene width="96" height="96" zoom={2} interactive={true}>
    <Block id="minecraft:diamond_block" />
  </GameScene>
  <GameScene width="96" height="96" zoom={4} interactive={true}>
    <Block id="minecraft:diamond_block" />
  </GameScene>
  <GameScene width="96" height="96" zoom={6} interactive={true}>
    <Block id="minecraft:diamond_block" />
  </GameScene>
</Row>
