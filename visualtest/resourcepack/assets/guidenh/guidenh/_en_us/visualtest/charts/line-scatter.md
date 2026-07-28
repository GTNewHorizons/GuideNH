---
navigation:
  title: Line and Scatter Charts
  position: 8250
---

TEST GOAL / 测试目标：LineChart numericX + ScatterChart + LineSeries

INVARIANTS / 不变式：折线连续、散点不压文字、组合图 LineSeries 覆盖正确

## LineChart with Categorical X

Expected: Lines across five day categories; corner legend in top-right corner; hover tooltip shown.

<LineChart title="Temperature" categories="Mon,Tue,Wed,Thu,Fri" yAxisUnit="C" cornerLegend="topRight">
  <Series name="Outdoor" data="5,8,11,9,6" color="#4E79A7"/>
  <Series name="Indoor" data="18,19,20,21,20" color="#E15759"/>
</LineChart>

## LineChart with Numeric X

Expected: Line chart with `numericX={true}`; X axis labelled "Distance (m)", Y axis labelled "Strength (dB)"; series uses `points` string.

<LineChart title="Signal Decay" numericX={true} xAxisLabel="Distance (m)" yAxisLabel="Strength (dB)">
  <Series name="Measured" points="0:0,5:-3,10:-7,20:-12,40:-20" color="#4E79A7"/>
</LineChart>

## ScatterChart

Expected: Scatter plot showing two sample groups with distinct colors; corner legend at bottom-right.

<ScatterChart title="Height-Weight" xAxisLabel="Height (cm)" yAxisLabel="Weight (kg)" cornerLegend="bottomRight">
  <Series name="Sample A" points="160:55,165:58,170:65,175:70,180:78" color="#4E79A7"/>
  <Series name="Sample B" points="158:52,168:62,172:68,178:75" color="#59A14F"/>
</ScatterChart>

## ColumnChart with LineSeries Overlay

Expected: Column chart with a `LineSeries` overlay sharing the same value axis; bars and line both visible.

<ColumnChart title="Production vs Target" categories="Q1,Q2,Q3,Q4" yAxisUnit="k">
  <Series name="Actual" data="45,62,58,80" color="#4E79A7"/>
  <LineSeries name="Target" data="50,60,60,75" color="#E15759"/>
</ColumnChart>
