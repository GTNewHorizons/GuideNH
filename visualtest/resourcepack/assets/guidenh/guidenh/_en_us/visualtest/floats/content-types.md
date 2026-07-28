---
navigation:
  title: Floating Content Types
  position: 8390
---

TEST GOAL / 测试目标：任意块可浮——表格、代码块、GameScene、Recipe、图表、Column 容器各以浮动形式展示

INVARIANTS / 不变式：非段落块（LytFloatAwareBlock 路径）可用宽度缩减正确；内容不与浮动重叠

## Floating Table (CsvTable)

Expected: `CsvTable` floats left with `wrap="square"`; text wraps to the right of the table.

<CsvTable src="../assets/test-table.csv" wrap="square" align="left" />

This paragraph flows to the right of the CSV table. The table registers as a left-side document float and subsequent paragraphs shrink their available width to avoid it, just like a CSS `float: left`. Multiple lines of text here demonstrate the wrapping effect around the floated table.

<br clear="all">

## Floating Code Block

Expected: Fenced code block floats left with `wrap="square"` applied on the fence line; text wraps to the right.

```java wrap=square align=left
public class FloatTest {
    public static void main(String[] args) {
        System.out.println("floating code block");
    }
}
```

This paragraph flows to the right of the floated code block. The code block registers as a left-side document float and the text fills the remaining width. The code block background and syntax highlighting should render correctly within the float region.

<br clear="all">

## Floating GameScene

Expected: `GameScene` floats left with `wrap="square"`; text wraps to the right of the scene viewport.

<GameScene wrap="square" align="left" width="120" height="80" zoom="5">
  <Block id="minecraft:diamond_block" />
</GameScene>

This paragraph flows to the right of the scene viewport. The `wrap="square" align="left"` combination registers the scene as a left-side document float. All subsequent paragraphs narrow their available width until the float clears. This demonstrates that block-level 3D scenes participate in the float system.

<br clear="all">

## Floating Recipe

Expected: `Recipe` floats left with `wrap="square"`; text wraps to the right. NEI may be unavailable in test environment — rendered fallback text is acceptable.

<Recipe id="minecraft:stone" wrap="square" align="left" fallbackText="(recipe unavailable)" />

This paragraph flows to the right of the recipe box. The recipe registers as a left-side document float. In a real GTNH environment the recipe grid renders; in headless mode the fallback text is shown. The float behavior (text wrapping) should work identically in both cases.

<br clear="all">

## Floating Chart (BarChart)

Expected: `BarChart` floats left with `wrap="square"`; text wraps to the right of the chart.

<BarChart wrap="square" align="left" width="200" height="120" title="Floating Chart">
  <Series name="Test" data="3,7,5,9" />
</BarChart>

This paragraph flows to the right of the bar chart. The chart registers as a left-side document float. Text fills the available space beside the chart, demonstrating that chart components also participate in the floating layout system.

<br clear="all">

## Floating Column Container

Expected: `Column` with `wrap="square"` floats left; text wraps to the right of the column.

<Column wrap="square" align="left" gap="4" width="120">

**Floated Column**

This column contains structured content and floats left like any other block-level tag.

</Column>

This paragraph flows to the right of the column container. The `<Column>` block carrying `wrap="square" align="left"` floats left and subsequent paragraph text fills the space beside it. This demonstrates that flex containers also participate in the float system via the `LytFloatAwareBlock` path.
