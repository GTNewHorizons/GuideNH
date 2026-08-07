---
navigation:
  title: Mermaid Mixed Multi Segments
  position: 8118
---

TEST GOAL / 测试目标：单页多段混合——mindmap 与 flowchart 交替出现、多段连环、fenced 与 `<Mermaid>` 标签混用

INVARIANTS / 不变式：每段独立编译渲染、类型互不干扰、全部非 stub、无编译错误

## Segment 1: Mindmap (Overview)

Expected: First mindmap renders on its own.

```mermaid
mindmap
  root((Part 1))
    Topics
      Mermaid
      Markdown
    Tools
      Renderer
      Layout
```

## Segment 2: Flowchart (Pipeline)

Expected: First flowchart renders on its own.

```mermaid
flowchart LR
  A[Input] --> B[Parse] --> C[Render]
  B --> D[Error]
  D --> E[Retry]
  E --> B
```

## Segment 3: Mindmap (Detail)

Expected: Second mindmap renders after the flowchart without interference.

```mermaid
mindmap
  root((Part 3))
    Detail
      Deep
        Deeper
          Deepest
    Wide
      W1
      W2
      W3
```

## Segment 4: Flowchart (Loop Chain)

Expected: A longer chain mixed with a branch; consecutive mermaid blocks do not bleed into each other.

```mermaid
flowchart TB
  Start([Start])
  Load[Load Config]
  Check{Ready?}
  Build[Build Index]
  Serve[Serve Page]
  Done(((Done)))
  Fail[Fail]

  Start --> Load
  Load --> Check
  Check -->|Yes| Build
  Check -->|No| Fail
  Build --> Serve
  Serve --> Done
```

## Segment 5: Mermaid Tag (Mindmap)

Expected: An `<Mermaid>` tag mindmap renders as its own block in the same page.

<Mermaid width="400" height="300">
mindmap
  root["Tag Segment"]
    Tag Node A
    Tag Node B
      Tag Leaf B1
</Mermaid>

## Segment 6: Mermaid Tag (Flowchart)

Expected: An `<Mermaid>` tag flowchart renders as its own block in the same page.

<Mermaid width="500" height="320">
flowchart LR
  subgraph TagSub["Tag Subgraph"]
    A[Start]
    B[End]
  end
  A --> B
</Mermaid>

## Segment 7: Final Combined

Expected: A final alternating pair closes the page.

```mermaid
mindmap
  root((Final))
    Mindmap Node
    Another
```

```mermaid
flowchart LR
  X[Begin] --> Y[Middle] --> Z[End]
```
