---
navigation:
  title: Floats in ContentTabs
  position: 8370
---

TEST GOAL / 测试目标：ContentTabs 内浮动 + tabs 块前后浮动——tab 内浮动不泄漏；tabs 块不受外部浮动压缩

INVARIANTS / 不变式：tab 内浮动不泄漏到 tab 外；tabs 块自身（fullWidth LytFloatAwareBlock 路径）不受外部浮动异常压缩

## Floating Image Inside Tab

Expected: A `ContentTabs` with three tabs. The second tab contains a floating image with text wrapping. The float is confined within the tab content area and does not leak outside.

<ContentTabs title="Floats Inside Tabs" iconItem="minecraft:redstone">
<Tab title="Plain">
This tab contains only text. No float is present here.
</Tab>
<Tab title="Float">
<FloatingImage src="../assets/red-64.png" wrap="square" align="left" width="64" height="64" scaleX="2" scaleY="2" title="tab float" />

This text wraps around the floating image inside the tab. The float must be contained within the tab content bounds — it must not leak into adjacent tabs or outside the `ContentTabs` container. When switching to other tabs the float should not be visible.
</Tab>
<Tab title="Empty">
This tab has no float content — verifying that tab switching works correctly.
</Tab>
</ContentTabs>

## GameScene Inside Tab

Expected: A `ContentTabs` where a tab contains a floating `GameScene`. The scene float is contained within the tab area.

<ContentTabs title="Scene in Tab" default="Scene">
<Tab title="Text">
Regular paragraph text in the first tab.
</Tab>
<Tab title="Scene">
<GameScene wrap="square" align="right" width="120" height="80" zoom="5">
  <Block id="minecraft:diamond_block" />
</GameScene>

This text wraps to the left of the floating scene inside the tab. The scene is registered as a right-side document float confined to the tab content area. Switching tabs must not show this scene outside its tab.
</Tab>
</ContentTabs>

## External Float Before ContentTabs

Expected: A left float appears above the `ContentTabs` block. The `ContentTabs` block itself is not compressed by the external float — it renders at full available width.

<FloatingImage src="../assets/wide-256x64.png" wrap="square" align="left" width="256" height="64" scaleX="0.75" scaleY="1" title="external before" />

This paragraph wraps around the external float above the tabs. The float precedes the `ContentTabs` block.

<ContentTabs title="After External Float">
<Tab title="First">
The `ContentTabs` block appears below the external float. The tabs container should not be horizontally compressed by the active left float — it should render at its normal full width and push past the float vertical extent.
</Tab>
<Tab title="Second">
Additional tab content to verify tab switching still works correctly after an external float.
</Tab>
</ContentTabs>

## External Float After ContentTabs

Expected: A right float appears below the `ContentTabs` block. Text after the tabs wraps around the float normally.

<ContentTabs title="Before External Float" iconItem="minecraft:crafting_table">
<Tab title="A">
Content inside the tabs before the external float.
</Tab>
<Tab title="B">
More content inside the tabs.
</Tab>
</ContentTabs>

<FloatingImage src="../assets/checker-128.png" wrap="square" align="right" width="128" height="128" scaleX="0.75" scaleY="0.75" title="external after" />

This paragraph wraps to the left of the external float that follows the `ContentTabs` block. The float appears after the tabs and should not affect the internal layout of the tabs themselves.
