---
navigation:
  title: Basic Tables
  position: 8800
---

TEST GOAL / 测试目标：2 列窄表、3 列对齐表（左/中/右）

INVARIANTS / 不变式：总宽 ≤ 页宽；折行不溢出列界；行高一致；列分隔线对齐

## Two-Column Narrow Table

Expected: Two columns rendered side by side with minimum width; no overflow; header row in bold.

| Material | Count |
| --- | --- |
| Iron Ingot | 128 |
| Gold Ingot | 64 |
| Diamond | 9 |

## Three-Column Alignment Table (Left / Center / Right)

Expected: First column left-aligned, second center-aligned, third right-aligned; separator lines aligned.

| Name | Amount | Price |
| :--- | :----: | ----: |
| Iron | 42 | 128 |
| Gold | 17 | 64 |
| Diamond | 5 | 512 |

## Single-Row Table Edge Case

Expected: A single-data-row table renders correctly with bold header and equal column widths.

| Key | Value |
| --- | ----- |
| version | 1.0 |
