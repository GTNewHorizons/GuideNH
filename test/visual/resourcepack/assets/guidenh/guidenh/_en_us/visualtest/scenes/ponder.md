---
navigation:
  title: Ponder Animations
  position: 7840
---

TEST GOAL: the `<ImportPonder>` keyframe timeline (minimal case; recorded as a deviation when external JSON resources are unavailable)

INVARIANTS: the scene loads without crashing; the timeline UI (play/pause/progress bar) occupies the correct area in offline rendering

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
