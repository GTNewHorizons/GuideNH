---
navigation:
  title: FloatingImage Single-Param
  position: 8475
---

TEST GOAL: `<FloatingImage>` single-parameter modes (width-only / height-only) — the missing dimension is inferred from the image's natural aspect ratio, the whole image is shown (no cropping), and no red error is reported anymore

INVARIANTS: with width-only, height = width × natural_h/natural_w; with height-only, width = height × natural_w/natural_h; no red error text; the image actually renders (not an error placeholder)

## Width-Only

Expected: The wide 256×64 image is displayed whole at width=128. Height is inferred as 128 × 64/256 = 32 px.

<FloatingImage src="../assets/wide-256x64.png" align="left" width="128" title="width-only: wide-256x64 at w=128" />

<br clear="all">

## Height-Only

Expected: The tall 64×256 image is displayed whole at height=128. Width is inferred as 128 × 64/256 = 32 px.

<FloatingImage src="../assets/tall-64x256.png" align="left" height="128" title="height-only: tall-64x256 at h=128" />

<br clear="all">
