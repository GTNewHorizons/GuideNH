---
navigation:
  title: Baseline Alignment (true text-baseline)
  position: 8310
---

## Baseline (text: baseline = image bottom edge)

<Row alignItems="baseline" gap="5">
Baseline text must sit on the image baseline.

<BlockImage id="minecraft:stone" scale="2" />

Baseline text must sit on the image baseline.
</Row>

Expected: two text children in a baseline row share the block image's bottom edge as their common baseline line. Per CSS, a replaced element's baseline is its bottom edge, so the PLAN 2.7 post-layout (RowBaselineAligner) shifts both text boxes DOWN so their first-line baselines sit exactly on the image's bottom edge. Exemption protocol (WORKFLOW §3.3): this BASELINE row's second-pass geometry deviates from the raw taffy writeback (which would bottom-degrade the text); non-BASELINE containers are byte-identical.

## Control: start (text, top flush)

Expected: the same [text, image, text] row with `alignItems="start"` — both text boxes start at the row top (their top margins), the image extends downward. Non-BASELINE container, untouched by the post-layout.

<Row alignItems="start" gap="5">
Baseline text must sit on the image baseline.

<BlockImage id="minecraft:stone" scale="2" />

Baseline text must sit on the image baseline.
</Row>

## Control: center (text, middle flush)

Expected: the same [text, image, text] row with `alignItems="center"` — both text boxes are vertically centered against the image. Non-BASELINE container, untouched by the post-layout.

<Row alignItems="center" gap="5">
Baseline text must sit on the image baseline.

<BlockImage id="minecraft:stone" scale="2" />

Baseline text must sit on the image baseline.
</Row>

## Baseline (images: bottom-edge is the CSS image baseline)

Expected: A Row with `alignItems="baseline"` and mixed-height children (BlockImage scale 1 vs scale 2). Per CSS, a replaced element's baseline is its bottom edge, so the images' bottom edges are flush — taffy's BASELINE output for image-only rows is already correct and is NOT modified by the PLAN 2.7 post-layout. Compare against the start/center controls below.

<Row alignItems="baseline" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

## Control: start (top-edge flush)

Expected: The same mixed-height children with `alignItems="start"`. Children's top edges are flush; the taller middle child extends downward. Non-BASELINE container — untouched by the post-layout.

<Row alignItems="start" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

## Control: center (middle-line flush)

Expected: The same mixed-height children with `alignItems="center"`. Children share a common vertical middle line. Non-BASELINE container — untouched by the post-layout.

<Row alignItems="center" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>
