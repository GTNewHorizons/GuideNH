---
navigation:
  title: 图表
  parent: index.md
  position: 185
categories:
  - markdown
  - charts
---

# 图表

GuideNH 内置五种交互式图表标签：`<ColumnChart>`（竖柱）、`<BarChart>`（横条）、`<LineChart>`（折线）、`<PieChart>`（饼图）、`<ScatterChart>`（散点图）。

通用属性：
- `title`：图表标题（字符串）。
- `categories`：逗号分隔的类目标签（字符串），对应横轴（ColumnChart/LineChart/BarChart）或图例（PieChart）。
- `xAxisLabel` / `yAxisLabel`：轴标签，不填则使用 `xAxisUnit` / `yAxisUnit`。
- `xAxisUnit` / `yAxisUnit`：轴单位，追加在自动生成标签后。
- `cornerLegend`：折线图与散点图的图内图例位置，`none`、`topRight`、`topLeft`、`bottomRight` 或 `bottomLeft`。
- `width` / `height`：画布尺寸（像素），默认为图表类型的合理默认值。

柱形图、折线图和散点图默认提供鼠标水平辅助线：将鼠标移入绘图区，金色横虚线跟随鼠标高度，左侧坐标轴以金色底突出显示对应数值和单位。虚线上不显示文字，无需命中数据点；移出绘图区后自动隐藏。

固定参考线使用灰色，与金色鼠标线同时显示；本页第一个示例固定在 `y=100`。横、竖轴的刻度网格使用更淡、更稀疏的虚线，便于对齐 50、100 等数值；可通过 `showXGrid` / `showYGrid` 分别开关。

## ColumnChart

<ColumnChart
  title="季度产量"
  categories="Q1,Q2,Q3,Q4"
  width="400"
  height="240"
  yAxisMin="0"
  yAxisMax="250"
  yAxisStep="50"
>
  <Series name="铁" color="#888888" data="120,200,150,80" />
  <Series name="金" color="#FFAA00" data="30,50,90,70" />
  <ReferenceLine y="100" />
</ColumnChart>

把鼠标移到柱子之间的空白处，或沿绘图区上下移动，可观察辅助线与左侧高亮数值的变化。关闭网格也可使用辅助线。

## BarChart

横条图使用竖向金色鼠标辅助线，左右移动鼠标时在底部数值轴高亮当前值。灰色固定竖线通过 `<ReferenceLine x="200"/>` 指定；横、竖刻度网格仍为淡虚线。

<BarChart
  title="模组下载量 (万)"
  categories="GTNH,IC2,Thermal,Mekanism"
  width="400"
  height="240"
>
  <Series name="2024" color="#4488ff" data="350,180,210,90" />
  <ReferenceLine x="200" />
</BarChart>

## LineChart

<LineChart title="图内图例示例" categories="A,B,C,D" cornerLegend="topRight">
  <Series name="系列 A" color="#4488ff" data="1,3,2,4" />
  <Series name="系列 B" color="#ff6644" data="2,2,4,3" />
</LineChart>

**类目 X 轴：**

<LineChart
  title="温度"
  categories="周一,周二,周三,周四,周五"
  yAxisUnit="℃"
  width="400"
  height="240"
>
  <Series name="室外" color="#ff6644" data="18,22,25,20,17" />
  <Series name="室内" color="#44aaff" data="22,23,23,22,22" />
</LineChart>

**数值 X 轴**（不设 `categories`，`<Point>` 给出 `x y`）：

<LineChart
  title="信号衰减"
  xAxisLabel="距离 (m)"
  yAxisLabel="强度 (dB)"
  width="400"
  height="240"
>
  <Series name="空气" color="#44ff88">
    <Point x="0"  y="0"   />
    <Point x="10" y="-2"  />
    <Point x="20" y="-6"  />
    <Point x="40" y="-14" />
    <Point x="80" y="-26" />
  </Series>
  <Series name="混凝土" color="#cc8844">
    <Point x="0"  y="0"   />
    <Point x="10" y="-8"  />
    <Point x="20" y="-20" />
    <Point x="40" y="-40" />
  </Series>
</LineChart>

## PieChart

<PieChart title="资源占比" width="340" height="240">
  <Slice name="铁"   color="#888888" value="40" />
  <Slice name="铜"   color="#CC6633" value="25" />
  <Slice name="金"   color="#FFAA00" value="15" />
  <Slice name="钻石" color="#55DDFF" value="10" />
  <Slice name="其它" color="#AAAAAA" value="10" />
