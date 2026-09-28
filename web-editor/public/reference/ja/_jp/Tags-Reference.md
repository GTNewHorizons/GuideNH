# タグリファレンス

GuideNH の構文と実行時機能を説明します。コード、タグ、パス、ID、属性値は仕様のためそのまま保持しています。


この ページ lists  built-で 実行時 タグ 登録済み by `DefaultExtensions`.

## 使用規則

- タグ できます appear いずれか で ブロック context または inline context depending on  コンパイラー.
- MDX コメント using `{/* ... */}` です 対応 で ページ コンテンツ と です ignored by  実行時 parser.
- 無効 タグ または 無効 属性 描画 ガイド errors inline 代わりに of silently failing.
- Large feature タグ 例として レシピ と 3D シーン です documented で their 独自の pages:
  - [Recipes](Recipes)
  - [GameScene](GameScene)
  - [Annotations](Annotations)

## インラインタグとフロータグ

| タグ | Purpose | キー 属性 |
| --- | --- | --- |
| `<a>` | 内部/外部 リンク と 任意の anchor 名前 | `href`, `title`, `name` |
| `<br>` | 行 break | `clear="none\|left\|right\|all"` |
| `<kbd>` | keyboard-style inline emphasis | none |
| `<sub>` | smaller inline subscript-style テキスト | none |
| `<sup>` | smaller inline superscript-style テキスト | none |
| `<Color>` | colored inline テキスト | `id` または `color` |
| `<Spoiler>` | 非表示 inline テキスト revealed on ホバー | none |
| `<Tooltip>` | リッチ ホバー ツールチップ とともに Markdown/タグ 子要素 | `label` |
| `<SoundLink>` | clickable リッチ-テキスト sound trigger | `sound` または `src`, `volume`, `pitch`, `cooldown` |
| `<mark>` | inline highlighted テキスト; equivalent へ `==text==` とともに 任意の 色 control | `color` |
| `<PlayerName>` | inserts 現在の player username | none |
| `<KeyBind>` | inserts keybinding 表示 名前 | `id` または `action` |
| `<ItemImage>` | inline アイテム icon | `id` または `ore`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | アイテム ツールチップ + 任意の ナビゲーション リンク | `id` または `ore`, `linksTo`, `showTooltip`, `noTooltip`, `showIcon` |
| `<CommandLink>` | clickable chat command リンク | `command`, `title`, `close` |
| `<Latex>` | LaTeX math 数式; inline で flow context, centered 表示 ブロック で ブロック context | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting quest リンク とともに 状態-aware styling (compat タグ, のみ 登録済み 場合 BetterQuesting です 読み込み済み) | `id`, `text`, `show_tooltip` |

Inline Markdown も 対応します action リンク 用 sound playback:

