---
navigation:
  title: FloatingImage Features
  position: 8470
---

TEST GOAL / 测试目标：`<FloatingImage>` 全套：裁剪 (x/y/w/h)、缩放 (scaleX/scaleY)、`<ImageAnnotation>` 热区、`<SoundArea>`（仅查渲染，不发声）

INVARIANTS / 不变式：裁剪区域正确截取原图子矩形；缩放不裁切内容；热区 hover tooltip 显示正确文本；SoundArea 区域渲染无崩溃

## Basic Crop (x, y, width, height)

Expected: The full 64×64 red image is displayed at native size. Crop matches the original image bounds exactly.

<FloatingImage src="../assets/red-64.png" align="left" x="0" y="0" width="64" height="64" title="full red 64x64" />

<br clear="both">

## Cropped Top-Left Quadrant

Expected: Only the top-left 32×32 pixels of the red image are displayed. The crop rectangle selects (0,0)-(32,32).

<FloatingImage src="../assets/red-64.png" align="left" x="0" y="0" width="32" height="32" title="top-left 32x32 crop" />

<br clear="both">

## Cropped Center with w and h Aliases

Expected: The center 32×32 pixels of the checker image are displayed using the `w`/`h` attribute aliases. The crop rectangle selects (48,48)-(80,80).

<FloatingImage src="../assets/checker-128.png" align="left" x="48" y="48" w="32" h="32" title="center 32x32 using w/h" />

<br clear="both">

## Scale Stretch (scaleX)

Expected: The 32×32 cropped region of the wide image is stretched horizontally (scaleX=3) while height remains at native scale (scaleY=1). Result is 96×32 pixels.

<FloatingImage src="../assets/wide-256x64.png" align="left" x="0" y="0" width="32" height="32" scaleX="3" scaleY="1" title="stretch X 3x" />

<br clear="both">

## Scale Stretch (scaleY)

Expected: The 32×32 cropped region of the tall image is stretched vertically (scaleY=3) while width remains at native scale (scaleX=1). Result is 32×96 pixels.

<FloatingImage src="../assets/tall-64x256.png" align="left" x="0" y="0" width="32" height="32" scaleX="1" scaleY="3" title="stretch Y 3x" />

<br clear="both">

## Whole-Image Annotation

Expected: Hovering anywhere over the image displays the tooltip text "This annotation covers the entire image area."

<FloatingImage src="../assets/red-64.png" align="left" x="0" y="0" width="64" height="64">
  <ImageAnnotation>
    This annotation covers the entire image area.
  </ImageAnnotation>
</FloatingImage>

<br clear="both">

## Region Annotation with Border

Expected: The annotated region (10,10)-(50,30) has a visible red border (borderColor="#FFFF4444", borderThickness=2). Hovering the region shows the tooltip "Red-bordered region tooltip."

<FloatingImage src="../assets/checker-128.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="40" h="20" border borderColor="#FFFF4444" borderThickness="2">
    Red-bordered region tooltip.
  </ImageAnnotation>
</FloatingImage>

<br clear="both">

## Multiple Annotations

Expected: Two annotation regions on the same image. The left half (0,0)-(64,128) shows tooltip "Left region" with a green border. The right half (64,0)-(64,128) shows tooltip "Right region" with a blue border.

<FloatingImage src="../assets/checker-128.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="128" border borderColor="#FF44FF44" borderThickness="1">
    Left region
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="128" border borderColor="#FF4444FF" borderThickness="1">
    Right region
  </ImageAnnotation>
</FloatingImage>

<br clear="both">

## Annotation on Scaled Crop

Expected: The cropped region (0,0)-(64,64) is stretched (scaleX=2, scaleY=1.5). The annotation covers the entire cropped region. The border follows the stretch.

<FloatingImage src="../assets/red-64.png" align="left" x="0" y="0" width="64" height="64" scaleX="2" scaleY="1.5">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FFFFFF88" borderThickness="2">
    Annotation follows scale transform
  </ImageAnnotation>
</FloatingImage>

<br clear="both">

## SoundArea (Click Region)

Expected: The left half (0,0)-(32,64) is a clickable SoundArea that would play a sound in a real environment. No audio is verified in this test; only rendering of the annotation region is checked.

<FloatingImage src="../assets/red-64.png" align="left" x="0" y="0" width="64" height="64">
  <SoundArea x="0" y="0" w="32" h="64" sound="guidenh:test.left_click" />
</FloatingImage>

<br clear="both">

## SoundArea (Hover Trigger)

Expected: The right half (32,0)-(32,64) is a SoundArea with trigger="hover". Hovering the region would activate the sound. Only rendering is checked.

<FloatingImage src="../assets/red-64.png" align="left" x="0" y="0" width="64" height="64">
  <SoundArea x="32" y="0" w="32" h="64" sound="guidenh:test.right_hover" trigger="hover" />
</FloatingImage>

<br clear="both">
