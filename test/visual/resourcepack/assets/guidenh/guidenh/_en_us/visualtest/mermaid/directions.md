---
navigation:
  title: Mermaid Directions
  position: 8110
---

TEST GOAL: the four flowchart directions (LR / TB / RL / BT) + the `graph` synonym (including the TD abbreviation)

INVARIANTS: each diagram's layout direction is correct, arrow directions follow the diagram direction, no compile errors

## LR (Left to Right)

Expected: Nodes laid out left-to-right; arrows point right.

```mermaid
flowchart LR
  A[Start] --> B[Process] --> C[End]
  B --> D[Branch]
  D --> C
```

## TB (Top to Bottom)

Expected: Nodes laid out top-to-bottom; arrows point down.

```mermaid
flowchart TB
  Top[Top] --> Mid[Middle] --> Bot[Bottom]
  Mid --> Side[Side]
  Side --> Bot
```

## RL (Right to Left)

Expected: Nodes laid out right-to-left; arrows point left.

```mermaid
flowchart RL
  A[Right] --> B[Center] --> C[Left]
  B --> D[Extra]
  D --> C
```

## BT (Bottom to Top)

Expected: Nodes laid out bottom-to-top; arrows point up.

```mermaid
flowchart BT
  A[Bottom] --> B[Middle] --> C[Top]
  B --> D[Branch]
  D --> C
```

## graph Synonym (TD abbreviation)

Expected: `graph TD` behaves identically to `flowchart TB`; `graph` is accepted as a flowchart declaration keyword.

```mermaid
graph TD
  A[Graph Start] --> B[Graph End]
  A --> C[Graph Branch]
  C --> B
```

## graph LR

Expected: `graph LR` renders the same layout as `flowchart LR`.

```mermaid
graph LR
  P[Phase 1] --> Q[Phase 2]
  P --> R[Phase 3]
  Q --> S[Phase 4]
  R --> S
```
