---
navigation:
  title: Mermaid Nested Subgraphs
  parent: visualtest/index.md
  position: 8170
---

<!--
测试目标：嵌套 subgraph（2层、3层）+ 跨 subgraph 边
不变式：嵌套框包含关系正确、配色分层、跨边不穿框
-->

## Two-Level Nesting

Expected: Outer subgraph contains two inner subgraphs; edges connect nodes across subgraph boundaries; subgraph borders use two different color layers (index 0, 1).

```mermaid
flowchart TB
  subgraph Outer["Outer Process"]
    direction TB
    A[Start]
    subgraph Left["Left Branch"]
      B[Step 1]
      C[Step 2]
    end
    subgraph Right["Right Branch"]
      D[Step 3]
      E[Step 4]
    end
    F[Merge]
  end

  A --> B
  A --> D
  B --> C
  C --> F
  D --> E
  E --> F
```

## Three-Level Nesting

Expected: Three layers of subgraph nesting with distinct background colors per depth level (index 0, 1, 2); edges cross subgraph boundaries correctly.

```mermaid
flowchart LR
  subgraph Level1["Level 1"]
    direction LR
    A[Root]
    subgraph Level2["Level 2"]
      B[Middle]
      subgraph Level3["Level 3"]
        C[Leaf 1]
        D[Leaf 2]
      end
    end
  end

  A --> B
  B --> C
  B --> D
  A --> C
```

## Cross-Subgraph Edges

Expected: Edges connecting nodes in different subgraphs pass through subgraph box boundaries without being clipped or misrouted.

```mermaid
flowchart TB
  subgraph SG1["Subgraph A"]
    A[Alpha]
    B[Beta]
  end
  subgraph SG2["Subgraph B"]
    C[Gamma]
    D[Delta]
  end
  subgraph SG3["Subgraph C"]
    E[Epsilon]
  end

  A --> C
  B --> D
  A --> E
  C --> E
```
