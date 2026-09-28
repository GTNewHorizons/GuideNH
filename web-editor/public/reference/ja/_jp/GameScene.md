# GameScene

GuideNH の構文と実行時機能を説明します。コード、タグ、パス、ID、属性値は仕様のためそのまま保持しています。


`<GameScene>` は GuideNH の 3D プレビュー タグ. `<Scene>` は alias とともに  同じ behavior.

## シーン属性

| 属性 | 型 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `width` | 整数 | `256` | viewport 幅 で pixels |
| `height` | 整数 | `192` | viewport 高さ で pixels |
| `zoom` | 浮動小数点数 | `1.0` | カメラ ズーム multiplier |
| `perspective` | 文字列 | `isometric-north-east` | カメラ preset |
| `rotateX` | 浮動小数点数 | auto | 明示的な X 回転 上書き |
| `rotateY` | 浮動小数点数 | auto | 明示的な Y 回転 上書き |
| `rotateZ` | 浮動小数点数 | auto | 明示的な Z 回転 上書き |
| `offsetX` | 浮動小数点数 | auto | 画面空間 horizontal pan |
| `offsetY` | 浮動小数点数 | auto | 画面空間 vertical pan |
| `centerX` | 浮動小数点数 | auto | 明示的な ワールド 回転 中央 X |
| `centerY` | 浮動小数点数 | auto | 明示的な ワールド 回転 中央 Y |
| `centerZ` | 浮動小数点数 | auto | 明示的な ワールド 回転 中央 Z |
| `interactive` | 真偽値 式 | `true` | マウス操作を有効にします |
| `showBackground` | 真偽値 式 | `true` | shows  シーン 背景 fill と 境界線 |
| `allowLayerSlider` | 真偽値 | `true` | shows  vertical layer slider |
| `gridButtonEnabled` | 真偽値 | `true` | shows  floor grid toggle button |
| `showGrid` | 真偽値 | `false` | 初期 表示状態 of  floor grid |

## ブロック統計オーバーレイ

シーン that 含む ブロック enable  ブロック-stat toggle button by 既定値. 追加します a `<BlockStats>`
子 場合 you want へ 上書き its モード, placement, filters, 表示状態, または サイズ.  一覧 です
cached と のみ rebuilt 場合  シーン ブロック, Ponder アニメーション timeline 状態, StructureLib 選択, または
ブロック-stat settings 変更; 通常 描画 reuses  prepared 行. Long lists です clipped へ
`maxWidth` と `maxHeight`; もし those です omitted, 各 です  larger of  固定 `224` by `96` pixels と
40% of  シーン サイズ.
あふれた内容にはドラッグ可能なスクロールバーが付きます, と  mouse wheel scrolls  一覧 中  cursor です
上  overlay. Hold Shift へ wheel-スクロール horizontally.

で 自動 モード, GuideNH scans  シーン's filled ブロック と resolves 各 ブロック へ  アイテム
stack users normally 参照してください. ブロック that 含む 複数の 表示 components できます contribute 複数の
項目 から  同じ 座標; この 含みます AE2 cable bus parts と facades, ForgeMultipart part
drops, と Carpenters' ブロック covers または overlays 場合 those mods です installed. Counts です grouped
by `item:meta` と sorted by count.

自動 lists できます も なる docked 外部  シーン とともに `dock="left"`, `dock="top"`,
`dock="right"`, または `dock="bottom"`. Docked lists wrap into extra columns または 行 based on 
attached 側 length, reserve レイアウト 空間, と avoid  シーン button 列 on  右. クリック an
アイテム で 自動 一覧 へ 強調表示 すべての 一致する シーン placements とともに their 解決された collision
boxes using 常に-on-上 face overlay; クリック  同じ アイテム again へ clear  強調表示. Counts
です 描画される 通じて  ItemStack stack-サイズ overlay. 設定します `showNames={true}` へ append  count
後 各 名前 as well, と ホバー an アイテム へ 参照してください  exact ブロック count で  ツールチップ.

Filters できます 非表示 一般的な ブロック または 表示 のみ 選択済み ブロック:

````md
<GameScene>
  <Block id="minecraft:stone" />
  <Block id="minecraft:furnace" x="1" />
  <BlockStats corner="topRight" filterMode="blacklist" filter="minecraft:air minecraft:stone"
    maxWidth="160" maxHeight="96" />
</GameScene>
````

使用 manual モード 場合 a ガイド wants へ 表示 planned material 一覧 代わりに of  literal シーン
contents:

````md
<GameScene>
  <Block id="minecraft:furnace" />
  <BlockStats mode="manual" corner="topRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:cobblestone" count="8" />
    <BlockStat item="minecraft:furnace" count="1" />
  </BlockStats>
</GameScene>
````

## デバッグモードのオーバーレイ

