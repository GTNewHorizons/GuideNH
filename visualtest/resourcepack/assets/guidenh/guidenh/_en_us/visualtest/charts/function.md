---
navigation:
  title: Function Graphs
  position: 8230
---

TEST GOAL / 测试目标：FunctionGraph + Plot + Point、Function 简写、xRange/quadrants

INVARIANTS / 不变式：曲线平滑连续、标记点位置正确、象限限制生效、标签不重叠

## FunctionGraph with Plots and Points

Expected: Three curves (sin, parabola, absolute value) plotted on all quadrants; explicit `Point` at origin and a plot-anchored `Point` at `atX=1.5708` on plot index 0.

<FunctionGraph width="360" height="220" xRange="-6..6" yRange="-3..3" quadrants="all" cornerLegend="topRight">
  <Plot expr="sin(x)" color="#ff5566" label="sin x" pointEveryX="1" autoPointLabel="x"/>
  <Plot expr="x^2 / 4" color="#3399ff" domain="-4..4" label="x² / 4"/>
  <Plot expr="|x| - 1" color="#88cc77" label="|x| - 1" pointEveryY="1"/>
  <Point x="0" y="0"/>
  <Point plot="0" atX="1.5708"/>
</FunctionGraph>

## Function Shorthand

Expected: Single-curve graph rendered from the `<Function>` short tag with expression, range, and color.

<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff"/>

## Quadrant Restriction

Expected: Only quadrants 1 and 4 visible; the curve is clipped to the upper-right and lower-right regions.

<FunctionGraph width="300" height="200" xRange="-2..6" yRange="-3..3" quadrants="1,4">
  <Plot expr="x - 1" color="#E15759" label="x - 1"/>
</FunctionGraph>

## Fenced FunctionGraph Block

Expected: Function graph rendered from a `funcgraph` fenced code block with same style as the container version.

```funcgraph
width=360 height=220 xRange=-pi..pi yRange=-2..2 quadrants=all cornerLegend=topRight
sin(x)        | color=#ff5566 label="sin" pointEveryX=1 autoPointLabel=x
cos(x)        | color=#3399ff label="cos" pointEveryY=1
x/2           | color=#88cc77 domain=-pi..pi
:0,0
@plot=0 atX=1.5708
```
