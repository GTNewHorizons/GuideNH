# 標籤參考

本頁說明指南作者可使用的內建標籤。

## 使用規則

- 標籤可以出現在塊級上下文或行內上下文，具體取決於對應編譯器。
- 頁面內容支援使用 `{/* ... */}` 形式的 MDX 註釋，執行時解析器會忽略這些註釋。
- MDX 屬性統一使用駝峰，例如 `showTooltip`、`showDesc` 及 `fromNbt`。
- 無效標籤或無效屬性不會靜默失敗，而是以內聯指南錯誤的形式顯示。
- 配方和 3D 場景這類大型功能標籤會在獨立頁面中說明：
  - [配方](Recipes)
  - [遊戲場景](GameScene)
  - [註解](Annotations)

## 行內與流式標籤

| 標籤 | 用途 | 關鍵屬性 |
| --- | --- | --- |
| `<a>` | 內部/外部連結，以及可選錨點名 | `href`, `title`, `name` |
| `<br>` | 換行 | `clear="none\|left\|right\|all"` |
| `<wbr>` | 詞內可選換行點 | 無 |
| `<kbd>` | 鍵位風格行內強調 | 無 |
| `<sub>` | 較小的下標風格行內文字 | 無 |
| `<sup>` | 較小的上標風格行內文字 | 無 |
| `<Color>` | 彩色行內文字 | `id` 或 `color` |
| `<Amount>` | 格式化數字與單位 | `value`, `unit`, `format`, `decimals` |
| `<Spoiler>` | 懸停後顯示的行內黑幕文字 | 無 |
| `<Tooltip>` | 帶 Markdown/標籤子內容的富懸浮提示 | `label` |
| `<SoundLink>` | 可點選的富文字音效觸發器 | `sound` 或 `src`，`volume`，`pitch`，`cooldown` |
| `<mark>` | 行內高亮文字；等價於 `==text==`，並可自定義顏色 | `color` |
| `<PlayerName>` | 插入當前玩家使用者名稱 | 無 |
| `<KeyBind>` | 插入按鍵繫結顯示名 | `id` 或 `action` |
| `<ItemImage>` | 行內物品圖示 | `id`, `ore`, `nbt`, `scale`, `noTooltip`, `showTooltip`, `showIcon`, `label`, `format`, `yOffset`, `labelYOffset` |
| `<ItemLink>` | 物品 tooltip + 可選導航連結 | `id` 或 `ore`，`linksTo`，`showTooltip`，`noTooltip`，`showIcon` |
| `<FluidImage>` / `<FluidLink>` | 原生流體圖示與頁面導航 | `id`, `amount`, `nbt`, `scale`, `showTooltip`, `noTooltip`, `showIcon`, `label`, `linksTo` |
| `<ResearchLink>` / `<BindingLink>` | 研究或自定義內容連結 | `id`, `type`（通用標籤）, `text`, `target`, `showTooltip` |
| `<ResearchIcon>` / `<BindingIcon>` | 研究或自定義內容圖示，也可用於塊級上下文 | `id`, `type`（通用標籤）, `scale`, `showTooltip` |
| `<CommandLink>` | 可點選的聊天命令連結 | `command`, `title`, `close` |
| `<Latex>` | LaTeX 數學公式；在流式上下文中行內渲染，在塊級上下文中居中顯示為獨立公式塊 | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting 任務連結，按任務狀態自動調整樣式（相容標籤，僅當 BetterQuesting 已載入時註冊） | `id`, `text`, `showTooltip` |

行內 Markdown 也支援音效動作連結：

