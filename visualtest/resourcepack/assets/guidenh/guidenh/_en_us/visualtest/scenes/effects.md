---
navigation:
  title: Scene Effects
  position: 7870
---

TEST GOAL / 测试目标：`<Particle>` / `<Weather>`（雨+雪）/ `<PlaySound>`（仅查渲染存在性）

INVARIANTS / 不变式：粒子在场景内正确位置显示；雨雪动画不溢出场景区；声音触发不导致崩溃

## Static Billboard Particle

Expected: Default billboard particle at (1.5, 1.85, 0.5) above furnace; smoke particle at (1.5, 1.35, 0.5). Both stationary within the scene.

<GameScene width="192" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:furnace" x="1" />
  <Particle x="1.5" y="1.85" z="0.5" size="0.22" />
  <Particle name="smoke" x="1.5" y="1.35" z="0.5" size="0.18" />
</GameScene>

## Particle Name Variants

Expected: Five particles with different names (explode, flash, largeexplode, hugeexplosion, largesmoke) spaced across a cobblestone platform; each visible at its position.

<GameScene width="320" height="128" zoom={4} interactive={false}>
  <PlaceBlock id="minecraft:cobblestone" dx="5" dy="1" dz="1" />
  <Particle name="explode" x="0.5" y="1.2" z="0.5" size="0.15" />
  <Particle name="flash" x="1.5" y="1.2" z="0.5" size="0.15" />
  <Particle name="largeexplode" x="2.5" y="1.2" z="0.5" size="0.15" />
  <Particle name="hugeexplosion" x="3.5" y="1.2" z="0.5" size="0.15" />
  <Particle name="largesmoke" x="4.5" y="1.2" z="0.5" size="0.15" />
</GameScene>

## Weather Rain

Expected: Rain falling over a 2-block-wide area above stone and grass blocks; rain columns visible within the scene viewport.

<GameScene width="224" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
</GameScene>

## Weather Snow

Expected: Snow falling over a single column above a stone block at (2,0,0); snow flakes drift downward.

<GameScene width="224" height="128" zoom={5} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="snow" x="2" z="0" density="8" />
</GameScene>

## PlaySound With Click Trigger

Expected: Scene loads without crash; click-triggered sound cue registered (render existence only — no audible playback during offline capture).

<GameScene width="256" height="128" zoom={4} interactive={true}>
  <Block id="minecraft:note_block" />
  <PlaySound sound="guidenh:guide.sample_click" trigger="click" volume="0.75" />
</GameScene>
