---
navigation:
  title: Mermaid Mindmap Modes
  position: 8190
---

TEST GOAL / 测试目标：mindmap 两种布局模式 + 节点形状 + 多层嵌套

INVARIANTS / 不变式：树连线正确、节点不重叠、深层嵌套不串层

## Default Layout (Alternating Left-Right)

Expected: Root rendered center with children alternating left and right; ≥4 levels of nesting; shapes rendered as rounded (ROUNDED), circle (CIRCLE), hexagon (HEXAGON), cloud (CLOUD), bang (BANG).

```mermaid
mindmap
  root((GuideNH))
    rounded(Blocks)
      circle((Tag))
        hexagon{{JSON}}
          cloud)XML(
        bang))NBT((
    subcategories(Recipes)
      Shaped
      Shapeless
    elements(Items)
      Tools
      Armor
      Blocks
```

## TIDY_TREE Layout Mode

Expected: Tree rendered top-down with children stacked vertically below parent; frontmatter activates tidy-tree mode.

```mermaid
---
config:
  layout: tidy-tree
---
mindmap
  root((GuideNH))
    rounded(Blocks)
      circle((Tag))
        hexagon{{JSON}}
        bang))NBT((
    subcategories(Recipes)
      Shaped
        Mining
        Smelting
      Shapeless
        Potions
    elements(Items)
      Tools
      Armor
```

## Deep Nesting (6 Levels)

Expected: In default mindmap mode a deep single-child chain renders as a horizontal spine (each child centered on its parent's row, depth expressed along X) with elbow connection lines and no sibling overlap; vertical top-down hierarchy is the tidy-tree mode (see section above). (Engine enhancement candidate: vertical stagger for deep unary chains to match reference mermaid.)

```mermaid
mindmap
  A(Alpha)
    B(Bravo)
      C(Charlie)
        D(Delta)
          E(Echo)
            F(Foxtrot)
    G(Golf)
      H(Hotel)
        I(India)
```