````md
&[启动机器](sound:guidenh:machine.start)
&[播放文件音效](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## 塊級標籤

| 標籤 | 用途 | 關鍵屬性 |
| --- | --- | --- |
| `<div>` | 透傳塊包裝器 | 無 |
| `<ContentTabs>` | 將可替代的富內容分組到獨立標籤頁中 | `title`、`color`、`background`、`icon`、`iconPng`、`iconItem`, `iconFluid`、`default`、`defaultIndex` |
| `<Tab>` | `<ContentTabs>` 內的單個內容面板 | `title` |
| `<details>` | 可摺疊執行時塊 | `open`、`width`、`height`、`wrap`、`align` |
| `<FileTree>` | 目錄樹式大綱（帶連線線） | `indent`、`gap` |
| `<Row>` | 橫向 flex 佈局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | 縱向 flex 佈局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | 執行時 Markdown 腳註使用的限寬腳註容器 | `width` |
| `<ItemGrid>` | 物品與流體圖示的緊湊網格 | 支援 `<ItemIcon id="..."/>`、`<ItemIcon ore="..."/>` 和 `<FluidIcon id="..."/>` 混排 |
| `<BlockImage>` | 非互動式的 3D 單方塊預覽 | `id`, `ore`, `meta`, `nbt`, `scale`, `width`, `height`, `perspective`, `wrap`, `align` |
| `<FloatingImage>` | 支援浮動或真正行內放置的裁剪圖片塊 | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | 導航子頁面列表 | `id`, `alphabetical` |
| `<Category>` | 分類頁面列表 | `name`, `rows` |
| `<Special>` | 內建 MediaWiki 特殊頁面列表 | `name`, `rows` |
| `<Structure>` | 2.5D 等軸方塊佈局預覽 | `width`, `height` |
| `<Mermaid>` | 執行時 Mermaid 圖匯入/內聯 | `src` |
| `<CsvTable>` | 執行時 CSV 檔案匯入表格 | `src`, `header`, `widths` |
| `<ColumnChart>` | 簇狀柱形圖 | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | 橫向條形圖 | 同 `<ColumnChart>` |
| `<LineChart>` | 折線圖（類別或數值 X 軸） | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*`, `cornerLegend` |
| `<PieChart>` | 餅圖 | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY 散點圖 | `xAxis*`, `yAxis*`, `legend`, `labelPosition`, `cornerLegend` |
| `<FunctionGraph>` | Desmos 風格的多曲線函式圖 | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes`, `cornerLegend` |
| `<Function>` | 單曲線簡寫，等價於只含一個 `<Plot>` 的 `<FunctionGraph>` | `expr`，及全部 `<FunctionGraph>` 面板屬性 |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | 配方渲染器 | 詳見 [配方](Recipes) |
| `<GameScene>`, `<Scene>` | 3D 指南遊戲場景 | 詳見 [遊戲場景](GameScene) |
| `<ResearchCard>` / `<BindingCard>` | 研究或自定義內容摘要卡片 | `id`, `type`（通用標籤）, `title`, `target`, `showDesc`, `showIcon`, `showParents` |
| `<QuestCard>` | 塊級 BetterQuesting 任務摘要卡片（相容標籤，僅當 BetterQuesting 已載入時註冊） | `id`, `showDesc`, `showTooltip` |

`<ImportStructureLib>` 是 `<GameScene>` 的子標籤。它支援 `controller`、`piece`、`facing`、
`rotation`、`flip`、`tier`、`channel`、`channelName`、`value`、`locked` 屬性，也支援用於設定 StructureLib 預設值的子標籤：
`<Tier>`、`<Channel>`、`<Facing>`、`<Rotation>`、`<Flip>`、`<Orientation>`、
`<GregTechActiveController>`、`<GregTechPlaceHatches>`。

## 標籤細節

### `<a>`

其行為類似 HTML 風格錨點標籤：

````md
<a href="subpage.md" title="Go to subpage">Open Subpage</a>
<a href="https://example.com">External Link</a>
<a name="details" />
````

- `href` 可以是相對路徑、根路徑、顯式 `modid:path`，或 HTTP/HTTPS 連結
- `title` 會作為 tooltip 使用
- `name` 會插入一個頁面錨點目標

### `<br>`

GuideNH 也支援帶浮動清理能力的 MDX 風格換行標籤：

````md
Text before.<br clear="all" />Text after.
````

可接受的 `clear` 值：

- `none`
- `left`
- `right`
- `all`

### `<wbr>`

在單詞內部標記一個可選的換行位置，作用與 HTML 的 `<wbr>` 元素相同。它不顯示任何內容，也不佔寬度；只有整個單詞放不下時才會在這裡換行，例如低解析度下窄表格單元格里的長名稱或長數字。

````md
| Block | Rate |
| --- | --- |
| Cobble<wbr>stone/s | 11,<wbr>520 |
````

`<wbr>` 和 `<wbr />` 兩種寫法都可以，標籤不接受屬性。與插入空格不同，單詞放得下時不會留下任何間隙。Site Export 和 Web Editor 會輸出原生的 `<wbr>`。

### `<kbd>`、`<sub>` 與 `<sup>`

GuideNH 執行時支援一小組常見的小寫文件標籤：

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

用於建立可摺疊的執行時內容塊。`<summary>` 行支援常規行內 Markdown/標籤內容，正文則可以放普通文字與任意塊級標籤，例如 `<BlockImage>`、`<FloatingImage>`、`<GameScene>`、表格、圖表或佈局容器。設定 `height` 時，只有正文區域滾動，標題行和外框保持固定。

`<summary>`、正文和 `</details>` 之間的空行不是必需的。請使用複數標籤 `<details>`，不支援 `<detail>`。標題和正文都支援行內 Markdown、`<ItemLink>`、`<ItemImage>` 等行內標籤。連續的 `<details>` 塊各自獨立，不會吞掉後續內容。

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>更多 <ItemImage id="minecraft:diamond" /></summary>

默认折叠的正文文本，里面可以放[普通页面链接](./index.md)。

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

屬性：

- `open` — 存在時預設展開
- `width` — 外層首選寬度，單位畫素
- `height` — 正文視口首選高度，單位畫素；超出部分會在遊戲內和站點匯出中變為可滾動區域
- `wrap` — 支援常見塊嵌入模式，例如 `square`、`tight`、`through`
- `align` — `left`、`center` 或 `right`；與浮動型 `wrap` 搭配時會讓整個 details 塊浮動

### `<ContentTabs>`

將可替代的富內容分組到獨立標籤頁中。`<ContentTabs>` 只接受直接的 `<Tab>` 子標籤。容器本身也可以在標籤頁上方渲染一個類似 Markdown 標記/引用塊的標題行。

````mdx
<ContentTabs
  title="实现方案"
  icon="</>"
  color="#4f8cff"
  background="#20252d"
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

- `default` 會匹配第一個 `title` 完全相同的標籤頁
- `defaultIndex` 使用從 `0` 開始的下標，並且在同時出現時優先順序高於 `default`
- `color` 可選，用 `#RRGGBB` 或 `#AARRGGBB` 覆蓋左側強調線與選中標籤的高亮顏色
- `background` 可選，用 `#RRGGBB`、`#AARRGGBB` 或 `transparent` 覆蓋標籤容器背景；省略時保留預設灰色引用塊背景
- `title` 會在標籤欄上方增加一個可選的純文字標題
- `icon`、`iconPng` / `iconPng`、`iconItem` / `iconItem` 與 Markdown 標記/引用塊標題的圖示語義完全一致
- 非法子節點或非法預設值會渲染為面向作者的可見錯誤

### `<FileTree>`

渲染目錄樹式大綱，並依據每行字首符號繪製真實的連線線。字首同時支援 Unicode 框線（`│ ├ └ ─`）與 ASCII 形式（`| +-- \-- ` / 4 個空格），可任意混用。每行的文字部分支援常規行內 Markdown（連結、**加粗**、`代码` 等），這些連結在遊戲內和內建站點匯出中都可以點選。等價語法是 ` ```tree ` / ` ```filetree ` 圍欄程式碼塊。

窄欄中會自動壓縮層級縮排，為文字保留可讀寬度。長路徑優先在 `/`、`\`、`_`、`-` 後換行，連續長檔名可在字元間換行；續行與名稱起點對齊，圖示和分支連線線對齊首行。

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

可在每行開頭加入可選圖示指令：

- `{:icon=文本}` — 簡短的文字圖示
- `{:iconPng=path/to/file.png}` — PNG 資源（按當前頁面解析路徑）
- `{:iconItem=modid:item_id[:meta][:{snbt}]}` — Minecraft 物品圖示。可選 `meta` 為損詞值（`*` 表示通配）；可選末尾 `:{snbt}` 能附加 SNBT 到物品。

````md
```filetree
world
|-- {:iconItem=minecraft:grass} 草地生物群系
|   \-- {:icon=树} 橡木森林
\-- {:iconPng=test1.png} 示例资源
```
````

屬性：

- `indent` — 每層縮排畫素數（預設 `14`）
- `gap` — 行間額外畫素數（預設 `0`）

### `<Color>`

可使用符號顏色 id，或顯式十六進位制顏色值：

````md
<Color id="RED">Symbolic red</Color>
<Color color="#FF00D2FC">ARGB or RGB color</Color>

### `<Spoiler>`

`<Spoiler>` 用于行内隐藏文本。其内容仍然保留富文本能力，因此内部嵌套的 Markdown 和 `<Color>`
这类运行时行内标签会在显示后正常渲染。

```mdx
<Spoiler>隐藏的 **加粗** 文本，以及 <Color color="#55ccff">染色</Color> 内容。</Spoiler>
<Spoiler>[锚点链接](#标题) 在黑幕里显示后，依然会像普通链接一样正常悬停和显示下划线。</Spoiler>
```

````

規則：

- `id` 和 `color` 在實際使用中應二選一
- `color` 支援 `#RRGGBB`、`#AARRGGBB` 或 `transparent`

### `<Tooltip>`

建立帶下劃線的文字，並在懸停時顯示富內容 tooltip。

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

若省略 `label`，預設觸發文字為 `tooltip`。

### `<SoundLink>` 與音效動作連結

`<SoundLink>` 會渲染一段可點選的富文字內容，點選後播放音效。它不會進行頁面導航，
並且本次點選會使用自定義音效替代指南預設點選音效。

````md
<SoundLink sound="guidenh:machine.start" volume="0.8" pitch="1.0">
  **启动机器**
</SoundLink>

&[启动机器](sound:guidenh:machine.start)
&[使用音效文件](sound-src:guidenh:sounds/machine/start.ogg)
````

音效屬性：

- `sound` 是音效事件 id，例如 `modid:event.name`
- `src` 指向 `.ogg` 檔案；`modid:sounds/machine/start.ogg` 會轉換為 `modid:machine.start`
- `volume` 預設 `1.0`
- `pitch` 預設 `1.0`
- `cooldown` 是重複播放的間隔毫秒數，預設 `250`
- `radius` 和 `minVolume` 用於場景中的螢幕空間衰減

### `<Amount>`

`<Amount>` 用於格式化數字數量。省略 `unit`（或使用 `unit="none"`）時只顯示數字；`unit="item"` 也不會新增字尾。`unit="fluid"` 遵循 GTNHLib 的流體單位偏好，顯示 `mB` 或 `L`。`format` 支援 `default`、`plain`、`compact` 和 `scientific`；`decimals` 用於限制小數位數。速率字尾如 `/s` 請寫在標籤外。

| 屬性 | 含義 |
| --- | --- |
| `value` | 必填的有限十進位制數值 |
| `unit` | 可選值 `none`、`item` 或 `fluid`，預設 `none` |
| `format` | 可選數字格式，預設 `default` |
| `decimals` | 可選的非負小數位上限 |

示例：

````md
<Amount value="250" />
<Amount value="250" unit="fluid" />/s
<Amount value="1200000" format="compact" /> 个物品
````

### `<PlayerName>`

插入當前 Minecraft 會話使用者名稱：

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

透過 id 或 action 查詢按鍵繫結，並渲染玩家當前實際繫結的按鍵名稱。

可接受的 id 形式：

- 繫結本身的描述 id，例如 `key.jump` 或 `key.guidenh.open_guide`
- 相容舊寫法的 `category.description` 形式，例如 `key.categories.movement.key.jump`

示例：

````md
Press <KeyBind id="key.jump" /> to jump.
攻击键：<KeyBind action="key.attack" />。
````

### MDX 註釋

GuideNH 會忽略頁面內容裡的 MDX 註釋：

````md
Visible text. {/* hidden inline comment */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH 也會忽略顯式 `<Comment>` 標籤：

````md
可见文字。<Comment>这里不会渲染。</Comment>仍然可见。
````

### `<ItemImage>`

顯示行內物品圖示。

| 屬性 | 含義 |
| --- | --- |
| `ore` | 可選礦物詞典選擇器；同時提供 `id` |
| `id` | 必需的物品引用；使用 `ore` 時也必須提供 |
| `nbt` | 可選的物品 SNBT；會合併到 `id` 的內聯 SNBT 之上 |
| `scale` | float，預設 `1` |
| `noTooltip` | 傳入真值字串或空屬性時禁用 tooltip（舊寫法，推薦改用 `showTooltip`） |
| `showTooltip` | boolean，預設 `true`；`false` 時禁用滑鼠懸停 tooltip |
| `showIcon` | boolean，預設 `true`；`false` 時不渲染圖示圖形 |
| `label` | `left` 或 `right`——在圖示左側或右側以文字顯示物品名稱；省略時不顯示文字 |
| `format` | 標籤文字的格式模式；支援 Markdown 風格的包裹標記（`**粗体**`、`*斜体*`、`~~删除线~~`、`__下划线__`、`^^波浪__`、`::点状::`），可包含 `%s` 佔位符代替物品名；預設（不寫本屬性）以斜體渲染物品名 |
| `yOffset` | 行內整體垂直偏移，以 `scale=1` 的畫素計；預設 `0`，正值向下 |
| `labelYOffset` | 僅標籤文字的垂直偏移，以 `scale=1` 的畫素計；預設 `0` |

說明：

- 同時提供 `ore` 和 `id` 時，優先使用 `ore`
- 若安裝了 GregTech，選中的礦辭結果會先經過 `GTOreDictUnificator.setStack(...)` 統一化
- `label` 需要至少一個可見元素（圖示或文字），若同時設定 `showIcon="false"` 且不寫 `label`，則渲染為空
- `format` 僅在設定了 `label` 時生效；若 `format` 中沒有 `%s`，則以格式字面量作為標籤文字
- `id` 中的內聯 SNBT 仍然有效；兩種寫法同時存在時，獨立的 `nbt` 屬性最後合併，同名鍵以它為準

示例：

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage id="minecraft:iron_ingot" ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"自定义钻石"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{display:{Name:"Storage Chest"}}'
/>
````

### `<ItemLink>`

使用物品顯示名建立文字連結，並附帶物品 tooltip。若 `item_ids` 把該物品對映到了某一頁面，點選後還會導航過去。也可以用 `ore` 透過礦辭的第一個匹配結果來決定顯示的物品。

| 屬性 | 預設值 | 含義 |
| --- | --- | --- |
| `id` | — | 物品註冊 ID，如 `minecraft:compass` 或 `minecraft:wool:1` |
| `ore` | — | 礦辭名稱，使用第一個匹配的物品堆疊 |
| `linksTo` | *（自動）* | 覆蓋跳轉目標，接受帶可選 `#anchor` 的頁面 ID，如 `./crafting.md#usage` 或 `#usage`；省略時由 `item_ids` / `ore_ids` 索引自動解析 |
| `showTooltip` | `true` | 設為 `false` 時懸停不顯示 tooltip；`noTooltip` 是舊版相容別名 |
| `showIcon` | *（無）* | `left` 或 `right`（或任意真值 → 右側）— 在連結文字的左側或右側顯示物品圖示；省略則僅顯示文字 |
| `scale` | `1.0` | 可選物品圖示的顯示縮放倍率；省略 `showIcon` 時此屬性無效 |

示例：

````md
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" />
<ItemLink id="appliedenergistics2:tile.BlockSkyChest" showIcon="left" />
<ItemLink id="minecraft:diamond" showIcon="left" scale="2" />
<ItemLink id="minecraft:diamond" showIcon="right" showTooltip="false" />
<ItemLink ore="stickWood" />
<ItemLink id="minecraft:iron_ore" linksTo="./crafting.md#smelting" />
<ItemLink id="minecraft:compass" linksTo="#usage" />
````

### `<FluidImage>`、`<FluidLink>` 與 `<FluidIcon>`

原生流體圖示透過完整的 Forge 流體註冊名查詢，例如 `water`、`steam`、`molten.iron`。文件必須儲存註冊名，不能儲存數字流體 ID 或 NEI 展示物品的後設資料。渲染直接使用 Forge 流體紋理與染色，不依賴 NEI。

預設懸停提示顯示流體名稱、開爾文溫度及氣體/液體狀態。明確填入 `amount`（包含零）時才顯示數量，單位遵循 L/mB 設定；省略數量不會加入數量行。進階提示也會顯示註冊名稱。載入 GregTech 後，化學式等材料資訊遵循 GT 的提示設定。遊戲內提示、已連線的執行階段預覽與 ExportSite 共用這些資訊；未連線的 Web 預覽只能顯示已有名稱與數量。不依賴 NEI。

ExportSite 的物品數量和流體數量使用網站自帶的 Pixeloid Mono 字型，以網頁文字覆蓋在圖示上；物品耐久條保留在圖示圖片中。

| 屬性 | 規則 |
| --- | --- |
| `id` | 必填的完整流體註冊名，不補名稱空間，也不解析物品後設資料 |
| `amount` | 可選整數數量，按流體單位設定顯示|
| `nbt` | 可選 SNBT，應用到獨立的 FluidStack 標籤 |
| `showTooltip`、`noTooltip` | 提示可見性優先順序與物品圖示相同；關閉提示不會關閉 G 導航 |
| `scale`、`showIcon`、`label`、`format`、`yOffset`、`labelYOffset` | FluidImage 沿用 ItemImage 的顯示引數；縮放必須有限且大於零 |
| `showIcon`、`showText`、`linksTo` | FluidLink 沿用 ItemLink 的標籤、圖示位置和顯式頁面/錨點規則 |

`FluidIcon` 只能作為 ItemGrid 子標籤，接受 `id`、`amount`、`nbt`、`showTooltip`、`noTooltip`。同一個 ItemGrid 可以按原始順序混排 ItemIcon 與 FluidIcon，不存在獨立的 FluidGrid。`ore` 與物品後設資料不能用於選擇流體。

```mdx
<FluidImage id="water" amount={1000} scale="2" label="right" />
<FluidLink id="steam" showIcon="left">蒸汽</FluidLink>
<ItemGrid>
  <ItemIcon id="minecraft:bucket" />
  <FluidIcon id="water" amount={1000} />
  <FluidIcon id="steam" />
</ItemGrid>
```

頁面透過 `fluid_id` / `fluid_ids` 繫結。滑鼠停在原生流體圖示或支援的容器/展示物件上，長按可改鍵的開啟指南快捷鍵（預設 G）；單個目標直接開啟，多個目標顯示選擇列表，先列流體頁面，再列容器頁面。數量和 NBT 影響顯示例項，頁面繫結只匹配註冊名。空容器保留普通物品導航；沒有懸停資料介面的流體 GUI 需要註冊介面卡。

導航使用 `navigation.icon_fluid` / `icon_fluids` 設定靜態/輪換頁面圖示；ContentTabs、FileTree 指令、引用指令和圖表 Series / LineSeries / Slice 使用 `iconFluid`。流體圖示不能與物品、PNG 或文字圖示同時配置。六種 Recipe/Usage 標籤支援 `fluid`、`inputFluid`、`outputFluid`；InputAnnotation 支援 `fluid`、`amount`、`nbt`。完整規則見配方、頁面格式和註解文件。

### 任務內容（BetterQuesting）

`<QuestLink>` 與 `<QuestCard>` 僅在 BetterQuesting 模組已載入時才會被註冊。詳細說明位於 [模組相容](Mod-Compatibility) 頁面，下面給出常用用法摘要。

#### `<QuestLink>`

左鍵開啟 BetterQuesting 任務介面。右鍵開啟繫結的指南頁；沒有繫結指南時，右鍵也開啟 BetterQuesting。多個頁面使用與物品相同的目標選擇介面，不同正文錨點保留為獨立目標。

| 屬性 | 含義 |
| --- | --- |
| `id` | 必填，BetterQuesting 任務 id；支援標準 UUID 字串和緊湊 Base64 id |
| `text` | 可選，覆蓋顯示文字 |
| `showTooltip` | 可選布林值，預設 `true`；設為 `false` 可關閉任務描述 tooltip |

按玩家進度在頁面掛載時決定外觀：

- 已可見 / 已完成的任務渲染為可點選連結（已完成的會被染為綠色並追加 `✓`）
- 已鎖定但未隱藏的任務仍會渲染為可點選任務連結，因此仍可開啟 BetterQuesting 任務介面或對應的索引指南頁
- 隱藏 / 機密的任務渲染為更深的斜體佔位符，使用 `guidenh.binding.state.hidden`
- 未知任務 id 渲染為紅色佔位符，使用 `guidenh.binding.state.missing`

示例：

````md
下一步請參考 <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" />。
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="第二階段任務" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" showTooltip="false" />
然後再看 <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="緊湊 quest id 示例" />。
````

#### `<QuestCard>`

塊級任務摘要卡片。標題使用與 `<QuestLink>` 相同的狀態相關樣式；當任務對當前玩家可見時，會附帶任務描述作為正文段落。

| 屬性 | 含義 |
| --- | --- |
| `id` | 必填，BetterQuesting 任務 id；支援標準 UUID 字串和緊湊 Base64 id |
| `title` | 可選，覆蓋卡片標題 |
| `showDesc` | 可選布林，預設 `true`；設為 `false` 可隱藏描述正文 |
| `showTooltip` | 可選布林值，預設 `true`；設為 `false` 可關閉可點選標題上的任務描述 tooltip |

卡片邊框顏色隨任務狀態變化：已完成綠色、鎖定 / 隱藏灰色、缺失紅色、可見時使用標準連結色。標題在可見、已完成以及鎖定但未隱藏時都會保持可點選。

示例：

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" showDesc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" showTooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````

### 研究與自定義內容

研究標籤對應 Thaumcraft；通用標籤由 `type` 指定內容系統。繫結頁面的寫法見 [Frontmatter](Guide-Page-Format#外部內容繫結)。

#### 導航規則

以下規則用於 ResearchLink/Card 和 BindingLink/Card 的連結或卡片標題。

| `target` | 左鍵 | 右鍵 |
| --- | --- | --- |
| `auto`（預設） | 優先開啟指南，無繫結時開啟原生介面 | 開啟指南，無繫結時開啟原生介面 |
| `native` | 開啟原生介面 | 開啟指南，無繫結時開啟原生介面 |
| `guide` | 只開啟指南，無繫結時給出提示 | 開啟指南，無繫結時開啟原生介面 |

原生跳轉開啟研究樹並定位至對應圖示，無論研究是否完成，都不會自動解鎖。隱藏研究不顯示名稱、簡介或圖示。多個指南目標會顯示選擇介面。

#### `<ResearchLink>`

行內內容連結。

| 屬性 | 說明 |
| --- | --- |
| `id` | 必填，原始內容 ID；研究 ID 區分大小寫 |
| `text` / `title` | 可選，覆蓋連結文字 |
| `target` | `auto`、`native` 或 `guide`，預設 `auto`，規則見上表 |
| `showTooltip` | 顯示提示，預設 `true` |

```mdx
<ResearchLink id="INFUSION" text="檢視研究" target="native" />
```

#### `<ResearchCard>`

塊級內容摘要卡片，點選標題進行導航。

| 屬性 | 說明 |
| --- | --- |
| `id` | 必填，原始內容 ID；研究 ID 區分大小寫 |
| `title` | 可選，覆蓋卡片標題 |
| `showDesc` | 顯示內容簡介；預設 `true` |
| `showIcon` | 顯示圖示；預設 `true` |
| `showParents` | 顯示可見前置內容；預設 `false` |
| `target` | `auto`、`native` 或 `guide`，預設 `auto`，規則見上表 |
| `showTooltip` | 顯示提示，預設 `true` |

```mdx
<ResearchCard id="INFUSION" title="注魔研究" showParents={true} />
```

#### `<ResearchIcon>`

內容圖示及 tooltip；懸停時按住開啟指南鍵可檢視指南，點選不開啟原生內容。

| 屬性 | 說明 |
| --- | --- |
| `id` | 必填，原始內容 ID；研究 ID 區分大小寫 |
| `scale` | 有限正數，預設 `1` |
| `showTooltip` | 顯示提示，預設 `true` |

```mdx
<ResearchIcon id="INFUSION" scale={1.5} />
```

#### `<BindingLink>`

行內內容連結。

| 屬性 | 說明 |
| --- | --- |
| `type` | 必填，已註冊內容型別，如 `thaumcraft`、`betterquesting` |
| `id` | 必填，原始內容 ID；研究 ID 區分大小寫 |
| `text` / `title` | 可選，覆蓋連結文字 |
| `target` | `auto`、`native` 或 `guide`，預設 `auto`，規則見上表 |
| `showTooltip` | 顯示提示，預設 `true` |

```mdx
<BindingLink type="thaumcraft" id="INFUSION" text="檢視研究" target="native" />
```

#### `<BindingCard>`

塊級內容摘要卡片，點選標題進行導航。

| 屬性 | 說明 |
| --- | --- |
| `type` | 必填，已註冊內容型別，如 `thaumcraft`、`betterquesting` |
| `id` | 必填，原始內容 ID；研究 ID 區分大小寫 |
| `title` | 可選，覆蓋卡片標題 |
| `showDesc` | 顯示內容簡介；預設 `true` |
| `showIcon` | 顯示圖示；預設 `true` |
| `showParents` | 顯示可見前置內容；預設 `false` |
| `target` | `auto`、`native` 或 `guide`，預設 `auto`，規則見上表 |
| `showTooltip` | 顯示提示，預設 `true` |

```mdx
<BindingCard type="thaumcraft" id="INFUSION" title="注魔研究" showParents={true} />
```

#### `<BindingIcon>`

內容圖示及 tooltip；懸停時按住開啟指南鍵可檢視指南，點選不開啟原生內容。

| 屬性 | 說明 |
| --- | --- |
| `type` | 必填，已註冊內容型別，如 `thaumcraft`、`betterquesting` |
| `id` | 必填，原始內容 ID；研究 ID 區分大小寫 |
| `scale` | 有限正數，預設 `1` |
| `showTooltip` | 顯示提示，預設 `true` |

```mdx
<BindingIcon type="thaumcraft" id="INFUSION" scale={1.5} />
```

#### 匯出與預覽

網站匯出和瀏覽器預覽不讀取匯出者的研究進度。繫結指南顯示為連結或多目標列表；原生介面動作顯示遊戲內操作說明。未安裝內容模組時，Research 與 Binding 標籤顯示整合不可用佔位。Quest 標籤僅在 BetterQuesting 已載入時註冊。

### `<CommandLink>`

點選後傳送聊天命令。

| 屬性 | 含義 |
| --- | --- |
| `command` | 必填，且必須以 `/` 開頭 |
| `title` | 可選 tooltip 標題 |
| `close` | 布林屬性；當前會被解析，但不會實際關閉指南 |

示例：

````md
<CommandLink command="/tp @p 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` 與 `<Column>`

用於塊內容的 flex 風格容器。

| 屬性 | 含義 |
| --- | --- |
| `gap` | 子元素間距，整數，預設 `5` |
| `alignItems` | `start`、`center`、`end` |
| `fullWidth` | boolean expression，預設 `false` |
| `width` | 整數首選寬度，適合約束列表等塊內容的行寬 |

示例：

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

如果想約束普通 Markdown 列表的寬度，可以這樣包一層：

````md
<Column width="220">
- 较窄的列表项
- 另一条较窄的列表项
</Column>
````

### `<FootnoteList>`

GuideNH 會在執行時 Markdown 腳註展開後內部使用這個塊標籤。必要時也可以手寫：

````md
<FootnoteList width="220">
1. 第一条脚注
2. 第二条脚注
</FootnoteList>
````

### `<ItemGrid>`

渲染緊湊圖示網格。網格編譯器直接解析 ItemIcon 與 FluidIcon，並保留混排順序；物品選擇使用 `id` 或 `ore`，流體選擇使用註冊名 `id`，可選數量與 NBT。

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

渲染一個非互動式的 3D 單方塊場景。這個預覽沒有場景背景、沒有場景按鈕、沒有 layer 控制元件，
也不支援註解功能，但滑鼠懸停時仍然會顯示選中線框和 tooltip。若使用 `ore`，該礦辭必須
解析到一個方塊物品。

| 屬性 | 含義 |
| --- | --- |
| `id` | 方塊 id；支援常規 `modid:block[:meta][:{snbt}]` 寫法 |
| `ore` | 礦辭查詢；使用第一個匹配到的方塊物品 |
| `scale` | 相機縮放倍率，預設 `4` |
| `meta` | 方塊後設資料，覆蓋 `id` 內的後設資料 |
| `width` / `height` | 預覽視口畫素尺寸，寬高分別預設 `128` |
| `wrap` | 文字環繞模式，預設 `inline` |
| `align` | 水平對齊：`left`（預設）、`center` 或 `right` |
| `float` | 舊版流式浮動支援：`left` 或 `right` |
| `perspective` | `isometric-north-east`（預設）、`isometric-north-west` 或 `up` |
| `nbt` | 可選的 TileEntity SNBT；會合併到 `id` 內聯 SNBT 之上 |

說明：

- 為相容舊內容，`id` 裡的內聯 SNBT 仍然有效，但更推薦寫成獨立的 `nbt="..."` 屬性
- 當同時提供內聯 SNBT 和 `nbt` 時，`nbt` 會最後合併，因此同名鍵會以 `nbt` 為準
- GuideNH 當前的 1.7.10 執行時不支援這裡的現代方塊狀態屬性語法，因此不會支援
  GuideME 裡的 `p:<state>` 寫法

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

完整行為請參見 [圖片與資源](Images-And-Assets)。

```mdx
<FloatingImage src="test1.png" displayWidth="128" wrap="inline" />

<FloatingImage src="test1.png" x="8" y="8" width="16" height="16" displayWidth="128" wrap="inline" />
```

### `<SubPages>`、`<Category>` 與 `<Special>`

完整導航行為請參見 [導航](Navigation)。

### `<Structure>`

當你在靜態結構預覽和完整 3D 遊戲場景之間做選擇時，可結合 [示例](Examples) 和 [遊戲場景](GameScene) 參考。

### `<CsvTable>`

用於在執行時把 CSV 檔案解析成表格：

````md
<CsvTable src="./markdown-table.csv" />
````

`src` 路徑會像場景匯入和普通資源連結一樣，相對當前頁面解析。

可選屬性：

- `header`
  預設是 `true`；設定 `header={false}` 時，首行不會加粗
- `widths`
  逗號分隔的整數列寬 hint，例如 `widths="120,80"`

示例：

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
````

對應的圍欄 CSV 執行時寫法也支援相同語義的 `meta`：

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

### HTML `<table>`

GuideNH 頁面支援原生 HTML 表格標籤。使用 `<caption>`、`<colgroup>` / `<col>` 以及 `<thead>` / `<tbody>` / `<tfoot>` 描述表格結構。單元格支援 `colspan`、`rowspan`、`align`、`valign`；表頭單元格還支援 `scope`、`abbr` 和 `headers`。表格 `align` 包含逗號時（例如 `left,center,right`）表示逐列對齊；單獨的 `left`、`center` 或 `right` 會調整整個表格的位置。遊戲內渲染和站點匯出都支援 `width`、`border`、`cellpadding` 和 `cellspacing`。Markdown 表格仍支援 `{: widths="130,70,150" }` 這類 kramdown 後設資料。

````md
<table width="100%" cellpadding="2">
  <caption>燃料速率</caption>
  <colgroup><col width="40%" /><col width="60%" /></colgroup>
  <thead><tr><th scope="col">燃料</th><th scope="col">速率</th></tr></thead>
  <tbody><tr><td>煤炭</td><td align="right">42</td></tr></tbody>
</table>
````

### `<Latex>`

使用 jlatexmath 渲染 LaTeX 數學公式。在行內（段落或文字流中）使用時，渲染為自動縮放的字形，並將所在行的行高擴充套件以適應公式高度。獨立成段（塊級上下文）使用時，居中渲染為展示式公式塊。

| 屬性 | 型別 | 預設值 | 說明 |
| --- | --- | --- | --- |
| `formula` | 字串 | *（必填）* | LaTeX 源字串 |
| `color` | `#RRGGBB` 或 `#AARRGGBB` | `#FFFFFF` | 字形填充顏色 |
| `scale` | float | `1.0` | 在自動行高縮放的基礎上額外疊加的顯示大小倍率 |
| `sourceScale` | float | `100.0` | jlatexmath 排版尺寸（測量基準）。遊戲內公式始終按實際螢幕解析度光柵化；站點匯出時決定 PNG 解析度 |
| `tooltip` | 字串 | *無* | 滑鼠懸停時顯示的普通文字 tooltip |
| `showTooltip` | boolean | `false` | 滑鼠懸停時以 tooltip 展示原始 LaTeX 源文字 |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | 僅限行內公式。行內垂直對齊方式：`baseline`（預設）使公式數學基線與文字基線對齊；`top` 使公式頂部與行頂對齊；`center` 將公式垂直居中於文字行高；`bottom` 使公式底部與文字底部對齊 |
| `offsetX` | int | `0` | 對齊後額外施加的水平畫素偏移（正值為向右） |
| `offsetY` | int | `0` | 對齊後額外施加的垂直畫素偏移（正值為向下） |

示例：

````md
行内：<Latex formula="E=mc^2" />

分数（自动扩展行高）：<Latex formula="\frac{a+b}{c-d}" />

金色：<Latex formula="\sqrt{x^2+y^2}" color="#FFD700" />

放大：<Latex formula="\pi" scale="1.5" />

悬停显示源码：<Latex formula="\sum_{n=1}^{\infty} \frac{1}{n^2}" showTooltip={true} />

自定义普通 tooltip：<Latex formula="E=mc^2" tooltip="能量等于质量乘以光速平方。" />

富文本 tooltip：
<Latex formula="\Delta G = \Delta H - T\Delta S">
  **吉布斯自由能**

  - <Latex formula="\Delta H" />：焓变
  - <Latex formula="T\Delta S" />：熵项
</Latex>

底部对齐（公式底部与文字底部对齐）：<Latex formula="\frac{a}{b}" valign="bottom" />

显式基线对齐（与默认效果相同）：<Latex formula="E=mc^2" valign="baseline" />

顶部对齐并向上微调：<Latex formula="x^2" valign="top" offsetY="-1" />

<Latex formula="\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}" />

<Latex formula="\begin{pmatrix} a & b \\ c & d \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix} = \begin{pmatrix} ax+by \\ cx+dy \end{pmatrix}" />
````

#### `$$公式$$` 簡寫語法

你可以在 Markdown 文字中直接使用 `$$公式$$`，無需使用 `<Latex>` 標籤。
所有渲染引數均使用預設值（白色、比例 1.0、無懸停提示、基線對齊）。

- **行內模式**：嵌入段落內的 `$$公式$$` 使用行內渲染。
- **展示模式**：整個段落內容僅為 `$$公式$$`（首尾可有空白）時，渲染為居中的展示式公式塊。

````md
行内简写：$$E=mc^2$$ 和 $$a^2+b^2=c^2$$

行内分数：$$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

注意事項：

- 公式高度以當前行文字高度為基準自動校準。簡單公式與文字等高；含分數、求和、積分等高度較大的公式會自動擴充套件所在行的行高。
- `valign` 僅對行內公式生效。塊級（展示式）公式始終水平居中；如需垂直方向調整，請使用 `offsetY`。
- `color` 預設為白色（`#FFFFFF`）。如需半透明填充，使用 `#AARRGGBB` 格式。
- `sourceScale` 不改變顯示大小。遊戲內公式會按螢幕實際畫素尺寸（考慮 GUI 縮放）光柵化，因此分數線、減號等細筆畫在低解析度螢幕上也不會丟失；`sourceScale` 隻影響站點匯出 PNG 的解析度。低於 `16` 的值會被截斷為 `16`。
- tooltip 優先順序為：標籤體富文字 Markdown、`tooltip="..."`、最後才是 `showTooltip={true}` 的原始原始碼回退。
- 標籤體 tooltip 會按普通指南 Markdown 編譯，因此可以包含粗體、列表、連結、物品標籤和巢狀 `<Latex>` 公式。
- `$$公式$$` 簡寫語法始終使用預設引數。如需自定義顏色、比例、對齊或懸停提示，請使用 `<Latex>` 標籤。

### 場景執行時標籤

這些標籤僅能在遊戲場景（`<GameScene>` / `<Scene>`）內部工作：

| 標籤 | 用途 | 關鍵屬性 |
| --- | --- | --- |
| `<ImportStructure>` | 匯入外部 SNBT/NBT 結構資源 | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | 透過控制器 id 匯入 StructureLib 多方塊 | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `tier`, `channel`, `channelName`, `value`, `locked`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | 移除已放置且匹配指定方塊匹配器的方塊 | `id` |
| `<BlockAnnotation>` | 高亮單個方塊 | `pos`, `color`, `colorMode`, `colorPeriod`, `thickness`, `alwaysOnTop` |
| `<BoxAnnotation>` | 高亮任意軸對齊包圍盒 | `min`, `max`, `color`, `colorMode`, `colorPeriod`, `thickness`, `alwaysOnTop` |
| `<OutlineAnnotation>` | 沿方塊或實體的實際渲染剪影描邊 | `pos`, `min`, `max`, `entity`, `target`, `mode`, `color`, `colorMode`, `colorPeriod`, `thickness`, `alwaysOnTop`, `pulse`, `pulsePeriod` |
| `<BlockAnnotationTemplate>` | 把同一組子註解擴充套件到場景中所有匹配方塊上 | `id` |

`colorMode`: `solid`（預設）或 `rainbow`；彩虹圍繞註解投影中心旋轉 360°，替換 RGB 並保留 `color` 的透明度. `colorPeriod`: 彩虹迴圈週期，單位秒，必須為正且有限，預設 `2.4`；非法值會報告頁面錯誤. 遊戲和 Site Export 使用相同的彩虹漸變規則與迴圈週期。

關於場景匯入/移除行為，請參見 [遊戲場景](GameScene)；關於註解模板規則，請參見 [註解](Annotations)。


## 資料圖表

`<ColumnChart>`、`<BarChart>`、`<LineChart>`、`<PieChart>`、`<ScatterChart>` 均為互動式圖表塊。所有圖表共享下列通用屬性：

| 屬性 | 說明 | 預設 |
| --- | --- | --- |
| `title` | 標題 | 無 |
| `width` / `height` | 顯式尺寸 | 320 / 200 |
| `background` / `border` | 背景與邊框顏色（支援 `#RGB`、`#RRGGBB`、`#AARRGGBB`、`0x...`） | 深灰 |
| `titleColor` / `labelColor` | 標題與資料標籤顏色 | 淺灰 |
| `legend` | 圖例位置：`none`/`top`/`bottom`/`left`/`right` | `top` |
| `labelPosition` | 資料標籤位置：`none`/`inside`/`outside`/`above`/`below`/`center` | `none` |
| `cornerLegend` | 圖內角落圖例位置：`none`/`topRight`/`topLeft`/`bottomRight`/`bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | 圖內圖例框最大尺寸 | `120` / `64` |
| `cornerLegendBackground` | 圖內圖例背景色 | `#AA111922` |

笛卡爾系圖表（柱形/條形/折線/散點）支援座標軸屬性 `xAxisLabel`、`xAxisMin`、`xAxisMax`、`xAxisStep`、`xAxisUnit`、`xAxisTickFormat`，以及對應的 `yAxis*`，外加 `showXGrid={true}` 與 `showYGrid={true}` 控制網格線。

無效或過大的刻度格式會回退為自動數字格式。格式字串最多 256 個字元，欄位寬度最多 128，精度最多 20。 數值刻度文字擁擠時會自動省略部分標籤，其對應的網格線保持不變。

`ColumnChart`、`LineChart` 和 `ScatterChart` 在滑鼠進入繪圖區後顯示金色水平輔助虛線，空白處也生效，左側座標軸高亮當前 Y 值。`BarChart` 使用豎向輔助線，並在底部高亮 X 值。數值遵循對應軸的刻度格式和單位；虛線上不放文字，移出繪圖區後隱藏。兩軸網格採用淡而稀疏的虛線，透過 `showXGrid`、`showYGrid` 分別控制。極小的正刻度步長會自動合併，避免渲染工作量失控。

子元素：

* `<Series name="..." color="#..." data="10,20,30"/>` — 用於按類別取值的圖表（柱形/條形/類別 X 折線）。
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` — 用於數值 X（折線 `numericX={true}`、散點）。
* `<Slice name="..." value="..." color="#..."/>` — 僅 `<PieChart>` 使用。
* `<ReferenceLine y="100"/>` — 在 `ColumnChart`、`LineChart`、`ScatterChart` 的指定 Y 值繪製固定水平虛線；`BarChart` 使用 `<ReferenceLine x="100"/>` 繪製固定豎線，也相容 `y` 寫法。座標必須是有限數值。可新增多條，預設半透明灰色，可用 `color` 修改；可選 `label` 顯示在繪圖區內，避免與座標刻度重疊。參考線獨立於網格開關，不擴充套件自動座標範圍，範圍外不顯示；`PieChart` 不支援此標籤。

未指定 `color` 時按內建 16 色調色盤迴圈分配。

`<Series>` 與 `<Slice>` 還接受以下可選圖示/tooltip 屬性：

* `icon="modid:item"`（可帶 `@meta` 或互動 NBT JSON，同 `<ItemImage>` 的 `id`）— 使用 `ItemStack` 作為圖例色塊與懸停圖示；懸停時會顯示原版物品 tooltip 並在末尾追加圖表說明。
* `iconImage="images/foo.png"` — 使用 PNG 資源作為圖例色塊（被 `icon` 覆蓋）。
* `tooltip="..."` — 額外追加到 tooltip 末尾的文字（多行用 `\n`）。

示例：

```mdx
<PieChart title="产出占比">
  <Slice name="铁錠" value="40" icon="minecraft:iron_ingot" tooltip="来自冶炼烉" />
  <Slice name="金錠" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

附加屬性：`categories`（X 軸/Y 軸類別，逗號分隔）、`barWidthRatio`（預設 0.7）。`<BarChart>` 類別在 Y 軸、數值在 X 軸。

#### 組合擴充套件

`<ColumnChart>` 和 `<BarChart>` 額外支援兩類子元素，便於在同一座標系內疊加多種圖形：

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — 在柱簇上方再疊加一條折線。每個折點位於對應類別簇中心，與宿主圖共享數值軸；可同時宣告多條 `<LineSeries>`。
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — 在繪圖區四角之一（`topRight`/`topLeft`/`bottomRight`/`bottomLeft`）繪製小型餅圖，內部 `<Slice>` 子元素與 `<PieChart>` 一致。

`PieInset` 扇區優先於底層柱條和折線響應懸停：提示包含名稱、數值、佔比及 `tooltip` 附加說明；遊戲內有物品圖示時保留物品提示。懸停扇區時隱藏主圖輔助線。僅使用第一個內嵌餅圖；`position="rightOutside"` 在右側預留獨立區域。支援 `startAngleDeg="0"`、`direction="counterclockwise"` 和單扇區完整圓形。

```mdx
<ColumnChart title="季度产量" categories="Q1,Q2,Q3,Q4">
  <Series name="铁"  data="40,60,55,70"  color="#a0a0a0"/>
  <Series name="金"  data="20,30,25,35"  color="#e0c060"/>
  <LineSeries name="合计" data="60,90,80,105" color="#ff5050"/>
  <PieInset size="60" position="topRight" title="合计占比">
    <Slice name="铁" value="225" color="#a0a0a0"/>
    <Slice name="金" value="110" color="#e0c060"/>
  </PieInset>
</ColumnChart>
```

### `<LineChart>`

附加屬性：`numericX={true}`（啟用數值 X 軸，子元素改用 `points`）、`showPoints={false}`（隱藏點）。懸停某個資料點時該點沿曲線法向外推 2px、半徑 +2 並加黑邊，相鄰線段加粗。

`<LineChart>` 與 `<ScatterChart>` 可以用 `cornerLegend="topRight"` 等位置值在繪圖區內部顯示緊湊圖例。圖例條目使用已有系列名和系列顏色。

### `<PieChart>`

附加屬性：`startAngle`（起始角度，預設 -90 即 12 點鐘方向）、`clockwise={false}` 反向。懸停時被懸停扇區沿其角平分線方向外移 4px。

### `<ScatterChart>`

僅繪製資料點；`<Series>` 必須使用 `points` 屬性。X 軸始終為數值軸。

## 函式圖

`<FunctionGraph>` 與單曲線簡寫 `<Function>` 渲染互動式 Desmos 風格面板。同一面板也可透過 ` ```funcgraph ` 圍欄程式碼塊寫出，詳細示例見執行時 [Markdown 示例](resourcepack/assets/guidenh/guidenh/_zh_cn/markdown.md)。

面板屬性（容器、簡寫、圍欄首行通用）：

- `width` / `height`（預設 `320` × `220`）
- `title`、`background`、`border`、`axisColor`、`gridColor`
- `showGrid` / `showAxes`（預設 `true`）
- `xRange="a..b"`（或 `xMin` / `xMax` 分寫），`xStep` 控制刻度間距；Y 軸同理
- `xLabel` / `yLabel` 分別在繪圖區下方和上方顯示類似 Excel 的軸標題，並支援內聯 `$$...$$` LaTeX；沒有顯式 X 範圍時，`domain="a..b"` 是相容舊內容的 `xRange` 別名
- `quadrants="1,2,3,4"` 或 `quadrants="all"` 強制可見象限；不寫則預設僅第一象限，並在取樣發現 `y < 0` 時自動追加第三、第四象限
- `cornerLegend`、`cornerLegendWidth`、`cornerLegendHeight`、`cornerLegendBackground` 可以把帶 `label` 的曲線顯示為繪圖區內部的緊湊圖例

曲線子節點（`<Plot>` / `<Function>`）：

- `expr="..."`：表示式。支援 `+ - * / % ^`、字尾階乘 `!`（gamma 推廣至實數）、`|x|` 絕對值、`√`/`sqrt`、`∛`/`cbrt`、隱式乘法以及常量 `pi`、`tau`、`e`、`phi`。內建呼叫覆蓋常規 trig/log/exp/rounding 函式，並提供雙引數 `atan2`、`min`、`max`、`pow`、`hypot`、`mod`。
- `inverse={true}` 將表示式解釋為 `x = f(y)` 並旋轉曲線。
- `domain="a..b"`（x 上下界簡寫）或逗號分隔的比較子句，如 `x>=0, x<5`。
- `color`、`label`。設定非空名稱的曲線會自動出現在面板下方的圖例裡：一個小色塊加曲線名，按從左到右排列，寬度不夠時自動換到下一行。
- `tooltip` 會在計算出的提示資訊後追加純文字。`showFunction` 和 `showValues` 預設均為 `true`，設為 `false` 可分別隱藏函式式或實時 `(x, y)` 值。帶有 `expr` 的 `<Plot>` / 內嵌 `<Function>` 可以寫子 Markdown 和 GuideNH 標籤，作為富文字 tooltip 正文。順序始終為曲線 `label`、函式式、實時值、`tooltip` 文字、子節點富文字；預設或關閉的計算行會按此順序跳過。
- `pointEveryX="step"` 按固定 x 間隔在該曲線上生成點。
- `pointEveryY="step"` 在曲線與固定 y 間隔的交點處生成點，內部使用有上限的掃描和二分求解。
- `autoPointLabel="none|x|y|xy"` 控制自動點標籤，預設 `none`。
- `autoPointColor="#..."` 覆蓋自動點顏色；省略時繼承曲線顏色。

標記點（`<Point>`）：

- 顯式座標：`x="..."` + `y="..."`。
- 錨定到曲線：`plot="N"` 配合 `atX="v"` 或 `atY="v"`（執行時在該曲線 x 域上二分求解）。
- 可選 `color`、`label`。

互動：滑鼠懸停曲線會高亮該曲線；按住可沿曲線滑動一個標記點。若提供 `label`，提示框最上方先顯示函式名，之後預設顯示函式式和實時 `(x, y)` 值；可用對應開關關閉。提示框預設錨定在該點正上方，頂部空間不足時自動翻到下方。
