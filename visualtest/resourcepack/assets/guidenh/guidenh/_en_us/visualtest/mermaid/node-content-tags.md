---
navigation:
  title: Mermaid MermaidTag Size Variants
  position: 8117
---

TEST GOAL / 测试目标：`<Mermaid width height>` 标签的多个 width/height 变体（200x150、400x300、900x600、1200x800）

INVARIANTS / 不变式：各尺寸标签按声明尺寸渲染、非 stub、无编译错误；大尺寸标签暴露水平/垂直溢出行为

## 200x150

Expected: Canvas rendered at 200x150 with the 3-node mindmap inside.

<Mermaid width="200" height="150">
mindmap
  root["Tiny"]
    A
    B
    C
</Mermaid>

## 400x300

Expected: Canvas rendered at 400x300 with the 5-node mindmap inside.

<Mermaid width="400" height="300">
mindmap
  root["Small"]
    One
    Two
    Three
    Four
</Mermaid>

## 900x600

Expected: Canvas rendered at 900x600 with a wider 9-node mindmap inside.

<Mermaid width="900" height="600">
mindmap
  root["Medium"]
    Alpha
      Alpha1
      Alpha2
    Beta
      Beta1
      Beta2
    Gamma
      Gamma1
    Delta
      Delta1
</Mermaid>

## 1200x800

Expected: Canvas rendered at 1200x800 — wider than the 900px page layout, so horizontal overflow behaviour is exposed.

<Mermaid width="1200" height="800">
mindmap
  root["Wide"]
    Branch A
      A1
      A2
      A3
      A4
      A5
    Branch B
      B1
      B2
      B3
      B4
    Branch C
      C1
      C2
    Branch D
      D1
      D2
      D3
      D4
    Branch E
      E1
      E2
</Mermaid>
