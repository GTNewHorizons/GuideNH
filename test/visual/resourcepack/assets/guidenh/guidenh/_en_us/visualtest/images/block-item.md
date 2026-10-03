---
navigation:
  title: BlockImage and ItemImage
  position: 8460
---

TEST GOAL: `<BlockImage>` (scale/wrap/align/float) + block-level `<ItemImage>` comparison

INVARIANTS: BlockImage renders a 3D block preview; scale does not distort; wrap/align/float control text wrap positioning; ItemImage renders in item form and at a large scale serves as the block-level comparison

## BlockImage Scale Variants

Expected: Five BlockImages at scale 1 through 6. Each renders a 3D diamond block preview at progressively larger size; no distortion.

<Row gap="4">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:diamond_block" scale="2" />
<BlockImage id="minecraft:diamond_block" scale="3" />
<BlockImage id="minecraft:diamond_block" scale="4" />
<BlockImage id="minecraft:diamond_block" scale="6" />
</Row>

## BlockImage with Different Blocks

Expected: A row of different blocks at the same scale (3). Each BlockImage renders its respective 3D block preview.

<Row gap="4">
<BlockImage id="minecraft:stone" scale="3" />
<BlockImage id="minecraft:gold_block" scale="3" />
<BlockImage id="minecraft:emerald_block" scale="3" />
<BlockImage id="minecraft:redstone_block" scale="3" />
</Row>

## BlockImage wrap="square" align="left"

Expected: BlockImage floats to the left with square wrap; text wraps around its right side.

<BlockImage id="minecraft:diamond_block" scale="4" wrap="square" align="left" />

Surrounding paragraph text that wraps around the left-floated BlockImage. The block renders as a 3D diamond block preview at scale 4. Text fills the available space to the right and continues below the image. Additional filler ensures multiple wrapping lines are visible.

<br clear="all">

## BlockImage wrap="square" align="right"

Expected: BlockImage floats to the right with square wrap; text wraps around its left side.

<BlockImage id="minecraft:stone" scale="4" wrap="square" align="right" />

Surrounding paragraph text that wraps around the right-floated BlockImage. The block renders as a 3D stone block preview at scale 4. Text fills the space to the left and continues below. Additional filler ensures visible wrapping.

<br clear="all">

## BlockImage align="center"

Expected: BlockImage is horizontally centered on the page; no text wrapping (default wrap is inline). Equal margin on left and right.

<BlockImage id="minecraft:gold_block" scale="5" align="center" />

## BlockImage align="right"

Expected: BlockImage is right-aligned on the page; no text wrapping. Flush against the right margin.

<BlockImage id="minecraft:emerald_block" scale="5" align="right" />

## BlockImage float="left" (Flow Context)

Expected: BlockImage floats left within a paragraph or inline flow; text wraps around its right side.

Text before the floated BlockImage. <BlockImage id="minecraft:diamond_block" scale="3" float="left" /> This text appears after the inline BlockImage and wraps around its right side. The float attribute is the legacy inline floating mechanism for flow context. Additional filler ensures wrapping behavior is visible across multiple lines.

<br clear="all">

## BlockImage float="right" (Flow Context)

Expected: BlockImage floats right within a paragraph or inline flow; text wraps around its left side.

Text before the right-floated BlockImage. <BlockImage id="minecraft:redstone_block" scale="3" float="right" /> This text appears before the floated image and wraps around its left side. Additional filler ensures wrapping behavior is visible across multiple lines.

<br clear="all">

## ItemImage Comparison (Block-Style at Large Scale)

Expected: ItemImages rendered at large scale with labels behave as block-like inline elements. This contrasts with the 3D BlockImage previews above.

<Row gap="4">
<ItemImage id="minecraft:diamond" scale="4" />
<ItemImage id="minecraft:emerald" scale="4" />
<ItemImage id="minecraft:gold_ingot" scale="4" />
</Row>

## ItemImage with Labels (Block Comparison)

Expected: Each ItemImage shows its item icon with a text label to the right (default label position), demonstrating block-like layout alongside BlockImage elements above.

<Row gap="8">
<ItemImage id="minecraft:diamond" scale="3" label="right" />
<ItemImage id="minecraft:emerald" scale="3" label="right" />
<ItemImage id="minecraft:gold_ingot" scale="3" label="right" />
</Row>
