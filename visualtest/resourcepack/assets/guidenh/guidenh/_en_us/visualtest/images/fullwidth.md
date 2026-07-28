---
navigation:
  title: FullWidth Comparison (K4)
  position: 8480
---

TEST GOAL / 测试目标：fullWidth 图片/表格/代码块对照；backlog K4 复现（LytFloatAwareBlock 包装层丢 fullWidth）

INVARIANTS / 不变式：fullWidth 块的渲染宽度等于内容可用宽度（bounds 可断言）；普通块保留自然宽度；K4 场景中被 `LytFloatAwareBlock` 包装的 fullWidth 块应填满全页宽、不因浮动物而缩窄

## FullWidth Image via Container

Expected: The Column with fullWidth spans the full available width. The image inside appears left-aligned within the full-width container.

<Column fullWidth alignItems="start" gap="4">
**Image inside fullWidth Column**

![Red 64×64](../assets/red-64.png)
</Column>

## FullWidth Table via Container

Expected: The Column with fullWidth spans the full available width. The table inside uses the full column width; column widths are evenly distributed.

<Column fullWidth alignItems="start" gap="4">
**Table inside fullWidth Column**

| Material | Density | Hardness |
|----------|---------|----------|
| Iron     | 7.87    | 4        |
| Gold     | 19.32   | 2        |
| Diamond  | 3.51    | 10       |
</Column>

## FullWidth Code Block via Container

Expected: The Column with fullWidth spans the full available width. The code block inside occupies the full width of the container.

<Column fullWidth alignItems="start" gap="4">
**Code block inside fullWidth Column**

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("FullWidth test");
    }
}
```
</Column>

## Natural Width (No fullWidth) for Comparison

Expected: Without fullWidth, the Column shrinks to content width. The image, table, and code block appear at their natural widths, not stretching to fill the page.

<Column gap="4" alignItems="start">
**Image at natural width**

![Checker 128×128](../assets/checker-128.png)
</Column>

<Column gap="4" alignItems="start">
**Table at natural width**

| A | B |
|---|---|
| Short | Table |

</Column>

## K4 Replication: Float + FullWidth Table

Expected: The floating image narrows the text lane. The table below is wrapped in `LytFloatAwareBlock` (all non-paragraph blocks are). If fullWidth is correctly propagated through the wrapper, the table fills the full page width. If the K4 bug is present, the table only fills the narrowed lane (width clipped to the space beside the float).

<FloatingImage src="../assets/wide-256x64.png" align="left" x="0" y="0" width="256" height="64" title="float left for K4" />

Wrapping paragraph text beside the floated image. This text fills the space to the right of the image to demonstrate the narrowed lane. After the image clears, the paragraph continues below.

| K4 Test | Expected | Actual |
|---------|----------|--------|
| fullWidth table | Page width | Lane width if bug present |
| Table wrapped in FloatAwareBlock | Full width | Check with bounds JSON |

<br clear="all">

## K4 Replication: Float + FullWidth Code Block

Expected: Similar to the table case above. The code block is non-paragraph content and receives `LytFloatAwareBlock` wrapping. Its rendering width determines whether fullWidth propagates correctly.

<FloatingImage src="../assets/checker-128.png" align="right" x="0" y="0" width="128" height="128" title="float right for K4" />

Wrapping paragraph beside the right-floated image. The text fills the space to the left. After the image there is a code block that should fill the full width.

```
K4 fullWidth code block
```
<br clear="all">
