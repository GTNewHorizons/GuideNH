---
navigation:
  title: Wide Tables
  position: 8790
---

TEST GOAL / 测试目标：5 列宽表、超长英文词单元格、多行单元格

INVARIANTS / 不变式：总宽 ≤ 页宽；折行不溢出列界；行高一致；列分隔线对齐

## Five-Column Wide Table

Expected: All 5 columns visible across full page width; header bold; column separator lines continuous.

| A | B | C | D | E |
| --- | --- | --- | --- | --- |
| 1 | 2 | 3 | 4 | 5 |
| Iron | Gold | Diamond | Redstone | Emerald |
| 64 | 32 | 9 | 128 | 4 |

## Cell With Very Long English Word

Expected: Long unbroken word wraps inside its column without overflowing column boundary.

| Item | Description |
| --- | ----------- |
| Potion | Antidisestablishmentarianism |
| Map | Supercalifragilisticexpialidocious documentation |
| Tool | Pneumonoultramicroscopicsilicovolcanoconiosis |

## Multi-Line Cell Content

Expected: Cells with `<br>` line breaks render as multiple visual lines within the same table row; row height accommodates tallest cell.

| Command | Output |
| ------- | ------ |
| help | Shows available commands<br>Use /guidenhc open |
| list | Page listing<br>Filter by category<br>Sort alphabetically |
| version | Current build<br>Release 1.0.0 |
