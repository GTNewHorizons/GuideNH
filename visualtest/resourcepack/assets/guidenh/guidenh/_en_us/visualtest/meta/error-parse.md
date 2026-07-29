---
navigation:
  title: Error Page (Parse Failure)
  position: 7800
---

TEST GOAL / 测试目标：错误文本应以红色渲染（createErrorFlowContent / buildErrorPage 路径）

INVARIANTS / 不变式：属性错误文本、Color 标签红、§4 红三者颜色一致；错误文本不是灰色正文色。

Color reference A (Color tag): <Color id="red">THIS_SHOULD_BE_RED_VIA_COLOR_TAG</Color>

Color reference B (section code): §4THIS_SHOULD_BE_RED_VIA_SECTION_CODE§r

Attribute error below (should also be red, was gray):

<ItemImage id={} />
