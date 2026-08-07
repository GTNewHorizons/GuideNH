---
navigation:
  title: Mermaid Node Shapes
  position: 8111
---

TEST GOAL / 测试目标：全部节点形状语法变体 + 中文标签节点

INVARIANTS / 不变式：每种形状渲染出对应轮廓、中文标签不丢字不重叠、无编译错误

## All Shape Variants

Expected: Each node renders its documented shape distinct from the others.

```mermaid
flowchart TB
  A[Square]
  B(Round)
  C([Stadium])
  D[[Subroutine]]
  E[(Cylinder)]
  F{Diamond}
  G{{Hexagon}}
  H(-Ellipse-)
  I(((Double Circle)))
  J[/Trapezoid\]
  K[\InvTrapezoid/]
  L[/LeanRight/]
  M[\LeanLeft\]
  N>Asymmetric]
  O))Bang((
  P)Cloud(
```

## Chinese Labels (with shape mix)

Expected: Chinese labels render without missing characters; node boxes size to fit CJK text.

```mermaid
flowchart TB
  c1[合成配方]
  c2(合成与分解)
  c3([圆角中文])
  c4[[子流程中文]]
  c5[(数据库中文)]
  c6{判定中文}
  c7{{六角中文}}
  c8[地狱岩浆引擎升级]

  c1 --> c2 --> c3
  c2 --> c4
  c3 --> c5
  c4 --> c6
  c6 --> c7
  c6 --> c8
```

## Shape + classDef combo

Expected: Shape geometries combine with classDef colors; hexagon/diamond/trapezoid keep their outline under fill styling.

```mermaid
flowchart LR
  classDef blue fill:#7aa2f7,stroke:#2f3b54,color:#fff
  classDef green fill:#9ece6a,stroke:#2f3b54,color:#fff

  s1[Square]:::blue
  s2{Diamond}:::green
  s3{{Hexagon}}:::blue
  s4[(Cylinder)]:::green
  s5([Stadium]):::blue

  s1 --> s2 --> s3
  s2 --> s4
  s4 --> s5
```
