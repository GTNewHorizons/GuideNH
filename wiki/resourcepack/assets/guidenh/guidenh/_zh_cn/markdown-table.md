---
navigation:
  title: Markdown 表格扩展
  parent: markdown.md
  position: 189
  recommend: 0
categories:
  - markdown
---

# Markdown 表格扩展

本页是一份可以直接复制的 GuideNH 表格参考。游戏内指南、Web Editor 和 Site Export 使用同一套源码解析规则。普通数据先使用标准 GFM Markdown；需要合并单元格、样式或固定布局时，再使用这里的扩展语法。

## 基础表格

```markdown
| 材料 | 数量 | 备注 |
| --- | ---: | :--- |
| 铁锭 | 42 | 常见 |
| 金锭 | 17 | 稀有 |
```

第一行是表头。对齐标记是可选的：`:---` 左对齐，`:---:` 居中，`---:` 右对齐。

## 宽度提示

将 `widths` IAL 直接写在表格**前一行**。数值是从左到右应用的像素宽度提示；省略某个值时，该列由渲染器自动计算。

```markdown
{: widths="120,80,160" }
| 材料 | 数量 | 备注 |
| --- | ---: | :--- |
| 铁锭 | 42 | 常见 |
| 金锭 | 17 | 稀有 |
```

为了可读性也可以写 `px`，`80` 和 `80px` 都能识别。宽度是提示而不是强制溢出规则，因此窄屏仍可能自动换行或出现滚动区域。

## 列对齐与单元格属性

单元格两侧可以用冒号控制对齐。单元格末尾的 IAL 会应用到该单元格。支持 `color`、`background`/`bgcolor`、`style`、`align`、`valign`、`id` 和 `class`。安全样式包括颜色、粗体、下划线和文本对齐。

```markdown
{:warning: style="background: #4b2418; color: #ffd8c2; font-weight: bold"}
| 阶段 | 产物 | 备注 |
| :--- | :---: | ---: |
| 处理 | 2 ATP | {: warning } |
| 输出 | 1 ADP | {: style="text-align: right; color: #9ece6a" } |
```

可复用的 IAL 必须单独写在表格前。定义行不会作为普通段落显示。

```markdown
{:muted: style="color: #a8b0c0; font-style: italic"}
{:total: style="background: #26364d; color: #ffffff; font-weight: bold"}
| 资源 | 数量 | 说明 |
| --- | ---: | --- |
| 铁锭 | 42 | {: muted } |
| 合计 {: total } |||
```

## 跨列

在单元格后增加一个 `|` 表示多占一列。因此 `||` 表示 `colspan="2"`，`|||` 表示 `colspan="3"`。

```markdown
| 组件 | 等级 | 备注 |
| --- | --- | --- |
| 控制器 || 所有等级共用 |
| 外壳 | 高压 ||
| 合计 {: style="text-align: right; font-weight: bold" } |||
```

合并范围附加在单元格后面的分隔符上，不要再为被合并的列填充占位文本。

## 跨行

单元格以 `^^` 开头时，会继续合并上方同列单元格。内容会以换行追加到原单元格，并增加原单元格的 `rowspan`。

```markdown
| 分组 | 物品 | 数量 |
| --- | --- | ---: |
| 存储 | 铁锭 | 42 |
| ^^ | 金锭 | 17 |
| 处理 | 红石 | 64 |
```

被继续的行仍然要为其他列提供内容。`^^` 是控制标记，不会显示在最终单元格中。

## 跨行与跨列组合

只要合并区域不互相重叠，就可以组合使用跨行和跨列。渲染器会把后续单元格放到下一处空闲列。

```markdown
| 区域 | 输入 | 输出 | 备注 |
| --- | --- | --- | --- |
| 熔炼 || 锭 | 一个输入、一个输出 |
| ^^ | 粉末 | 锭 |
| 冷却 | 水 || 共用水箱 |
```

如果表格几何结构很复杂，建议改用 HTML 表格，让每个跨度都显式写出，便于维护和检查。

## 多行单元格

在物理行末尾写一个反斜杠，可以把下一行追加到相同单元格。反斜杠会被移除，每一段内容显示在新的一行。

```markdown
| : 配方 : | 输入 | 输出 |
| :--- | :--- | :--- |
| 合金 | 铁锭 + 镍锭 \
| 合金 | 1 个殷钢锭 |
```

只有一个未转义的行末反斜杠才是续行标记。转义反斜杠（`\\`）会保留并显示。

## 无表头表格

将分隔行（`|--|--|`）作为第一行。后续行全部作为数据，不会自动加粗。

```markdown
|--|--|--|--|
|♜| |♝|♛|
| |♟|♟|♟|
```

这适合物品栏、棋盘、图标网格等不适合添加表头的布局。

## 单元格中的 Markdown 与 MDX

单元格内容保留完整的 GuideNH 行内解析能力。链接、强调、代码、LaTeX、物品图片和方块图片可以混排在同一个单元格中。

```markdown
| 部件 | 说明 | 公式 |
| --- | --- | --- |
| <ItemImage id="minecraft:iron_ingot" /> | 用于[熔炉](./index.md)的**铁锭** | <Latex formula="m=42" /> |
| <BlockImage id="minecraft:furnace" /> | `fuel` 槽位 | <ItemLink id="minecraft:coal" /> |
```

单元格内容过高时，游戏和网页渲染器会根据可用宽度换行，并保证行高足以容纳其中最高的行内元素。

## 完整 HTML 表格

当需要标题、列组、明确的行组、无障碍语义或复杂跨度布局时，可以使用 HTML。`caption` 显示在表格上方；`colgroup` 与 `col` 接受像素或百分比宽度；`span` 会将同一宽度重复应用到多个列。`thead`、`tbody` 和 `tfoot` 会保留语义行组。`th` 与 `td` 支持 `rowspan`、`colspan`、`align`、`valign`；`th` 还支持 `scope`、`abbr` 和 `headers`。

```html
<table widths="150,70,70,150" border="1" rules="groups" cellpadding="4" cellspacing="0">
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

站点导出会在安全属性过滤后保留 `width`、`border`、`cellpadding`、`cellspacing`、`frame`、`rules`、`summary`、`bgcolor` 和 `bordercolor` 等表格布局属性。外部 URL 和危险 CSS 会被忽略。

## 如何选择表格形式

- 普通数据、对齐、宽度、跨度和单元格样式使用 Markdown 表格。
- 棋盘、图标列表等网格使用无表头 Markdown 表格。
- 需要行组、标题、无障碍 scope 或不规则几何结构时使用 HTML。
- 需要加载运行时 CSV 数据时使用 `csv` 围栏或 `<CsvTable>`。
- 宽度提示应保持适中；游戏 UI、Web Editor 和手机端 Site Export 可能会缩小它们以适应视口。

完整语法目录参见 [Markdown 基础](./markdown.md)，数据可视化参见 [图表](./charts.md)。
