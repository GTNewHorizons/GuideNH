---
navigation:
  title: Multiple Floats and Clear
  position: 8380
---

TEST GOAL / 测试目标：多浮动 + 清除——连续左浮3个、左右对浮、浮动紧跟浮动、br clear 三变体

INVARIANTS / 不变式：浮动间不重叠；clear 后文字从浮动底边以下开始

## Three Consecutive Left Floats

Expected: Three images stacked vertically on the left side; text flows to the right of the stack. Floats do not overlap one another.

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="1" scaleY="1" title="float A" />

<FloatingImage src="../assets/checker-128.png" wrap="square" align="left" width="128" height="128" scaleX="0.5" scaleY="0.5" title="float B" />

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="1.5" scaleY="1.5" title="float C" />

This paragraph flows to the right of three consecutive left floats. Each float occupies its own vertical position on the left margin without overlapping the previous float. The layout engine tracks the cumulative float region and adjusts the text lane accordingly. The third float sits below the second, and text continues on the right of all three.

<br clear="both">

## Left-Right Paired Floats

Expected: One image floats left and another floats right simultaneously; text fills the space between them.

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="2" scaleY="2" title="left float" />

<FloatingImage src="../assets/checker-128.png" wrap="square" align="right" width="128" height="128" scaleX="0.5" scaleY="0.5" title="right float" />

This paragraph flows in the space between the left and right floats. The left float claims space on the left side and the right float claims space on the right side. The text occupies the reduced horizontal lane between them. Multiple lines of text demonstrate the centred text column created by opposing floats.

<br clear="both">

## Float Immediately Followed by Float

Expected: A right float appears directly below a left float with no text between; both floats occupy their respective sides without overlapping.

<FloatingImage src="../assets/wide-256x64.png" wrap="square" align="left" width="256" height="64" scaleX="0.5" scaleY="1" title="first float" />

<FloatingImage src="../assets/tall-64x256.png" wrap="square" align="right" width="64" height="256" scaleX="1" scaleY="0.5" title="second float" />

Text after the pair of adjacent floats. The first float occupies the left side and the second float occupies the right side. There is no intervening text or clear between them — the engine handles float stacking at the same vertical level.

<br clear="both">

## br clear="left"

Expected: Text after `<br clear="left">` starts below the cleared left float; right float remains active.

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="2" scaleY="2" title="left clear target" />

<FloatingImage src="../assets/checker-128.png" wrap="square" align="right" width="128" height="128" scaleX="0.5" scaleY="0.5" title="right stays" />

Text between the floats to show the wrapping lane.
<br clear="left">
This line starts below the left float after `clear="left"` but the right float is still active and text wraps around it.

<br clear="both">

## br clear="right"

Expected: Text after `<br clear="right">` starts below the cleared right float; left float remains active.

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="2" scaleY="2" title="left stays" />

<FloatingImage src="../assets/checker-128.png" wrap="square" align="right" width="128" height="128" scaleX="0.5" scaleY="0.5" title="right clear target" />

Text in the lane between floats.
<br clear="right">
This line starts below the right float after `clear="right"` but the left float is still active and text wraps around it.

<br clear="both">

## br clear="both"

Expected: Text after `<br clear="both">` starts below both cleared floats; full width is restored.

<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="2" scaleY="2" title="left" />

<FloatingImage src="../assets/checker-128.png" wrap="square" align="right" width="128" height="128" scaleX="0.5" scaleY="0.5" title="right" />

Text in the lane.
<br clear="both">
This line starts below both floats after `clear="both"`. The full page width is restored and text flows normally from margin to margin without any active float constraint.
