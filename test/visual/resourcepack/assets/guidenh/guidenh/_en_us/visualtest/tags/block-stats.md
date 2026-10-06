---
navigation:
  title: BlockStats Tag
  position: 9100
---

TEST GOAL: compilation and rendering of the `<BlockStats>` child tag (visible / buttonEnabled attributes) inside a GameScene

INVARIANTS: a scene containing `<BlockStats>` renders normally as a scene block with no red error text; the scene keeps its true width and height

Syntax reference: `SceneScript.applyBlockStatsConfig` reads `visible` and `buttonEnabled` (the code-supported attribute set; the wiki documents additional attributes not wired in the current compiler).

## BlockStats Visible With Blocks

Here it should: render a small scene of three blocks with the block-statistics overlay requested explicitly via `<BlockStats visible={true} />` — no compile error, scene block with real dimensions.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="2" />
  <Block id="minecraft:torch" x="4" />
  <BlockStats visible={true} />
</GameScene>

## BlockStats With Button Disabled

Here it should: render the same scene with the statistics toggle button disabled via `buttonEnabled={false}` — scene still mounts without a red error paragraph.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <Block id="minecraft:cobblestone" />
  <Block id="minecraft:glass" x="2" />
  <BlockStats visible={true} buttonEnabled={false} />
</GameScene>

## BlockStats With Visible False

Here it should: render a scene that explicitly hides the statistics overlay via `visible={false}` while keeping the toggle button — no error output.

<GameScene width="320" height="128" zoom={4} interactive={true}>
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:gold_block" x="2" />
  <BlockStats visible={false} buttonEnabled={true} />
</GameScene>
