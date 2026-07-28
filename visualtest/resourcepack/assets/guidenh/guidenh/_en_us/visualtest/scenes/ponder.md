---
navigation:
  title: Ponder Animations
  position: 7840
---

TEST GOAL / 测试目标：`<ImportPonder>` 关键帧时间线（最小用例，外部 JSON 资源不可用时记偏差）

INVARIANTS / 不变式：场景加载无崩溃；时间线 UI（播放/暂停/进度条）在离线渲染中占据正确区域

## Minimal Ponder Scene

Expected: (environment-limited) `<ImportPonder>` requires an external JSON file with keyframe definitions and a corresponding SNBT structure. Without `ponder_demo.json` and `ponder_demo.snbt` resources in the environment, the scene renders placeholder/empty state. This page validates that the compiler accepts the tag syntax without crash.

<GameScene width="320" height="200" zoom={2.5} interactive={true}>
  <ImportStructure src="../assets/test-structure.snbt" />
  <ImportPonder src="../assets/ponder_demo.json" />
</GameScene>

## Ponder With StructureLib Base

Expected: (environment-limited) Same as above — `<ImportPonder>` with an `<ImportStructureLib>` base requires both a controller mod and a JSON timeline. Marking as environment-limited; no crash is the minimum acceptance.

<GameScene width="320" height="200" zoom={2.5} interactive={true}>
  <ImportStructureLib controller="guidenh:dummy_ponder_controller">
    <Tier value="1" />
  </ImportStructureLib>
  <ImportPonder src="../assets/ponder_demo.json" />
</GameScene>
