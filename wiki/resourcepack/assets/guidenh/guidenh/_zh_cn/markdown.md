---
navigation:
  title: Markdown 基础
  parent: index.md
  position: 190
  recommend: 0
categories:
  - markdown
---

# Markdown 测试

这一页是运行时渲染检查页，用来在游戏里确认 GuideNH 当前支持的 Markdown 与相关扩展效果。

另见：[图表](./charts.md) · [函数图](./function-graph.md) · [Markdown 表格扩展](./markdown-table.md)

## LaTeX 公式

使用 `<Latex formula="..." />` 在行内渲染 LaTeX 数学公式；将同一标签单独写成一段（块级上下文）则渲染为居中展示的独立公式块。

### 行内公式

爱因斯坦质能方程：<Latex formula="E=mc^2" />，勾股定理：<Latex formula="a^2+b^2=c^2" />

含分数的公式会自动撑高行高：
本行包含 <Latex formula="\frac{1}{2}" /> 与 <Latex formula="\frac{a+b}{c-d}" />。

自定义颜色（金色）：<Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

放大显示（scale=1.5）：<Latex formula="\pi" scale="1.5" />

悬停显示原式（showTooltip）：<Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}" showTooltip={true} />

自定义普通 tooltip：<Latex formula="E=mc^2" tooltip="能量等于质量乘以光速平方。" />

富文本 tooltip：
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **吉布斯自由能** 结合了焓与熵。

  - <Latex formula="\Delta H" /> 表示焓变。
  - <Latex formula="T\Delta S" /> 表示熵项。
  - 悬停 tooltip 可以使用 **普通 Markdown** 和行内指南标签。
</Latex>

底部对齐（valign=bottom）：<Latex formula="\frac{a}{b}" valign="bottom" /> 与正常文字底部对齐。

顶部对齐（valign=top）：<Latex formula="x^2" valign="top" /> 与行顶对齐。

手动偏移：<Latex formula="E=mc^2" offsetX="2" offsetY="-1" /> 向右偏 2px，向上偏 1px。

### `$$公式$$` 简写语法

在 Markdown 中使用 `$$公式$$`，均使用默认参数。嵌入文本时按行内公式渲染，作为段落唯一内容时渲染为居中的展示公式。

行内简写：$$E=mc^2$$ 和 $$a^2+b^2=c^2$$

行内分数：$$\frac{1}{2}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$

### 块级（展示式）公式

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />

<Latex formula="\oint_C \mathbf{E} \cdot d\mathbf{l} = -\frac{d}{dt}\iint_S \mathbf{B} \cdot d\mathbf{S}" showTooltip={true} />

### 属性速查

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `formula` | 字符串（必填） | — | LaTeX 源码 |
| `color` | `#RRGGBB` 或 `#AARRGGBB` | `#FFFFFF` | 字形颜色 |
| `scale` | float | `1.0` | 显示大小倍率 |
| `sourceScale` | float | `100.0` | jlatexmath 排版基准尺寸（站点导出 PNG 分辨率；游戏内始终匹配屏幕分辨率） |
| `tooltip` | 字符串 | — | 自定义普通 tooltip 文本 |
| `showTooltip` | boolean | `false` | 没有自定义 tooltip 时，悬停显示原式内容 |
| `valign` | `baseline`/`top`/`center`/`bottom` | `baseline` | 行内公式垂直对齐方式 |
| `offsetX` | int | `0` | 对齐后的水平像素偏移 |
| `offsetY` | int | `0` | 对齐后的垂直像素偏移 |

## 行内格式