場合  `enableDebugMode` オプション 有効な場合 で  GuideNH mod config,  following extra
overlays become 利用可能 で  3D シーン プレビュー.

### Grid 座標 Labels

場合 debug モード です **on** と  floor grid です **表示**, 座標 labels です 描画される
below 各 grid 行:

- **X-axis numbers** です 表示される along  近く edge of  grid (north/−Z edge で  既定値
  `isometric-north-east` カメラ).  各 整数 X ワールド-座標 receives ラベル.
- **Z-axis numbers** です 表示される along  近く edge of  grid (east/+X edge).  各 整数
  Z ワールド-座標 receives ラベル.
- **Cardinal direction initials** (`N`, `S`, `E`, `W`) です drawn at  midpoint of 各
  respective grid edge.

座標 follow  実際の ワールド X/Z 値 stored で  シーン level, so これらは できます なる
negative 場合  structure 含みます ブロック とともに negative 座標.

 grid toggle button です **常に 有効** 中 debug モード です active, に関係なく 
`gridButtonEnabled` 属性, so you できます 表示 または 非表示  grid と its labels at 任意の time.
 既定値 grid 表示状態 (`showGrid`) です ない affected.

### ブロック 座標 ツールチップ

場合 debug モード です **on** と  cursor hovers 上 a ブロック 内部  シーン, second
ツールチップ です 描画される 上に  primary ブロック ツールチップ, showing  ワールド空間 ブロック 位置
as `X, Y, Z` で gold テキスト.

もし  座標 ツールチップ would なる clipped at  上 of  画面 it 自動的に snaps
below  cursor area 代わりに (magnetic snapping).

## 視点プリセット

Accepted `perspective` 値:

- `isometric-north-east`
- `isometric-north-west`
- `up`

不明な 値 fall back へ `isometric-north-east`.

## コンテンツの埋め込みとテキスト折り返し

任意の ブロック-level タグ — including `<GameScene>` — 対応します 2 つ 任意の 属性 that control
how it です embedded で  ページ, Microsoft Word の "テキスト Wrapping" オプション.

| 属性 | 値 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | テキスト-wrapping モード |
| `align` | `left` · `center` · `right` | `left` | 水平配置 |

### 折り返しモード

| モード | Word equivalent | Effect |
| --- | --- | --- |
| `inline` | で 行 とともに テキスト | 既定値 flow: シーン occupies its 独自の vertical slot (嵌入型) |
| `square` | Square | シーン floats 左 または 右; surrounding テキスト wraps で rectangle around it (方形环绕) |
| `tight` | Tight | Tighter wrap; equivalent へ `square` で この レイアウト system (紧密型) |
| `through` | 通じて | 通じて-wrap; equivalent へ `square` で この レイアウト system (穿越型) |
| `top-bottom` | 上 と 下 | テキスト のみ 上に と below, ない beside; respects `align` 用 horizontal placement (上下型) |
| `behind` | Behind テキスト | ブロック 描画 behind surrounding テキスト; respects `align` (衬于文字下方) |
| `front` | で front of テキスト | ブロック 描画 で front of surrounding テキスト; respects `align` (浮于文字上方) |

### 例

左-floating シーン — テキスト で  次の paragraph wraps へ  右:

````md
<GameScene wrap="square" align="left" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

シーンの右側に流れるテキスト…
````

右-floating シーン:

````md
<GameScene wrap="square" align="right" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>

シーンの左側に流れるテキスト…
````

Centred シーン (いいえ テキスト wrapping):

````md
<GameScene align="center" width="200" height="150">
  <Block id="minecraft:stone" />
</GameScene>
````

Inline で テキスト (flow context) — テキスト wraps around small シーン:

````md
Some text {<GameScene wrap="square" align="left" width="80" height="80">
  <Block id="minecraft:grass" />
</GameScene>} and more text that wraps to the right.
````

## 例

````md
<GameScene width="256" height="160" zoom={4} perspective="isometric-north-east" interactive={true}>
  <Block id="minecraft:stone" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:glass" z="1" />
</GameScene>
````

## Import 例

これらの 例 focus on  シーン-側 behavior that most 多くの場合 trips people up 場合 importing
structures.

StructureLib import とともに 明示的な facing, 回転, flip, と offsets:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib
    name="main"
    controller="gregtech:gt.blockmachines:2741"
    facing="north"
    rotation="clockwise_180"
    flip="none"
    offsetX="2"
    offsetY="1"
    offsetZ="-3"
  />
</GameScene>
````

GregTech controllers stay unformed by 既定値, even 場合  imported multiblock です otherwise
有効:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" />
</GameScene>
````

設定します `formed={true}` のみ 場合  プレビュー する必要があります intentionally 表示  formed controller 状態:

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:2741" formed={true} />
</GameScene>
````

 同じ 既定値 も 適用します へ controllers placed directly とともに `<Block>`, including GregTech
