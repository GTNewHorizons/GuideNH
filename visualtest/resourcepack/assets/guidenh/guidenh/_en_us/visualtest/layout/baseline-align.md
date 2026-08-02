---
navigation:
  title: Baseline Alignment (degraded bottom-edge)
  position: 8310
---

TEST GOAL / 测试目标：验证 `<Row alignItems="baseline">` 属性解析并路由到 taffy AlignItems::BASELINE，对照 start/center 可辨

INVARIANTS / 不变式：baseline 当前退化为底边对齐（taffy 0.12.1 叶子无基线度量，GuideNH 未提供文本基线数据）——混合高度子项底边齐平，与 start 顶边齐平、center 中线齐平互斥可辨；非真·文本基线对齐，真·基线需 P7 补充基线度量后实现

## Baseline (degraded to bottom-edge)

Expected: A Row with `alignItems="baseline"` and mixed-height children (BlockImage scale 1 vs scale 2). Because taffy reports no leaf baseline metrics, BASELINE degrades to bottom-edge alignment: the children's bottom edges are flush and the taller middle child extends upward. Compare against the start/center controls below.

<Row alignItems="baseline" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

## Control: start (top-edge flush)

Expected: The same mixed-height children with `alignItems="start"`. Children's top edges are flush; the taller middle child extends downward. This is the distinguishing control for baseline's bottom-edge signature.

<Row alignItems="start" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>

## Control: center (middle-line flush)

Expected: The same mixed-height children with `alignItems="center"`. Children share a common vertical middle line; their top and bottom edges differ. This is the distinguishing control for baseline's bottom-edge signature.

<Row alignItems="center" gap="5">
<BlockImage id="minecraft:diamond_block" scale="1" />
<BlockImage id="minecraft:stone" scale="2" />
<BlockImage id="minecraft:gold_block" scale="1" />
</Row>