| Markdown                            | 另一种写法        | 效果                              |
|-------------------------------------|-------------------|-----------------------------------|
| `*Italic*`                          | `_Italic_`        | *Italic*                          |
| `**Bold**`                          | `__Bold__`        | **Bold**                          |
| `~~Strikethrough~~`                 | `~Strikethrough~` | ~~Strikethrough~~                 |
| `++下划线++`                          |                   | ++下划线++                          |
| `^^波浪下划线^^`                      |                   | ^^波浪下划线^^                      |
| `::着重号::`                          |                   | ::着重号::                          |
| `==高亮==`                         | `<mark>高亮</mark>` | ==高亮==                         |
| `[Link](http://a.com)`              |                   | [Link](http://a.com)              |
| `[Relative Link](./index.md)`       |                   | [Relative Link](./index.md)       |
| `[Absolute Link](guidenh:index.md)` |                   | [Absolute Link](guidenh:index.md) |
| `[其他命名空间](gregtech:guide.md)` |                   | [其他命名空间](gregtech:guide.md) |
| `[锚点链接](#Headings)`             |                   | [锚点链接](#标题)                   |
| `[跨页锚点](./index.md#navigation)` |                | [跨页锚点](./index.md#navigation)               |
| `[绝对路径锚点](guidenh:index.md#navigation)` |      | [绝对路径锚点](guidenh:index.md#navigation)     |
| `` `Inline Code` ``                 |                   | `Inline Code`                     |
| `![Image](test1.png)`               |                   | ![Image](test1.png)               |

自动链接：访问 https://example.com/docs、www.example.org 或 guide@example.com

自定义高亮颜色：<mark color="#8A6A00">暗金色高亮</mark>

<Spoiler>把鼠标移上来后显示这段隐藏文字</Spoiler>

<Spoiler>[锚点链接](#标题) 在黑幕显示后，依然会像普通链接一样正常悬停并显示下划线</Spoiler>

<Spoiler>**加粗** *斜体* ==高亮== 和 <Color color="#55ccff">染色</Color> 在黑幕里依然正常生效</Spoiler>

<Spoiler>隐藏文本同样可以使用 **Markdown** 和 <Color color="#ff8844">运行时标签</Color>。</Spoiler>

Press <kbd>Shift</kbd> + <sub>1</sub>

<a href="./index.md" title="Open index">打开首页</a><br clear="all" />

## Amount 数字格式化

`<Amount>` 默认只显示数字。注册的单位类型可以添加自己的显示规则；`fluid` 会遵循 GTNHLib 的 `mB`/`L` 偏好。

| 属性 | 含义 |
| --- | --- |
| `value` | 必填的有限十进制数值 |
| `unit` | 可选值 `none`、`item` 或 `fluid`，默认 `none` |
| `format` | 可选数字格式，默认 `default` |
| `decimals` | 可选的非负小数位上限 |

```md
<Amount value="250" unit="fluid" />/s
<Amount value="1200000" unit="fluid" format="compact" />/s
```

<Amount value="250" unit="fluid" />/s
<Amount value="1200000" unit="fluid" format="compact" />/s

## 标题

标题可以通过前缀 `#` 定义。

# 一级标题

## 二级标题

### 三级标题

#### 四级标题

##### 五级标题

###### 六级标题

## 引用块与提示块

Markdown：

```
> Blockquote
```

效果：

> Blockquote

> [!NOTE]
> GitHub 风格的 NOTE 提示块，检查左侧线、图标与标题颜色。

> [!TIP]
> GitHub 风格的 TIP 提示块，检查左侧线、图标与标题颜色。

> [!IMPORTANT]
> GitHub 风格的 IMPORTANT 提示块，检查左侧线、图标与标题颜色。

> [!WARNING]
> GitHub 风格的 WARNING 提示块，检查左侧线、图标与标题颜色。

> [!CAUTION]
> GitHub 风格的 CAUTION 提示块，检查左侧线、图标与标题颜色。

自定义运行时引用块：

```markdown
> {: title="Custom Quote" color="#638ef1" icon="i" }
> 自定义标题、颜色和文本图标。
```

> {: title="Custom Quote" color="#638ef1" icon="i" }
> 自定义标题、颜色和文本图标。

```markdown
> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> 头部使用物品图标。
```

> {: title="Item Quote" color="#61b75d" iconItem="minecraft:emerald" }
> 头部使用物品图标。

```markdown
> {: title="PNG Quote" color="#c79d3e" iconPng="./diamond.png" }
> 从指南资源加载的 PNG 图标。
```

> {: title="PNG Quote" color="#c79d3e" iconPng="./diamond.png" }
> 从指南资源加载的 PNG 图标。

## 列表

Markdown：

```markdown
* List
* List
* List

1. One
2. Two
3. Three
```

效果：

* List
* List
* List

1. One
2. Two
3. Three

- [x] 已完成任务
- [ ] 待处理任务

<Column width="220">
- 限宽列表行示例
- 这一项会更早换行，方便检查自定义宽度
</Column>

## 表格

跨列、跨行、多行单元格、无表头表格、单元格 IAL 和完整 HTML 表格的可复制示例请参见[Markdown 表格扩展](./markdown-table.md)。游戏内、Web Editor 和 Site Export 都支持同一套语法。


Markdown：

```markdown
| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |
```

效果：

| First Header  | Second Header |
| ------------- | ------------- |
| Content Cell  | Content Cell  |
| Content Cell  | Content Cell  |

对齐检查：

| Left | Center | Right |
| :--- | :----: | ----: |
| iron |   42   |   128 |
| gold |   17   |    64 |

普通 Markdown 表格也可以使用运行时列宽 hint：

{: widths="120,80" }
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |

三列表宽示例：

{: widths="130,70,150" }
| Material | Count | Notes |
| --- | --- | --- |
| Iron | 42 | base line |
| Gold | 17 | compact |
| Diamond | 9 | rare |

HTML 表格可以表示标题、列分组、语义行分组和合并单元格。`<caption>` 会显示在表格上方。`<colgroup>` 与 `<col>` 接受像素或百分比 `width` 值（可选 `px` 后缀）；`span` 会将同一宽度重复应用到多个列。使用 `<thead>`、`<tbody>` 和 `<tfoot>` 对行分组。`th` 和 `td` 支持 `colspan`、`rowspan`、`align`、`valign`；`th` 还支持 `scope`、`abbr` 和 `headers`。站点导出会保留表格的 `width`、`border`、`cellpadding`、`cellspacing`、`frame`、`rules`、`summary`、`bgcolor` 和 `bordercolor` 属性。表格级别的 `widths` 属性也可以提供以逗号分隔的像素宽度 hint：

```html
<table widths="150,70,70,150">
  <caption>燃料消耗速率</caption>
  <colgroup>
    <col width="150" />
    <col width="70" span="2" />
    <col width="150" />
  </colgroup>
  <thead>
    <tr>
      <th scope="col" rowspan="2" align="left">燃料</th>
      <th scope="colgroup" colspan="2" align="center">燃烧速率</th>
      <th scope="col" rowspan="2" align="right">备注</th>
    </tr>
    <tr>
      <th scope="col" align="right">木炭</th>
      <th scope="col" align="right">煤炭</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th scope="row" rowspan="2" align="left">固体燃料</th>
      <td align="right">42</td>
      <td align="right">17</td>
      <td rowspan="2" align="left">每刻</td>
    </tr>
    <tr>
      <td colspan="2" align="right">合计 59</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <th scope="rowgroup" align="left">总计</th>
      <td colspan="3" align="right">59 单位</td>
    </tr>
  </tfoot>
</table>

```

效果：

<table widths="150,70,70,150">
  <caption>燃料消耗速率</caption>
  <colgroup>
    <col width="150" />
    <col width="70" span="2" />
    <col width="150" />
  </colgroup>
  <thead>
    <tr>
      <th scope="col" rowspan="2" align="left">燃料</th>
      <th scope="colgroup" colspan="2" align="center">燃烧速率</th>
      <th scope="col" rowspan="2" align="right">备注</th>
    </tr>
    <tr>
      <th scope="col" align="right">木炭</th>
      <th scope="col" align="right">煤炭</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th scope="row" rowspan="2" align="left">固体燃料</th>
      <td align="right">42</td>
      <td align="right">17</td>
      <td rowspan="2" align="left">每刻</td>
    </tr>
    <tr>
      <td colspan="2" align="right">合计 59</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <th scope="rowgroup" align="left">总计</th>
      <td colspan="3" align="right">59 单位</td>
    </tr>
  </tfoot>
</table>

用 `<wbr>` 指定可选换行点。下面的列宽故意设得很窄：第一行会在每个 `<wbr>` 处换行，第二行没有 `<wbr>`，内容会被裁切。单词放得下时 `<wbr>` 不显示任何内容。

```markdown
{: widths="50,24" }
| Item | Rate |
| --- | --- |
| Cobble<wbr>stone/s | 11,<wbr>520 |
| Cobblestone/s | 11,520 |
```

{: widths="50,24" }
| Item | Rate |
| --- | --- |
| Cobble<wbr>stone/s | 11,<wbr>520 |
| Cobblestone/s | 11,520 |

## 引用式链接与图片

[Guide Ref][doc]

![Machine Diagram][img]

[doc]: ./index.md#top
[img]: test1.png "Machine Diagram"

## 折叠详情

`<details>` 支持 `width`、`height`、`wrap`、`align`。`<summary>` 支持行内标签，正文则可以混排普通文本与任意运行时块。设置高度时，标题和外框保持固定，只有正文区域滚动。

标题、正文和结束标签之间不必保留空行，连续的面板也各自独立。
标题和正文都支持行内 Markdown，以及 `<ItemLink>`、`<ItemImage>` 等行内标签。
请使用复数标签 `<details>`，不支持 `<detail>`。

<details open>
  <summary>紧凑面板 1 <ItemImage id="minecraft:diamond" /></summary>
  **加粗文本**、*斜体文本*，以及 <ItemLink id="minecraft:diamond" /> <ItemImage id="minecraft:diamond" />。
</details>
<details open>
  <summary>紧凑面板 2 <ItemLink id="minecraft:crafting_table" /></summary>
  这是独立的面板，包含 <ItemImage id="minecraft:crafting_table" /> 和**格式化文本**。
</details>

这个段落位于两个面板之外。折叠任意面板后，它都应该保持可见。

<details open width="220" height="150" wrap="square" align="right">
<summary>混合运行时内容 <ItemImage id="minecraft:diamond" /></summary>

这个 details 正文可以同时放[普通页面链接](./index.md)、行内格式，以及各种块级内容，并保持在同一个可滚动面板中。

![Machine Diagram](test1.png)

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />

<GameScene width="120" height="90" zoom={5} interactive={false}>
  <Block id="minecraft:diamond_block" />
  <Block id="minecraft:glass" x="1" />
</GameScene>
</details>

当使用 `wrap="square"` 时，块外文本仍应继续围绕它排版。

## 内容标签页

`<ContentTabs>` 可以把可替代的富内容放进同一组标签页中。每个子节点都必须是直接的
`<Tab title="...">`。默认选中项可以通过 `default` 或 `defaultIndex` 指定，`color`
可以覆盖类似引用块的强调线与高亮颜色，`background` 可以用 `#RRGGBB`、`#AARRGGBB` 或 `transparent` 覆盖标签容器背景，容器本身还支持通过 `title` 配合
`icon`、`iconPng` / `icon_png`、`iconItem` / `icon_item` 渲染一个可选的标记式标题行。

<ContentTabs
  title="构建方案"
  iconItem="minecraft:crafting_table"
  color="#4f8cff"
  background="#20252d"
  default="场景"
>
  <Tab title="代码">
    ```java
    System.out.println("Hello GuideNH");
    ```
  </Tab>
  <Tab title="场景">
    <GameScene width="180" height="120">
      <Block id="minecraft:crafting_table" />
    </GameScene>
  </Tab>
  <Tab title="公式">
    $$a^2 + b^2 = c^2$$
  </Tab>
</ContentTabs>

## 代码块

显式语言：

```lua
local value = 42
print(value)
```

显式 Scala：

```scala
object Demo extends App {
  println("scala language label")
}
```

显式 Markdown：

```markdown
* List
* List
* List

1. One
2. Two
3. Three
```

这个未标注语言的围栏应保持为代码块，并自动识别为 Scala：

```
object Demo extends App {
  println("auto detected scala")
}
```

这个未标注语言的围栏应保持为代码块，并自动识别为 CSV，而不是直接渲染成表格：

```
name,value
iron,42
gold,17
```

缩进代码块：

    print("indented code block")

强制代码块视口高度，并在内部滚动：

```java width=220 height=96
line 01
line 02
line 03
line 04
line 05
line 06
line 07
line 08
line 09
line 10
line 11
line 12
line 13
line 14
line 15
line 16
line 17
line 18
line 19
line 20
line 21
line 22
line 23
line 24
```

## CSV 运行时表格

这个显式 `csv` 围栏会渲染成运行时表格：

```csv
name,value
iron,42
gold,17
```

这个显式 `csv` 围栏还会应用列宽 hint：

```csv widths=120,80
name,value
iron,42
gold,17
```

带引号的 widths 与 `header=false`：

```csv widths="120,80" header=false
iron,42
gold,17
diamond,9
```

导入 CSV：

<CsvTable src="./markdown-table.csv" />

带宽度的导入 CSV：

<CsvTable src="./markdown-table.csv" widths="120,80" />

## 文件树

`tree` / `filetree` 围栏代码块会渲染目录式大纲，并绘制真实的连接线。前缀字符同时支持 Unicode 框线（`│ ├ └ ─`）和 ASCII 形式（`| +-- \-- ` / 4 个空格），可任意混用。每行的文本部分支持常规行内 Markdown（链接、**加粗**、`代码` 等），这些链接在游戏内和站点导出中都应可点击。

```tree
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
```

ASCII 形式，并演示按行图标（`{:icon=…}` 纯文本，`{:iconPng=path.png}` 图片，`{:iconItem=mod:item[:meta][:{snbt}]}` 物品）：

```filetree
world
|-- {:iconItem=minecraft:grass} 草地生物群系
|   |-- {:icon=橡} 橡木森林
|   \-- {:icon=丘} 起伏丘陵
|-- {:iconItem=minecraft:wool:14} 红色羊毛
\-- {:iconPng=test1.png} 示例资源
```

在 MDX 块标签中也可使用 `<FileTree>`，可选属性 `indent`（每层缩进像素，默认 `14`）和 `gap`（行间额外像素，默认 `0`）：

```html
<FileTree indent="16" gap="2">
docs
├── [intro.md](./index.md#标题)
└── advanced
    ├── [tags.md](./index.md#折叠详情)
    └── [recipes.md](./charts.md)
</FileTree>
```

### 长路径与深层目录

下面包含长路径、连续无分隔符文件名、中英文混排、多个同级文件和九层目录。缩窄窗口或调大指南缩放，观察长名称的自动换行：续行应对齐名称开头，图标保持在首行，目录连接线贯穿多行；深层目录会压缩缩进，为文件名保留阅读空间。

```filetree
GuideNH-Large-Project/
├── assets/
│   ├── guidenh/
│   │   ├── guides/
│   │   │   ├── zh_cn/
│   │   │   │   ├── progression/
│   │   │   │   │   ├── electrical_age/
│   │   │   │   │   │   ├── high_voltage/
│   │   │   │   │   │   │   ├── advanced_processing/
│   │   │   │   │   │   │   │   ├── `superconductingwirefactorycontrollerconfiguration.json`
│   │   │   │   │   │   │   │   ├── `大型化工厂连续生产线与副产物回收配置说明.md`
│   │   │   │   │   │   │   │   └── `configuration/processing/machines/very_long_machine_recipe_with_multiple_byproducts.json`
│   │   │   │   │   │   │   ├── `高压时代的能源输入与多方块机器排布建议.md`
│   │   │   │   │   │   │   └── `machine_maintenance_and_energy_distribution_checklist.md`
│   │   │   │   │   │   └── `README-high-voltage-machine-progression-and-material-requirements.md`
│   │   │   │   │   ├── medium_voltage/
│   │   │   │   │   │   ├── `ore_processing_with_secondary_products_and_recycling.json`
│   │   │   │   │   │   └── `电路生产线 从原料准备到成品自动化.md`
│   │   │   │   │   └── low_voltage/
│   │   │   │   │       └── [低压入门与基础机器配置](./index.md)
│   │   │   │   ├── `跨维度物流与远程物品传输网络的完整使用手册.md`
│   │   │   │   └── `installation-and-troubleshooting-for-custom-resource-packs.md`
│   │   │   ├── en_us/
│   │   │   │   ├── `getting_started_with_large_multiblock_factory_layouts.md`
│   │   │   │   └── `configuration_reference_for_automated_processing_networks.md`
│   │   │   └── `shared_navigation_and_localization_fallback_configuration.json`
│   │   ├── textures/
│   │   │   ├── {:iconPng=test1.png} `machine_controller_front_panel_with_active_indicator.png`
│   │   │   ├── `超大型生产线总览图 与机器连接方向说明.png`
│   │   │   └── `high_resolution_resource_pack_preview_for_all_voltage_tiers.png`
│   │   └── lang/
│   │       ├── `zh_CN.lang`
│   │       ├── `en_US.lang`
│   │       └── `additional_localization_entries_for_factory_controllers.lang`
│   └── minecraft/
│       └── `textures/blocks/custom_machine_casing_with_connected_textures.png`
├── config/
│   ├── {:iconItem=minecraft:iron_ingot} `industrial_material_processing_and_energy_balance.json`
│   ├── `client_rendering_options_for_small_screens_and_large_gui_scales.json`
│   ├── `世界生成、矿脉分布与大型矿场自动化配置.toml`
│   └── `server_side_recipe_overrides_and_machine_compatibility_settings.cfg`
├── documentation/
│   ├── **安装与升级说明（包含旧版配置迁移步骤）**
│   ├── [图表交互与数值显示示例](./charts.md)
│   ├── `known-issues-and-workarounds-for-complex-directory-layouts.md`
│   └── `Frequently Asked Questions for Guide Authors and Resource Pack Maintainers.md`
├── `README-with-installation-steps-and-supported-modpack-versions.md`
├── `CHANGELOG-for-machine-recipe-updates-and-localization-improvements.md`
└── `LICENSE`
```

### 较浅目录中的连续长名称

这个示例保留较大的行距，便于检查长文件名换行后的连接线，以及图标与首行的对齐。

<FileTree indent="16" gap="3">
factory-backups/
├── {:iconItem=minecraft:chest} `2026-11-14-complete-factory-backup-including-machine-settings-and-item-routing.zip`
├── {:icon=档} `连续生产线配置备份（包含全部机器参数、物品过滤与流体路由规则）.json`
├── `aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.json`
├── `LV-MV-HV-EV-IV-LuV-ZPM-UV-UHV-UEV-UIV-UMV-UXV-complete-progression-checklist.md`
├── [资源包目录与指南编写说明](./index.md)
└── `最后一个超长文件名用于检查多行内容下方的末端分支是否正确结束.md`
</FileTree>

## 函数图

<Function expr="x^2 - 2x + 1" xRange="-2..4" yRange="-1..5" color="#3399ff" />

完整的容器、围栏、曲线与标记点示例见[函数图](./function-graph.md)。

## Mermaid 思维导图

内联 Mermaid 围栏：

```mermaid
mindmap
  root((GuideNH))
    Runtime
      Markdown
      CSV
    Languages
      Lua
      Scala
    Mindmap::icon(fa fa-sitemap)
      Drag to pan
      Wheel to zoom
```

带显式节点坐标的思维导图：

```mermaid
mindmap
  Root((Pinned Root))
    Branch[Branch]::pos(120,80)
      Child A
      Child B::icon(fa fa-code)
```

导入 Mermaid 文件：

<Mermaid src="./markdown-mindmap.mmd" />

固定尺寸的运行时 Mermaid 视口：

<Mermaid src="./markdown-mindmap.mmd" width="320" height="220" />

带有富文本标签和显式节点内容的思维导图：

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [首页](./index.md)"]
    runtime["运行时块"]
    preview["场景预览"]

<NodeContent id="runtime">
运行时节点里可以混排普通文本与指南块内容。

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="preview">
<BlockImage id="minecraft:diamond_block" scale={2} />
</NodeContent>
</Mermaid>

## Mermaid 流程图

展示所有形状、扩展属性、图标和配方节点的流程图：

<Mermaid src="./markdown-flowchart.mmd" width="600" height="600">
  <NodeContent id="Craft">
    <RecipeFor id="minecraft:crafting_table" />
  </NodeContent>
</Mermaid>

## 脚注

脚注引用[^one]

[^one]: 这里是脚注的提示内容
