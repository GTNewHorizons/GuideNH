---
navigation:
  title: Basic Tables
  position: 8800
---

TEST GOAL: a narrow 2-column table, a 3-column alignment table (left/center/right)

INVARIANTS: total width ≤ page width; wrapping does not overflow column bounds; row heights are consistent; column separators are aligned

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