controllers that rely on surrounding multiblock casings:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="gregtech:gt.blockmachines:2741" />
  <Block id="gregtech:gt.blockmachines:1000" x="3" formed={true} />
</GameScene>
````

Simple ブロック-のみ layouts できます 引き続き なる authored directly と remain compatible とともに multiblock
inspection logic:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:water" />
  <Block id="minecraft:water" x="-1" />
  <Block id="minecraft:water" x="1" />
  <Block id="minecraft:grass" z="1" />
  <Block id="minecraft:grass" x="1" z="1" />
  <Block id="minecraft:glass" z="2" />
  <Block id="minecraft:glass" x="1" z="2" />
</GameScene>
````

## シーンの子要素

GuideNH 現在 registers これらの シーン 子 タグ:

- `<Block>`
- `<ImportStructure>`
- `<ImportStructureLib>`
- `<IsometricCamera>`
- `<BlockStats>`
- `<PlaySound>`
- `<RemoveBlocks>`
- `<RemoveEntity>`
- `<ReplaceBlock>`
- `<PlaceBlock>`
- `<BlockAnnotationTemplate>`
- `<Entity>`
- 注釈 タグ 例として `<BoxAnnotation>` と `<LineAnnotation>`

## シーンのサウンド

`<PlaySound>` できます なる placed 内部 `<GameScene>` へ play sounds から シーン interaction または timeline
エントリ. 使用できるトリガーは次のとおりです:

- `click`,  既定値
- `hover`, fired once 場合  cursor enters  シーン
- `enter`, fired once 場合  シーン 最初の 描画

```mdx
<GameScene width="256" height="160">
  <Block id="minecraft:furnace" />
  <PlaySound sound="guidenh:machine.start" trigger="click" volume="0.8" />
  <PlaySound src="guidenh:sounds/machine/hum.ogg" trigger="hover" volume="0.35" />
</GameScene>
```

場合 `x`, `y`, と `z` です 指定された,  sound volume です attenuated で 画面 空間 から 
projected シーン 座標 へ  クリック 点 または シーン 中央. `radius` defaults へ 75% of 
shorter シーン 側, と `minVolume` defaults へ `0.15`.

## `<BlockStats>` と `<BlockStat>`

Declares または customizes a ブロック統計オーバーレイ. シーン とともに ブロック enable  自動 toggle
button even 場合 この 子 省略された場合. Adding 1 つ または さらに `<BlockStat>` 子要素 switches 
overlay へ manual statistics モード 用 that シーン.

`<BlockStats>` 属性:

| 属性 | 必須 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `visible` | いいえ | config, 既定値 `false` | 初期 overlay 表示状態 |
| `buttonEnabled` | いいえ | config, 既定値 `true` | shows  ブロック statistics toggle button |
| `mode` | いいえ | `auto` | `auto` または `manual`; 子 `<BlockStat>` エントリ force manual モード |
| `corner` | いいえ | `topRight` | overlay corner: `topRight`, `topLeft`, `bottomRight`, または `bottomLeft` |
| `dock` | いいえ | `inside` | 自動 lists できます attach へ `inside`, `left`, `top`, `right`, または `bottom`; manual モード 常に 使用します  内部 overlay |
| `showNames` | いいえ | `false` | whether へ 表示 アイテム names beside icons; 場合 有効  count です も appended 後  名前 |
| `filterMode` | いいえ | `blacklist` | `blacklist` または `whitelist` |
| `filter` | いいえ | 空 | アイテム キー 例として `minecraft:stone` または `minecraft:stone:0`, separated by spaces, commas, または semicolons |
| `maxWidth` | いいえ |  larger of `224` px と 40% of  シーン 幅 | maximum overlay 幅 で pixels 前 horizontal スクロールすると |
| `maxHeight` | いいえ |  larger of `96` px と 40% of  シーン 高さ | maximum overlay 高さ で pixels 前 vertical スクロールすると |

`<BlockStat>` 属性:

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `item` | はい, 場合を除き `id` です 使用されます | アイテム id 表示される で  一覧 |
| `id` | はい, 場合を除き `item` です 使用されます | existing アイテム-stack 属性 form |
| `count` | いいえ | displayed count; omitting it shows  行 once, と `count="0"` hides  行 |

例:

````md
<GameScene>
  <BlockStats corner="bottomRight" maxWidth="160" maxHeight="96">
    <BlockStat item="minecraft:stone" count="16" />
    <BlockStat item="minecraft:torch" count="4" />
  </BlockStats>
</GameScene>
````

## `<Block>`

