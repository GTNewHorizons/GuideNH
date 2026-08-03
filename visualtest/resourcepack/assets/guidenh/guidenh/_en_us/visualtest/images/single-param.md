---
navigation:
  title: FloatingImage Single-Param
  position: 8475
---

TEST GOAL / 测试目标：`<FloatingImage>` 单参数模式（width-only / height-only）——缺失维按图片自然宽高比推断，整图显示（无裁剪），不再报红错

INVARIANTS / 不变式：width-only 时高度 = width × natural_h/natural_w；height-only 时宽度 = height × natural_w/natural_h；无红错文本；图片实际渲染（非错误占位）

## Width-Only

Expected: The wide 256×64 image is displayed whole at width=128. Height is inferred as 128 × 64/256 = 32 px.

<FloatingImage src="../assets/wide-256x64.png" align="left" width="128" title="width-only: wide-256x64 at w=128" />

<br clear="all">

## Height-Only

Expected: The tall 64×256 image is displayed whole at height=128. Width is inferred as 128 × 64/256 = 32 px.

<FloatingImage src="../assets/tall-64x256.png" align="left" height="128" title="height-only: tall-64x256 at h=128" />

<br clear="all">
