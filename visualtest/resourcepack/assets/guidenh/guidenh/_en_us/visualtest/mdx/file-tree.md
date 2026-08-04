---
navigation:
  title: FileTree MDX Tag
  position: 9050
---

TEST GOAL / 测试目标：MDX 块标签 `<FileTree>`（indent / gap 属性 + 图标后缀）渲染为目录树

INVARIANTS / 不变式：FileTree 渲染为树形行结构而非纯文本；缩进与行距属性生效；无红色错误文本

Syntax reference: `FileTreeTagCompiler` — `<FileTree indent="<px>" gap="<px>">` recovers its raw children source and parses the tree. Supported forms follow the wiki `markdown.md` example.

## FileTree With Indent And Gap

Here it should: render a directory tree with 16px indent per depth and 2px extra row gap — real tree connector lines, not literal text.

<FileTree indent="16" gap="2">
docs
├── intro
│   ├── getting-started.md
│   └── advanced.md
└── guides
    ├── recipes.md
    └── reference.md
</FileTree>

## FileTree With Text Icons

Here it should: render a tree whose rows carry `{:icon=...}` text-icon suffixes displayed beside the row text.

<FileTree>
world
|-- {:icon=Oak} oak forest
|   \-- {:icon=Hill} rolling hills
|-- {:icon=Des} desert oasis
\-- {:icon=Cave} deep cave
</FileTree>

## FileTree With Item Icons

Here it should: render a tree whose rows carry `{:iconItem=minecraft:...}` item icons beside the row text.

<FileTree indent="18">
inventory
|-- {:iconItem=minecraft:grass} grass block
|-- {:iconItem=minecraft:wool:14} red wool
\-- {:iconItem=minecraft:diamond} diamond
</FileTree>

## FileTree With PNG Icon

Here it should: render a tree whose row carries `{:iconPng=...}` pointing at the fixture asset `../assets/red-64.png` — the icon loads and renders beside the row text.

<FileTree>
assets
\-- {:iconPng=../assets/red-64.png} red-64.png
</FileTree>
