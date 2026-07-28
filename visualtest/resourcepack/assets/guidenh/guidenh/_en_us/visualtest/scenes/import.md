---
navigation:
  title: Import Structure
  position: 7850
---

TEST GOAL / 测试目标：`<ImportStructure>` + `<ImportStructureLib>` + 子标签 + `PlaceBlock`/`ReplaceBlock`/`RemoveBlocks` 在导入场景中的应用

INVARIANTS / 不变式：SNBT 结构正确加载；palette/pos/meta/nbt 各字段按 schema 解析；非可用环境用例记偏差

## ImportStructure From SNBT

Expected: A 3x1x4 structure appears — cobblestone floor, glowstone center, stone_stairs (meta=2) at corner, stone_slab (meta=0) on the right, torch (meta=5) at far right, chest with diamond and iron NBT at the back. All blocks placed at their palette-indexed positions.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <ImportStructure src="../assets/test-structure.snbt" />
</GameScene>

## ImportStructure With ReplaceBlock

Expected: The imported structure has its glowstone block replaced with glass via `<ReplaceBlock>` — only the center block changes appearance.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <ImportStructure src="../assets/test-structure.snbt" />
  <ReplaceBlock from="minecraft:glowstone" to="minecraft:glass" />
</GameScene>

## ImportStructure With RemoveBlocks

Expected: The imported structure has its torch removed via `<RemoveBlocks id="minecraft:torch">` — torch vanishes, other blocks remain.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <ImportStructure src="../assets/test-structure.snbt" />
  <RemoveBlocks id="minecraft:torch" />
</GameScene>

## ImportStructureLib With Child Tags

Expected: (environment-limited) StructureLib requires a registered multiblock controller from a mod (GregTech, BotanicHorizons, etc.). In environments without such mods, the scene renders the fallback placeholder. The child tag syntax is valid: `<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <ImportStructureLib controller="guidenh:dummy_controller">
    <Tier value="1" />
    <Channel name="voltage" value="4" />
    <Facing value="north" />
    <Rotation value="clockwise_180" />
    <Flip value="none" />
    <Orientation value="north_up" />
  </ImportStructureLib>
</GameScene>

## ImportStructureLib GT Child Tags

Expected: (environment-limited) GregTech tags (`<GregTechActiveController>`, `<GregTechPlaceHatches>`) require GregTech mod. Scene renders placeholder without error.

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <ImportStructureLib controller="guidenh:dummy_controller">
    <Tier value="4" />
    <Channel name="voltage" value="4" />
    <Facing value="north" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>

## ImportStructure With Offset

Expected: Structure loaded at offset x=1 and z=1 (shifted right and back relative to origin); blocks visible at (1,0,1)-(3,0,4) instead of default (0,0,0)-(2,0,3).

<GameScene width="320" height="160" zoom={4} interactive={true}>
  <ImportStructure src="../assets/test-structure.snbt" x="1" z="1" />
</GameScene>
