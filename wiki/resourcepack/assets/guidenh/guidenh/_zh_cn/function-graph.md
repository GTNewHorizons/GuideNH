---
navigation:
  title: 函数图
  parent: index.md
  position: 180
categories:
  - markdown
  - charts
---

# 函数图

交互函数图支持拖拽平移、滚轮缩放和沿曲线查看数值。`funcgraph` 围栏、`<FunctionGraph>` 容器与 `<Function>` 简写使用相同的参数规则。

## 面板参数

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `title` | — | 面板标题 |
| `xLabel`, `yLabel` | — | 坐标轴标题 |
| `width`, `height` | 320, 220 | 面板首选像素尺寸 |
| `xRange`, `yRange` | 自动 | 使用 `a..b` 指定轴范围，优先于单独边界 |
| `xMin`, `xMax`, `yMin`, `yMax` | 自动 | 单独设置轴边界，接受数值或 `pi` 等常量 |
| `domain` | — | 容器上的兼容 X 范围写法，仅在未设置其他 X 范围或边界时生效 |
| `xStep`, `yStep` | 自动 | 刻度与网格间距 |
| `quadrants` | 自动 | `1,2,3,4` 或 `all`；默认行为见下文 |
| `background`, `border` | 主题颜色 | 面板填充与边框颜色 |
| `axisColor`, `gridColor` | 主题颜色 | 坐标轴与网格颜色 |
| `showGrid`, `showAxes` | true | 网格与坐标轴开关 |
| `cornerLegend` | none | `none`、`topLeft`、`topRight`、`bottomLeft`、`bottomRight`；在绘图区内列出有名称的曲线 |
| `cornerLegendWidth`, `cornerLegendHeight` | 120, 64 | 图内图例的最大像素尺寸 |
| `cornerLegendBackground` | #AA111922 | 图内图例背景颜色 |

省略范围与 `quadrants` 时，默认显示第一象限；若采样发现负 Y 值且未显式设置 Y 边界，会扩展到第三、第四象限。有 `label` 的曲线会显示在面板下方图例中；`cornerLegend` 可控制绘图区内图例。

## 曲线参数

用于容器内的 `<Plot>` / `<Function>`，以及围栏中的表达式行。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `expr` | 必需 | 函数表达式 |
| `inverse` | false | 绘制 `x = f(y)` |
| `domain` | — | 曲线定义域，例如 `-pi..pi` 或 `x>=0,x<5` |
| `color` | 调色板 | 曲线颜色 |
| `label` | — | 图例与提示框中的名称 |
| `tooltip` | — | 提示框内的附加纯文本 |
| `showFunction`, `showValues` | true | 在提示框中显示函数式和当前坐标 |
| `pointEveryX`, `pointEveryY` | 关闭 | 沿 X 或 Y 方向的正数采样间距，为本曲线生成标记点 |
| `autoPointLabel` | none | `none`、`x`、`y` 或 `xy` |
| `autoPointColor` | 曲线颜色 | 自动点颜色，省略时继承曲线颜色 |

提示框依次显示名称、函数式、实时坐标、`tooltip` 文本、子节点富文本；缺省或关闭的项目会跳过。带 `expr` 的曲线支持以子 Markdown 与 GuideNH 标签编写富文本提示，站点导出会保留这些内容。

## 标记点参数

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `x`, `y` | — | 显式点坐标，必须成对提供 |
| `plot` | — | 锚定曲线的下标，从 0 开始 |
| `atX`, `atY` | — | 与 `plot` 配合，指定一个坐标并沿曲线计算另一个 |
| `color` | 自动 | 显式点默认白色，锚定点默认继承曲线颜色 |
| `label` | — | 点标签 |

## 围栏代码块

首行为面板属性；后续非空行为表达式、`:x,y` 显式点或 `@plot=N atX=v` / `@plot=N atY=v` 锚定点。表达式后的 `|` 分隔曲线属性，`#` 开始注释。

```funcgraph
width=360 height=220 xRange=-pi..pi yRange=-2..2 quadrants=all cornerLegend=topRight
sin(x) | color=#ff5566 label="sin" pointEveryX=1 autoPointLabel=x
cos(x) | color=#3399ff label="cos" pointEveryY=1
:0,0
@plot=0 atX=1.5708
```

## `<FunctionGraph>` 容器

```mdx
<FunctionGraph width="360" height="220" xRange="-4..4" yRange="-2..6" quadrants="all" cornerLegend="topRight">
  <Plot expr="x^2" color="#4488ff" label="x²" pointEveryX="1" autoPointLabel="xy">
    **燃料效率示例：** <ItemLink id="minecraft:coal" />
  </Plot>
  <Function expr="x - 1" color="#ff6644" label="x - 1" showFunction={false} />
  <Point x="0" y="0" label="O" />
  <Point plot="0" atX="1" />
</FunctionGraph>
```

<FunctionGraph width="360" height="220" xRange="-4..4" yRange="-2..6" quadrants="all" cornerLegend="topRight">
  <Plot expr="x^2" color="#4488ff" label="x²" pointEveryX="1" autoPointLabel="xy">
    **燃料效率示例：** <ItemLink id="minecraft:coal" />
  </Plot>
  <Function expr="x - 1" color="#ff6644" label="x - 1" showFunction={false} />
  <Point x="0" y="0" label="O" />
  <Point plot="0" atX="1" />
</FunctionGraph>

## `<Function>` 简写

独立的 `<Function>` 自动创建单曲线面板，同时接受面板与曲线参数。

<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff" />

## 表达式语法

- 运算符：`+ - * / %`、右结合的 `^`、一元负号；优先级为 `^`、一元负号、`* / %`、`+ -`。
- 隐式乘法：`2x`、`2pi`、`(x+1)(x-1)`；`|expr|` 表示绝对值。
- 一元函数：`sin cos tan asin acos atan sinh cosh tanh exp ln log log2 log10 sqrt cbrt abs sign floor ceil round deg rad gamma`。三角函数使用弧度，`log` 等同于 `log10`，`round` 在恰好半整数时取最近的偶数。
- 双参数函数：`atan2(y,x)`、`min(a,b)`、`max(a,b)`、`pow(a,b)`、`hypot(a,b)`、`mod(a,b)`；任意底数对数可写成 `ln(x)/ln(b)`。
- 后缀 `!` 表示阶乘，并通过 Gamma 函数扩展到实数；负整数返回 NaN。
- 常量：`pi`、`tau`、`e`、`phi`；`√` / `sqrt` 和 `∛` / `cbrt` 表示平方根与立方根。
- `domain` 支持 `min..max` 和逗号分隔的条件（例如 `x>=0,x<5`）；`inverse={true}` 将表达式按 `x = f(y)` 绘制。
