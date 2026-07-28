---
navigation:
  title: Chart Options (Legend, Axes, Grid)
  position: 8220
---

TEST GOAL / 测试目标：legend 五值、cornerLegend、轴 label/min/max/step/unit/tickFormat、grid 开关与颜色

INVARIANTS / 不变式：图例位置按属性渲染、轴范围/步长正确、grid 显隐与颜色可区分

## Legend Positions

Expected: Five `ColumnChart` blocks each showing legend at `none`, `top`, `bottom`, `left`, `right`.

<ColumnChart title="Legend: none" legend="none" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</ColumnChart>

<ColumnChart title="Legend: top" legend="top" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</ColumnChart>

<ColumnChart title="Legend: bottom" legend="bottom" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</ColumnChart>

<ColumnChart title="Legend: left" legend="left" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</ColumnChart>

<ColumnChart title="Legend: right" legend="right" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</ColumnChart>

## Corner Legend

Expected: In-plot compact legend at `topRight`, `topLeft`, `bottomRight`, `bottomLeft` positions in a `LineChart`.

<LineChart title="Corner Legend" cornerLegend="topRight" width="300" height="150">
  <Series name="Series A" data="10,20,15,25" color="#4E79A7"/>
  <Series name="Series B" data="5,15,10,20" color="#F28E2B"/>
</LineChart>

<LineChart title="Corner TL" cornerLegend="topLeft" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</LineChart>

<LineChart title="Corner BR" cornerLegend="bottomRight" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</LineChart>

<LineChart title="Corner BL" cornerLegend="bottomLeft" width="300" height="150">
  <Series name="A" data="10,20,15" color="#4E79A7"/>
  <Series name="B" data="5,15,10" color="#F28E2B"/>
</LineChart>

## Axis Labels, Min, Max, Step, Unit, TickFormat

Expected: Bar chart with explicit axis range (`yAxisMin`/`yAxisMax`), step (`yAxisStep`), unit label (`yAxisUnit`), and tick format (`yAxisTickFormat`); X and Y axis labels visible.

<BarChart title="Axis Options" categories="A,B,C,D" xAxisLabel="Category" yAxisLabel="Value" yAxisMin="0" yAxisMax="50" yAxisStep="10" yAxisUnit="%" yAxisTickFormat="0'%'">
  <Series name="Data" data="25,40,15,35" color="#4E79A7"/>
</BarChart>

## Grid Toggle and Colors

Expected: Chart with both grids disabled (`showXGrid={false}` `showYGrid={false}`); chart with custom grid colors (`xGridColor="#ff0000"` `yGridColor="#00ff00"`).

<LineChart title="Grid Off" showXGrid={false} showYGrid={false} width="300" height="150">
  <Series name="No Grid" data="10,20,15,25" color="#4E79A7"/>
</LineChart>

<ColumnChart title="Grid Colors" xGridColor="#ff0000" yGridColor="#00ff00" width="300" height="150">
  <Series name="Colored Grid" data="10,20,15,25" color="#4E79A7"/>
</ColumnChart>
