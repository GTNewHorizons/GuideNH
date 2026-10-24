---
navigation:
  title: Markdown Table Extensions
  parent: markdown.md
  position: 189
  recommend: 0
categories:
  - markdown
---

# Markdown Table Extensions

This page is a copy-ready reference for the GuideNH table extensions. The same source is parsed by the in-game guide, Web Editor, and Site Export. Start with ordinary GFM Markdown; use the compact extensions when a table needs merged cells, styling, or a fixed layout.

## Basic table

```markdown
| Material | Amount | Notes |
| --- | ---: | :--- |
| Iron | 42 | common |
| Gold | 17 | rare |
```

The first row is the header. Alignment markers are optional: `:---` is left, `:---:` is centered, and `---:` is right aligned.

## Width hints

Put a `widths` IAL directly **before** the table. Values are pixel hints and are applied from left to right. A missing value lets the renderer size that column automatically.

```markdown
{: widths="120,80,160" }
| Material | Amount | Notes |
| --- | ---: | :--- |
| Iron | 42 | common |
| Gold | 17 | rare |
```

Use `px` only when it improves readability; both `80` and `80px` are accepted. Widths are hints rather than a hard overflow rule, so narrow screens can still reflow or scroll.

## Column alignment and cell attributes

A cell can use edge colons for alignment. A trailing IAL applies safe attributes to that cell. Supported attributes are `color`, `background`/`bgcolor`, `style`, `align`, `valign`, `id`, and `class`. Safe style declarations include colors, font weight, text decoration, and text alignment.

```markdown
{:warning: style="background: #4b2418; color: #ffd8c2; font-weight: bold"}
| Stage | Products | Notes |
| :--- | :---: | ---: |
| Processing | 2 ATP | {: warning } |
| Output | 1 ADP | {: style="text-align: right; color: #9ece6a" } |
```

Reusable IAL definitions must be declared on their own line before the table. The definition is not rendered as a paragraph.

```markdown
{:muted: style="color: #a8b0c0; font-style: italic"}
{:total: style="background: #26364d; color: #ffffff; font-weight: bold"}
| Resource | Amount | Comment |
| --- | ---: | --- |
| Iron | 42 | {: muted } |
| Total {: total } |||
```

## Column spans

Add one extra `|` after a cell for each additional column it occupies. Therefore `||` means `colspan="2"`, and `|||` means `colspan="3"`.

```markdown
| Component | Tier | Notes |
| --- | --- | --- |
| Controller || Shared by every tier |
| Casing | HV ||
| Total {: style="text-align: right; font-weight: bold" } |||
```

The span is attached to the cell before the extra separators. Do not add placeholder text to the columns covered by that span.

## Row spans

Start a cell with `^^` to continue the cell directly above it. The content is appended to the original cell with a line break and the original cell receives a larger `rowspan`.

```markdown
| Group | Item | Amount |
| --- | --- | ---: |
| Storage | Iron | 42 |
| ^^ | Gold | 17 |
| Processing | Redstone | 64 |
```

The continued row still supplies values for the other columns. `^^` is a control marker and is removed from the displayed cell.

## Combined row and column spans

Spans can be combined as long as the occupied cells do not overlap. The renderer places later cells in the next free column.

```markdown
| Area | Input | Output | Notes |
| --- | --- | --- | --- |
| Smelting || Ingot | one input, one output |
| ^^ | Dust | Ingot |
| Cooling | Water || Shared tank |
```

For complicated geometry, prefer an HTML table where every span is explicit and easier to review.

## Multiline cell content

End a physical row with one trailing backslash to append the next physical row to the same cells. The backslash is removed and each part is rendered on a new line.

```markdown
| : Recipe : | Input | Output |
| :--- | :--- | :--- |
| Alloy | Iron + Nickel \
| Alloy | 1 Invar |
```

Only one unescaped trailing backslash is a continuation marker. An escaped backslash (`\\`) remains visible.

## Headerless tables

Use a delimiter row as the first row (`|--|--|`). Its rows remain data rows and are not bolded.

```markdown
|--|--|--|--|
|♜| |♝|♛|
| |♟|♟|♟|
```

This is useful for inventories, chessboards, icon grids, and other tables where a header would be misleading.

## Markdown and MDX inside cells

Cell content keeps normal GuideNH inline parsing. Links, emphasis, code, LaTeX, item images, and block images can be mixed in one cell.

```markdown
| Part | Description | Formula |
| --- | --- | --- |
| <ItemImage id="minecraft:iron_ingot" /> | **Iron** used by [the smelter](./index.md) | <Latex formula="m=42" /> |
| <BlockImage id="minecraft:furnace" /> | `fuel` slot | <ItemLink id="minecraft:coal" /> |
```

When a cell becomes too tall for the available width, the game and web renderers wrap the cell content while preserving the row height needed by its tallest inline element.

## Full HTML table

Use HTML syntax when you need captions, column groups, explicit row groups, accessibility metadata, or a complex span layout. `caption` is rendered above the table. `colgroup` and `col` accept pixel or percentage widths; `span` repeats one width for multiple columns. `thead`, `tbody`, and `tfoot` preserve semantic row groups. `th` and `td` accept `rowspan`, `colspan`, `align`, and `valign`; `th` also accepts `scope`, `abbr`, and `headers`.

```html
<table widths="150,70,70,150" border="1" rules="groups" cellpadding="4" cellspacing="0">
  <caption>Fuel burn rates</caption>
  <colgroup>
    <col width="150" />
    <col width="70" span="2" />
    <col width="150" />
  </colgroup>
  <thead>
    <tr>
      <th scope="col" rowspan="2" align="left">Fuel</th>
      <th scope="colgroup" colspan="2" align="center">Burn rate</th>
      <th scope="col" rowspan="2" align="right">Notes</th>
    </tr>
    <tr>
      <th scope="col" align="right">Charcoal</th>
      <th scope="col" align="right">Coal</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <th scope="row" rowspan="2" align="left">Solid fuel</th>
      <td align="right">42</td>
      <td align="right">17</td>
      <td rowspan="2" align="left">Per tick</td>
    </tr>
    <tr>
      <td colspan="2" align="right">59 combined</td>
    </tr>
  </tbody>
  <tfoot>
    <tr>
      <th scope="rowgroup" align="left">Total</th>
      <td colspan="3" align="right">59 units</td>
    </tr>
  </tfoot>
</table>
```

The site exporter preserves table layout attributes such as `width`, `border`, `cellpadding`, `cellspacing`, `frame`, `rules`, `summary`, `bgcolor`, and `bordercolor` after applying the normal safe-attribute filter. External URLs and unsafe CSS are ignored.

## Choosing a table form

- Use a Markdown table for ordinary data, alignment, widths, spans, and cell-level styling.
- Use a headerless Markdown table for grids or icon lists.
- Use HTML when row groups, captions, accessibility scopes, or irregular geometry matter.
- Use `<CsvTable>` or a `csv` fence for data that should be loaded as a runtime CSV table.
- Keep width hints modest. The game UI, Web Editor, and mobile Site Export may reduce them to fit the available viewport.

See [Markdown Basics](./markdown.md) for the complete syntax catalogue and [Charts](./charts.md) for data visualizations.
