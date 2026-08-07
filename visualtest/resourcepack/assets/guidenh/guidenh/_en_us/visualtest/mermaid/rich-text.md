---
navigation:
  title: Mermaid Rich Text Labels
  position: 8116
---

TEST GOAL / 测试目标：节点内富文本标记（**加粗**、_斜体_、`代码`）、行内 `<br>`、特殊字符（中文 / emoji / 数学符号 / 长 URL）

INVARIANTS / 不变式：富文本标记以字面文本渲染不崩溃、`<br>` 正确换行、特殊字符不丢不重叠、无编译错误

## Markup Literals in Labels

Expected: `**bold**` / `_italic_` / `` `code` `` markers render as literal text inside node labels (markdown markup is not processed); no crash, no truncated text.

```mermaid
flowchart TB
  B[**bold text**]
  I[_italic text_]
  C[plain `code` span]
  M[**bold** and _italic_ together]
  S[~~strikethrough~~ marker]

  B --> I --> C
  C --> M
  M --> S
```

## Inline <br> Line Breaks

Expected: `<br>` / `<br/>` inside a label produce a line break; node box height grows accordingly.

```mermaid
flowchart TB
  A[Line one<br>Line two]
  B[Alpha<br/>Beta<br/>Gamma]
  C[Short<br>also works]
  D[Multi<br>line<br>node<br>with<br>five<br>rows]

  A --> B
  B --> C
  C --> D
```

## Special Characters: Chinese / emoji / math

Expected: CJK, emoji, and math symbols render without missing glyphs or overlapping boxes.

```mermaid
flowchart LR
  zh1[合成配方]
  zh2[地狱岩浆引擎升级]
  em1[⚡ 闪电]
  em2[⛏ 镐]
  em3[🔥 火焰]
  em4[🌍 世界]
  math1[E = mc²]
  math2[∑ π √ ∞ ∫]
  mixed[配方 3 × 5 = 15]

  zh1 --> zh2
  em1 --> em2
  em3 --> em4
  math1 --> math2
  zh1 --> mixed
  em4 --> mixed
```

## Long URL in Label

Expected: A long unbroken URL folds via codepoint-level wrapping; node stays inside the diagram bounds.

```mermaid
flowchart TB
  U[https://example.com/very/long/path/with/many/segments/and?a=1&b=2&c=3#fragment]
  V[Short URL https://a.b/c]
  W[中文链接 https://例子.中文/路径/很长/的一部分]

  U --> V
  V --> W
```

## Mindmap Rich Labels

Expected: Mindmap nodes carry the same rich/special text without breaking the tree structure.

```mermaid
mindmap
  root((GuideNH))
    **Bold** Branch
      _Italic_ Leaf
      `Code` Leaf
    Emoji Branch
      ⚡ Fast
      🔥 Hot
    Math Branch
      E = mc²
      π ≈ 3.14159
    Chinese
      合成配方
      矿物词典
    Line Break
      First<br>Second
