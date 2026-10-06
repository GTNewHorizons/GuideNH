---
navigation:
  title: Wide Tables
  position: 8790
---

TEST GOAL: a wide 5-column table, cells with very long English words, multi-line cells

INVARIANTS: total width ≤ page width; wrapping does not overflow column bounds; row heights are consistent; column separators are aligned

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
