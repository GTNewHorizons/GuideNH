---
navigation:
  title: Basic Images
  position: 8500
---

TEST GOAL / 测试目标：`![alt](src)` standard markdown images at various sizes, center alignment, image caption via title attribute

INVARIANTS / 不变式：Images render at their native pixel dimensions; no distortion, cropping, or stretching; title text appears as a tooltip on hover; center-aligned image is horizontally centered in the available width

## Small Image (64×64)

Expected: The red 64×64 pixel square renders at its native size without distortion.

![Red 64×64](../assets/red-64.png)

## Medium Image (128×128)

Expected: The checkerboard 128×128 pixel square renders at its native size; all squares are evenly spaced.

![Checker 128×128](../assets/checker-128.png)

## Wide Image (256×64)

Expected: The 256×64 pixel wide banner (blue top half, green bottom half) renders at its native size; wider than tall.

![Wide 256×64](../assets/wide-256x64.png)

## Tall Image (64×256)

Expected: The 64×256 pixel tall bar (orange left half, purple right half) renders at its native size; taller than wide.

![Tall 64×256](../assets/tall-64x256.png)

## Image with Title (Caption / Tooltip)

Expected: Hovering the image displays the title text "A red square tile" as a tooltip; the image renders identically to the Small Image case above.

![Red 64×64](../assets/red-64.png "A red square tile")

## Center-Aligned Image

Expected: The image is horizontally centered within the available content width; equal empty space on both left and right sides.

![Red 64×64 centered](../assets/red-64.png){align=center}
