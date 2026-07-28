---
navigation:
  title: Pie Charts
  position: 8240
---

TEST GOAL / 测试目标：PieChart + Slice（startAngle/clockwise/labelPosition）

INVARIANTS / 不变式：饼图切片比例正确、标签不重叠、方向/起始角按属性渲染

## Default PieChart

Expected: Five slices with automatic color palette; labels positioned outside; legend on the right side.

<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice name="Iron" value="45" color="#4E79A7"/>
  <Slice name="Copper" value="25" color="#F28E2B"/>
  <Slice name="Gold" value="15" color="#E15759"/>
  <Slice name="Diamond" value="10"/>
  <Slice name="Other" value="5"/>
</PieChart>

## Custom Start Angle

Expected: Pie chart starting from 45 degrees (1:30 position) instead of default -90 degrees (12 o'clock).

<PieChart title="Custom Start Angle" startAngle="45" labelPosition="outside">
  <Slice name="Iron" value="40" color="#4E79A7"/>
  <Slice name="Gold" value="30" color="#F28E2B"/>
  <Slice name="Copper" value="30" color="#59A14F"/>
</PieChart>

## Counterclockwise Direction

Expected: Slices arranged counterclockwise (`clockwise={false}`) instead of the default clockwise order.

<PieChart title="Counterclockwise" clockwise={false} labelPosition="center">
  <Slice name="A" value="30" color="#E15759"/>
  <Slice name="B" value="25" color="#F28E2B"/>
  <Slice name="C" value="25" color="#4E79A7"/>
  <Slice name="D" value="20" color="#59A14F"/>
</PieChart>

## Label Position Inside

Expected: Slice labels rendered inside the pie segments; no labels outside the pie boundary.

<PieChart title="Inside Labels" labelPosition="inside">
  <Slice name="Red" value="35" color="#E15759"/>
  <Slice name="Blue" value="30" color="#4E79A7"/>
  <Slice name="Green" value="20" color="#59A14F"/>
  <Slice name="Yellow" value="15" color="#F28E2B"/>
</PieChart>
