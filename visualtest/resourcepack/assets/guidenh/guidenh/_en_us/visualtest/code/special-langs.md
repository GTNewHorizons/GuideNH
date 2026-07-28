---
navigation:
  title: Special Language Renderers
  position: 8690
---

TEST GOAL / 测试目标：tree/filetree（含 {:icon=} 后缀）+ csv（对照 tables/csv.md）special-lang 渲染而非纯文本代码块

INVARIANTS / 不变式：tree/filetree 渲染为目录树而非纯文本；csv 渲染为运行时表格而非纯文本

## Tree (Unicode Box-Drawing)

Expected: Root rendered at top with children indented below; Unicode box-drawing characters (│ ├ └ ─) produce real connector lines; leaf text supports inline markdown; no toolbar/language label shown.

```tree
project
├── src
│   ├── main
│   │   ├── java
│   │   │   └── [App.java](./index.md)
│   │   └── resources
│   │       └── config.xml
│   └── test
│       └── java
│           └── AppTest.java
├── README.md
└── build.gradle
```

## FileTree with {:icon=} Text Icons

Expected: ASCII-style (| +-- \--) connectors render as real tree lines; `{:icon=}` suffix renders as short text label beside the row; icons do not break the alignment of sibling entries.

```filetree
world
|-- {:icon=Oak} oak forest
|   |-- {:icon=Tree} tall oak
|   \-- {:icon=Hill} rolling hills
|-- {:icon=Des} desert oasis
\-- {:icon=Cave} deep cave
```

## FileTree with {:iconItem=} and {:iconPng=}

Expected: `{:iconItem=}` renders as a small item icon; `{:iconPng=}` renders as a small image icon; both icons appear before the row text; fallback text displayed if asset not found.

```filetree
inventory
|-- {:iconItem=minecraft:grass} grass block
|-- {:iconItem=minecraft:wool:14} red wool
|-- {:iconPng=guidenh:textures/gui/logo.png} sample asset
\-- {:iconItem=minecraft:diamond} diamond
```

## CSV Basic Table

Expected: Fenced `csv` block renders as a runtime table with header row highlighted; first row treated as header by default; cells separated by commas; no pure-text code block fallback.

```csv
name,value,count
iron_ingot,42,128
gold_ingot,17,64
diamond,9,32
emerald,3,16
```

## CSV with widths=120,80

Expected: Column widths applied via `widths=` hint as space-separated pixel values; header row present; table layout respects specified column widths.

```csv widths=120,80
item,amount
Iron Ingot,42
Gold Ingot,17
Diamond,9
```

## CSV with widths="120,80" header=false

Expected: `header=false` suppresses header row styling; all rows rendered as data rows; column widths applied; quoted widths value correctly parsed.

```csv widths="120,80" header=false
Iron Ingot,42
Gold Ingot,17
Diamond,9
Emerald,3
```
