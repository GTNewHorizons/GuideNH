# 画像とアセット

GuideNH の構文と実行時機能を説明します。コード、タグ、パス、ID、属性値は仕様のためそのまま保持しています。


GuideNH 対応します 両方 通常 Markdown 画像 と several 実行時-特定の visual elements.

## アセット Resolution 規則

ガイド assets 解決 とともに  同じ 規則 使用されます by ページ リンク.

| パス form | 例 | 意味 |
| --- | --- | --- |
| 相対 | `test1.png` | 相対 へ  現在の ページ ファイル |
| rooted | `/assets/example_structure.snbt` | 相対 へ  現在の ガイド ルート |
| 明示的な リソース id | `guidenh:textures/gui/example.png` | 絶対 `modid:path` lookup |

## Markdown 画像

通常 Markdown 画像 です 対応:

````md
![Example](test1.png)
````

GuideNH resolves  パス と 読み込みます  binary アセット から  ガイド コンテンツ ルート.

## `FloatingImage`

`<FloatingImage>` 描画 cropped bitmap region that できます 浮動小数点数 とともに テキスト または sit truly inline 内部 a
paragraph. It も accepts 明示的な `modid:path` texture ids, so it できます reference texture assets から
他の mods directly.

### 属性

| 属性 | 必須 | 意味 |
| --- | --- | --- |
| `src` | はい | 画像 パス |
| `x` | はい | crop 開始 X で ソース-画像 pixels |
| `y` | はい | crop 開始 Y で ソース-画像 pixels |
| `width` / `w` | はい | crop 幅 で ソース-画像 pixels; 正確に 1 つ spelling 必要があります なる 使用されます |
| `height` / `h` | はい | crop 高さ で ソース-画像 pixels; 正確に 1 つ spelling 必要があります なる 使用されます |
| `scaleX` | いいえ | horizontal 表示 multiplier, 既定値 `1.0` |
| `scaleY` | いいえ | vertical 表示 multiplier, 既定値 `1.0` |
| `displayWidth` | いいえ | 最終 表示 幅 で pixels; preserves  crop aspect ratio 場合 使用されます alone |
| `displayHeight` | いいえ | 最終 表示 高さ で pixels; preserves  crop aspect ratio 場合 使用されます alone |
| `wrap` | いいえ | `inline` 用 true inline placement, otherwise 使用  通常 wrapping modes |
| `align` | いいえ | `left` または `right` 用 floating placement; ignored 場合 `wrap="inline"` |
| `title` | いいえ | ツールチップ/タイトル テキスト |
| `sound` | いいえ | sound event played by  whole 画像 |
| `soundSrc` | いいえ | sound ファイルパス 用  whole 画像 |
| `trigger` | いいえ | `click` by 既定値, または `hover` 用 ホバー playback |

### 注記

- `x`, `y`, `width` / `w`, と `height` / `h` です すべての 必須 together 場合 cropping
- 場合  crop 属性 です すべての omitted, `displayWidth` または `displayHeight` displays  完全な ソース 画像
- `width` と `height` now describe  crop rectangle, ない  最終 表示 サイズ
- `scaleX` と `scaleY` 計算  最終 表示 サイズ as `cropWidth * scaleX` by `cropHeight * scaleY`
- `displayWidth` または `displayHeight` sets  最終 表示 サイズ で pixels; 場合 のみ 1 つ です present,  他の dimension です 計算済み から  crop aspect ratio
- providing 両方 `displayWidth` と `displayHeight` 許可します intentional non-proportional stretching
- `displayWidth` / `displayHeight` できません なる combined とともに `scaleX` / `scaleY`
- 単一の-axis stretching です 対応 by setting のみ 1 つ scale differently
- `width` とともに `w`, または `height` とともに `h`, です 無効 と 描画 a 表示 エラー
- old `FloatingImage width/height as display size` コンテンツ です intentionally breaking と 必要があります なる migrated 手動で
- `src` 可能性があります なる 相対, rooted, または 明示的な `modid:path` texture id 例として `minecraft:textures/gui/options_background.png`

### 例

````md
<FloatingImage
  src="minecraft:textures/gui/options_background.png"
  x="0"
  y="0"
  width="32"
  height="32"
  displayWidth="64"
  displayHeight="64"
  wrap="inline"
  title="Example"
/>
````

## `ImageAnnotation`

`<ImageAnnotation>` は 子 element of `<FloatingImage>` that attaches リッチ-テキスト ツールチップ (と
an 任意の colored 境界線) へ rectangular region of  画像. 座標 です specified で
**cropped-画像 pixels** と です 自動的に proportionally scaled 場合  cropped 画像 です resized
または stretched.