Places a ブロック into  プレビュー ワールド.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい, 場合を除き `ore` です 使用されます | ブロック id |
| `ore` | いいえ | ore dictionary 名前;  最初の 一致する stack 必要があります 解決 へ a ブロック アイテム |
| `x` | いいえ | 整数 ワールド X, 既定値 `0` |
| `y` | いいえ | 整数 ワールド Y, 既定値 `0` |
| `z` | いいえ | 整数 ワールド Z, 既定値 `0` |
| `meta` | いいえ | 整数 ブロック metadata |
| `facing` | いいえ | `down`, `up`, `north`, `south`, `west`, `east` |
| `nbt` | いいえ | SNBT TileEntity compound |
| `formed` | いいえ | whether  placed structure controller する必要があります なる treated as formed 中 プレビュー sync; 既定値 `false` |

注記:

- `ore` takes precedence 上 `id`; もし GregTech です installed,  選択済み stack です unified 通じて `GTOreDictUnificator.setStack(...)`
- もし `meta` 省略された場合 と an `ore` 一致 carries concrete non-wildcard アイテム damage, that damage です 使用されます 前  `facing` フォールバック
- もし `meta` 省略された場合, some ブロック derive sensible 既定値 から `facing`
- もし `nbt` 作成します TileEntity successfully,  プレビュー 使用します it
- 設定します `formed={false}` 場合 controller-based structure する必要があります stay unformed で プレビュー even though  surrounding structure です otherwise 有効

例:

````md
<Block id="minecraft:furnace" x="2" facing="south" />
<Block ore="logWood" x="3" />
<Block id="minecraft:chest" x="4" nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}' />
````

## `<ImportStructure>`

読み込みます 外部 structure ファイル into  シーン.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `src` | はい | structure アセット パス |
| `x` | いいえ | 整数 翻訳 X (alias 用 `offsetX`) |
| `y` | いいえ | 整数 翻訳 Y (alias 用 `offsetY`) |
| `z` | いいえ | 整数 翻訳 Z (alias 用 `offsetZ`) |
| `offsetX` | いいえ | 整数 翻訳 X (推奨 上 `x`) |
| `offsetY` | いいえ | 整数 翻訳 Y, clamped へ `[0, worldHeight-1]` (推奨 上 `y`) |
| `offsetZ` | いいえ | 整数 翻訳 Z (推奨 上 `z`) |
| `formed` | いいえ | whether imported structure controllers する必要があります なる treated as formed 中 プレビュー sync; 既定値 `false` |

対応形式:

- SNBT テキスト
- gzipped binary NBT
- uncompressed binary NBT

必須 structure キー:

- `palette`
- `blocks`

例:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ImportStructure src="/assets/example_structure.snbt" x="4" />
<ImportStructure src="/assets/example_structure.snbt" formed={false} />
````

## `<ImportStructureLib>`

Imports StructureLib multiblock プレビュー by controller id.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `controller` | はい | controller ブロック id, using `modid:block[:meta]` |
| `name` | いいえ | 任意の binding 名前 使用されます by `showWhenStructure` on 注釈, templates, と sounds |
| `piece` | いいえ | StructureLib piece 名前 上書き |
| `facing` | いいえ | facing 上書き passed へ  importer |
| `rotation` | いいえ | 回転 上書き passed へ  importer |
| `flip` | いいえ | flip/mirror 上書き passed へ  importer |
| `channel` | いいえ | 整数 channel 上書き 用 channel-aware structures |
| `offsetX` | いいえ | 整数 X offset applied へ すべての placed ブロック (既定値 `0`) |
| `offsetY` | いいえ | 整数 Y offset applied へ すべての placed ブロック, clamped へ `[0, worldHeight-1]` (既定値 `0`) |
| `offsetZ` | いいえ | 整数 Z offset applied へ すべての placed ブロック (既定値 `0`) |
| `formed` | いいえ | whether imported StructureLib controllers する必要があります なる treated as formed 中 プレビュー sync; 既定値 `false` |

注記:

-  imported structure starts から シーン `0 0 0`;  controller です ない forced へ なる placed at `0 0 0`
- この タグ enables StructureLib-特定の ツールチップ, hatch 強調表示, と channel slider UI 場合 metadata です 利用可能
- controller 一致する 対応します  GTNH-style `modid:block:meta` form
- 使用 `name` 場合  シーン 含みます 複数の StructureLib imports と another タグ needs へ 対象 1 つ 特定の structure 状態
- `facing`, `rotation`, と `flip` 使用  同じ orientation vocabulary as StructureLib export; 場合 requested combination です ない allowed by  controller, GuideNH falls back へ  最初の 有効 alignment 自動的に
- GregTech controller previews now 既定値 へ  controller's opposite horizontal facing から  older プレビュー orientation, rotating  プレビュー front by 180 degrees around  Y axis
- 設定します `formed={false}` 場合  imported controller する必要があります remain visibly unformed; この です  対応 alternative へ shipping intentionally broken NBT

