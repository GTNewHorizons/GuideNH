---
navigation:
  title: Mermaid Stress Large
  position: 8114
---

TEST GOAL / 测试目标：100+ 节点 flowchart TB（多层多分支）+ 50+ 节点 mindmap，压力/性能暴露

INVARIANTS / 不变式：大图布局完成不崩溃、无编译错误、非 stub

## Large Flowchart (100 Nodes, 10x10 mesh)

Expected: 100 nodes laid out in a 10-layer grid with vertical, horizontal, and diagonal edges; layout completes without crash or unbounded canvas.

```mermaid
flowchart TB
  classDef p fill:#7aa2f7,stroke:#2f3b54,color:#fff
  classDef d fill:#9ece6a,stroke:#2f3b54,color:#fff

  L0N0[L0N0]:::p
  L0N1[L0N1]:::d
  L0N2[L0N2]:::p
  L0N3[L0N3]:::d
  L0N4[L0N4]:::p
  L0N5[L0N5]:::d
  L0N6[L0N6]:::p
  L0N7[L0N7]:::d
  L0N8[L0N8]:::p
  L0N9[L0N9]:::d
  L1N0[L1N0]:::p
  L1N1[L1N1]:::d
  L1N2[L1N2]:::p
  L1N3[L1N3]:::d
  L1N4[L1N4]:::p
  L1N5[L1N5]:::d
  L1N6[L1N6]:::p
  L1N7[L1N7]:::d
  L1N8[L1N8]:::p
  L1N9[L1N9]:::d
  L2N0[L2N0]:::p
  L2N1[L2N1]:::d
  L2N2[L2N2]:::p
  L2N3[L2N3]:::d
  L2N4[L2N4]:::p
  L2N5[L2N5]:::d
  L2N6[L2N6]:::p
  L2N7[L2N7]:::d
  L2N8[L2N8]:::p
  L2N9[L2N9]:::d
  L3N0[L3N0]:::p
  L3N1[L3N1]:::d
  L3N2[L3N2]:::p
  L3N3[L3N3]:::d
  L3N4[L3N4]:::p
  L3N5[L3N5]:::d
  L3N6[L3N6]:::p
  L3N7[L3N7]:::d
  L3N8[L3N8]:::p
  L3N9[L3N9]:::d
  L4N0[L4N0]:::p
  L4N1[L4N1]:::d
  L4N2[L4N2]:::p
  L4N3[L4N3]:::d
  L4N4[L4N4]:::p
  L4N5[L4N5]:::d
  L4N6[L4N6]:::p
  L4N7[L4N7]:::d
  L4N8[L4N8]:::p
  L4N9[L4N9]:::d
  L5N0[L5N0]:::p
  L5N1[L5N1]:::d
  L5N2[L5N2]:::p
  L5N3[L5N3]:::d
  L5N4[L5N4]:::p
  L5N5[L5N5]:::d
  L5N6[L5N6]:::p
  L5N7[L5N7]:::d
  L5N8[L5N8]:::p
  L5N9[L5N9]:::d
  L6N0[L6N0]:::p
  L6N1[L6N1]:::d
  L6N2[L6N2]:::p
  L6N3[L6N3]:::d
  L6N4[L6N4]:::p
  L6N5[L6N5]:::d
  L6N6[L6N6]:::p
  L6N7[L6N7]:::d
  L6N8[L6N8]:::p
  L6N9[L6N9]:::d
  L7N0[L7N0]:::p
  L7N1[L7N1]:::d
  L7N2[L7N2]:::p
  L7N3[L7N3]:::d
  L7N4[L7N4]:::p
  L7N5[L7N5]:::d
  L7N6[L7N6]:::p
  L7N7[L7N7]:::d
  L7N8[L7N8]:::p
  L7N9[L7N9]:::d
  L8N0[L8N0]:::p
  L8N1[L8N1]:::d
  L8N2[L8N2]:::p
  L8N3[L8N3]:::d
  L8N4[L8N4]:::p
  L8N5[L8N5]:::d
  L8N6[L8N6]:::p
  L8N7[L8N7]:::d
  L8N8[L8N8]:::p
  L8N9[L8N9]:::d
  L9N0[L9N0]:::p
  L9N1[L9N1]:::d
  L9N2[L9N2]:::p
  L9N3[L9N3]:::d
  L9N4[L9N4]:::p
  L9N5[L9N5]:::d
  L9N6[L9N6]:::p
  L9N7[L9N7]:::d
  L9N8[L9N8]:::p
  L9N9[L9N9]:::d

  L0N0 --> L1N0
  L0N1 --> L1N1
  L0N2 --> L1N2
  L0N3 --> L1N3
  L0N4 --> L1N4
  L0N5 --> L1N5
  L0N6 --> L1N6
  L0N7 --> L1N7
  L0N8 --> L1N8
  L0N9 --> L1N9
  L1N0 --> L2N0
  L1N1 --> L2N1
  L1N2 --> L2N2
  L1N3 --> L2N3
  L1N4 --> L2N4
  L1N5 --> L2N5
  L1N6 --> L2N6
  L1N7 --> L2N7
  L1N8 --> L2N8
  L1N9 --> L2N9
  L2N0 --> L3N0
  L2N1 --> L3N1
  L2N2 --> L3N2
  L2N3 --> L3N3
  L2N4 --> L3N4
  L2N5 --> L3N5
  L2N6 --> L3N6
  L2N7 --> L3N7
  L2N8 --> L3N8
  L2N9 --> L3N9
  L3N0 --> L4N0
  L3N1 --> L4N1
  L3N2 --> L4N2
  L3N3 --> L4N3
  L3N4 --> L4N4
  L3N5 --> L4N5
  L3N6 --> L4N6
  L3N7 --> L4N7
  L3N8 --> L4N8
  L3N9 --> L4N9
  L4N0 --> L5N0
  L4N1 --> L5N1
  L4N2 --> L5N2
  L4N3 --> L5N3
  L4N4 --> L5N4
  L4N5 --> L5N5
  L4N6 --> L5N6
  L4N7 --> L5N7
  L4N8 --> L5N8
  L4N9 --> L5N9
  L5N0 --> L6N0
  L5N1 --> L6N1
  L5N2 --> L6N2
  L5N3 --> L6N3
  L5N4 --> L6N4
  L5N5 --> L6N5
  L5N6 --> L6N6
  L5N7 --> L6N7
  L5N8 --> L6N8
  L5N9 --> L6N9
  L6N0 --> L7N0
  L6N1 --> L7N1
  L6N2 --> L7N2
  L6N3 --> L7N3
  L6N4 --> L7N4
  L6N5 --> L7N5
  L6N6 --> L7N6
  L6N7 --> L7N7
  L6N8 --> L7N8
  L6N9 --> L7N9
  L7N0 --> L8N0
  L7N1 --> L8N1
  L7N2 --> L8N2
  L7N3 --> L8N3
  L7N4 --> L8N4
  L7N5 --> L8N5
  L7N6 --> L8N6
  L7N7 --> L8N7
  L7N8 --> L8N8
  L7N9 --> L8N9
  L8N0 --> L9N0
  L8N1 --> L9N1
  L8N2 --> L9N2
  L8N3 --> L9N3
  L8N4 --> L9N4
  L8N5 --> L9N5
  L8N6 --> L9N6
  L8N7 --> L9N7
  L8N8 --> L9N8
  L8N9 --> L9N9
  L0N0 --> L0N1
  L0N1 --> L0N2
  L0N2 --> L0N3
  L0N3 --> L0N4
  L0N4 --> L0N5
  L0N5 --> L0N6
  L0N6 --> L0N7
  L0N7 --> L0N8
  L0N8 --> L0N9
  L1N0 --> L1N1
  L1N1 --> L1N2
  L1N2 --> L1N3
  L1N3 --> L1N4
  L1N4 --> L1N5
  L1N5 --> L1N6
  L1N6 --> L1N7
  L1N7 --> L1N8
  L1N8 --> L1N9
  L2N0 --> L2N1
  L2N1 --> L2N2
  L2N2 --> L2N3
  L2N3 --> L2N4
  L2N4 --> L2N5
  L2N5 --> L2N6
  L2N6 --> L2N7
  L2N7 --> L2N8
  L2N8 --> L2N9
  L3N0 --> L3N1
  L3N1 --> L3N2
  L3N2 --> L3N3
  L3N3 --> L3N4
  L3N4 --> L3N5
  L3N5 --> L3N6
  L3N6 --> L3N7
  L3N7 --> L3N8
  L3N8 --> L3N9
  L4N0 --> L4N1
  L4N1 --> L4N2
  L4N2 --> L4N3
  L4N3 --> L4N4
  L4N4 --> L4N5
  L4N5 --> L4N6
  L4N6 --> L4N7
  L4N7 --> L4N8
  L4N8 --> L4N9
  L5N0 --> L5N1
  L5N1 --> L5N2
  L5N2 --> L5N3
  L5N3 --> L5N4
  L5N4 --> L5N5
  L5N5 --> L5N6
  L5N6 --> L5N7
  L5N7 --> L5N8
  L5N8 --> L5N9
  L6N0 --> L6N1
  L6N1 --> L6N2
  L6N2 --> L6N3
  L6N3 --> L6N4
  L6N4 --> L6N5
  L6N5 --> L6N6
  L6N6 --> L6N7
  L6N7 --> L6N8
  L6N8 --> L6N9
  L7N0 --> L7N1
  L7N1 --> L7N2
  L7N2 --> L7N3
  L7N3 --> L7N4
  L7N4 --> L7N5
  L7N5 --> L7N6
  L7N6 --> L7N7
  L7N7 --> L7N8
  L7N8 --> L7N9
  L8N0 --> L8N1
  L8N1 --> L8N2
  L8N2 --> L8N3
  L8N3 --> L8N4
  L8N4 --> L8N5
  L8N5 --> L8N6
  L8N6 --> L8N7
  L8N7 --> L8N8
  L8N8 --> L8N9
  L9N0 --> L9N1
  L9N1 --> L9N2
  L9N2 --> L9N3
  L9N3 --> L9N4
  L9N4 --> L9N5
  L9N5 --> L9N6
  L9N6 --> L9N7
  L9N7 --> L9N8
  L9N8 --> L9N9
  L0N0 --> L1N9
  L1N0 --> L2N9
  L2N0 --> L3N9
  L3N0 --> L4N9
  L4N0 --> L5N9
  L5N0 --> L6N9
  L6N0 --> L7N9
  L7N0 --> L8N9
  L8N0 --> L9N9
```

