---
navigation:
  title: Charts
  parent: index.md
  position: 185
categories:
  - markdown
  - charts
---

# Charts

GuideNH ships with five interactive chart tags: `<ColumnChart>` clustered columns, `<BarChart>` horizontal bars, `<LineChart>` line, `<PieChart>` pie, and `<ScatterChart>` XY scatter. All charts show value + percentage tooltips on hover, and highlight the hovered element (column/bar grows, line points pop along the normal, pie slices pop outward, scatter points enlarge).

## Common Attributes

* `title`
* `width` / `height` (defaults: 320 x 200)
* `background` / `border` colors
* `titleColor` / `labelColor`
* `legend` position: `none` / `top` / `bottom` / `left` / `right` (default `top`)
* `labelPosition`: `none` / `inside` / `outside` / `above` / `below` / `center`
* `cornerLegend`: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft`; compact in-plot legend for line and scatter charts

Color formats: `#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`.

Column, line, and scatter charts show a gold horizontal guide anywhere inside the plot, with the current value and axis unit highlighted on the left. Bar charts use a vertical guide with the X value below. The guide disappears outside the plot or over a pie-inset slice.


## ColumnChart

```mdx
<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisMin="0" yAxisMax="250" yAxisStep="50" yAxisUnit="t" labelPosition="above">
  <Series name="Iron" data="120,180,150,210" color="#4E79A7"/>
  <Series name="Gold" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>
```

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisMin="0" yAxisMax="250" yAxisStep="50" yAxisUnit="t" labelPosition="above">
  <Series name="Iron" data="120,180,150,210" color="#4E79A7"/>
  <Series name="Gold" data="30,42,55,48" color="#F28E2B"/>
</ColumnChart>

Extra attributes: `categories` (X-axis labels, comma separated), `barWidthRatio` (default 0.7), `xAxisLabel` / `yAxisLabel` / `yAxisMin` / `yAxisMax` / `yAxisStep` / `yAxisUnit` / `yAxisTickFormat`, `showXGrid={true}` / `showYGrid={true}`.

## BarChart

```mdx
<BarChart title="Mod downloads (10k)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>
```

<BarChart title="Mod downloads (10k)" categories="GTNH,IC2,Thermal,Mekanism" labelPosition="outside">
  <Series data="320,210,180,150"/>
</BarChart>

Same attributes as ColumnChart but categories are on the Y-axis.

## LineChart

Categorical X:

```mdx
<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C" cornerLegend="topRight">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>
```

<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C" cornerLegend="topRight">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>

Numeric X:

```mdx
<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>
```

<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20"/>
</LineChart>

Extra: `numericX={true}` enables a numeric X axis; `showPoints={false}` hides point markers.

## PieChart

```mdx
<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice name="Iron" value="45" color="#4E79A7"/>
  <Slice name="Copper" value="25" color="#F28E2B"/>
  <Slice name="Gold" value="15" color="#E15759"/>
  <Slice name="Diamond" value="10"/>
  <Slice name="Other" value="5"/>
</PieChart>
```

<PieChart title="Resource Share" labelPosition="outside" legend="right">
  <Slice name="Iron" value="45" color="#4E79A7"/>
  <Slice name="Copper" value="25" color="#F28E2B"/>
  <Slice name="Gold" value="15" color="#E15759"/>
  <Slice name="Diamond" value="10"/>
  <Slice name="Other" value="5"/>
</PieChart>

Extra: `startAngle` (default -90, i.e. 12 o'clock); `clockwise={false}` to reverse direction.

## ScatterChart

```mdx
<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)" cornerLegend="bottomRight">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>
```

<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)" cornerLegend="bottomRight">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>

## Combo: ColumnChart + LineSeries + PieInset

`<ColumnChart>` and `<BarChart>` accept extra `<LineSeries>` (line overlay sharing the value axis) and `<PieInset>` (small corner pie) children, letting one chart combine several styles.

The pie inset's `position` attribute accepts `topRight` / `topLeft` / `bottomRight` / `bottomLeft` (corner overlay) or `right` (the chart auto-extends its width and the pie occupies a dedicated outside column).

Line overlays share hover/tooltip behavior with the underlying columns/bars: hovering a line point thickens its adjacent segments, enlarges the point, and shows a tooltip with the series name and value.

Pie-inset slices take hover priority and show their name, value, percentage, and optional `tooltip` text. Item tooltips remain available in game. The main guide is hidden over a slice, including in an inset outside the plot.

```mdx
<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="Total share">
    <Slice name="Iron" value="225" color="#a0a0a0"/>
    <Slice name="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>

```

<ColumnChart title="Quarterly Output" categories="Q1,Q2,Q3,Q4" yAxisUnit="t" labelPosition="above">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="right" title="Total share">
    <Slice name="Iron" value="225" color="#a0a0a0"/>
    <Slice name="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>

## Negative values and axis units

Move over the empty plot area to see signed values formatted with the axis unit.

<LineChart width="360" height="200" categories="A,B,C,D" yAxisMin="-100" yAxisMax="100" yAxisStep="50" yAxisUnit="EU/t" legend="none">
  <Series name="Energy change" data="-80,40,-20,90" color="#6cadc8" />
</LineChart>

## Bar chart with a pie inset

The gold vertical guide follows horizontal pointer movement. Hover the blue or yellow slice to see its name, value, percentage, and additional description; moving back to the main plot restores the guide.

<BarChart width="360" height="220" categories="LV,MV,HV,EV" xAxisMin="0" xAxisMax="200" xAxisStep="50" xAxisUnit="items/t" legend="none">
  <Series name="Output" data="60,90,130,170" color="#6cadc8" />
  <PieInset position="topRight" size="64" title="Output share">
    <Slice name="Main product" value="70" color="#6cadc8" tooltip="Main product: sent for further processing" />
    <Slice name="Byproduct" value="30" color="#f2c14e" tooltip="Byproduct: sent to the recycling line" />
  </PieInset>
</BarChart>

## Combined columns and an outside pie

The pie occupies a separate area on the right and remains interactive outside the main plot.

<ColumnChart width="300" height="220" categories="LV,MV,HV,EV" yAxisMin="0" yAxisMax="200" yAxisStep="50" legend="none">
  <Series name="Output" data="60,90,130,170" color="#6cadc8" />
  <LineSeries name="Target" data="70,100,140,180" color="#e09070" />
  <PieInset position="rightOutside" size="64" title="Output share" direction="counterclockwise">
    <Slice name="Main product" value="70" color="#6cadc8" tooltip="Main product: sent for further processing" />
    <Slice name="Byproduct" value="30" color="#f2c14e" tooltip="Byproduct: sent to the recycling line" />
  </PieInset>
</ColumnChart>

## A full-circle inset starting at zero degrees

The single positive slice fills the circle, while its tooltip reports 100%. Zero-valued slices have no hover target.

<ColumnChart categories="A,B" width="320" height="220" legend="none">
  <Series name="Output" data="20,40" />
  <PieInset size="64" title="Complete" startAngleDeg="0" direction="counterclockwise">
    <Slice name="Finished" value="100" tooltip="All output is complete" />
    <Slice name="Pending" value="0" />
  </PieInset>
</ColumnChart>
