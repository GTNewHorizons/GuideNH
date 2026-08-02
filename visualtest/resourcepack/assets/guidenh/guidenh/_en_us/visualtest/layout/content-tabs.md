---
navigation:
  title: Content Tabs
  position: 8270
---

TEST GOAL / 测试目标：ContentTabs 多 Tab（不同内容类型）、title/default/defaultIndex/color/icon 变体

INVARIANTS / 不变式：Tab 头与内容对应；切换 Tab 显示正确内容；title/color/icon 渲染正确

## Three Tabs with Different Content Types

Expected: ContentTabs with three tabs: Text, Code, and Image. Clicking each tab reveals its respective content type.

<ContentTabs>
<Tab title="Text">
This is plain text content inside the first tab.

A second paragraph of text.
</Tab>
<Tab title="Code">
```xml
<Block id="minecraft:diamond_block" />
```
</Tab>
<Tab title="Image">
![](../assets/red-64.png)
</Tab>
</ContentTabs>

## Four Tabs with Title and Icon

Expected: ContentTabs with a title heading "Resource Variants", an icon of a crafting table, and four tabs showing different block images.

<ContentTabs title="Resource Variants" iconItem="minecraft:crafting_table">
<Tab title="Diamond">
<BlockImage id="minecraft:diamond_block" scale="1.5" />
</Tab>
<Tab title="Iron">
<BlockImage id="minecraft:iron_block" scale="1.5" />
</Tab>
<Tab title="Gold">
<BlockImage id="minecraft:gold_block" scale="1.5" />
</Tab>
<Tab title="Emerald">
<BlockImage id="minecraft:emerald_block" scale="1.5" />
</Tab>
</ContentTabs>

## Default Tab via defaultIndex

Expected: ContentTabs with defaultIndex=1. The second tab (Code) is selected on first render instead of the first.

<ContentTabs defaultIndex="1">
<Tab title="Intro">
Introduction text shown first by default without defaultIndex.
</Tab>
<Tab title="Code">
```java
// This tab is selected by default via defaultIndex=1
int x = 42;
```
</Tab>
<Tab title="Notes">
Additional notes that require manual tab switch.
</Tab>
</ContentTabs>

## Default Tab via default Title

Expected: ContentTabs with default="Notes". The tab titled "Notes" is selected on first render.

<ContentTabs default="Notes">
<Tab title="Intro">
Welcome text.
</Tab>
<Tab title="Details">
Detailed explanation.
</Tab>
<Tab title="Notes">
This tab is selected by default via the default attribute.
</Tab>
</ContentTabs>

## Color and Icon Variants

Expected: ContentTabs with color="#ff6b6b" (red accent) and iconItem="minecraft:redstone". The accent color is applied to the tab decoration line or highlight.

<ContentTabs color="#ff6b6b" iconItem="minecraft:redstone" title="Red Theme">
<Tab title="Overview">
Overview content with red accent theme.
</Tab>
<Tab title="Config">
Configuration details.
</Tab>
<Tab title="Output">
Output visualization.
</Tab>
</ContentTabs>

## Text and PNG Icons

Expected: ContentTabs with a title heading that renders the plain-text icon `</>` before the bold title "Code Style"; the tab strip and content render normally.

<ContentTabs icon="</>" title="Code Style">
<Tab title="Java">
```java
int x = 42;
```
</Tab>
<Tab title="XML">
```xml
<Block id="minecraft:diamond_block" />
```
</Tab>
</ContentTabs>

Expected: ContentTabs with a title heading that renders the 8×8 red-64.png image icon before the bold title "Red Theme"; the tab strip and content render normally.

<ContentTabs iconPng="../assets/red-64.png" title="Red Theme">
<Tab title="Overview">
Overview content with an image heading icon.
</Tab>
<Tab title="Config">
Configuration details.
</Tab>
</ContentTabs>
