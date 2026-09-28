# Ponder Animation Timeline

GuideNH の構文と実行時機能を説明します。コード、タグ、パス、ID、属性値は仕様のためそのまま保持しています。


GuideNH 対応します Ponder アニメーション-style animated timelines 内部 `<GameScene>` ブロック. You supply an
外部 JSON ファイル that defines keyframes, カメラ movements と で-ワールド 注釈, と
GuideNH 描画 an インタラクティブ progress bar とともに play/pause controls below  3D シーン.

## クイックスタート

1. 作成 Ponder アニメーション JSON ファイル と 配置 it で 自身の リソースパック (参照してください [File Placement](#file-placement)).
2. 追加します `<ImportPonder src="..."/>` 内部 a `<GameScene>` ブロック alongside `<ImportStructure>`.

```mdx
<GameScene zoom="4">
  <ImportStructure src="scenes/my_machine.snbt" />
  <ImportPonder src="scenes/my_machine_ponder.json" />
</GameScene>
```

> **注記:** `<ImportPonder>` 必要があります appear 内部 a `<GameScene>` ブロック.  `src` 属性 です
> 必須. Structure データ です 引き続き 指定された by `<ImportStructure>` または `<ImportStructureLib>`.

## ファイル配置

Ponder アニメーション JSON files follow  同じ リソース-pack パス 規則 as SNBT structures:

```
assets/<modid>/guidebooks/
  pages/machines/my_machine.mdx       <- guide page
  pages/machines/my_machine.snbt      <- structure data
  pages/machines/my_machine.json      <- Ponder JSON
```

 `src` 属性 accepts 両方 相対 と 絶対 IDs:

| 例 | 解決された as |
|---------|-------------|
| `src="my_machine.json"` | 相対 へ  現在の ページ's ディレクトリ |
| `src="mymod:guidebooks/pages/machines/my_machine.json"` | 絶対 (`mymod` namespace) |

## JSON 形式

```json
{
  "totalTime": 120,
  "keyframes": [
    {
      "time": 0,
      "label": "Start",
      "camera": {
        "zoom": 2.0,
        "rotX": 25.0,
        "rotY": -30.0,
        "rotZ": 0.0,
        "offX": 0.0,
        "offY": 0.0
      },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Mid-point",
      "camera": {
        "rotY": -90.0
      },
      "annotations": [
        {
          "type": "diamond",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "color": "0xFFFF4444",
          "tooltip": "Input hatch",
          "alwaysOnTop": false
        }
      ]
    },
    {
      "time": 120,
      "camera": {
        "rotY": -150.0
      }
    }
  ]
}
```

### ルートフィールド

| フィールド | 型 | 必須 | 説明 |
|-------|------|----------|-------------|
| `totalTime` | 整数 | はい | Total duration で ticks (20 ticks = 1 second). Minimum clamped へ 1. |
| `keyframes` | array | はい | 一覧 of keyframe objects. 可能性があります なる 空. |

### キーフレームフィールド

| フィールド | 型 | 必須 | 説明 |
|-------|------|----------|-------------|
| `time` | 整数 | はい | Tick at これらは この keyframe occurs (0 <= time <= totalTime). |
| `hidden` | 真偽値 | いいえ | 場合 `true`,  keyframe 引き続き 適用します カメラ/NBT/entity/注釈 状態 at its `time`, but いいえ 表示 node です drawn 用 it on  progress bar と 表示 keyframe ナビゲーション skips it. |
| `label` | 文字列 | いいえ | 任意の フォールバック ラベル 表示される 場合 ホバーすると  keyframe node on  progress bar. |
| `labelKey` | 文字列 | いいえ | 翻訳キー 用  keyframe ラベル. 場合 解決された, it 上書きします `label`. |
| `camera` | object | いいえ | カメラ 状態 at この keyframe. Null フィールド inherit から  前の keyframe. |
| `cameraEaseTicks` | 整数 または null | いいえ | How many ticks  カメラ takes へ ease から  **前の** keyframe へ この 1 つ. `null` (既定値) = ease 上  完全な segment. `0` = instant snap. `N > 0` = ease 上 N ticks, then hold at  対象 位置. |
| `layer` | 整数 または null | いいえ | 表示 layer 上書き. `null` (または omitted) shows すべての layers. 1-based index. |
| `annotations` | array | いいえ | 一覧 of 注釈 objects 表示される 中 この keyframe です active. |
| `sounds` | array | いいえ | 一覧 of sounds played once 場合 この keyframe になります active 中 forward playback. |
| `particles` | array | いいえ | 一覧 of 実行時 particle bursts または presets fired 場合 この keyframe になります active 中 forward playback. |
| `blockChanges` | array | いいえ | 一覧 of ブロック replacements applied 場合 この keyframe 最初の になります active. |
| `mergeTileNBT` | array | いいえ | Merge SNBT compounds into tile entities at ブロック positions. |
| `modifyTileNBT` | array | いいえ | 設定します 1 つ tile-entity NBT パス へ SNBT 値. |
| `removeTileNBT` | array | いいえ | 削除 1 つ tile-entity NBT パス. |
| `createEntities` | array | いいえ | 作成 Ponder アニメーション-owned entities that できます なる referenced by later entity NBT operations. |
| `setEntityNBT` | array | いいえ | 置き換え referenced entity's NBT とともに  supplied SNBT compound. |
| `mergeEntityNBT` | array | いいえ | Merge SNBT compound into referenced entity. |
| `modifyEntityNBT` | array | いいえ | 設定します 1 つ referenced entity NBT パス へ SNBT 値. |
| `removeEntityNBT` | array | いいえ | 削除 1 つ referenced entity NBT パス. |
| `removeEntities` | array | いいえ | 削除 1 つ または さらに Ponder アニメーション-owned entities by `ref` using  stable シーン-entity registry. |

非表示 keyframes です useful 場合 you want additional intermediate 状態 変更 なしで adding new 表示 node
へ  timeline. 用 例, you できます split several `modifyTileNBT` updates across 複数の ticks, mark 
intermediate keyframes as 非表示, と 保持します のみ  major beats 表示 on  progress bar.

### カメラフィールド

すべての カメラフィールド です 任意の. 任意の `null` または omitted フィールド inherits its 値 から  nearest
prior keyframe that defined it; もし いいえ prior keyframe defined  フィールド,  シーン's 既定値
カメラ 値 です 使用されます.

| フィールド | 型 | 説明 |
|-------|------|-------------|
| `zoom` | 浮動小数点数 | カメラ ズーム level (0.1 - 10.0). |
| `rotX` | 浮動小数点数 | X-axis 回転 で degrees. |
| `rotY` | 浮動小数点数 | Y-axis 回転 で degrees. |
| `rotZ` | 浮動小数点数 | Z-axis 回転 で degrees. |
| `offX` | 浮動小数点数 | Horizontal pan offset で 画面 pixels. |
| `offY` | 浮動小数点数 | Vertical pan offset で 画面 pixels. |

 カメラ smoothly interpolates 間 adjacent keyframes using an **ease-で/ease-out** 曲線.
使用 `cameraEaseTicks` on  **destination** keyframe へ control  easing duration:

```json
{ "time": 60, "cameraEaseTicks": 0,  "camera": { "rotY": 90 } }   <- instant snap
{ "time": 120, "cameraEaseTicks": 20, "camera": { "rotY": 180 } }  <- ease over 20 ticks then hold
{ "time": 180, "camera": { "rotY": 270 } }                         <- ease over full segment (default)
```

## ブロック変更

 `blockChanges` array で keyframe replaces ブロック で  live structure 場合 that keyframe
になります active. この 許可します  animation へ 表示 前-と-後 states, 配置 または 削除
ブロック, または animate machine powering on.

```json
{
  "time": 60,
  "blockChanges": [
    { "x": 1, "y": 1, "z": 1, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
    { "x": 1, "y": 2, "z": 1, "block": "minecraft:air", "particles": false },
    {
      "x": 2, "y": 1, "z": 2, "block": "minecraft:chest", "meta": 2,
      "nbt": "{Items:[{Slot:0b,id:\"minecraft:iron_ingot\",Count:8b,Damage:0s}]}"
    }
  ]
}
```

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `x`, `y`, `z` | 整数 | - | **必須.** 位置 of  ブロック へ 変更 (structure 座標). |
| `block` | 文字列 | - | **必須.** Registry 名前, e.g. `"minecraft:furnace"`. 使用 `"minecraft:air"` へ 削除. |
| `meta` | 整数 | `0` | ブロック metadata / damage 値. |
| `particles` | 真偽値 | `true` | Whether へ spawn ブロック-texture particle effects 場合 この ブロック 変更 fires 中 forward playback. Particles です taken から  ブロック's 独自の icon texture. 設定します へ `false` へ suppress (e.g., 用 silent removal). |
| `nbt` | 文字列 | `null` | SNBT 文字列 用 tile entity タグ, e.g. 用 chests, furnaces, etc. 解析済み とともに `JsonToNBT`. キー 必要があります なる **unquoted** (標準 SNBT format). Ignored もし  ブロック has いいえ tile entity. |

**参照k-safe:** 場合 seeking backwards  実行時 restores すべての changed positions へ their
original structure 状態, then re-適用します 変更 から keyframes 0 通じて  現在の 1 つ.
 displayed structure です 常に correct に関係なく seek direction.

> **注記 on particles:** ブロック-texture particles fire once, のみ 中 forward playback 場合 
> keyframe 最初の になります active. これらは です cleared on seek, restart, または 初期 読み込み.

## キーフレームのサウンド

追加します a `sounds` array へ keyframe へ play 1 つ または さらに ガイド sounds 場合  keyframe になります active
中 forward playback. 参照king と 初期 読み込み do ない play キーフレームのサウンド; restart clears 
play history so  sounds できます fire again.

```json
{
  "time": 130,
  "label": "Furnace lights up",
  "sounds": [
    { "sound": "guidenh:machine.start", "volume": 0.8 },
    { "src": "guidenh:sounds/machine/hum.ogg", "volume": 0.4, "x": 1.5, "y": 1.5, "z": 1.5 }
  ]
}
```

Sound フィールド:

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `sound` | 文字列 | - | Sound event id, e.g. `guidenh:machine.start`. |
| `src` | 文字列 | - | Sound ファイル id または パス; `guidenh:sounds/machine/start.ogg` になります `guidenh:machine.start`. |
| `volume` | 浮動小数点数 | `1.0` | Playback volume 前 attenuation. |
| `pitch` | 浮動小数点数 | `1.0` | Playback pitch. |
| `cooldown` | 整数 | `250` | Minimum milliseconds 前  同じ sound できます play again. |
| `x`, `y`, `z` | 浮動小数点数 | none | 任意の シーン-空間 ソース 位置 用 画面空間 attenuation. |
| `radius` | 浮動小数点数 | シーン short 側 * 0.75 | Attenuation radius で 画面 pixels. |
| `minVolume` | 浮動小数点数 | `0.15` | Minimum attenuation factor. |

---

## キーフレームのパーティクル

追加します a `particles` array へ keyframe 場合 you want 1 つ-shot particle bursts または timeline-ローカル
天候 overlays 中 forward playback. これらの particles です ない re-fired 中 reverse
scrubbing, と seek/restart clears them 前 replaying  active 状態.

Generic particles:

```json
{
  "time": 110,
  "particles": [
    {
      "name": "smoke",
      "x": 1.5,
      "y": 1.85,
      "z": 1.5,
      "vx": 0.0,
      "vy": 0.01,
      "vz": 0.0,
      "size": 0.18,
      "time": 16,
      "amount": 3
    }
  ]
}
```

Explosion preset:

```json
{
  "time": 160,
  "particles": [
    {
      "preset": "explosion",
      "x": 1.5,
      "y": 1.45,
      "z": 1.5,
      "time": 8,
      "power": 2.4
    }
  ]
}
```

天候 preset:

```json
{
  "time": 220,
  "particles": [
    {
      "preset": "rain",
      "weather": "snow",
      "x": [0, 2],
      "z": [0, 2],
      "time": 100,
      "amount": 8
    }
  ]
}
```

Particle フィールド:

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `preset` | 文字列 | none | Special preset. `explosion` spawns vanilla-style flash/smoke burst. `rain` enables  天候 preset. |
| `weather` | 文字列 | `rain` | 天候 型 使用されます by `preset: "rain"`. 対応 値: `rain`, `snow`. |
| `name` | 文字列 | none | Generic particle appearance. 対応 値: `billboard`, `smoke`, `largesmoke`, `explode`, `flash`, `largeexplode`, `hugeexplosion`. |
| `particle` / `kind` | 文字列 | none | Compatibility aliases 用 `name`. |
| `x`, `z` | 浮動小数点数 または array | シーン bounds | Particle origin または 天候 coverage. Generic particles 使用 scalar 座標. 用 `preset: "rain"`, scalar 値 対象 1 つ precipitation 列 と arrays define rectangular coverage by endpoint pairs. |
| `vx`, `vy`, `vz` | 浮動小数点数 | `0.0` | 初期 motion ベクトル. `motionX/Y/Z` です accepted aliases. |
| `time` / `lifetime` | 整数 | preset-特定の | Particle lifetime で ticks. 用 `preset: "rain"` この です  total 天候 duration, including fade-で と fade-out. |
| `size` | 浮動小数点数 | preset-特定の | Generic particle half-サイズ で ブロック units. |
| `amount` | 整数 | preset-特定の | Generic particle count. 用 `explosion`, omitted amount scales から `power`. 用 `preset: "rain"`, この です  average per-tick 天候 density. |
| `power` | 浮動小数点数 | `2.0` | Explosion strength 用  `explosion` preset. |

天候 preset 注意:

- `preset: "rain"` です  shared 天候 preset エントリ 点. 使用 `weather: "rain"` 用 rainfall または
  `weather: "snow"` 用 snowfall.
- この preset です timeline-owned 天候. It 対応します replay, pause, seek, と fast-forward together
  とともに  rest of  Ponder アニメーション timeline.
- 用 常に-on シーン 天候 外部  Ponder アニメーション timeline, 使用  `<Weather>` タグ 内部
  `<GameScene>` 代わりに.
- 天候 presets ignore `y`;  vertical spawn range です derived から  現在の シーン bounds.
- `x: 5, z: 8` targets 1 つ precipitation 列. Arrays 使用 endpoint pairs:
  `x: [1, 5, 10, 12], z: [2, 6, 20, 24]` 作成します 2 つ covered rectangles.
- もし 1 つ axis has extra unmatched array 値,  unmatched tail です ignored.
-  実行時 自動的に shapes  effect とともに short 開始 transition, steady middle
  section, と an 終端 transition.
- Rain spawns fast falling drops plus occasional ground splashes. Snow spawns slower drifting
  flakes なしで splash particles.
-  天候 area です derived から  現在の GameScene bounds so  effect scales とともに 
  imported structure 代わりに of hard-coded box.
-  同じ `x/z` 列 決して stacks 複数の 天候 types at  同じ time. Earlier overlapping
  天候 declarations 保持します  shared columns; later ones のみ 描画 on  remaining area.

---

## Tile Entity NBT Operations

使用 `mergeTileNBT`, `modifyTileNBT`, と `removeTileNBT` 場合  ブロック stays で 配置 but its
tile entity データ 変更. Operations です seek-safe: GuideNH restores  original tile NBT と
then replays すべての operations から keyframe 0 通じて  active keyframe.

```json
{
  "time": 80,
  "mergeTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "nbt": "{InputTanks:[{Level:{Speed:0.25,Target:0.25,Value:0.0},TankContent:{Amount:250,FluidName:\"minecraft:lava\"}}]}"
    }
  ],
  "modifyTileNBT": [
    {
      "x": 2, "y": 1, "z": 2,
      "path": "InputTanks[0].TankContent.Amount",
      "value": "500"
    }
  ],
  "removeTileNBT": [
    { "x": 2, "y": 1, "z": 2, "path": "InputTanks[0].Level.Target" }
  ]
}
```

| フィールド | 使用されます by | 説明 |
|-------|---------|-------------|
| `x`, `y`, `z` | すべての | Tile-entity ブロック 位置 で structure 座標. |
| `nbt` | `mergeTileNBT` | SNBT compound merged into  tile entity. Existing compound キー です merged recursively; 他の 値 置き換え  old 値. |
| `path` | `modifyTileNBT`, `removeTileNBT` | Dotted NBT パス とともに 一覧 indexes, e.g. `Items[0].Count` または `InputTanks[0].TankContent.Amount`. |
| `value` | `modifyTileNBT` | SNBT 値 written at `path`, e.g. `3b`, `500`, `"\"text\""`, `{Count:1b,id:"minecraft:stone"}`. |

Paths follow  同じ idea as Minecraft's `/data` paths: 使用 dots 用 compound キー と
`[index]` 用 一覧 エントリ. 一覧 traversal 現在 expects  traversed 一覧 エントリ へ なる
compounds, これらは 一致します 一般的な tile NBT 例として inventories, tanks, と recipe slots.

---

## エンティティ操作

Regular `<Entity>` タグ です すでに 対応 で `GameScene`. Ponder アニメーション timelines できます も 作成
their 独自の entities とともに `createEntities`, then 対象 those entities by `ref` で later keyframes.

```json
{
  "time": 0,
  "createEntities": [
    {
      "ref": "marker",
      "sceneEntityId": "marker",
      "id": "minecraft:pig",
      "x": 1.5, "y": 1.0, "z": 2.5,
      "yaw": 180,
      "nbt": "{CustomName:\"Before\",CustomNameVisible:1b}"
    }
  ]
}
```

| フィールド | 説明 |
|-------|-------------|
| `ref` | 必須 ローカル reference 名前 用 later operations. |
| `sceneEntityId` | 任意の stable シーン-ローカル id 使用されます 用 mount relations, replay-safe replacement, import/export restore, と 任意の later removal of この logical entity. Defaults へ 内部 id derived から `ref`. |
| `id` | Entity ID, e.g. `minecraft:pig`, `Pig`, または mod entity ID 対応 by  シーン entity loader. |
| `x`, `y`, `z` | 任意の spawn 位置. Defaults へ `0, 0, 0` 場合を除き `nbt` supplies `Pos`. |
| `yaw`, `pitch` | 任意の spawn 回転. Defaults へ `0, 0` 場合を除き `nbt` supplies `Rotation`. |
| `bodyYaw`, `headYaw` | 任意の living-entity 本文/head yaw 上書きします. もし omitted 中 `yaw` です present, これらは follow `yaw`. |
| `nbt` | 任意の SNBT compound applied 場合  entity です 作成された. |
| `name`, `uuid` | 任意の プレビュー-player profile フィールド 場合 creating a プレビュー player entity. |
| `mount` | 任意の stable `sceneEntityId` of  vehicle that この entity する必要があります ride 後 creation または later replay. |
| `unmount` | 任意の 真偽値 that clears  entity's 現在の stable mount relation 前 任意の later `mount` です applied. |

後 creation, 使用  entity NBT operations:

```json
{
  "time": 60,
  "mergeEntityNBT": [
    { "ref": "marker", "nbt": "{Saddle:1b}" }
  ],
  "modifyEntityNBT": [
    { "ref": "marker", "path": "CustomName", "value": "\"After\"" }
  ],
  "removeEntityNBT": [
    { "ref": "marker", "path": "CustomNameVisible" }
  ]
}
```

`setEntityNBT` です も 利用可能 場合 you want へ 置き換え  entity's NBT 代わりに of merging:

```json
{
  "time": 100,
  "setEntityNBT": [
    { "ref": "marker", "nbt": "{Pos:[0.0d,0.0d,0.0d],Rotation:[0.0f,0.0f],CustomName:\"Reset\"}" }
  ]
}
```

エンティティ操作 できます も 更新 transform, プレビュー-player pose, と stable mount 状態 なしで
changing NBT:

```json
{
  "time": 80,
  "createEntities": [
    { "ref": "cart", "sceneEntityId": "cart", "id": "minecraft:minecart", "x": 1.5, "y": 1.0, "z": 1.5 },
    { "ref": "rider", "sceneEntityId": "rider", "id": "player", "name": "GuideNH", "x": 1.5, "y": 1.0, "z": 1.5, "mount": "cart" }
  ],
  "modifyEntityNBT": [
    { "ref": "rider", "headYaw": 270.0, "leftArmRotation": "-40 0 0" }
  ]
}
```

へ detach または 削除 timeline entity, 使用 `unmount` または `removeEntities`:

```json
{
  "time": 120,
  "modifyEntityNBT": [
    { "ref": "rider", "unmount": true, "x": 2.5, "y": 1.0, "z": 1.5 }
  ],
  "removeEntities": [
    { "ref": "cart" }
  ]
}
```

Like tile operations, entity operations です replayed から  beginning whenever  active
keyframe 変更, so seeking backwards removes Ponder アニメーション-作成された entities と recreates  correct
状態 用  対象 tick.

注記:

- `ref` identifies これらは Ponder アニメーション-owned entity  現在の action する必要があります edit または 削除.
- `sceneEntityId` と `mount` identify stable cross-entity relations. `mount` 常に 点 へ a
  stable シーン id, ない へ another `ref`.
- Relying on 未加工 passenger NBT alone です ない recommended 用 cross-entity シーン relationships. 
  stable registry です what keeps mount と removal behavior deterministic across replay, rebuild,
  import/export, と エディター プレビュー refresh.

---

## 注釈 Fade

注釈 smoothly fade で 上 **5 game ticks** (250 ms) whenever  active keyframe
変更 中 playback. 参照king または pausing 常に shows 注釈 at 完全な opacity.

---

## 注釈の種類

各 エントリ で  `annotations` array requires a `type` フィールド. Seven types です 利用可能.

---

### `diamond`

描画 3D diamond marker at a ワールド 位置.

```json
{
  "type": "diamond",
  "x": 1.5,
  "y": 2.0,
  "z": 1.5,
  "color": "0xFFFF8800",
  "tooltip": "Click me",
  "alwaysOnTop": false
}
```

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `x`, `y`, `z` | 浮動小数点数 | `0.0` | ワールド空間 位置 of  diamond tip. |
| `color` | 文字列 | `"0xFF00E000"` | ARGB 色 as `"0xAARRGGBB"`. |
| `tooltip` | 文字列 | `""` | フォールバック テキスト 表示される on ホバー. |
| `tooltipKey` | 文字列 | `""` | 翻訳キー 用  ホバー テキスト. 場合 解決された, it 上書きします `tooltip`. |
| `alwaysOnTop` | 真偽値 | `false` | もし true, 描画される 通じて solid ブロック. |

---

### `box`

描画 wireframe 軸に揃った box から `min` へ `max`.

```json
{
  "type": "box",
  "minX": 0.0, "minY": 0.0, "minZ": 0.0,
  "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
  "color": "0x8800FFFF",
  "lineWidth": 1.5,
  "alwaysOnTop": false
}
```

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `minX/Y/Z` | 浮動小数点数 | `0.0` | Minimum corner. |
| `maxX/Y/Z` | 浮動小数点数 | `1.0` | Maximum corner. |
| `color` | 文字列 | `"0xFFFFFFFF"` | ARGB 行 色. |
| `lineWidth` | 浮動小数点数 | 既定値 | GL 行 幅. |
| `alwaysOnTop` | 真偽値 | `false` | 描画 通じて ブロック. |

---

### `block`

描画 wireframe around 1 つ whole ブロック. この です  Ponder アニメーション JSON equivalent of
`<BlockAnnotation pos="x y z">` で regular `GameScene`.

```json
{
  "type": "block",
  "pos": [1, 0, 1],
  "color": "0xFFFF8800",
  "lineWidth": 1.5,
  "alwaysOnTop": true
}
```

`"type": "blockBox"` と `"type": "block_box"` です accepted aliases.

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `pos` | 数値[3] または 文字列 | `[0, 0, 0]` | ブロック 座標 as `[x, y, z]` または `"x y z"`. |
| `x`, `y`, `z` | 数値 | `0` | Alternative ブロック 座標 フィールド. 値 です floored. |
| `blockX/Y/Z` | 整数 | `0` | Legacy-style ブロック 座標 フィールド. |
| `color` | 文字列 | `"0xFFFFFFFF"` | ARGB 行 色. |
| `lineWidth` | 浮動小数点数 | 既定値 | GL 行 幅. |
| `alwaysOnTop` | 真偽値 | `false` | 描画 通じて ブロック. |

---

### `line`

描画 a 線分 または 折れ線 間 ワールド positions. `points` takes priority 上
`fromX/Y/Z` と `toX/Y/Z` 場合 it 含みます at least 2 つ 有効 点.

```json
{
  "type": "line",
  "fromX": 0.5, "fromY": 0.5, "fromZ": 0.5,
  "toX": 2.5,   "toY": 0.5,   "toZ": 0.5,
  "color": "0xFFFFFF00",
  "arrow": "end",
  "lineWidth": 2.0,
  "alwaysOnTop": true
}
```

折れ線 点 できます なる written いずれか as a 文字列 または as array:

```json
{
  "type": "line",
  "points": [[0.5, 1.2, 0.5], [1.5, 1.8, 1.5], [2.5, 1.2, 2.5]],
  "color": "0xFFFFCC44",
  "arrow": "end"
}
```

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `fromX/Y/Z` | 浮動小数点数 | `0.0` | 開始 点. |
| `toX/Y/Z` | 浮動小数点数 | `1.0` | 終端 点. |
| `points` | 文字列 または array | `null` | 折れ線 点. 使用 `"x y z; x y z; ..."` または `[[x,y,z], ...]`. |
| `color` | 文字列 | `"0xFFFFFFFF"` | ARGB 行 色. |
| `arrow` | 文字列 | `null` | `start` または `end`; omitted または 無効 値 描画 いいえ 矢印. |
| `lineWidth` | 浮動小数点数 | 既定値 | GL 行 幅. |
| `alwaysOnTop` | 真偽値 | `false` | 描画 通じて ブロック. |

---

### `blockface`

単一ブロックのすべての面を半透明の塗りつぶしで強調表示します。

```json
{
  "type": "blockface",
  "pos": [1, 0, 1],
  "color": "0x8833FF33",
  "alwaysOnTop": false
}
```

`"type": "blockFace"` と `"type": "block_face"` です accepted aliases.

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `pos` | 数値[3] または 文字列 | `[0, 0, 0]` | ブロック 座標 as `[x, y, z]` または `"x y z"`. |
| `x`, `y`, `z` | 数値 | `0` | Alternative ブロック 座標 フィールド. 値 です floored. |
| `blockX/Y/Z` | 整数 | `0` | Legacy-style ブロック 座標 フィールド. |
| `color` | 文字列 | `"0x80FFFFFF"` | ARGB overlay 色. |
| `alwaysOnTop` | 真偽値 | `false` | 描画 通じて ブロック. |

---

### `text`

描画 speech-bubble ラベル anchored へ a ワールド 位置.  box appears 上に  anchor by
既定値 と です connected へ it とともに short vertical 行. テキスト コンテンツ です keyframe-driven: 使用
異なる `text` 注釈 エントリ on 異なる keyframes へ 変更  displayed テキスト 上 time.

```json
{
  "type": "text",
  "x": 1.5,
  "y": 2.5,
  "z": 1.5,
  "text": "Place items here",
  "color": "0xFF44AAFF",
  "connectorSide": "right",
  "connectorOffset": 8,
  "connectorLength": 12
}
```

用 a 固定 画面空間 位置 that しません project から ワールド 座標, 使用
**independent モード**.  bubble です centered horizontally で  シーン と placed at
`yOffset` pixels below  シーン's vertical centre.

```json
{
  "type": "text",
  "text": "Independent label",
  "color": "0xFFFFCC00",
  "backgroundAlpha": 160,
  "independent": true,
  "yOffset": 40
}
```

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `x`, `y`, `z` | 浮動小数点数 | `0.0` | ワールド空間 anchor 位置 (ignored で independent モード). |
| `text` | 文字列 | - | **必須.** テキスト へ 表示 内部  bubble. |
| `color` | 文字列 | `"0xFFAAAAAA"` | ARGB 色 of  bubble 境界線. |
| `backgroundAlpha` | 整数 | `204` | 背景 opacity から `0` (transparent) へ `255` (opaque).  RGB 色 remains  既定値 dark navy. |
| `maxWidth` | 整数 | `0` | もし &gt; 0, wraps テキスト at この 幅 で pixels. Omit または 設定します へ `0` 用 a 単一の-行 ラベル. |
| `independent` | 真偽値 | `false` | もし `true`, 位置 です 相対 へ  シーン centre rather than a ワールド 点. |
| `yOffset` | 整数 | `0` | Pixel offset から  シーン's vertical centre (positive = downward). 使用されます とともに `independent: true`. |
| `connectorSide` | 文字列 | `"bottom"` | `bottom`, `top`, `left`, `right`, または `none`. Ignored で independent モード. |
| `connectorOffset` | 整数 | `0` | Pixel offset along  選択済み bubble edge; positive moves 右 用 上/下 と down 用 左/右. |
| `connectorLength` | 整数 | `6` | Pixel length of  connector 行. `0` hides  行 中 keeping 側-based placement. |
| `hlMinX/Y/Z` | 浮動小数点数 | `0.0` | Minimum corner of an 任意の 強調表示 box drawn alongside  テキスト bubble. |
| `hlMaxX/Y/Z` | 浮動小数点数 | `1.0` | Maximum corner of  任意の 強調表示 box. |
| `highlightColor` | 文字列 | `"0x8000FFAA"` | ARGB 色 of  強調表示 box. |

場合 `hlMinX` (または 任意の `hlMin/Max` 座標) です present, an `InWorldBoxAnnotation` です も
作成された at  specified bounds とともに `highlightColor`. この です useful 用 pointing at 特定の
ブロック regions 中 explaining them.

 背景 は dark navy bubble by 既定値 (`#CC0E0E20`), と `backgroundAlpha` controls its
opacity. で ワールド-anchored モード connector 行 リンク  box へ  anchor. テキスト 対応します 
完全な GuideNH inline リッチ-テキスト 構文: Markdown formatting と MDX inline タグ. It です 描画される とともに
drop-shadow.

> **リッチ テキスト:**  `text` フィールド 対応します  同じ inline markup 使用されます で GuideNH ガイド pages:
> `**bold**`, `*italic*`, `~~strikethrough~~`, `<Color id="RED">colored</Color>`,
> `<ItemLink id="minecraft:iron_ingot" />`, と すべての 他の inline MDX タグ.
> プレーン Minecraft `§` format codes です **ない** 対応; 使用 MDX 構文 代わりに.

> A `text` 注釈 なしで a `text` フィールド (または 空 文字列) です silently ignored.

---

### `input`

描画 mouse-入力 icon (左 button, 右 button, または スクロール wheel) anchored へ a ワールド
位置. この です 使用されます へ hint that  player する必要があります perform 特定の interaction.

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb"
}
```

とともに an 任意の modifier キー prefix と an アイテム icon:

```json
{
  "type": "input",
  "x": 0.5,
  "y": 1.5,
  "z": 0.5,
  "inputType": "lmb",
  "modifier": "sneak",
  "item": "minecraft:iron_ingot"
}
```

| フィールド | 型 | 既定値 | 説明 |
|-------|------|---------|-------------|
| `x`, `y`, `z` | 浮動小数点数 | `0.0` | ワールド空間 anchor 位置. |
| `inputType` | 文字列 | `"lmb"` | 1 つ of `"lmb"`, `"rmb"`, または `"scroll"`. Case-insensitive. |
| `modifier` | 文字列 | `null` | 任意の modifier キー: `"sneak"` または `"ctrl"`. Shows prefix テキスト 上に  icon. |
| `item` | 文字列 | `null` | 任意の アイテム registry ID (e.g. `"minecraft:iron_ingot"`). 描画  アイテム icon へ  左 of  mouse icon. 対応します `"modid:item:meta"` format 用 meta 値. |

 icon は 16x16 sprite drawn から `ponder_widgets.png`.  box 背景 です semi-transparent
dark (`#CC0E0E20`) とともに light-blue 境界線 (`#80AAAADD`). 場合 an `item` です specified  box
expands へ accommodate 両方  アイテム icon と  mouse icon 側 by 側.

---

## 色の形式

Colors です ARGB hexadecimal strings. 両方 `"0xFFFFFF00"` (とともに `0x` prefix) と
`"FFFF00"` (なしで prefix) です accepted.

- `FF` alpha = fully opaque
- `80` alpha = 50% transparent
- `00` alpha = invisible
- `"0xFF00E000"` - fully opaque green (既定値 diamond 色)
- `"0x8022CCFF"` - semi-transparent blue
- `"0xFFAAAAAA"` - light grey (既定値 テキスト bubble 境界線)

## 再生動作

### Controls

| Control | Action |
|---------|--------|
| **Prev keyframe** | Jump へ  開始 of  前の 表示 keyframe segment. 非表示 keyframes です skipped. |
| **Play/Pause** | Toggle playback; restarts から  beginning もし すでに finished. |
| **Restart** | Return へ tick 0, reset 状態, と begin playing. |
| Progress bar | クリック または ドラッグ へ seek へ 任意の 位置. 参照king 常に pauses playback. |
| Keyframe nodes | Small tick marks on  bar 用 表示 keyframes; ホバー へ 参照してください  ラベル と direction 矢印. |

### 初期状態

場合 a ページ containing `<ImportPonder>` です 最初の opened,  シーン starts **paused at tick 0**.
Press Play へ begin.

### カメラロック

中 playback です **active** (ない paused):
-  カメラ follows  interpolated パス defined by keyframes.
- Mouse ドラッグ と スクロール ズーム です **無効**.
-  layer slider と StructureLib sliders です **非表示**.

中 playback です **paused** または **finished**:
- 完全な インタラクティブ カメラ ドラッグ, ズーム, と layer/StructureLib control です restored.

### Keyframe node labels

場合 you ホバー 上 keyframe node on  progress bar:
-  node grows slightly へ indicate it です hovered.
- もし  keyframe has a `label`, it です displayed beside  node.
- 非表示 keyframes do ない 作成 hoverable nodes, but これらは 引き続き apply their timeline 状態 場合 playback または seeking reaches them.

### Layer control 中 playback

 `layer` フィールド of  active keyframe 上書きします  表示-layer フィルター 中 playback:
- `null` (または omitted) -> 表示 すべての layers.
- `1`, `2`, `3`, ... -> restrict へ that 1-based layer index.

## 完全な例

 following 例 demonstrates すべての 注釈 型 across four-keyframe シーン.

### ディレクトリ構成

```
assets/mymod/guidebooks/
  pages/machines/grinder.mdx
  pages/machines/grinder.snbt
  pages/machines/grinder.json
```

### `grinder.json`

```json
{
  "totalTime": 240,
  "keyframes": [
    {
      "time": 0,
      "label": "Overview",
      "camera": { "zoom": 1.5, "rotX": 20, "rotY": 225 },
      "layer": null,
      "annotations": []
    },
    {
      "time": 60,
      "label": "Input hatch",
      "camera": { "rotY": 180 },
      "layer": null,
      "annotations": [
        {
          "type": "diamond",
          "x": 0.5, "y": 1.5, "z": 1.5,
          "color": "0xFF44FF44",
          "tooltip": "EV Input Bus",
          "alwaysOnTop": true
        },
        {
          "type": "text",
          "x": 0.5, "y": 3.0, "z": 1.5,
          "text": "Insert ore here",
          "color": "0xFF44FF44"
        },
        {
          "type": "input",
          "x": 0.5, "y": 2.0, "z": 1.5,
          "inputType": "rmb"
        }
      ]
    },
    {
      "time": 140,
      "label": "Output side",
      "camera": { "rotY": 90 },
      "layer": null,
      "annotations": [
        {
          "type": "box",
          "minX": 2.0, "minY": 0.0, "minZ": 0.0,
          "maxX": 3.0, "maxY": 2.0, "maxZ": 3.0,
          "color": "0x8800AAFF",
          "lineWidth": 1.5
        },
        {
          "type": "line",
          "fromX": 2.0, "fromY": 1.0, "fromZ": 1.5,
          "toX": 2.5, "toY": 1.0, "toZ": 1.5,
          "color": "0xFFFFAA00",
          "lineWidth": 2.0,
          "alwaysOnTop": true
        },
        {
          "type": "blockface",
          "pos": [2, 1, 1],
          "color": "0x8833FF33"
        },
        {
          "type": "text",
          "x": 2.5, "y": 3.0, "z": 1.5,
          "text": "Collect dust here",
          "color": "0xFF00AAFF"
        },
        {
          "type": "input",
          "x": 2.5, "y": 2.0, "z": 1.5,
          "inputType": "lmb"
        }
      ]
    },
    {
      "time": 220,
      "label": "Scroll layer",
      "camera": { "rotY": 225 },
      "layer": null,
      "annotations": [
        {
          "type": "input",
          "x": 1.5, "y": 2.5, "z": 1.5,
          "inputType": "scroll"
        },
        {
          "type": "text",
          "x": 1.5, "y": 3.5, "z": 1.5,
          "text": "Scroll to show layers",
          "color": "0xFFFFCC00"
        }
      ]
    },
    {
      "time": 240,
      "camera": { "rotY": 225 }
    }
  ]
}
```

### `grinder.snbt`

標準 NBT structure ファイル (作成された とともに  `/structure save` command または tool 例として
Litematica). 参照 [Getting Started](Guide-Page-Format) 用  完全な SNBT format reference.

### `grinder.mdx`

```mdx
# Grinder

<GameScene zoom="4">
  <ImportStructure src="grinder.snbt" />
  <ImportPonder src="grinder.json" />
</GameScene>

粉砕機は鉱石を2倍の粉末にします。**再生**を押すとアニメーション手順を確認できます。
```

## 注記

-  Ponder アニメーション progress bar です drawn 上に  layer/StructureLib sliders. 中 playback 
  structural sliders です 非表示 へ 保持します  UI clean; これらは reappear 場合 paused.
- カメラ interpolation です 常に smooth (ease-で/out) even もし some keyframes のみ 変更 a
  subset of カメラ axes. 使用 `cameraEaseTicks` on keyframe へ snap  カメラ instantly (`0`)
  または ease 上 a 固定 数値 of ticks 前 holding  対象 位置.
- 注釈 belong へ a 単一の keyframe - これらは appear のみ 中 that keyframe です active
  (i.e., から its `time` tick until  次の keyframe's `time` tick). Overlay テキスト 注釈
  fade out smoothly 場合  keyframe 変更 中 playback.
- Ponder アニメーション `line` 注釈 使用  同じ 実行時 renderer as regular `LineAnnotation`, including
  折れ線 bends と 開始/終端 arrows. 点 marker cubes remain MDX-のみ feature.
- Ponder アニメーション `text` 注釈 使用  同じ 実行時 renderer as regular `TextAnnotation`, including
  connector 側, offset, と length. Dynamic テキスト です keyframe-based rather than interpolated per tick.
- のみ 1 つ `<ImportPonder>` タグ です effective per `<GameScene>`. second タグ overwrites  最初の.
- A `text` 注釈 とともに 空 または absent `text` フィールド です silently skipped.
-  `inputType` フィールド defaults へ `"lmb"` もし omitted または unrecognised.
- `blockChanges` です applied で 順序 から  最初の へ  現在の keyframe すべての time 
  active keyframe 変更, so changing  同じ 位置 で 複数の keyframes works correctly.
- Tile/entity NBT operations 使用  同じ replay model as `blockChanges`; これらは です safe へ seek
  forwards または backwards.
- `text` 注釈 とともに a `maxWidth` &gt; 0 です word-wrapped using  vanilla font renderer;
   bubble box 高さ adjusts 自動的に 用 multi-行 テキスト.
- `nbt` strings で `blockChanges` 必要があります 使用 **unquoted** SNBT キー (標準 MC 1.7.10 format).
  Quoted キー します なる rejected by  parser. 文字列 値 引き続き require quotes:
  `{id:"minecraft:iron_ingot",Count:8b}`.
- `modifyTileNBT` と `modifyEntityNBT` 値 です SNBT 値, ない JSON 値. 用 a 文字列
  値, escape  SNBT quotes 内部 JSON: `"value": "\"hello\""`.