例:

````md
<ImportStructureLib controller="botanichorizons:automatedCraftingPool" />
<ImportStructureLib controller="gregtech:gt.blockmachines:1000" channel="7" />
<ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
<ImportStructureLib controller="gregtech:gt.blockmachines:15411" formed={false} />
````

Structure-aware 注釈 と sound 例:

````md
<GameScene interactive={true}>
  <ImportStructureLib name="main" controller="gregtech:gt.blockmachines:15411" />
  <ImportStructureLib name="aux" controller="gregtech:gt.blockmachines:15412" />

  <BlockAnnotation
    pos="5 1 2"
    color="#FFD24C"
    showWhenStructure="main"
    showWhenTier="2..4,!3"
    showWhenChannels="input:1..3, casing:!2"
  >
    Visible only for matching `main` states.
  </BlockAnnotation>

  <PlaySound
    sound="guidenh:machine.start"
    trigger="click"
    showWhenStructure="aux"
    showWhenTier="1..2"
  />
</GameScene>
````

StructureLib defaults できます も なる supplied as 子 タグ. これらの defaults です part of  シーン's
初期 インタラクティブ 状態, so  reset-view button restores them 後  user 変更 tier または
channel sliders.

| 子 タグ | 意味 |
| --- | --- |
| `<Tier value="1" />` | Master tier 値. |
| `<Channel name="channelName" value="1" />` | Named StructureLib channel 上書き. Repeat 用 複数の channels. |
| `<Facing value="north" />` | 既定値 facing. |
| `<Rotation value="normal" />` | 既定値 回転. |
| `<Flip value="none" />` | 既定値 flip/mirror. |
| `<Orientation value="north:normal:none" />` | Facing, 回転, と flip で 1 つ タグ. |
| `<GregTechActiveController />` | GregTech のみ: 描画  controller とともに its active texture 場合 possible. |
| `<GregTechPlaceHatches />` | GregTech のみ: 配置 通常 GT hatches 用 hatch-のみ プレビュー positions. なしで この, GT previews 引き続き 使用 survival construct 用 hatch-aware machines, but 空 hatch positions fall back へ casing ブロック. |

用 GregTech controllers, GuideNH now 使用します  同じ StructureLib survival-プレビュー パス as 
export command. この fixes hatch-のみ positions that 通常 `construct()` できません populate 中
keeping フォールバック casings by 既定値.

````md
<GameScene width="384" height="256" zoom={4} interactive={true}>
  <ImportStructureLib controller="gregtech:gt.blockmachines:1000">
    <Tier value="4" />
    <Channel name="voltage" value="4" />
    <Facing value="north" />
    <Rotation value="normal" />
    <Flip value="none" />
    <GregTechActiveController />
    <GregTechPlaceHatches />
  </ImportStructureLib>
</GameScene>
````

## `<IsometricCamera>`

適用します 明示的な isometric カメラ yaw/pitch/roll.

もし この タグ 省略された場合,  シーン keeps using  `<GameScene>` `perspective` preset.  既定値
`isometric-north-east` preset です equivalent へ:

````md
<IsometricCamera yaw="225" pitch="30" />
````

| 属性 | 意味 |
| --- | --- |
| `yaw` | 浮動小数点数 |
| `pitch` | 浮動小数点数 |
| `roll` | 浮動小数点数 |

例:

````md
<IsometricCamera yaw="45" pitch="30" roll="0" />
````

## `<RemoveBlocks>`

Removes すべての すでに-placed ブロック 一致する 対象 ブロック id.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい | ブロック id へ 削除, using `modid:block[:meta]` |

この です useful 後 importing structure 場合 you want へ 非表示 特定の ブロック 用 clarity.

例:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<RemoveBlocks id="minecraft:stone" />
<RemoveBlocks id="minecraft:stone:3" />
````

## `<ReplaceBlock>`

Replaces すでに-placed ブロック that 一致 ソース ブロック id (と optionally partial tile entity NBT
pattern) とともに new ブロック.  検索 できます なる グローバル (すべての filled ブロック) または restricted へ a
bounding box.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `from` | はい | ソース ブロック へ 一致, using `modid:block[:meta]` |
| `from_nbt` | いいえ | partial SNBT compound; a ブロック 一致します のみ 場合 its tile entity NBT 含みます すべての listed キー |
| `to` | はい | replacement ブロック, using `modid:block[:meta]` |
| `to_nbt` | いいえ | SNBT TileEntity compound へ apply へ  replacement |
| `x` | いいえ | bounding box 開始 X; もし 任意の of `x/y/z/dx/dy/dz` です present,  box モード です activated |
| `y` | いいえ | bounding box 開始 Y |
| `z` | いいえ | bounding box 開始 Z |
| `dx` | いいえ | bounding box length on  X axis (既定値 `1`) |
| `dy` | いいえ | bounding box 高さ on  Y axis (既定値 `1`) |
| `dz` | いいえ | bounding box 幅/depth on  Z axis (既定値 `1`) |
| `formed` | いいえ | whether replacement 結果 controllers する必要があります なる treated as formed 中 プレビュー sync; 既定値 `false` |

