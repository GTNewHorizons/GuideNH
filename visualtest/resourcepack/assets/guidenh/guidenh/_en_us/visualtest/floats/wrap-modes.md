---
navigation:
  title: Wrap Modes
  position: 8400
---

TEST GOAL / 测试目标：ContentWrapMode 六模式（square/tight/through/top-bottom/behind/front）各一例 + 文字环绕

INVARIANTS / 不变式：各模式环绕行为符合 ContentWrapMode 语义；behind 和 front 层序正确（z-order）

## Square Wrap

Expected: Image floats left; text wraps around its rectangular bounding box; no text intrudes into the image area.

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" x="0" y="0" scaleX="2" scaleY="2" title="square left" />

This paragraph demonstrates square wrapping. The image floats to the left of the text and the text flows to the right of it, occupying the remaining horizontal space. The wrap rectangle follows the image bounding box, creating a clean rectangular cutout in the text block. Multiple sentences here show how the layout engine reduces the available line width for each line by the float width.

<br clear="all">

## Tight Wrap

Expected: Image floats right; text wraps around it. In this layout system `tight` is functionally equivalent to `square`.

<FloatingImage src="../assets/checker-128.png" wrap="tight" align="right" width="128" height="128" x="0" y="0" scaleX="0.75" scaleY="0.75" title="tight right" />

This paragraph demonstrates tight wrapping. The image floats to the right and text flows to the left of the image. In the GuideNH layout engine `tight` is functionally equivalent to `square` — both use the rectangular bounding box for text flow. The text occupies the reduced line width on the left side of the float.

<br clear="all">

## Through Wrap

Expected: Image floats left; text wraps around it. In this layout system `through` is functionally equivalent to `square`.

<FloatingImage src="../assets/wide-256x64.png" wrap="through" align="left" width="256" height="64" x="0" y="0" scaleX="0.5" scaleY="1" title="through left" />

This paragraph demonstrates through wrapping. The image floats to the left and the text fills the space on the right. In the GuideNH layout engine `through` is functionally equivalent to `square`. The wide horizontal aspect ratio of this float creates a broad left-side float region that reduces available text width significantly.

<br clear="all">

## Top-Bottom Wrap

Expected: Image in top-bottom wrap; text only appears above and below, never beside it. (Engine note: FloatingImage align accepts only left/right — center is rejected with an error, so this case uses left.)

<FloatingImage src="../assets/tall-64x256.png" wrap="top-bottom" align="left" width="64" height="256" x="0" y="0" scaleX="1.5" scaleY="0.5" title="top-bottom center" />

This paragraph appears below the centred image. With `top-bottom` wrap, text never flows beside the block — it only appears above and below. The image occupies a full horizontal slot within the content area and text resumes only after the image vertical extent ends. This is equivalent to a non-floating block with explicit horizontal alignment.

<br clear="all">

## Behind Wrap

Expected: Image renders behind (below) surrounding text; text overlays the image area with no layout shift.

<FloatingImage src="../assets/red-64.png" wrap="behind" align="left" width="64" height="64" x="0" y="0" scaleX="3" scaleY="3" title="behind" />

This paragraph demonstrates behind wrapping. The image is rendered behind the text — the text is drawn on top of the image with full opacity. The image does not alter text layout: the paragraph occupies its normal full-width position. The image serves as a background decoration that the text overlays.

<br clear="all">

## Front Wrap

Expected: Image renders in front of (above) surrounding text; image overlays the text content.

<FloatingImage src="../assets/checker-128.png" wrap="front" align="right" width="128" height="128" x="0" y="0" scaleX="0.5" scaleY="0.5" title="front" />

This paragraph demonstrates front wrapping. The image is rendered in front of the text, overlaying the paragraph content. The text layout is unaffected by the image — it flows at full width. The image appears as an overlay on top of the paragraph, potentially obscuring the text beneath it.