````md
&[Start machine](sound:guidenh:machine.start)
&[Play file-backed sound](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## ブロックタグ

| タグ | Purpose | キー 属性 |
| --- | --- | --- |
| `<div>` | pass-通じて ブロック wrapper | none |
| `<ContentTabs>` | groups alternative リッチ コンテンツ under independent tabs | `title`, `color`, `icon`, `iconPng`, `icon_png`, `iconItem`, `icon_item`, `default`, `defaultIndex` |
| `<Tab>` | 1 つ コンテンツ panel 内部 `<ContentTabs>` | `title` |
| `<details>` | collapsible 実行時 ブロック | `open`, `width`, `height`, `wrap`, `align` |
| `<FileTree>` | ディレクトリ-style outline とともに connector 行 | `indent`, `gap` |
| `<Row>` | horizontal flex レイアウト | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | vertical flex レイアウト | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | 幅-constrained footnote container 使用されます by 実行時 Markdown footnotes | `width` |
| `<ItemGrid>` | compact grid of アイテム icons | 子要素 必要があります なる `<ItemIcon id="..."/>` または `<ItemIcon ore="..."/>` |
| `<BlockImage>` | non-インタラクティブ 3D 単一の-ブロック プレビュー | `id` または `ore`, 任意の `scale` (defaults へ `4`), `float`, `perspective`, `nbt` |
| `<FloatingImage>` | cropped 画像 ブロック とともに 浮動小数点数 または true inline placement | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | ナビゲーション 子 listing | `id`, `alphabetical` |
| `<Category>` | 一覧 pages から category | `name`, `rows` |
| `<Special>` | 一覧 built-で MediaWiki special pages | `name`, `rows` |
| `<Structure>` | 2.5D isometric ブロック レイアウト view | `width`, `height` |
| `<Mermaid>` | 実行時 Mermaid グラフ import/inline | `src`, `width`, `height` |
| `<CsvTable>` | 実行時 CSV ファイル import 表 | `src`, `header`, `widths` |
| `<ColumnChart>` | clustered 列 チャート | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | horizontal bar チャート | 同じ as `<ColumnChart>` |
| `<LineChart>` | 行 チャート とともに categorical または numeric X | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*` |
| `<PieChart>` | pie チャート | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY scatter チャート | `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<FunctionGraph>` | Desmos-style multi-曲線 関数 グラフ | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes` |
| `<Function>` | 単一の-曲線 shorthand 用 `<FunctionGraph>` | `expr`, plus すべての `<FunctionGraph>` panel 属性 |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | recipe renderers | 参照してください [Recipes](Recipes) |
| `<GameScene>`, `<Scene>` | 3D ガイド シーン | 参照してください [GameScene](GameScene) |
| `<QuestCard>` | ブロック-level BetterQuesting quest summary card (compat タグ, のみ 登録済み 場合 BetterQuesting です 読み込み済み) | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` は `<GameScene>` 子 タグ. It accepts `controller`, `piece`, `facing`,
`rotation`, `flip`, と `channel` 属性, と も 対応します StructureLib 既定値 子 タグ:
`<Tier>`, `<Channel>`, `<Facing>`, `<Rotation>`, `<Flip>`, `<Orientation>`,
`<GregTechActiveController>`, と `<GregTechPlaceHatches>`.

## タグの詳細

### `<a>`

Acts like HTML-style anchor タグ:

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` できます なる 相対, rooted, 明示的な `modid:path`, または HTTP/HTTPS
- `title` になります  ツールチップ
- `name` inserts a ページ anchor 対象

### `<br>`

GuideNH も 対応します MDX break タグ とともに 浮動小数点数 clearing:

````md
Text before.<br clear="all" />Text after.
````

Accepted `clear` 値:

- `none`
- `left`
- `right`
- `all`

### `<kbd>`, `<sub>`, と `<sup>`

GuideNH 実行時 対応します focused subset of lowercase documentation タグ 用 inline 使用:

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

作成します collapsible 実行時 ブロック とともに summary 行.  `<summary>` 行 対応します 通常
inline Markdown/タグ コンテンツ, と  本文 できます hold ordinary テキスト plus arbitrary ブロックタグ 例として
`<BlockImage>`, `<FloatingImage>`, `<GameScene>`, 表, チャート, と レイアウト containers.
場合 `height` 設定されている場合, のみ  本文 scrolls;  summary 行 と outer frame stay 固定.

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>More <ItemImage id="minecraft:diamond" /></summary>

既定で非表示になる本文と[通常のページリンク](./index.md)。

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

属性:

- `open` — starts expanded 場合 present
- `width` — 推奨 outer 幅 で pixels
- `height` — 推奨 本文 viewport 高さ で pixels; overflow になります scrollable で-game と で site export
- `wrap` — 対応します  usual ブロック embedding modes 例として `square`, `tight`, と `through`
- `align` — `left`, `center`, または `right`; 場合 combined とともに floating wrap モード,  whole details ブロック floats

### `<ContentTabs>`

Groups alternative リッチ コンテンツ under independent tabs. のみ 直接 `<Tab>` 子要素 です 有効.  container itself できます も 描画 quote-style heading 行 上に  tabs, 一致する  visual 言語 of Markdown callouts.

````mdx
<ContentTabs
  title="Implementation Options"
  icon="</>"
  color="#4f8cff"
  default="Java"
>
  <Tab title="Java">
    ```java
    System.out.println("Hello GuideNH");
    ```
  </Tab>
  <Tab title="Scene">
    <BlockImage id="minecraft:crafting_table" />
  </Tab>
</ContentTabs>
````

- `default` 一致します  最初の tab whose `title` 一致します 正確に
- `defaultIndex` です zero-based と wins 上 `default` 場合 両方 です present
- `color` optionally 上書きします  左 accent 行 と 選択済み-tab 強調表示 とともに `#RRGGBB` または `#AARRGGBB`
- `title` adds an 任意の プレーン-テキスト heading 上に  tab strip
- `icon`, `iconPng` / `icon_png`, と `iconItem` / `icon_item` 使用  同じ heading icon semantics as Markdown quote-style callouts
- 無効 子要素 または 無効 defaults 描画 表示 author-facing errors

### `<FileTree>`

描画 ディレクトリ-style outline とともに real connector 行 drawn から  prefix glyphs on 各 行. 両方 Unicode box-drawing (`│ ├ └ ─`) と ASCII (`| +-- \-- ` / four spaces) forms です accepted と 可能性があります なる mixed. Payload テキスト 対応します  usual inline Markdown (リンク, **bold**, `code`, …), と those リンク です clickable 両方 で-game と で  built-で site export.  同じ コンテンツ できます も なる written as fenced ` ```tree ` or ` ```filetree ` ブロック.

````md
<FileTree indent="14" gap="0">
project
├── src
│   ├── **main**
│   │   └── [App.java](./index.md)
│   └── *test*
└── `README.md`
</FileTree>
````

任意の per-行 icons です introduced by leading directive on  payload:

- `{:icon=Text}` — short テキスト ラベル (単一の または double quotes 任意の)
- `{:iconPng=path/to/file.png}` — PNG アセット 解決された against  現在の ページ
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft アイテム icon.  任意の `meta` segment は damage 値 (または `*` 用 wildcard); an 任意の trailing `:{snbt}` ブロック carries SNBT へ attach へ  stack.

````md
```filetree
ワールド
|-- {:iconItem=minecraft:grass} grass biome
|   \-- {:icon=Tree} oak forest
\-- {:iconPng=test1.png} sample アセット
```
````

属性:

- `indent` — pixels per depth level (既定値 `14`)
- `gap` — extra pixels 間 行 (既定値 `0`)

### 実行時 Blockquotes

通常 Markdown blockquotes 描画 at 実行時 とともに 左 accent 行. GitHub alert 構文 です 対応:

````md
> [!NOTE]
> Alert body
````

GuideNH も 対応します a 実行時-のみ カスタム directive on  最初の quoted 行:

````md
> {: title="Custom Quote" color="#638ef1" icon="i" }
> Body text
````

対応 directive キー:

- `title`
- `color`
- `icon` 用 プレーン テキスト symbols
- `iconItem` 用 an `ItemStack` id
- `iconPng` 用 a ガイド アセット png パス

のみ 1 つ icon ソース する必要があります なる 指定された.

### `<Color>`

使用 いずれか symbolic 色 id または 明示的な hex 値:

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

Use `<Spoiler>` for inline hidden text. The content stays rich-text aware, so nested markdown and runtime inline tags
such as `<Color>` still render correctly after reveal.

```mdx
<Spoiler>非表示 **bold** テキスト とともに <Color color="#55ccff">tint</Color>.</Spoiler>
<Spoiler>[Anchor Link](#headings) 引き続き behaves like 通常 hoverable リンク 場合 revealed.</Spoiler>
```

````

規則:

- `id` と `color` です mutually exclusive で practice; 提供 1 つ
- `color` accepts `#RRGGBB`, `#AARRGGBB`, または `transparent`

### `<Tooltip>`

作成します underlined テキスト that opens リッチ コンテンツ ツールチップ on ホバー.

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

もし `label` 省略された場合,  trigger テキスト defaults へ `tooltip`.

### `<SoundLink>` と Sound Action リンク

`<SoundLink>` 描画 リッチ inline コンテンツ that plays sound 場合 clicked. It しません navigate,
と its カスタム クリック sound replaces  通常 ガイド クリック sound 用 that クリック.

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **Start machine**
</SoundLink>

&[Start machine](sound:guidenh:machine.start)
&[Use a sound file](sound-src:guidenh:sounds/machine/start.ogg)
````

Sound 属性:

- `sound` は sound event id 例として `modid:event.name`
- `src` 点 at an `.ogg` ファイル; `modid:sounds/machine/start.ogg` になります `modid:machine.start`
- `volume` defaults へ `1.0`
- `pitch` defaults へ `1.0`
- `cooldown` です milliseconds 間 repeated plays, 既定値 `250`
- `radius` と `minVolume` control 画面空間 attenuation 場合 使用されます で シーン

### `<PlayerName>`

Inserts  現在の Minecraft session username:

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

Looks up keybinding by id または action と 描画  player's 現在の bound キー 名前.

Accepted ids:

-  binding 説明 id, 例として `key.jump` または `key.guidenh.open_guide`
-  legacy `category.description` form, 例として `key.categories.movement.key.jump`

例:

````md
Press <KeyBind id="key.jump" /> to jump.
Attack with <KeyBind action="key.attack" />.
````

### MDX コメント

GuideNH ignores MDX コメント で ページ コンテンツ:

````md
表示されるテキスト。{/* 非表示のインラインコメント */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH も ignores 明示的な `<Comment>` タグ:

````md
表示されるテキスト。<Comment>これは描画されません。</Comment> 引き続き表示されます。
````

### `<ItemImage>`

Shows inline アイテム icon.

| 属性 | 意味 |
| --- | --- |
| `ore` | ore dictionary 名前;  最初の 一致 wins |
| `id` | アイテム reference 使用されます 場合 `ore` です absent |
| `nbt` | 任意の SNBT アイテム データ; merged onto 任意の inline SNBT で `id` |
| `scale` | 浮動小数点数, 既定値 `1` |
| `noTooltip` | truthy 文字列 または 空 属性 suppresses ツールチップ (legacy; prefer `showTooltip`) |
| `showTooltip` | 真偽値, 既定値 `true`; `false` suppresses  ホバー ツールチップ |
| `showIcon` | 真偽値, 既定値 `true`; `false` hides  アイテム icon graphic |
| `label` | `left` または `right` — shows  アイテム 表示 名前 as テキスト on  specified 側 of  icon; omit 用 いいえ ラベル |
| `format` | format pattern 用  ラベル テキスト; 対応します Markdown-style wrappers (`**bold**`, `*italic*`, `~~strike~~`, `__underline__`, `^^wavy^^`, `::dotted::`) とともに 任意の `%s` placeholder 用  アイテム 名前; 既定値 (いいえ 属性) 描画  名前 で italic |
| `yOffset` | 整数 pixel offset 上書き 用  **icon** at scale `1`; しません affect  ラベル テキスト |
| `labelYOffset` | 整数 pixel offset 上書き 用  **ラベル テキスト** at scale `1`; しません affect  icon |

注記:

- `ore` takes precedence 上 `id` 場合 両方 です 指定された
- もし GregTech です installed,  選択済み ore 一致 です passed 通じて `GTOreDictUnificator.setStack(...)`
- `label` requires at least 1 つ of `showIcon` または `label` へ produce 表示 出力; setting 両方 `showIcon="false"` と omitting `label` 描画 nothing
- `format` のみ 適用します 場合 `label` 設定されている場合; もし `format` has いいえ `%s`,  literal format テキスト です 使用されます as  ラベル
- inline SNBT で `id` remains 対応; 場合 両方 forms です present,  standalone `nbt` 属性 です merged 最後の と 上書きします conflicting キー

例:

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"Custom Diamond"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<ItemLink>`

作成します a テキスト リンク using  アイテム's 表示 名前 と アイテム ツールチップ. もし `item_ids` 点 へ a ガイドページ, クリック navigates へ it. `ore` できます なる 使用されます へ 解決  表示 stack から  最初の ore dictionary 一致 代わりに of a 固定 registry id.

| 属性 | 既定値 | 意味 |
| --- | --- | --- |
| `id` | — | アイテム registry id, e.g. `minecraft:compass` または `minecraft:wool:1` |
| `ore` | — | ore-dictionary 名前; 使用します  最初の 一致する アイテム stack |
| `linksTo` | *(auto)* | 上書きします  リンク 対象; accepts a ページ ID とともに 任意の `#anchor`, e.g. `./crafting.md#usage` または `#usage`; 場合 omitted  対象 です 解決された から `item_ids` / `ore_ids` index |
| `showTooltip` | `true` | 設定します へ `false` へ suppress  ホバー ツールチップ; `noTooltip` は legacy alias |
| `showIcon` | *(none)* | `left` または `right` (または 任意の truthy 値 → 右) — 描画  アイテム icon beside  リンク テキスト; omit へ 表示 テキスト のみ |
| `scale` | `1.0` | 表示 scale 用  任意の アイテム icon; has いいえ effect 場合 `showIcon` 省略された場合 |

例:

````md
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />
<ItemLink id="minecraft:diamond" showIcon="left" scale="2" />
<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />
<ItemLink ore="stickWood" />
<ItemLink id="minecraft:iron_ore" linksTo="./crafting.md#smelting" />
<ItemLink id="minecraft:compass" linksTo="#usage" />
````

### `<CommandLink>`

Sends chat command 場合 clicked.

| 属性 | 意味 |
| --- | --- |
| `command` | 必須, 必要があります 開始 とともに `/` |
| `title` | 任意の ツールチップ heading |
| `close` | 解析済み 真偽値 属性; 現在 解析済み but ない 使用されます へ close  ガイド |

例:

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` と `<Column>`

Flex-style containers 用 ブロック コンテンツ.

| 属性 | 意味 |
| --- | --- |
| `gap` | 整数 gap 間 子要素, 既定値 `5` |
| `alignItems` | `start`, `center`, `end` |
| `fullWidth` | 真偽値 式, 既定値 `false` |
| `width` | 整数 推奨 幅; useful 用 constraining 一覧 行 幅 |

例:

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

へ constrain  幅 of 通常 Markdown lists, wrap them で container:

````md
<Column width="220">
- narrow list item
- another narrow item
</Column>
````

### `<FootnoteList>`

GuideNH 使用します この ブロック タグ internally 場合 実行時 Markdown footnotes です expanded. It できます も なる written 手動で もし needed.

````md
<FootnoteList width="220">
1. First footnote
2. Second footnote
</FootnoteList>
````

### `<ItemGrid>`

描画 compact アイテム grid. 子要素 必要があります なる 未加工 `<ItemIcon>` elements, これらは です 解析済み directly by  grid コンパイラー. 各 子 できます 使用 いずれか `id` または `ore`.

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

描画 non-インタラクティブ 3D 単一の-ブロック シーン.  プレビュー has いいえ シーン 背景, いいえ シーン
buttons, いいえ layer controls, と いいえ 注釈 features, but ホバーすると  ブロック 引き続き shows 
選択 outline と ツールチップ. `ore` 必要があります 解決 へ a ブロック アイテム stack.

| 属性 | 意味 |
| --- | --- |
| `id` | ブロック id; 対応します  通常 `modid:block[:meta][:{snbt}]` spelling |
| `ore` | ore dictionary lookup;  最初の 一致する ブロック アイテム wins |
| `scale` | カメラ ズーム multiplier, 既定値 `4` |
| `float` | legacy flow 浮動小数点数 対応: `left` または `right` |
| `perspective` | `isometric-north-east` (既定値), `isometric-north-west`, または `up` |
| `nbt` | 任意の SNBT tile-entity データ merged onto 任意の inline SNBT から `id` |

注記:

- inline SNBT 内部 `id` です 引き続き accepted 用 compatibility, but `nbt="..."` です  推奨
  authoring form
- 場合 両方 inline SNBT と `nbt` です present,  `nbt` 属性 です merged 最後の と したがって
  上書きします conflicting キー
- GuideNH 1.7.10 しません 対応 modern ブロック-状態 プロパティ 構文 here, so GuideME-style
  `p:<state>` 属性 です intentionally ない 対応

````md
<BlockImage id="minecraft:crafting_table" scale="3" />
<BlockImage ore="logWood" scale="3" perspective="isometric-north-west" />
<BlockImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<FloatingImage>`

参照 [Images And Assets](Images-And-Assets) 用  完全な behavior.

簡単 規則:

- `src` 対応します 相対 paths, rooted paths, と 明示的な `modid:path` texture ids
- `x`, `y`, `width` / `w`, と `height` / `h` define  crop rectangle on  original 画像 と 必要があります すべての なる present
- `scaleX` と `scaleY` resize  cropped 結果 と 対応 independent horizontal / vertical stretching
- `displayWidth` と `displayHeight` 設定します 最終 dimensions で pixels; 1 つ 値 preserves  crop aspect ratio, 中 2 つ 値 許可 stretching
- `displayWidth` / `displayHeight` できません なる combined とともに `scaleX` / `scaleY`
- `wrap="inline"` places  画像 truly inline 内部 テキスト flow; で that モード `align` です ignored
- old コンテンツ that 使用されます `width` / `height` as 最終 表示 サイズ 必要があります なる migrated 手動で

### `<SubPages>`, `<Category>`, と `<Special>`

参照 [Navigation](Navigation) 用 完全な ナビゲーション behavior.

### `<Structure>`

参照 [例s](例s) と [GameScene](GameScene) 場合 deciding whether へ 使用 static structure プレビュー または 完全な 3D シーン.

### `<Mermaid>`

使用されます 用 実行時 Mermaid コンテンツ. 現在の 実行時 対応 です focused on `mindmap`, いずれか inline または 通じて a ページ-相対 `src` import:

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [Index](./index.md)"]
    runtime["Runtime blocks"]

<NodeContent id="runtime">
ランタイムノードには通常のブロックを埋め込めます。

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
```

- `width` と `height` constrain  実行時 viewport box
- 内部  viewport, ドラッグ pans と  mouse wheel zooms
- quoted Mermaid labels 可能性があります 使用 リッチ inline Markdown 例として `**bold**` と ページ リンク
- `<NodeContent id="...">...</NodeContent>` できます なる added as 子要素 of `<Mermaid>` へ 置き換え node 本文 とともに arbitrary 実行時 ブロック

### `<CsvTable>`

使用されます へ 解析 CSV ファイル into a 実行時 表:

````md
<CsvTable src="./markdown-table.csv" />
```

`src` resolves 相対 へ  現在の ページ,  同じ way シーン imports と 通常 アセット リンク do.

任意の 属性:

- `header`
  Defaults へ `true`; 設定します `header={false}` へ 保持します  最初の 行 unbolded
- `widths`
  Comma-separated 整数 幅 hints 例として `widths="120,80"`

例:

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
```

 related fenced 実行時 CSV form も 対応します 一致する metadata:

````md
```csv widths="120,80" header=false
名前,値
iron,42
gold,17
```
````

### `<Latex>`

描画 LaTeX math 数式 using jlatexmath. 場合 使用されます inline (内部 paragraph または テキスト flow), it 描画 as scaled glyph that expands  行 高さ へ fit  数式. 場合 written as its 独自の paragraph (ブロック context), it 描画 centered as a 表示-モード 数式.

| 属性 | 型 | 既定値 | 説明 |
| --- | --- | --- | --- |
| `formula` | 文字列 | *(必須)* | LaTeX ソース 文字列 |
| `color` | `#RRGGBB` または `#AARRGGBB` | `#FFFFFF` | Glyph fill colour |
| `scale` | 浮動小数点数 | `1.0` | 表示 サイズ multiplier applied on 上 of  自動 行-高さ scaling |
| `sourceScale` | 浮動小数点数 | `100.0` | jlatexmath 内部 描画 resolution; higher 値 improve quality at large sizes |
| `tooltip` | 文字列 | *(none)* | プレーン ツールチップ テキスト 表示される on ホバー |
| `showTooltip` | 真偽値 | `false` | 表示  未加工 LaTeX ソース as a ツールチップ on ホバー |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | Inline-のみ. Vertical alignment 内部で  テキスト 行: `baseline` (既定値) aligns  数式's math baseline とともに  テキスト baseline; `top` aligns  数式 上 とともに  行 上; `center` centers it on  テキスト; `bottom` aligns  数式 下 とともに  テキスト 下 |
| `offsetX` | int | `0` | Horizontal pixel offset applied 後 alignment (positive = 右) |
| `offsetY` | int | `0` | Vertical pixel offset applied 後 alignment (positive = down) |

例:

````md
Inline: <Latex formula="E=mc^2" />

Fraction that expands line height: <Latex formula="\frac{a+b}{c-d}" />

Gold colour: <Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

Scaled up: <Latex formula="\pi" scale="1.5" />

With hover tooltip: <Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2}" showTooltip={true} />

Plain custom tooltip: <Latex formula="E=mc^2" tooltip="Energy equals mass times the speed of light squared." />

Rich tooltip:
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **Gibbs free energy**

  - <Latex formula="\Delta H" />: enthalpy change
  - <Latex formula="T\Delta S" />: entropy term
</Latex>

Bottom-aligned (formula bottom matches text bottom): <Latex formula="\frac{a}{b}" valign="bottom" />

Explicit baseline alignment (same as default): <Latex formula="E=mc^2" valign="baseline" />

Top-aligned with an upward nudge: <Latex formula="x^2" valign="top" offsetY="-1" />

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />
````

#### `$$formula$$` shorthand

As convenience you できます 書き込む `$$formula$$` directly で Markdown なしで using  `<Latex>` タグ.
すべての 描画 パラメーター 使用 their defaults (white colour, scale 1.0, いいえ ツールチップ, baseline-aligned).

- **Inline**: `$$formula$$` embedded 内部 paragraph 描画 as inline 数式.
- **表示**: paragraph whose entire コンテンツ です `$$formula$$` (とともに 任意の surrounding whitespace) 描画 as centred 表示-モード ブロック.

````md
Inline shorthand: $$E=mc^2$$ and $$a^2+b^2=c^2$$

Inline fraction: $$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

注記:

-  数式 高さ です calibrated へ  現在の 行 テキスト 高さ. Simple formulas 描画 at テキスト 高さ; taller formulas (fractions, summations, integrals, etc.) expand  enclosing 行 高さ 自動的に.
- `valign` のみ 適用します へ inline formulas. 表示-モード (ブロック-level) formulas です 常に centered horizontally; 使用 `offsetY` へ shift them vertically 内部で  ブロック.
- `color` defaults へ white (`#FFFFFF`). 使用 `#AARRGGBB` format 用 semi-transparent fill.
- `sourceScale` のみ affects 描画 sharpness, ない  displayed サイズ. 値 below `16` です clamped へ `16`.
- ツールチップ priority です: リッチ 子 Markdown コンテンツ, then `tooltip="..."`, then `showTooltip={true}` 未加工 ソース フォールバック.
- 子 ツールチップ コンテンツ です コンパイル済み as regular ガイド Markdown, so it できます 含める bold テキスト, lists, リンク, アイテム タグ, と nested `<Latex>` formulas.
-  `$$formula$$` shorthand 常に 使用します 既定値 パラメーター. 使用  `<Latex>` タグ 用 カスタム colour, scale, alignment または ツールチップ.

### シーン 実行時 タグ

これらの タグ のみ 動作します 内部 `<GameScene>` / `<Scene>`:

| タグ | Purpose | キー 属性 |
| --- | --- | --- |
| `<ImportStructure>` | import 外部 SNBT/NBT structure アセット | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | import StructureLib multiblock by controller id | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | 削除 すでに-placed ブロック that 一致 a ブロック matcher | `id` |
| `<BlockAnnotationTemplate>` | stamp  同じ 子 注釈 onto すべての 一致する placed ブロック | `id` |

参照 [GameScene](GameScene) 用 シーン import/removal behavior と [Annotations](Annotations) 用 注釈 template 規則.


## チャート

`<ColumnChart>`, `<BarChart>`, `<LineChart>`, `<PieChart>`, と `<ScatterChart>` です インタラクティブ チャート ブロック. すべての チャート share  following 一般的な 属性:

| 属性 | 説明 | 既定値 |
| --- | --- | --- |
| `title` | チャート タイトル | none |
| `width` / `height` | 明示的な サイズ | 320 / 200 |
| `background` / `border` | 背景 と 境界線 colors (`#RGB`, `#RRGGBB`, `#AARRGGBB`, `0x...`) | dark grey |
| `titleColor` / `labelColor` | タイトル と 値-ラベル colors | light grey |
| `legend` | 凡例 位置: `none` / `top` / `bottom` / `left` / `right` | `top` |
| `labelPosition` | 値-ラベル 位置: `none` / `inside` / `outside` / `above` / `below` / `center` | `none` |
| `cornerLegend` | 内部 plot 凡例 位置: `none` / `topRight` / `topLeft` / `bottomRight` / `bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | Maximum 内部 凡例 box サイズ | `120` / `64` |
| `cornerLegendBackground` | 内部 凡例 背景 色 | `#AA111922` |

Cartesian チャート (列 / Bar / 行 / Scatter) additionally 受け付けます axis 属性 `xAxisLabel`, `xAxisMin`, `xAxisMax`, `xAxisStep`, `xAxisUnit`, `xAxisTickFormat` と  一致する `yAxis*` 設定します, plus `showXGrid={true}` / `showYGrid={true}` へ toggle gridlines.

子要素:

* `<Series name="..." color="#..." data="10,20,30"/>` 用 category-based チャート (列 / Bar / categorical 行).
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` 用 numeric X (行 `numericX={true}`, Scatter).
* `<Slice name="..." value="..." color="#..."/>` 用 `<PieChart>` のみ.

場合 `color` 省略された場合 on a `<Series>` または `<Slice>`, GuideNH cycles 通じて built-で 16-色 palette.

`<Series>` と `<Slice>` も 受け付けます  following 任意の icon / ツールチップ 属性:

* `icon="modid:item"` (同じ 構文 as `<ItemImage>`'s `id`, 可能性があります 含める `@meta` と inline NBT JSON) — binds an `ItemStack` へ  エントリ;  凡例 swatch になります  アイテム icon と ホバーすると  データ 点 shows  vanilla アイテム ツールチップ とともに  チャート 説明 appended at  終端.
* `iconImage="images/foo.png"` — 使用 PNG アセット as  凡例 swatch (overridden by `icon`).
* `tooltip="..."` — extra free-form テキスト appended へ  ツールチップ (使用 `\n` 用 multi-行).

例:

```mdx
<PieChart title="Output share">
  <Slice name="Iron" value="40" icon="minecraft:iron_ingot" tooltip="From smelting" />
  <Slice name="Gold" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

Extra 属性: `categories` (X-axis または Y-axis labels, comma separated), `barWidthRatio` (既定値 0.7). `<BarChart>` puts  categories on  Y-axis と 値 on  X-axis.

#### Combo extensions

`<ColumnChart>` と `<BarChart>` 受け付けます 2 つ extra 子 element types so 複数の チャート styles できます share 1 つ plot area:

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — drawn as a 折れ線 overlay on 上 of  bars. 各 行 点 sits at  cluster 中央 of  一致する category index;  overlay shares  host チャート's 値 axis. You できます declare 複数の `<LineSeries>` へ overlay several trends.
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — small pie チャート drawn 内部 1 つ of  four corners (`topRight`, `topLeft`, `bottomRight`, `bottomLeft`) of  plot area. Its `<Slice>` 子要素 share  同じ 構文 as で `<PieChart>`.

```mdx
<ColumnChart title="Quarterly output" categories="Q1,Q2,Q3,Q4">
  <Series name="Iron"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="Gold"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="Total" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="Total share">
    <Slice name="Iron" value="225" color="#a0a0a0"/>
    <Slice name="Gold" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

### `<LineChart>`

Extra 属性: `numericX={true}` へ enable numeric X-axis (子要素 必要があります 使用 `points`); `showPoints={false}` hides 点 markers.  hovered 点 です pushed outward by 2px along  曲線 通常, enlarged, と outlined;  adjacent 行 segments thicken by 1px.

`<LineChart>` と `<ScatterChart>` できます 表示 compact 凡例 内部  plot area とともに `cornerLegend="topRight"` または another corner. エントリ 使用 existing 系列 names と colors.

### `<PieChart>`

Extra 属性: `startAngle` (既定値 `-90`, i.e. 12 o'clock); `clockwise={false}` へ reverse direction.  hovered スライス pops outward 4px along its bisector.

### `<ScatterChart>`

描画 点 のみ; `<Series>` 必要があります 使用 `points`.  X-axis です 常に numeric.

## 関数グラフ

`<FunctionGraph>` と  単一の-曲線 shorthand `<Function>` 描画 an インタラクティブ Desmos-style panel.  同じ panel です も 利用可能 通じて a ` ```funcgraph ` fenced コードブロック; 参照してください  実行時 [Markdown sample](resourcepack/assets/guidenh/guidenh/_en_us/markdown.md) 用 完全な walkthrough.

Panel 属性 (accepted by  container,  shorthand, と  fence header alike):

- `width` / `height` (defaults `320` x `220`)
- `title`, `background`, `border`, `axisColor`, `gridColor`
- `showGrid` / `showAxes` (既定値 `true`)
- `xRange="a..b"` (または `xMin` / `xMax` separately), `xStep` 用 tick spacing; 同じ 用  Y axis
- `xLabel` / `yLabel` 追加します Excel-style axis titles below と 上に  plot respectively. これらは 対応 inline `$$...$$` LaTeX; `domain="a..b"` は legacy alias 用 `xRange` 場合 いいえ 明示的な X range です present
- `quadrants="1,2,3,4"` または `quadrants="all"` へ force  表示 quadrants; omit へ 開始 で quadrant 1 とともに auto-expansion 場合 sampled `y < 0`
- `cornerLegend`, `cornerLegendWidth`, `cornerLegendHeight`, と `cornerLegendBackground` 表示 compact 凡例 内部  plot area using non-空 曲線 labels

曲線 子要素 (`<Plot>` / `<Function>`):

- `expr="..."` &mdash;  式. Operators `+ - * / % ^`, postfix factorial `!` (gamma-extended), `|x|` 絶対 値, `√` / `sqrt` / `∛` / `cbrt`, 暗黙的な multiplication, と  constants `pi`, `tau`, `e`, `phi` です 対応. Built-で calls cover  標準 trig/log/exp/rounding family plus 2 つ-arg `atan2`, `min`, `max`, `pow`, `hypot`, `mod`.
- `inverse={true}` evaluates  式 as `x = f(y)` と rotates  曲線.
- `domain="a..b"` (x bounds shorthand) または comma-separated clauses 例として `x>=0, x<5`.
- `color`, `label`. 任意の 曲線 とともに non-空 ラベル です 自動的に listed で 凡例 描画される just below  panel: small 色 swatch followed by  ラベル, とともに エントリ flowing 左-へ-右 と wrapping onto new 行 場合  次の エントリ would ない fit.
- `tooltip` adds プレーン テキスト below  computed ツールチップ. `showFunction` と `showValues` 既定値 へ `true`; 設定します いずれか へ `false` へ 非表示  描画される 方程式 または live `(x, y)` 値 respectively. A `<Plot>` / nested `<Function>` とともに `expr` 可能性があります 含む Markdown と GuideNH タグ as リッチ ツールチップ 本文.  順序 です 常に ラベル, 描画される 方程式, live 値, `tooltip` テキスト, then リッチ 子 コンテンツ; omitted または 無効 computed フィールド です skipped で that 順序.
- `pointEveryX="step"` adds 生成された 点 markers at regular x intervals on that 曲線.
- `pointEveryY="step"` adds 生成された 点 markers where  曲線 intersects regular y intervals, using bounded 検索.
- `autoPointLabel="none|x|y|xy"` controls 生成された 点 labels; 既定値 です `none`.
- `autoPointColor="#..."` 上書きします  生成された 点 色; omitted 意味：inherit  曲線 色.

Marked 点 (`<Point>`):

- 明示的な: `x="..."` と `y="..."`.
- Plot-anchored: `plot="N"` plus `atX="v"` または `atY="v"` ( 実行時 bisects on  plot's x-domain へ find  一致する `x`).
- 任意の `color`, `label`.

Interaction: ホバー 曲線 へ 強調表示 it; press と hold へ scrub a 点 along  曲線.  ツールチップ starts とともに `label` 場合 supplied, then  方程式 と live `(x, y)` 値 場合を除き their switches です 無効; it stays anchored 上に  点 と flips below 場合 there です いいえ headroom.

## BetterQuesting 互換タグ

`<QuestLink>` と `<QuestCard>` です のみ 登録済み 場合  BetterQuesting mod です 読み込み済み. これらは です documented で detail on  [Mod Compatibility](Mod-Compatibility) ページ;  summary below covers  most 一般的な usage.

### `<QuestLink>`

Inline リンク へ BetterQuesting quest. クリック opens  quest 内部  BetterQuesting GUI, 場合を除き  quest id です も present で  現在の ガイド's `quest_ids` frontmatter — で that case  リンク navigates へ that ページ 代わりに.

| 属性 | 意味 |
| --- | --- |
| `id` | 必須 BetterQuesting quest id; accepts canonical UUID strings と compact Base64 ids |
| `text` | 任意の 上書き 用  displayed テキスト |
| `show_tooltip` | 任意の 真偽値 (既定値 `true`); 設定します へ `false` へ suppress  quest-説明 ツールチップ. `showTooltip` です accepted as alias |

表示状態 behavior です decided per player at コンパイル time:

- 表示 / completed quests 描画 as clickable リンク (completed quests です tinted green と append a `✓` mark)
- locked but non-非表示 quests 引き続き 描画 as clickable quest リンク so これらは できます open  BetterQuesting quest 画面 または  indexed ガイドページ
- 非表示 / secret quests 描画 as darker italic placeholder using `guidenh.compat.bq.hidden`
- 不明な quest ids 描画 as red placeholder using `guidenh.compat.bq.missing`

例:

````md
See <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" /> for the next step.
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="Stage 2 quest" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
See <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="Compact quest id example" /> after that.
````

### `<QuestCard>`

ブロック-level summary card 用 BetterQuesting quest. 描画  quest タイトル とともに  同じ 状態-aware styling as `<QuestLink>`, plus  quest 説明 as 本文 paragraph 場合  quest です 表示 へ  player.

| 属性 | 意味 |
| --- | --- |
| `id` | 必須 BetterQuesting quest id; accepts canonical UUID strings と compact Base64 ids |
| `show_desc` | 任意の 真偽値 (既定値 `true`); 設定します へ `false` へ suppress  説明 本文 |
| `show_tooltip` | 任意の 真偽値 (既定値 `true`); 設定します へ `false` へ suppress  quest-説明 ツールチップ on  clickable タイトル. `showTooltip` です accepted as alias |

 accent 色 of  card 境界線 follows  quest 状態: green 用 completed, gray 用 locked / 非表示, red 用 不足, と  標準 リンク 色 用 表示 quests.  タイトル remains clickable 用 表示, completed, と locked-but-non-非表示 quests.

例:

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
