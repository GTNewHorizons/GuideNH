---
navigation:
  title: Tall Elements
  position: 7700
---

TEST GOAL / 测试目标：高度超过一屏的 GameScene、超长表格、超大 mermaid

INVARIANTS / 不变式：不崩溃不死循环；整体渲染完整（长截图机制下全高输出）

## Tall GameScene — 20-Layer Block Pillar

Expected: A 20-block-tall stone pillar renders inside a single `<GameScene>`; the scene content exceeds typical viewport height; no crash or deadlock during scene construction and rendering.

<GameScene width="256" height="320" zoom={3} perspective="isometric-north-east">
  <Block id="minecraft:stone" y="0" />
  <Block id="minecraft:stone" y="1" />
  <Block id="minecraft:stone" y="2" />
  <Block id="minecraft:stone" y="3" />
  <Block id="minecraft:stone" y="4" />
  <Block id="minecraft:stone" y="5" />
  <Block id="minecraft:stone" y="6" />
  <Block id="minecraft:stone" y="7" />
  <Block id="minecraft:stone" y="8" />
  <Block id="minecraft:stone" y="9" />
  <Block id="minecraft:stone" y="10" />
  <Block id="minecraft:stone" y="11" />
  <Block id="minecraft:stone" y="12" />
  <Block id="minecraft:stone" y="13" />
  <Block id="minecraft:stone" y="14" />
  <Block id="minecraft:stone" y="15" />
  <Block id="minecraft:stone" y="16" />
  <Block id="minecraft:stone" y="17" />
  <Block id="minecraft:stone" y="18" />
  <Block id="minecraft:stone" y="19" />
</GameScene>

## Very Long Table (30+ Rows)

Expected: A 30-row table renders in full across multiple screen heights; all rows visible in the long-format screenshot; no crash during table layout with many rows.

| # | Material | Category | Stack Size | Common Uses |
| --- | --- | --- | --- | --- |
| 1 | Stone | Building | 64 | Construction, Furnace |
| 2 | Cobblestone | Building | 64 | Tools, Furnace fuel |
| 3 | Diorite | Building | 64 | Decoration |
| 4 | Granite | Building | 64 | Decoration |
| 5 | Andesite | Building | 64 | Decoration |
| 6 | Oak Log | Wood | 64 | Planks, Charcoal |
| 7 | Spruce Log | Wood | 64 | Planks |
| 8 | Birch Log | Wood | 64 | Planks |
| 9 | Iron Ingot | Metal | 64 | Tools, Armor |
| 10 | Gold Ingot | Metal | 64 | Tools, Mechanism |
| 11 | Diamond | Gem | 64 | Tools, Armor |
| 12 | Redstone Dust | Dust | 64 | Wiring, Mechanisms |
| 13 | Lapis Lazuli | Gem | 64 | Enchanting, Dye |
| 14 | Emerald | Gem | 64 | Trading with Villagers |
| 15 | Coal | Fuel | 64 | Smelting, Torches |
| 16 | Charcoal | Fuel | 64 | Smelting |
| 17 | Glass | Building | 64 | Windows, Beacons |
| 18 | Sand | Building | 64 | Glass, TNT |
| 19 | Gravel | Building | 64 | Concrete, Flint |
| 20 | Clay | Building | 64 | Bricks, Flower Pots |
| 21 | Brick | Building | 64 | Brick Blocks, Decoration |
| 22 | Netherrack | Nether | 64 | Nether Base |
| 23 | Glowstone | Nether | 64 | Lighting, Potions |
| 24 | Obsidian | Building | 64 | Nether Portals |
| 25 | End Stone | End | 64 | End Dimension |
| 26 | Purpur Block | End | 64 | End Cities |
| 27 | Prismarine | Ocean | 64 | Ocean Monuments |
| 28 | Sea Lantern | Ocean | 64 | Underwater Lighting |
| 29 | Sponge | Ocean | 64 | Water Absorption |
| 30 | Coral Block | Ocean | 64 | Reef Decoration |

## Large Mermaid Flowchart (22 Nodes)

Expected: All 22 nodes and their edges rendered in a single flowchart diagram; known issue: mermaid renders placeholder box only (confirmed real engine problem — game client and offline rendering both show a placeholder label box; suspect ELK layout / async render chain lost during migration).

```mermaid
flowchart TB
  classDef process fill:#7aa2f7,stroke:#2f3b54,color:#fff
  classDef data fill:#9ece6a,stroke:#2f3b54,color:#fff
  classDef decision fill:#e0af68,stroke:#2f3b54,color:#fff
  classDef term fill:#f7768e,stroke:#2f3b54,color:#fff

  Start([Start]):::term
  Load[Load Config]:::process
  Validate{Valid?}:::decision
  Parse[Parse Input]:::process
  Check{Has Data?}:::decision
  Fetch[Fetch Records]:::process
  Process[Process Each]:::process
  Filter[Apply Filter]:::process
  Sort[Sort Results]:::process
  Format[Format Output]:::process
  Render[Render View]:::process
  Error[Error Handler]:::term
  Retry[Retry]:::process
  Log[Log Error]:::process
  Notify[Notify Admin]:::process
  Cache[Cache Result]:::data
  Store[(Database)]:::data
  Backup[Backup]:::process
  Audit[Audit Trail]:::process
  Done(((Done))):::term
  Report[Generate Report]:::process
  Export[Export Data]:::process

  Start --> Load
  Load --> Validate
  Validate -->|Yes| Parse
  Validate -->|No| Error
  Parse --> Check
  Check -->|Yes| Fetch
  Check -->|No| Error
  Fetch --> Process
  Process --> Filter
  Filter --> Sort
  Sort --> Format
  Format --> Render
  Render --> Done
  Error --> Retry
  Error --> Log
  Error --> Notify
  Retry --> Load
  Log --> Report
  Notify --> Report
  Done --> Cache
  Done --> Backup
  Done --> Audit
  Cache --> Store
  Backup --> Export
  Audit --> Report
