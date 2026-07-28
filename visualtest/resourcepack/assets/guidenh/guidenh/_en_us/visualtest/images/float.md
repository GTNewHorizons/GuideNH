---
navigation:
  title: Floating Images (Wrap × Align)
  position: 8490
---

TEST GOAL / 测试目标：`<FloatingImage>` wrap modes (square, tight, through) × align (left, right) matrix with surrounding text wrapping and `<br clear="all">`

INVARIANTS / 不变式：Text wraps around the floated image on the opposite side; no overlap between image and text; `<br clear="all">` clears all floats and resumes normal flow below the tallest float

## wrap="square" align="left"

Expected: Image floats to the left; text wraps around its right side in a rectangular boundary; image does not overlap text.

<FloatingImage src="../assets/red-64.png" align="left" wrap="square" x="0" y="0" width="64" height="64" title="square left" />

Surrounding text that wraps around the left-floated image. This paragraph should fill the space to the right of the image, then continue below it once the image height is passed. The engine should maintain the rectangular wrap boundary described by the image bounding box. Additional filler text ensures the wrapping behavior is visible across multiple lines.

<br clear="all">

## wrap="square" align="right"

Expected: Image floats to the right; text wraps around its left side in a rectangular boundary.

<FloatingImage src="../assets/checker-128.png" align="right" wrap="square" x="0" y="0" width="128" height="128" title="square right" />

Surrounding text that wraps around the right-floated image. This paragraph fills the space to the left of the image and then continues below it. The wrap boundary follows the right edge of the text area. Additional filler text ensures the wrapping behavior is visible across multiple lines of content.

<br clear="all">

## wrap="tight" align="left"

Expected: Image floats to the left; text wraps around its right side. The tight mode should produce the same visual result as square in this engine (functionally equivalent).

<FloatingImage src="../assets/wide-256x64.png" align="left" wrap="tight" x="0" y="0" width="256" height="64" title="tight left" />

Surrounding text that wraps around the left-floated wide image. The wide aspect ratio means the text column to the right is narrower. The text should still flow cleanly without overlapping the image boundaries. Additional filler text here ensures that multiple lines of wrapping are clearly visible.

<br clear="all">

## wrap="tight" align="right"

Expected: Image floats to the right; text wraps around its left side.

<FloatingImage src="../assets/tall-64x256.png" align="right" wrap="tight" x="0" y="0" width="64" height="256" title="tight right" />

Surrounding text that wraps around the right-floated tall image. The tall aspect ratio means the text column to the left is wide. Multiple short lines of text should fill the space beside the tall image before continuing below it. Additional filler ensures wrapping is visible across the full height of the image.

<br clear="all">

## wrap="through" align="left"

Expected: Image floats to the left; text wraps around its right side. Through mode should produce the same visual result as square in this engine (functionally equivalent).

<FloatingImage src="../assets/red-64.png" align="left" wrap="through" x="0" y="0" width="64" height="64" title="through left" />

Surrounding text wraps around the left-floated image. The through mode is documented as equivalent to square in this layout system. The text should fill the available space to the right and resume below the image without any gap or overlap.

<br clear="all">

## wrap="through" align="right"

Expected: Image floats to the right; text wraps around its left side. After the last float, `<br clear="all">` resets flow to below all floated content.

<FloatingImage src="../assets/checker-128.png" align="right" wrap="through" x="0" y="0" width="128" height="128" title="through right" />

Surrounding text wraps around the right-floated image. This is the last wrap test case. The text to the left should fill the available space and then resume below. After this paragraph the clear marker resets the float context entirely for subsequent content.

<br clear="all">