注記:

- 場合 none of `x/y/z/dx/dy/dz` です 指定された, すべての filled ブロック です scanned globally
- `from_nbt` は **partial** 一致: のみ  キー listed で  pattern 必要があります 一致; extra キー で
   実際の tile entity です ignored
-  replacement です performed via  同じ ブロック placement pipeline as `<Block>`, so GregTech MetaTile
  と BartWorks tile entities です handled correctly
- もし  replacement places controller, `formed={false}` keeps that controller unformed 中 プレビュー

例:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<ReplaceBlock from="minecraft:stone" to="minecraft:glass" />
<ReplaceBlock from="minecraft:stone:1" to="minecraft:stone:2" x="0" y="0" z="0" dx="5" dy="3" dz="5" />
````

## `<PlaceBlock>`

Fills an 軸に揃った box とともに a 単一の ブロック 型, overwriting whatever was there 前.
Unlike `<Block>` (これらは targets a 単一の 位置), `<PlaceBlock>` 対応します multi-ブロック regions via
`dx`/`dy`/`dz`, ordered as length, 高さ, と 幅/depth on  X/Y/Z axes.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい | ブロック id, using `modid:block[:meta]` |
| `nbt` | いいえ | SNBT TileEntity compound applied へ すべての placed ブロック |
| `x` | いいえ | region 開始 X, 既定値 `0` |
| `y` | いいえ | region 開始 Y, 既定値 `0` |
| `z` | いいえ | region 開始 Z, 既定値 `0` |
| `dx` | いいえ | region length on  X axis, 既定値 `1` |
| `dy` | いいえ | region 高さ on  Y axis, 既定値 `1` |
| `dz` | いいえ | region 幅/depth on  Z axis, 既定値 `1` |
| `formed` | いいえ | whether placed controllers する必要があります なる treated as formed 中 プレビュー sync; 既定値 `false` |

注記:

- すべての ブロック で  box です unconditionally placed (いいえ prior-ブロック check)
-  NBT compound です copied 各 individual placement
-  同じ ブロック placement pipeline as `<Block>` です 使用されます, so GregTech MetaTile と BartWorks tile entities
  です fully 対応
- もし  region places 1 つ または さらに controllers, `formed={false}` keeps すべての affected controller unformed

例:

````md
<PlaceBlock id="minecraft:stone" x="0" y="0" z="0" dx="5" dy="1" dz="5" />
<PlaceBlock id="minecraft:glass" y="1" dx="3" dz="3" />
<PlaceBlock id="gregtech:gt.blockmachines:15411" dx="3" dz="3" formed={false} />
````

## `<BlockAnnotationTemplate>`

Expands 1 つ または さらに 子 注釈 onto すべての 一致する ブロック that すでに exists で  現在の シーン.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい | ブロック matcher で `modid:block[:meta]` form |

規則:

- 配置 it 後  ブロック または imported structures that it する必要があります 一致
- 一致する happens against  現在の シーン 状態 at 解析 time
- 子 注釈 使用 ローカル 座標 相対 へ 各 一致する ブロック

例:

````md
<ImportStructure src="/assets/example_structure.snbt" />
<BlockAnnotationTemplate id="minecraft:log">
  <DiamondAnnotation pos="0.5 0.5 0.5" color="#ff0000">
    Highlighted by template.
  </DiamondAnnotation>
</BlockAnnotationTemplate>
````

## `<Entity>`

Adds entity へ  プレビュー シーン.

 属性 follow summon-style entity placement と SNBT データ.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `id` | はい | entity 型 id; legacy names like `Sheep`, modern vanilla ids like `minecraft:sheep`, と 登録済み mod entity ids で いずれか `modid.entityName` または `modid:entityName` form です accepted |
