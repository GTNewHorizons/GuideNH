---
navigation:
  title: Mermaid Mindmap Variants
  position: 8115
---

TEST GOAL / 测试目标：mindmap 多分支、深层嵌套（10+ 层）、默认与 tidy-tree 两种模式、形状与 class 变体

INVARIANTS / 不变式：树连线正确、节点不重叠、深层嵌套不串层、两种模式布局区分明显、无编译错误

## Multi-Branch (Default Mode)

Expected: Root with four mixed-depth subtrees; children alternate left/right around the root.

```mermaid
mindmap
  root((Guide))
    Overview
      What
      Why
      How
    Reference
      Mermaid
        Flowchart
        Mindmap
      Markdown
    Guide
      Tables
      Images
    Notes
      Tips
      Warnings
```

## Deep Nesting (12 Levels, Default Mode)

Expected: A single 12-level chain renders without level collision or node overlap.

```mermaid
mindmap
  L0(Level 0)
    L1(Level 1)
      L2(Level 2)
        L3(Level 3)
          L4(Level 4)
            L5(Level 5)
              L6(Level 6)
                L7(Level 7)
                  L8(Level 8)
                    L9(Level 9)
                      L10(Level 10)
                        L11(Level 11)
```

## Deep + Wide Mixed (Default Mode)

Expected: Alternating deep chains and wide fans; no sibling overlap at any depth.

```mermaid
mindmap
  root((Core))
    Alpha
      Alpha1
        Alpha1a
        Alpha1b
        Alpha1c
        Alpha1d
      Alpha2
      Alpha3
    Beta
      Beta1
        Beta1a
          Beta1a1
            Beta1a1a
        Beta1b
    Gamma
      Gamma1
      Gamma2
      Gamma3
      Gamma4
    Delta
      Delta1
        Delta1a
          Delta1a1
            Delta1a1a
              Delta1a1a1
            Delta1a1b
```

## Tidy-Tree Mode

Expected: Tree rendered top-down with children stacked below parents; frontmatter `layout: tidy-tree` activates the mode.

```mermaid
---
config:
  layout: tidy-tree
---
mindmap
  root((Project))
    Backend
      API
        Routes
        Middleware
      Database
        Schema
        Migrations
    Frontend
      React
        Components
        Hooks
      CSS
    DevOps
      CI
      Deploy
```

## Shape + Class Variants (Tidy-Tree)

Expected: Mindmap shapes (circle, hexagon, cloud, bang, subroutine, cylinder, stadium) plus `:::` class suffix render correctly under tidy-tree layout.

```mermaid
---
config:
  layout: tidy-tree
---
mindmap
  root((GuideNH))
    rounded(Blocks):::primary
      circle((Tags)):::primary
        hexagon{{JSON}}:::code
      cloud)Data(
    subroutine[[Recipes]]:::primary
      cylinder[(Storage)]:::data
      stadium([Output]):::code
    bang))Notes((
```