### 属性

| 属性 | 必須 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `x` | いいえ | — | 左 edge of  region で 画像 pixels |
| `y` | いいえ | — | 上 edge of  region で 画像 pixels |
| `w` | いいえ | — | 幅 of  region で 画像 pixels |
| `h` | いいえ | — | 高さ of  region で 画像 pixels |
| `border` | いいえ | `false` | 表示 colored 境界線 around  region |
| `borderColor` | いいえ | random | 境界線 色 (`#RRGGBB` または `#AARRGGBB`) |
| `borderThickness` | いいえ | `1` | 境界線 thickness で 表示 pixels |
| `sound` | いいえ | none | 任意の sound event played 用 この region |
| `src` | いいえ | none | 任意の sound ファイルパス; converted へ sound event id |
| `trigger` | いいえ | `click` | `click` または `hover` |

### 注記

- omitting すべての four of `x`, `y`, `w`, `h` makes  注釈 cover  **whole 画像**
- もし 任意の of  four です present,  remaining omitted ones 既定値 へ `0` (origin) または `1` (サイズ)
- 境界線 です **ない 表示される by 既定値**; 追加します `border` または `border={true}` へ enable it
- 場合 `borderColor` 省略された場合 と `border` 有効な場合, random fully-opaque 色 です 使用されます
- 子 MDX コンテンツ です 描画される as  ツールチップ 本文 と 可能性があります 含める 任意の inline/ブロック elements
- later 注釈 (lower で  一覧) take ホバー priority 上 earlier ones 場合 regions overlap

### 例

Whole-画像 注釈:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation>
    画像上にカーソルを置くと、このツールチップが表示されます。
  </ImageAnnotation>
</FloatingImage>
````

Region 注釈 とともに a 表示 境界線:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="10" y="10" w="60" h="40" border borderColor="#FFFF4444" borderThickness="2">
    これは**強調領域**のツールチップです。
  </ImageAnnotation>
</FloatingImage>
````

複数の regions on 1 つ 画像:

````md
<FloatingImage src="test1.png" align="left" x="0" y="0" width="128" height="128">
  <ImageAnnotation x="0" y="0" w="64" h="64" border borderColor="#FF44FF44">
    Left half
  </ImageAnnotation>
  <ImageAnnotation x="64" y="0" w="64" h="64" border borderColor="#FF4444FF">
    Right half
  </ImageAnnotation>
</FloatingImage>
````

画像 regions できます も play sounds. 使用 `<SoundArea>` 場合 you のみ need sound, または put `sound`
directly on `<ImageAnnotation>` 場合  同じ region も has a ツールチップ または 境界線.

````md
<FloatingImage
  src="test1.png"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  sound="guidenh:image.click"
>
  <SoundArea x="0" y="0" w="64" h="64" sound="guidenh:image.left" />
  <SoundArea x="64" y="0" w="64" h="64" sound="guidenh:image.right" trigger="hover" />
  <ImageAnnotation x="10" y="10" w="40" h="40" border sound="guidenh:image.note">
    この領域にはツールチップの内容とクリック音があります。
  </ImageAnnotation>
</FloatingImage>
````

`<FloatingImage sound="...">` covers  whole 画像. Region sounds 使用 cropped-画像 座標
と obey  同じ overlap priority as tooltips: later regions win.

## コンテンツの埋め込みとテキスト折り返し

すべての ブロック-level タグ — `<FloatingImage>`, `<Recipe>`, `<GameScene>`, `<ItemImage>`, `<BlockImage>`,
と 任意の 他の タグ backed by `BlockTagCompiler` — 対応 2 つ 任意の レイアウト 属性 that
提供 Word-style コンテンツ embedding.

| 属性 | 値 | 既定値 | 意味 |
| --- | --- | --- | --- |
| `wrap` | `inline` · `square` · `tight` · `through` · `top-bottom` · `behind` · `front` | `inline` | テキスト-wrapping モード |
| `align` | `left` · `center` · `right` | `left` | 水平配置 |

### 折り返しモード