| `x` | いいえ | 浮動小数点数 X 座標  entity です centered on, 既定値 `0.5` |
| `y` | いいえ | 浮動小数点数 Y 座標 at  下 of  entity, 既定値 `0` |
| `z` | いいえ | 浮動小数点数 Z 座標  entity です centered on, 既定値 `0.5` |
| `rotationY` | いいえ | yaw で degrees, 既定値 `-45` |
| `rotationX` | いいえ | pitch で degrees, 既定値 `0` |
| `data` | いいえ | summon-style SNBT merged into  entity NBT 前 spawn |
| `sceneEntityId` | いいえ | stable シーン-ローカル entity id 使用されます by later `<Entity>` / `<RemoveEntity>` operations と by imported シーン snapshots |
| `mount` | いいえ | stable `sceneEntityId` of  vehicle that この entity する必要があります ride 後 spawn |
| `unmount` | いいえ | 真偽値 式 that clears この entity's 現在の stable mount relation 後 spawn または 状態 replay |
| `baby` | いいえ | 真偽値 式 forcing 対応 entities into baby form; omitted leaves  entity's 通常 age/状態 unchanged |
| `name` | いいえ | プレビュー player 名前 場合 `id` です `player`, `fakeplayer`, `minecraft:player`, または `minecraft:fakeplayer` |
| `uuid` | いいえ | プレビュー player UUID 場合 using 1 つ of  player ids 上に |
| `showName` | いいえ | 真偽値 式 controlling  プレビュー player nameplate, 既定値 `true` 用 player プレビュー ids |
| `showCape` | いいえ | 真偽値 式 controlling  プレビュー player cape, 既定値 `true` 用 player プレビュー ids |
| `headRotation` | いいえ | プレビュー player head 回転 as `x y z` degrees |
| `leftArmRotation` | いいえ | プレビュー player 左 arm 回転 as `x y z` degrees |
| `rightArmRotation` | いいえ | プレビュー player 右 arm 回転 as `x y z` degrees |
| `leftLegRotation` | いいえ | プレビュー player 左 leg 回転 as `x y z` degrees |
| `rightLegRotation` | いいえ | プレビュー player 右 leg 回転 as `x y z` degrees |
| `capeRotation` | いいえ | プレビュー player cape 回転 as `x y z` degrees; defaults へ  standing-引き続き angle `6 0 0` |

注記:

- entity bounds participate で シーン auto-centering と 表示-layer フィルタリング
- entity creation falls back gracefully 場合  プレビュー ワールド です ない ready yet, then binds on 最初の 描画
- `sceneEntityId` です 任意の, but strongly recommended whenever later シーン mutations need へ find, 削除, remount, または restore  同じ logical entity なしで scanning by 未加工 実行時 id
- 1 つ `sceneEntityId` できます 独自の さらに than 1 つ 実行時 entity instance; `<RemoveEntity sceneEntityId="..."/>` removes すべての entity 現在 登録済み へ that stable id
- `mount` リンク entities by stable シーン id rather than by 未加工 NBT passenger lists, so replay, import/export, プレビュー rebuild, と Ponder アニメーション seeking できます すべての restore  同じ rider/vehicle relation deterministically
- `unmount={true}` clears  stable mount relation 用 that entity 前 任意の later mount です applied
- `baby={true}` 現在 対応します プレビュー players, ageable mobs, vanilla zombies, と modded entities that expose stable `setChild(boolean)` または `setBaby(boolean)` style APIs
- 子-状態 entities です re-aligned へ their 現在の 位置 後 resizing so ホバー と pick bounds stay centered on  描画される model
- player プレビュー ids 作成 client-側 fake remote player so  通常 player renderer と skin pipeline できます なる 使用されます
- 場合 両方 `name` と `uuid` です omitted 用 player プレビュー, GuideNH falls back へ `Steve` と  vanilla 既定値 skin
- 場合 のみ `name` です given 用 player プレビュー, GuideNH 最初の tries へ 解決  real online profile so skins と capes できます 読み込み; もし lookup fails, it falls back へ stable offline UUID
- 場合 のみ `uuid` です given 用 player プレビュー, GuideNH generates placeholder 表示 名前 と 引き続き tries へ 解決  skin から  profile
- `showName={false}` hides  プレビュー player's overhead 名前 なしで bypassing  通常 player renderer
- `showCape={false}` hides  プレビュー player's cape 中 引き続き respecting  通常 player 描画 パス と Forge hooks
- player pose 属性 使用 3 つ 空間-separated floats mapped へ model `X Y Z` 回転 で degrees
- omitted head と limb 回転 属性 保持します  通常 vanilla idle pose; omitted `capeRotation` falls back へ  standing-引き続き cape angle `6 0 0`
- player previews require active client ワールド at 解析 time because Minecraft's player entity constructor できません なる 作成された worldless
- ホバーすると entity shows its ローカライズ済み 表示 名前, または its カスタム 名前 もし 1 つ was 指定された

例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" data="{Color:2}" />
</GameScene>
````

Stable-id mount 例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
</GameScene>
````

Removal と unmount 例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:horse" x="1.5" y="1" sceneEntityId="horse" />
  <Entity id="player" x="1.5" y="2" sceneEntityId="rider" mount="horse" name="GuideNH" />
  <Entity id="player" x="3" y="1" sceneEntityId="rider" unmount={true} name="GuideNH" />
  <RemoveEntity sceneEntityId="horse" />