## Large Mindmap (64 Nodes)

Expected: 64-node mindmap with a wide root fanout and deep single-branch chains; alternating left/right subtrees do not overlap.

```mermaid
mindmap
  root((Root))
    Branch0
      Leaf0_0
      Leaf0_1
      Leaf0_2
      Leaf0_3
      Leaf0_4
      Leaf0_5
      Leaf0_6
      Leaf0_7
    Branch1
      Leaf1_0
      Leaf1_1
      Leaf1_2
      Leaf1_3
      Leaf1_4
      Leaf1_5
      Leaf1_6
      Leaf1_7
    Branch2
      Leaf2_0
      Leaf2_1
      Leaf2_2
      Leaf2_3
      Leaf2_4
      Leaf2_5
      Leaf2_6
      Leaf2_7
    Branch3
      Leaf3_0
      Leaf3_1
      Leaf3_2
      Leaf3_3
      Leaf3_4
      Leaf3_5
      Leaf3_6
      Leaf3_7
    Branch4
      Leaf4_0
      Leaf4_1
      Leaf4_2
      Leaf4_3
      Leaf4_4
      Leaf4_5
      Leaf4_6
      Leaf4_7
    Branch5
      Leaf5_0
      Leaf5_1
      Leaf5_2
      Leaf5_3
      Leaf5_4
      Leaf5_5
      Leaf5_6
      Leaf5_7
    Branch6
      Leaf6_0
      Leaf6_1
      Leaf6_2
      Leaf6_3
      Leaf6_4
      Leaf6_5
      Leaf6_6
      Leaf6_7
```
