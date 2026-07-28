---
navigation:
  title: CSV Tables
  position: 8760
---

TEST GOAL / 测试目标：`<CsvTable src>` 标签 + `csv` 代码块两种形式（header/widths 变体）

INVARIANTS / 不变式：总宽 ≤ 页宽；折行不溢出列界；行高一致；列分隔线对齐

## CsvTable Tag — Default (header=true)

Expected: CSV rendered as a table with bold header row; columns auto-sized.

<CsvTable src="../assets/test-table.csv" />

## CsvTable Tag — With Widths

Expected: Table columns constrained to 120px and 80px widths (header row bold).

<CsvTable src="../assets/test-table.csv" widths="120,80" />

## CsvTable Tag — No Header With Widths

Expected: Table rendered without bold header row; column widths 100, 60, 120 px.

<CsvTable src="../assets/test-table.csv" header={false} widths="100,60,120" />

## CSV Code Block — Default (header=true)

Expected: Inline CSV data rendered as a table with bold header row.

```csv
name,value
iron,42
gold,17
diamond,9
```

## CSV Code Block — With Widths

Expected: Columns constrained to 120px and 80px widths.

```csv widths=120,80
name,value
iron,42
gold,17
diamond,9
```

## CSV Code Block — No Header With Widths

Expected: Table rendered without header row; column widths 100px and 80px.

```csv widths="100,80" header=false
iron,42
gold,17
diamond,9
```