</GameScene>
````

Baby entity 例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:sheep" y="1" baby={true} data="{Color:14}" />
  <Entity id="minecraft:zombie" x="1.5" y="1" baby={true} />
</GameScene>
````

プレビュー player pose 例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity
    id="player"
    y="1"
    name="ArtherSnow"
    headRotation="0 20 0"
    rightArmRotation="-35 0 0"
    leftArmRotation="10 0 -12"
    rightLegRotation="8 0 0"
    leftLegRotation="-8 0 0"
    capeRotation="12 0 0"
  />
</GameScene>
````

プレビュー player 名前 と cape 例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="player" y="1" name="Huan_F" showName={true} showCape={true} />
  <Entity id="player" x="2" y="1" showName={false} showCape={false} />
</GameScene>
````

## `<RemoveEntity>`

Removes すべての 実行時 entity 現在 登録済み へ 1 つ stable `sceneEntityId`.

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `sceneEntityId` | はい | stable シーン-ローカル entity id へ 削除 |
| `unmount` | いいえ | 真偽値 式 that clears  stable mount relation 前 removal |

注記:

- この です  シーン-側 counterpart へ Ponder アニメーション's `removeEntities`
- removal works on  indexed stable-id registry, so it しません need へ scan すべての entities すべての frame
- もし 複数の imported または replayed entities share  同じ `sceneEntityId`, これらは です 削除された together

例:

````md
<GameScene zoom={4} interactive={true}>
  <Block id="minecraft:grass" />
  <Entity id="minecraft:pig" y="1" sceneEntityId="demoPig" />
  <RemoveEntity sceneEntityId="demoPig" />
</GameScene>
````

## 天候

`<Weather>` adds animated rain または snow directly へ a `GameScene`. Unlike Ponder アニメーション 天候 presets,
シーン 天候 です ない timeline-owned: it keeps looping 中 通常 シーン 描画, it しません
fade で または fade out, と it できません なる paused または scrubbed independently.  renderer 引き続き 使用します 
同じ precipitation ジオメトリ パス as Ponder アニメーション 天候, so ローカル プレビュー と site export stay aligned.

| 属性 | 既定値 | 説明 |
| --- | --- | --- |
| `weather` / `type` | `rain` | 天候 kind. 対応 値: `rain`, `snow`. |
| `x`, `z` | シーン bounds | Covered precipitation columns. scalar targets 1 つ 列. Arrays 使用 endpoint pairs へ define 1 つ または さらに rectangles. |
| `density` | 型-特定の | Coverage density. Higher 値 保持します さらに precipitation columns active; lower 値 sparsify  effect. |

注記:

- `<Weather>` ignores `y`;  vertical span です derived から  現在の シーン bounds と から 
  highest precipitation-blocking ブロック で 各 covered 列.
- もし 1 つ axis has unmatched extra array 値,  unmatched tail です ignored.
- 内部で 1 つ 天候 declaration, rain と snow 決して stack on  同じ `x/z` 列. もし 複数の
  天候 タグ overlap, earlier タグ 保持します  shared columns.
- 異なる non-overlapping columns で  同じ `GameScene` できます 描画 rain と snow at  同じ
  time.

例:

````md
<GameScene width="256" height="160" zoom={4} interactive={false}>
  <Block id="minecraft:grass" />
  <Block id="minecraft:stone" x="1" />
  <Block id="minecraft:stone" x="2" />
  <Weather weather="rain" x="0 1" z="0 0" density="10" />
  <Weather weather="snow" x="2 2" z="0 0" density="7" />
</GameScene>
````

## カメラ中心の動作

もし いいえ 明示的な `centerX/Y/Z` です given, GuideNH auto-centers  シーン から  placed ブロック bounds. もし 任意の 明示的な 中央 座標 設定されている場合, auto-centering 無効な場合 と 不足 座標 既定値 へ `0`.

## 操作に関する注意

場合 `interactive={true}`  シーン 対応します 回転, pan, ズーム, reset, 注釈 toggles, と 他の UI controls exposed by  ガイド 画面.

- シーン spanning 複数の Y levels 表示 a 表示-layer slider 上に  下 edge
- StructureLib シーン できます 追加します hatch-強調表示 toggle button plus channel slider at  very 下 場合  imported metadata 提供します them
- 注釈 ホバー takes priority 上 ブロック ホバー; ブロック tooltips appear normally once いいえ 注釈 hotspot です being hovered
- StructureLib ホバー keeps  ブロック 名前 on  最初の ツールチップ 行, adds structure-特定の テキスト starting on  second 行, と expands replacement candidates 場合 `Shift` です held

## 関連ページ

- [Annotations](Annotations)
- [Structure Export](Structure-Export)
- [Recipes](Recipes)
- [例s](例s)