| モード | Word equivalent | ブロック-context behaviour | Flow-context behaviour |
| --- | --- | --- | --- |
| `inline` | で 行 とともに テキスト | 既定値 stack (嵌入型) | Sits on  テキスト 行 |
| `square` | Square | Document-level 浮動小数点数; テキスト wraps around (方形环绕) | `FLOAT_LEFT` / `FLOAT_RIGHT` |
| `tight` | Tight | 同じ as `square` (紧密型) | 同じ as `square` |
| `through` | 通じて | 同じ as `square` (穿越型) | 同じ as `square` |
| `top-bottom` | 上 と 下 | 完全な-幅 slot; `align` repositions horizontally (上下型) | 行-inline とともに breaks |
| `behind` | Behind テキスト | Aligned inline slot; 描画 behind テキスト (衬于文字下方) | Sits on  行 |
| `front` | で front of テキスト | Aligned inline slot; 描画 で front of テキスト (浮于文字上方) | Sits on  行 |

### Alignment とともに floating wrap

用 `wrap=square/tight/through`:
- `align=left` (既定値) — ブロック floats へ  **左**; テキスト fills  右 側.
- `align=right` — ブロック floats へ  **右**; テキスト fills  左 側.
- `align=center` — ブロック です centred なしで floating (いいえ テキスト wrapping).

### 例

左-floating 画像 using  new `wrap` 属性:

````md
<FloatingImage
  src="test1.png"
  wrap="square"
  align="left"
  x="0"
  y="0"
  width="128"
  height="128"
  scaleX="0.5"
  scaleY="0.5"
/>

画像の右側に流れる段落テキスト…
````

右-floating recipe:

````md
<Recipe id="minecraft:stone" wrap="square" align="right" />

レシピ枠の左側に流れるテキスト…
````

Centred アイテム 画像 (いいえ テキスト wrapping):

````md
<ItemImage id="minecraft:diamond" align="center" />
````

右-aligned アイテム 画像:

````md
<ItemImage id="minecraft:diamond" align="right" />
````

アイテム NBT できます なる supplied separately から  アイテム id. Inline SNBT で `id` です 引き続き 対応;
場合 両方 forms です present,  standalone `nbt` 属性 です merged 最後の.

````md
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

> **注記** — `wrap="inline"` now gives `<FloatingImage>` true inline placement 内部 flow テキスト.
> で inline モード, `align` です ignored 代わりに of producing エラー.

## ナビゲーション Texture Icons

frontmatter できます 使用 `icon_texture` へ 表示 texture 代わりに of an アイテム で ナビゲーション/検索:

```yaml
navigation:
  title: Root
  icon_texture: test1.png
```

 ファイル 必要があります decode as an 画像.  パス です 解決された like 任意の 他の ガイド アセット パス.

## Non-画像 Assets

GuideNH pages 可能性があります も reference non-画像 実行時 assets, especially structure files, 用 例:

````md
<ImportStructure src="/assets/example_structure.snbt" />
````

これらの assets です 読み込み済み 通じて  同じ ガイド アセット pipeline but です consumed by カスタム タグ rather than 描画される directly as 画像.

## Best Practices

- 保持します ページ-ローカル 画像 近く  ページ that 使用します them
- 保持します reusable files under  ガイド ルート `assets/` フォルダー
- prefer rooted `/assets/...` paths 用 shared files referenced by 複数の pages
- 使用 texture icons のみ 用 real 画像 assets

## `BlockImage`

`<BlockImage>` 使用します  同じ ブロック-level embedding 規則 as `<FloatingImage>`, but  visual
コンテンツ は transparent 3D 単一の-ブロック プレビュー 代わりに of bitmap. It です best suited 用
showing how placed ブロック looks で-ワールド 中 引き続き fitting inline とともに 通常 ガイド prose.

キー behavior:

- transparent 背景 と 境界線
- いいえ シーン buttons, いいえ layer slider, いいえ 注釈 authoring surface
- ホバー 引き続き shows  選択済み ブロック outline と ツールチップ
- `scale` 変更 カメラ ズーム と defaults へ `4`
- `perspective` accepts `isometric-north-east`, `isometric-north-west`, と `up`
- `nbt` supplies tile-entity SNBT; inline `id="mod:block:meta:{...}"` SNBT 引き続き works, but 
  standalone `nbt` 属性 です easier へ 読み取る と です 推奨

例:

````md
<BlockImage id="minecraft:stone" />
<BlockImage id="minecraft:furnace" perspective="isometric-north-west" scale="2.5" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:apple",Count:8b,Damage:0s}]}'
/>
````

## 実行時 例 Files

- `wiki/resourcepack/assets/guidenh/guidenh/_en_us/test1.png`
- `wiki/resourcepack/assets/guidenh/guidenh/assets/example_structure.snbt`

## 関連ページ

- [Guide Page Format](Guide-Page-Format)
- [Tags Reference](Tags-Reference)
- [GameScene](GameScene)
