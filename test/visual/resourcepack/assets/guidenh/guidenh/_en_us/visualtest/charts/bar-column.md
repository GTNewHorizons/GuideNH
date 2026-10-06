---
navigation:
  title: Bar and Column Charts
  position: 8260
---

TEST GOAL: BarChart + ColumnChart basics, Series data/color/icon, PieInset embedded pie chart

INVARIANTS: bar/column charts render correctly, the legend does not overflow, the embedded pie chart does not obscure the main chart

## BarChart with Series data/color/icon

Expected: Horizontal bars for three mod categories; each bar colored per `Series` color with an item icon in the legend.

<BarChart title="Mod Downloads (10k)" categories="GTNH,IC2,AE2" labelPosition="outside">
  <Series name="GTNH" data="320" color="#4E79A7" icon="minecraft:iron_ingot"/>
  <Series name="IC2" data="210" color="#F28E2B" icon="minecraft:gold_ingot"/>
  <Series name="AE2" data="180" color="#E15759" icon="minecraft:diamond"/>
</BarChart>

## ColumnChart with Series data/color

Expected: Clustered columns across four categories; each series has distinct color; y-axis auto-scales to data range.

<ColumnChart title="Monthly Production" categories="Jan,Feb,Mar,Apr" yAxisUnit="t">
  <Series name="Iron" data="40,60,55,70" color="#4E79A7"/>
  <Series name="Gold" data="20,30,25,35" color="#F28E2B"/>
  <Series name="Copper" data="50,45,60,55" color="#59A14F"/>
</ColumnChart>

## ColumnChart with PieInset

Expected: Column chart with a `LineSeries` overlay and a `PieInset` positioned on the right side; the pie chart shows total share of the two `Series`.

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="Total share">
    <Slice name="Iron" value="225" color="#a0a0a0"/>
    <Slice name="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
