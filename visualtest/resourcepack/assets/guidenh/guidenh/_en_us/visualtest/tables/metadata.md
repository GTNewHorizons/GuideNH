---
navigation:
  title: Table Width Metadata
  position: 8770
---

TEST GOAL / 测试目标：`{: widths=}` 列宽元数据、宽窄组合

INVARIANTS / 不变式：总宽 ≤ 页宽；折行不溢出列界；行高一致；列分隔线对齐

## Widths Set on Two-Column Table

Expected: First column 120px wide, second column 80px wide; columns respect the specified widths.

| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
| Diamond | 9 |
{: widths="120,80" }

## Widths Set on Three-Column Table

Expected: Three columns with widths 130, 70, 150 px respectively; wider third column accommodates longer text.

| Material | Count | Notes |
| --- | --- | --- |
| Iron | 42 | base line |
| Gold | 17 | compact |
| Diamond | 9 | rare |
{: widths="130,70,150" }

## Wide-Narrow Combination

Expected: First column wide (200px), second narrow (50px); narrow column wraps text aggressively.

| Description | # |
| ----------- | -: |
| Stack of cobblestone blocks | 64 |
| Block of diamond | 9 |
| Tiny pile of redstone dust | 128 |
{: widths="200,50" }
