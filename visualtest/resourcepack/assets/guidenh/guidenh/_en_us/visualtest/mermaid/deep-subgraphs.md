---
navigation:
  title: Mermaid Deep Subgraphs
  position: 8113
---

TEST GOAL / 测试目标：4-5 层嵌套 subgraph + 跨子图层级连线 + 各层不同 direction

INVARIANTS / 不变式：嵌套框包含关系正确、跨层边不穿框不裁切、方向在各层生效、无编译错误

## Five-Level Nesting with Cross-Layer Edges

Expected: Five nested subgraph levels with alternating internal directions; edges jump multiple subgraph boundaries without being clipped.

```mermaid
flowchart TB
  subgraph L1["Level 1"]
    direction TB
    A[Root]
    subgraph L2["Level 2"]
      direction LR
      B[Node B]
      subgraph L3["Level 3"]
        direction TB
        C[Node C]
        subgraph L4["Level 4"]
          direction LR
          D[Node D]
          subgraph L5["Level 5"]
            E[Node E]
            F[Node F]
          end
        end
        G[Node G]
      end
    end
    subgraph L2B["Level 2 Branch"]
      direction TB
      H[Node H]
      I[Node I]
    end
  end

  A --> B
  A --> H
  B --> C
  C --> D
  C --> G
  D --> E
  D --> F
  E --> G
  F --> I
  G --> H
  B --> I
```

## Four-Level Nesting with Subgraph Edges

Expected: Four nested subgraphs; edges declared between nodes of sibling subgraphs route around boxes.

```mermaid
flowchart LR
  subgraph S1["Subgraph One"]
    direction LR
    A[Alpha]
    B[Beta]
    subgraph S2["Sub One Deep"]
      C[Gamma]
      D[Delta]
      subgraph S3["Sub One Deeper"]
        E[Epsilon]
        F[Zeta]
      end
    end
  end
  subgraph S4["Subgraph Two"]
    direction LR
    G[Eta]
    H[Theta]
  end

  A --> C
  B --> D
  C --> E
  D --> F
  E --> G
  F --> H
  A --> H
  B --> G
```

## Nested Subgraph with linkStyle

Expected: linkStyle indices target edges in declaration order even when edges are declared inside subgraphs.

```mermaid
flowchart TB
  subgraph Core["Core"]
    direction TB
    P[Parse]
    Q[Validate]
    R[Render]
  end
  subgraph Side["Side"]
    S[Log]
    T[Notify]
  end

  P --> Q
  Q --> R
  Q --> S
  S --> T
  T --> R

  linkStyle 0 stroke:#9ece6a,stroke-width:3px
  linkStyle 3 stroke:#e0af68,stroke-width:2px
```