</PieChart>

## ScatterChart

<ScatterChart title="散点图内图例" cornerLegend="bottomRight">
  <Series name="样本 A" color="#4488ff" points="1:1,2:3,3:2,4:4" />
  <Series name="样本 B" color="#ff6644" points="1:2,2:2,3:4,4:3" />
</ScatterChart>

<ScatterChart
  title="身高-体重"
  xAxisLabel="身高 (cm)"
  yAxisLabel="体重 (kg)"
  width="400"
  height="260"
>
  <Series name="样本 A" color="#ff6644">
    <Point x="160" y="55" />
    <Point x="170" y="65" />
    <Point x="175" y="70" />
    <Point x="180" y="75" />
    <Point x="185" y="82" />
  </Series>
  <Series name="样本 B" color="#44aaff">
    <Point x="155" y="50" />
    <Point x="162" y="58" />
    <Point x="168" y="63" />
    <Point x="178" y="72" />
  </Series>
</ScatterChart>

## 组合：ColumnChart + LineSeries + PieInset

`<LineSeries>` 叠加到 ColumnChart 上，`<PieInset>` 在右上角嵌入一个小饼图：

将鼠标移到饼图扇区可查看名称、数值和占比；附加 `tooltip` 和物品图标提示同样有效。饼图优先响应悬停，此时主图的鼠标辅助线隐藏；`BarChart` 中的 `PieInset` 也遵循此规则。

<ColumnChart
  title="月度产量组合"
  categories="1月,2月,3月,4月,5月,6月"
  width="480"
  height="280"
>
  <Series name="铁" color="#888888" data="80,120,200,150,180,220" />
  <Series name="金" color="#FFAA00" data="20,30,50,40,60,80" />
  <LineSeries name="合计" color="#FF4466" data="100,150,250,190,240,300" />
  <PieInset title="合计占比" width="120" height="120">
    <Slice name="铁" color="#888888" value="950" />
    <Slice name="金" color="#FFAA00" value="280" />
  </PieInset>
</ColumnChart>

## 负数与轴单位

此图的灰色参考线固定在 0；鼠标线仍按当前高度显示正、负数和轴单位。

<LineChart width="360" height="200" categories="A,B,C,D" yAxisMin="-100" yAxisMax="100" yAxisStep="50" yAxisUnit="EU/t" showXGrid="true" showYGrid="true" legend="none">
  <Series name="能量变化" data="-80,40,-20,90" color="#6cadc8" />
  <ReferenceLine y="0" />
</LineChart>

## 横条图与混合饼图悬停

横条图：金色竖虚线跟随鼠标左右移动，底部数字高亮；灰色竖虚线固定在 100。虚线上没有文字。

鼠标移到右上角饼图的蓝色或黄色扇区，检查名称、数值、百分比和附加说明。此时金色跟随线隐藏，移回主绘图区后恢复。

<BarChart width="360" height="220" categories="LV,MV,HV,EV" xAxisMin="0" xAxisMax="200" xAxisStep="50" xAxisUnit="件/t" showXGrid="true" showYGrid="true" legend="none">
  <Series name="产量" data="60,90,130,170" color="#6cadc8" />
  <ReferenceLine x="100" />
  <PieInset position="topRight" size="64" title="产物占比">
    <Slice name="主产物" value="70" color="#6cadc8" tooltip="主产物：用于后续加工" />
    <Slice name="副产物" value="30" color="#f2c14e" tooltip="副产物：进入回收生产线" />
  </PieInset>
</BarChart>

## 柱形混合图与外置饼图

此图的饼图独立放在右侧，悬停识别不受主绘图区边界影响。

<ColumnChart width="300" height="220" categories="LV,MV,HV,EV" yAxisMin="0" yAxisMax="200" yAxisStep="50" showXGrid="true" showYGrid="true" legend="none">
  <Series name="产量" data="60,90,130,170" color="#6cadc8" />
  <LineSeries name="目标" data="70,100,140,180" color="#e09070" />
  <ReferenceLine y="100" />
  <PieInset position="rightOutside" size="64" title="产物占比" direction="counterclockwise">
    <Slice name="主产物" value="70" color="#6cadc8" tooltip="主产物：用于后续加工" />
    <Slice name="副产物" value="30" color="#f2c14e" tooltip="副产物：进入回收生产线" />
  </PieInset>
</ColumnChart>
