[English](Tags-Reference)

# 標籤參考

本頁列出 `DefaultExtensions` 註冊的內建執行時間標籤。

## 使用規則

- 標籤可以出現在區塊級上下文或行內上下文，取決於對應編譯器。
- 頁面內容支援使用 `{/* ... */}` 形式的 MDX 註釋，執行時間解析器會忽略這些註釋。
- 無效標籤或無效屬性不會靜默失敗，而是以內嵌指南錯誤的形式顯示。
- 配方和 3D 場景這類大型功能標籤會在獨立頁面中說明：
  - [配方](Recipes)
  - [遊戲場景](GameScene)
  - [註](Annotations)

## 行內與流式標籤

| 標籤 | 用途 | 關鍵屬性 |
| --- | --- | --- |
| `<a>` | 內部/外部鏈接，以及可選錨點名 | `href`, `title`, `name` |
| `<br>` | 換行 | `clear="none\|left\|right\|all"` |
| `<kbd>` | 鍵位風格行內強調 | 無 |
| `<sub>` | 較小的下標風格行內文本 | 無 |
| `<sup>` | 較小的上標風格行內文本 | 無 |
| `<Color>` | 彩色行內文本 | `id` 或 `color` |
| `<Spoiler>` | 懸停後顯示的行內黑幕文本 | 無 |
| `<Tooltip>` | 附 Markdown/標籤子內容的富懸浮提示 | `label` |
| `<SoundLink>` | 可點擊的富文本音效觸發器 | `sound` 或 `src`，`volume`，`pitch`，`cooldown` |
| `<mark>` | 行內高亮文字；等價於 `==text==`，可自訂顏色 | `color` |
| `<PlayerName>` | 插入目前玩家用戶名 | 無 |
| `<KeyBind>` | 插入按鍵綁定顯示名 | `id` 或 `action` |
| `<ItemImage>` | 行內物品圖標 | `id` 或 `ore`，`scale`，`noTooltip`，`showTooltip`，`showIcon`，`label`，`format`，`yOffset`，`labelYOffset` |
| `<ItemLink>` | 物品 tooltip + 可選導航鏈接 | `id` 或 `ore`，`linksTo`，`showTooltip`，`noTooltip`，`showIcon` |
| `<CommandLink>` | 可點擊的聊天命令鏈接 | `command`, `title`, `close` |
| `<Latex>` | LaTeX 數學公​​式；在流式上下文中行內渲染，在區塊級上下文中居中顯示為獨立公式區塊 | `formula`, `color`, `scale`, `sourceScale`, `tooltip`, `showTooltip` |
| `<QuestLink>` | BetterQuesting 任務鏈接，按任務狀態自動調整樣式（相容標籤，僅當 BetterQuesting 已加載時註冊） | `id`, `text`, `show_tooltip` |

行內 Markdown 也支援音效動作連結：

````md
&[启动机器](sound:guidenh:machine.start)
&[播放文件音效](sound-src:guidenh:sounds/machine/start.ogg?volume=0.8&pitch=1.1)
````

## 區塊級標籤

| 標籤 | 用途 | 關鍵屬性 |
| --- | --- | --- |
| `<div>` | 透傳塊包裝器 | 無 |
| `<ContentTabs>` | 將可取代的富內容分組到獨立標籤頁中 | `title`、`color`、`icon`、`iconPng`、`icon_png`、`iconItem`、`icon_item`、`default`、`defaultIndex` |
| `<Tab>` | `<ContentTabs>` 內的單一內容面板 | `title` |
| `<details>` | 可折疊運行時區塊 | `open`、`width`、`height`、`wrap`、`align` |
| `<FileTree>` | 目錄樹式大綱（附連接線） | `indent`、`gap` |
| `<Row>` | 橫向 flex 佈局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<Column>` | 縱向 flex 佈局 | `gap`, `alignItems`, `fullWidth`, `width` |
| `<FootnoteList>` | 運行時 Markdown 腳註使用的限寬腳註容器 | `width` |
| `<ItemGrid>` | 緊湊物品圖示網格 | 子元素必須是 `<ItemIcon id="..."/>` 或 `<ItemIcon ore="..."/>` |
| `<BlockImage>` | 非交互式的 3D 单方块预览 | `id` 或 `ore`，可选 `scale`（默认 `4`），`float`，`perspective`，`nbt` |
| `<FloatingImage>` | 支援浮動或真正行內放置的裁切圖片區塊 | `src`, `x`, `y`, `width` / `w`, `height` / `h`, `scaleX`, `scaleY`, `displayWidth`, `displayHeight`, `wrap`, `align`, `title` |
| `<SubPages>` | 導覽子頁面列表 | `id`, `alphabetical` |
| `<Category>` | 分類頁面列表 | `name`, `rows` |
| `<Special>` | 內建 MediaWiki 特殊頁面列表 | `name`, `rows` |
| `<Structure>` | 2.5D 等軸方塊佈局預覽 | `width`, `height` |
| `<Mermaid>` | 運行時 Mermaid 圖導入/內聯 | `src`, `width`, `height` |
| `<CsvTable>` | 執行時間 CSV 檔案導入表格 | `src`, `header`, `widths` |
| `<ColumnChart>` | 簇狀長條圖 | `categories`, `barWidthRatio`, `xAxis*`, `yAxis*`, `legend`, `labelPosition` |
| `<BarChart>` | 橫向長條圖 | 同 `<ColumnChart>` |
| `<LineChart>` | 折線圖（類別或數值 X 軸） | `categories`, `numericX`, `showPoints`, `xAxis*`, `yAxis*`, `cornerLegend` |
| `<PieChart>` | 圓餅圖 | `startAngle`, `clockwise`, `legend`, `labelPosition` |
| `<ScatterChart>` | XY 散佈圖 | `xAxis*`, `yAxis*`, `legend`, `labelPosition`, `cornerLegend` |
| `<FunctionGraph>` | Desmos 風格的多曲線函數圖 | `width`, `height`, `xRange` / `yRange`, `quadrants`, `showGrid`, `showAxes`, `cornerLegend` |
| `<Function>` | 单曲线简写，等价于只含一个 `<Plot>` 的 `<FunctionGraph>` | `expr`，及全部 `<FunctionGraph>` 面板屬性 |
| `<Recipe>`, `<RecipeFor>`, `<RecipesFor>` | 配方渲染器 | 詳見 [配方](Recipes) |
| `<GameScene>`, `<Scene>` | 3D 指南遊戲場景 | 詳見 [遊戲場景](GameScene) |
| `<QuestCard>` | 區塊級 BetterQuesting 任務摘要卡片（相容標籤，僅當 BetterQuesting 已載入時註冊） | `id`, `show_desc`, `show_tooltip` |

`<ImportStructureLib>` 是 `<GameScene>` 的子標籤。它支援 `controller`、`piece`、`facing`、
`rotation`、`flip`、`channel` 屬性，也支援用於設定 StructureLib 預設值的子標籤：
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

- `href` 可以是相對路徑、根路徑、顯式 `modid:path`，或 HTTP/HTTPS 鏈接
- `title` 會作為 tooltip 使用
- `name` 會插入一個頁面錨點目標

### `<br>`

GuideNH 也支援具有浮動清理能力的 MDX 風格換行標籤：

````md
Text before.<br clear="all" />Text after.
````

可接受的 `clear` 值：

- `none`
- `left`
- `right`
- `all`

### `<kbd>`、`<sub>` 與 `<sup>`

GuideNH 運行時支援一小組常見的小寫文檔標籤：

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
Water is H<sub>2</sub>O and x<sup>2</sup> is a square.
````

### `<details>`

用於建立可折疊的運行時內容區塊。 `<summary>` 行支援常規行內 Markdown/標籤內容，正文則可以放普通文字與任意區塊級標籤，例如 `<BlockImage>`、`<FloatingImage>`、`<GameScene>`、表格、圖表或佈局容器。設定 `height` 時，只有正文區域滾動，標題行和外框保持固定。

````md
<details open width="220" height="140" wrap="square" align="right">
<summary>更多 <ItemImage id="minecraft:diamond" /></summary>

默认折叠的正文文本，里面可以放[普通页面链接](./index.md)。

<BlockImage id="minecraft:diamond_block" align="center" scale={2} />
</details>
````

屬性：

- `open` — 存在時預設展開
- `width` — 外層首選寬度，單位像素
- `height` — 正文視窗首選高度，單位像素；超出部分會在遊戲內和站點匯出中變為可滾動區域
- `wrap` — 支持常见块嵌入模式，例如 `square`、`tight`、`through`
- `align` — `left`、`center` 或 `right`；與浮動型 `wrap` 搭配時會讓整個 details 區塊浮動

### `<ContentTabs>`

将可替代的富内容分组到独立标签页中。 `<ContentTabs>` 只接受直接的 `<Tab>` 子標籤。容器本身也可以在標籤頁上方渲染一個類似 Markdown 標記/引用區塊的標題行。

````mdx
<ContentTabs
  title="实现方案"
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

- `default` 會符合第一個 `title` 完全相同的標籤頁
- `defaultIndex` 使用從 `0` 開始的下標，並且在同時出現時優先權高於 `default`
- `color` 可選，以 `#RRGGBB` 或 `#AARRGGBB` 覆蓋左側強調線與選取標籤的高亮顏色
- `title` 會在標籤列上方增加一個可選的純文字標題
- `icon`、`iconPng` / `icon_png`、`iconItem` / `icon_item` 与 Markdown 标记/引用块标题的图标语义完全一致
- 非法子节点或非法默认值会渲染为面向作者的可见错误

### `<FileTree>`

渲染目录树式大纲，并依据每行前缀符号绘制真实的连接线。前綴同時支援 Unicode 框線（`│ ├ └ ─`）與 ASCII 形式（`| +-- \-- ` / 4 個空格），可任意混用。每行的文字部分支援常規行內 Markdown（連結、**加粗**、`代码` 等），這些連結在遊戲內和內建網站匯出中都可以點擊。等價語法是 ` ```tree ` / ` ```filetree ` 圍欄代碼區塊。

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

- `{:icon=文本}` — 簡短的文字圖標
- `{:iconPng=path/to/file.png}` — PNG 資源（依目前頁面解析路徑）
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

- `indent` — 每層縮排像素數（預設 `14`）
- `gap` — 行間額外像素數（預設 `0`）

### 執行時引用區塊

普通 Markdown 引用區塊在執行時會顯示左側強調線，並支援 GitHub 提示語法：

````md
> [!NOTE]
> 提示內容
````

也支援只在執行時使用的自訂指令，必須寫在引用區塊第一行：

````md
> {: title="自訂引用" color="#638ef1" icon="i" }
> 內容
````

支援的指令鍵：

- `title`
- `color`
- `icon`：普通文字符號
- `iconItem`：物品堆 ID
- `iconPng`：指南資源中的 PNG 路徑

三種圖示來源只能選擇一種。

### `<Color>`

可使用符號顏色 id，或明確十六進位顏色值：

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

- `id` 和 `color` 在實際使用上應二選一
- `color` 支援 `#RRGGBB`、`#AARRGGBB` 或 `transparent`

### `<Tooltip>`

建立下劃線的文本，並在懸停時顯示富內容 tooltip。

````md
<Tooltip label="Hover me">
  **Bold text**
  <ItemImage id="minecraft:diamond" />
</Tooltip>
````

若省略 `label`，預設觸發文字為 `tooltip`。

### `<SoundLink>` 與音效動作鏈接

`<SoundLink>` 會渲染一段可點選的富文本內容，點選後播放音效。它不會進行頁面導航，
並且本次點擊會使用自訂音效替代指南預設點擊音效。

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
- `cooldown` 是重复播放的间隔毫秒数，默认 `250`
- `radius` 和 `minVolume` 用于场景中的屏幕空间衰减

### `<PlayerName>`

插入当前 Minecraft 会话用户名：

````md
Welcome, <PlayerName />!
````

### `<KeyBind>`

透過 id 或 action 尋找按鍵綁定，並渲染玩家目前實際綁定的按鍵名稱。

可接受的 id 形式：

- 綁定本身的描述 id，例如 `key.jump` 或 `key.guidenh.open_guide`
- 相容於舊寫法的 `category.description` 形式，例如 `key.categories.movement.key.jump`

範例：

````md
Press <KeyBind id="key.jump" /> to jump.
攻击键：<KeyBind action="key.attack" />。
````

### MDX 註釋

GuideNH 會忽略頁面內容裡的 MDX 註：

````md
可見文字。{/* 隱藏的行內註解 */}

{/*
multiline comment
*/}

More visible text.
````

GuideNH 也會忽略明確 `<Comment>` 標籤：

````md
可见文字。<Comment>这里不会渲染。</Comment>仍然可见。
````

### `<ItemImage>`

顯示行內物品圖示。

| 屬性 | 意義 |
| --- | --- |
| `ore` | 礦辭名；預設取第一個配對結果 |
| `id` | 未提供 `ore` 時所使用的物品引用 |
| `nbt` | 可選的物品 SNBT；會合併到 `id` 的內聯 SNBT 之上 |
| `scale` | float，預設 `1` |
| `noTooltip` | 傳入真值字串或空屬性時停用 tooltip（舊寫法，推薦改用 `showTooltip`） |
| `showTooltip` | boolean，預設 `true`；`false` 時停用滑鼠懸停 tooltip |
| `showIcon` | boolean，預設 `true`；`false` 時不渲染圖示圖形 |
| `label` | `left` 或 `right`－在圖示左側或右側以文字顯示物品名稱；省略時不顯示文字 |
| `format` | 标签文字的格式模式；支持 Markdown 风格的包裹标记（`**粗体**`、`*斜体*`、`~~删除线~~`、`__下划线__`、`^^波浪__`、`::点状::`），可包含 `%s` 占位符代替物品名；默认（不写本属性）以斜体渲染物品名 |
| `yOffset` | scale 為 `1` 時**圖示**的像素偏移覆蓋值；不影響標籤文字 |
| `labelYOffset` | scale 為 `1` 時**標籤文字**的像素偏移覆蓋值；不影響圖標 |

說明：

- 同時提供 `ore` 和 `id` 時，優先使用 `ore`
- 若安裝了 GregTech，選取的礦辭結果會先經過 `GTOreDictUnificator.setStack(...)` 統一化
- `label` 需要至少一個可見元素（圖示或文字），若同時設定 `showIcon="false"` 且不寫 `label`，則渲染為空
- `format` 僅在設定了 `label` 時生效；若 `format` 中沒有 `%s`，則以格式字面量為標籤文字
- `id` 中的內聯 SNBT 仍然有效；兩種寫法同時存在時，獨立的 `nbt` 屬性最後合併，同名鍵以它為準

範例：

````md
<ItemImage id="minecraft:diamond" scale="2" />
<ItemImage ore="ingotIron" />
<ItemImage id="minecraft:diamond_sword" noTooltip="true" />
<ItemImage id="minecraft:diamond" label="right" />
<ItemImage id="minecraft:iron_ingot" label="left" format="**%s**" />
<ItemImage id="minecraft:book" showIcon="false" label="right" format="~~%s~~" />
<ItemImage id="minecraft:emerald" label="right" showTooltip="false" />
<ItemImage id="minecraft:diamond" nbt='{display:{Name:"自定义钻石"}}' />
<ItemImage
  id="minecraft:chest"
  scale="2"
  nbt='{id:"Chest",Items:[{Slot:0b,id:"minecraft:diamond",Count:1b,Damage:0s}]}'
/>
````

### `<ItemLink>`

使用物品顯示名建立文字鏈接，並附帶物品 tooltip。若 `item_ids` 把該物品對應到了某一頁面，點選後還會導覽過去。也可以用 `ore` 透過礦辭的第一個配對結果來決定顯示的物品。

| 屬性 | 預設值 | 意義 |
| --- | --- | --- |
| `id` | — | 物品注册 ID，如 `minecraft:compass` 或 `minecraft:wool:1` |
| `ore` | — | 矿辞名称，使用第一个匹配的物品堆叠 |
| `linksTo` | *（自動）* | 覆蓋跳躍目標，接受帶有可選 `#anchor` 的頁面 ID，如 `./crafting.md#usage` 或 `#usage`；省略時由 `item_ids` / `ore_ids` 索引自動解析 |
| `showTooltip` | `true` | 設為 `false` 時懸停不顯示 tooltip；`noTooltip` 是舊版相容別名 |
| `showIcon` | *（無）* | `left` 或 `right`（或任意真值 → 右側）— 在連結文字的左側或右側顯示物品圖示；省略則僅顯示文字 |
| `scale` | `1.0` | 可選物品圖示的顯示縮放倍率；省略 `showIcon` 時此屬性無效 |

範例：

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

點選後發送聊天指令。

| 屬性 | 意義 |
| --- | --- |
| `command` | 必填，且必須以 `/` 開頭 |
| `title` | 可選 tooltip 標題 |
| `close` | 布林屬性；目前會被解析，但不會實際關閉指南 |

範例：

````md
<CommandLink command="/tp @s 0 90 0" title="Teleport">Teleport!</CommandLink>
````

### `<Row>` 与 `<Column>`

用于块内容的 flex 风格容器。

| 屬性 | 意義 |
| --- | --- |
| `gap` | 子元素間距，整數，預設 `5` |
| `alignItems` | `start`、`center`、`end` |
| `fullWidth` | boolean expression，默认 `false` |
| `width` | 整数首选宽度，适合约束列表等块内容的行宽 |

範例：

````md
<Row gap="8" alignItems="center">
  <ItemImage id="minecraft:iron_ingot" />
  <ItemImage id="minecraft:gold_ingot" />
</Row>
````

如果想要約束普通 Markdown 清單的寬度，可以這樣包裝一層：

````md
<Column width="220">
- 较窄的列表项
- 另一条较窄的列表项
</Column>
````

### `<FootnoteList>`

GuideNH 會在運行時 Markdown 腳註展開後內部使用這個區塊標籤。必要時也可以手寫：

````md
<FootnoteList width="220">
1. 第一条脚注
2. 第二条脚注
</FootnoteList>
````

### `<ItemGrid>`

渲染緊湊的物品圖示網格。其子元素必須是原始 `<ItemIcon>` 元素，由網格編譯器直接解析。每個子元素都可以使用 `id` 或 `ore`。

````md
<ItemGrid>
  <ItemIcon id="minecraft:iron_ingot" />
  <ItemIcon ore="ingotGold" />
  <ItemIcon id="minecraft:gold_ingot" />
  <ItemIcon id="minecraft:redstone" />
</ItemGrid>
````

### `<BlockImage>`

渲染一個非互動式的 3D 單方塊場景。這個預覽沒有場景背景、沒有場景按鈕、沒有 layer 控件，
也不支援註解功能，但滑鼠懸停時仍會顯示選取線框和 tooltip。若使用 `ore`，該礦辭必須
解析到一個方塊物品。

| 屬性 | 意義 |
| --- | --- |
| `id` | 方塊 id；支援常規 `modid:block[:meta][:{snbt}]` 寫法 |
| `ore` | 礦辭查詢；使用第一個符合的方塊物品 |
| `scale` | 相機縮放倍率，預設 `4` |
| `float` | 舊版串流浮動支援：`left` 或 `right` |
| `perspective` | `isometric-north-east`（預設）、`isometric-north-west` 或 `up` |
| `nbt` | 可選的 TileEntity SNBT；會合併到 `id` 內聯 SNBT 之上 |

說明：

- 為相容舊內容，`id` 裡的內聯 SNBT 仍然有效，但更推薦寫成獨立的 `nbt="..."` 屬性
- 當同時提供內聯 SNBT 和 `nbt` 時，`nbt` 會最後合併，因此同名鍵會以 `nbt` 為準
- GuideNH 目前的 1.7.10 運行時不支援這裡的現代方塊狀態屬性語法，因此不會支援
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

完整行為請參考 [圖片與資源](Images-And-Assets)。

快速規則：

- `src` 支援相對路徑、根路徑，以及顯式 `modid:path` 紋理 id
- `x`、`y`、`width` / `w`、`height` / `h` 用于描述原图上的裁剪区域，且必须全部提供
- `scaleX` 與 `scaleY` 用於縮放裁切結果，並支援橫向 / 縱向獨立拉伸
- `displayWidth` 與 `displayHeight` 以像素指定最終尺寸；只提供一個值時保持裁切區域比例，兩個值都提供時可拉伸
- `displayWidth` / `displayHeight` 不能與 `scaleX` / `scaleY` 同時使用
- `wrap="inline"` 會讓圖片真正作為行內內容插入文字流；此時 `align` 會被忽略
- 舊內容如果把 `width` / `height` 當成最終顯示尺寸，需要手動遷移

### `<SubPages>`、`<Category>` 與 `<Special>`

完整導航行為請參閱 [導航](Navigation)。

### `<Structure>`

當你在靜態結構預覽和完整 3D 遊戲場景之間做選擇時，可結合 [範例](Examples) 和 [遊戲場景](GameScene) 參考。

### `<Mermaid>`

用於執行時 Mermaid 內容。目前執行時重點支援 `mindmap`，可直接內嵌，也可透過相對於頁面的 `src` 匯入：

````md
<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [索引](./index.md)"]
    runtime["執行時方塊"]

<NodeContent id="runtime">
執行時節點可以嵌入普通方塊。

<ItemImage id="minecraft:diamond" />
</NodeContent>
</Mermaid>
````

- `width` 與 `height` 限制執行時視口尺寸
- 在視口內拖曳可平移，滑鼠滾輪可縮放
- 加引號的 Mermaid 標籤可以使用 `**粗體**` 和頁面連結等豐富行內 Markdown
- 可在 `<Mermaid>` 內加入 `<NodeContent id="...">...</NodeContent>`，以任意執行時方塊取代節點正文

### `<CsvTable>`

用於執行時把 CSV 檔案解析成表格：

````md
<CsvTable src="./markdown-table.csv" />
````

`src` 路徑會像場景導入和普通資源連結一樣，相對目前頁面解析。

可選屬性：

- `header`
預設是 `true`；設定 `header={false}` 時，首行不會加粗
- `widths`
逗號分隔的整數列寬 hint，例如 `widths="120,80"`

範例：

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
<CsvTable src="./markdown-table.csv" header={false} />
````

對應的圍欄 CSV 運行時寫法也支援相同語意的 `meta`：

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

### `<Latex>`

使用 jlatexmath 渲染 LaTeX 數學公式。在行內（段落或文字流中）使用時，渲染為自動縮放的字形，並將所在行的行高擴展以適應公式高度。獨立成段（區塊級上下文）使用時，居中渲染為展示式公式區塊。

| 屬性 | 類型 | 預設值 | 說明 |
| --- | --- | --- | --- |
| `formula` | 字串 | *（必填）* | LaTeX 來源字串 |
| `color` | `#RRGGBB` 或 `#AARRGGBB` | `#FFFFFF` | 字形填充顏色 |
| `scale` | float | `1.0` | 在自動行高縮放的基礎上額外疊加的顯示大小倍率 |
| `sourceScale` | float | `100.0` | jlatexmath 內部渲染解析度；數值越高，在較大尺寸下渲染越清晰 |
| `tooltip` | 字串 | *無* | 滑鼠懸停時顯示的普通文字 tooltip |
| `showTooltip` | boolean | `false` | 滑鼠懸停時以 tooltip 展示原始 LaTeX 來源文本 |
| `valign` | `baseline` / `top` / `center` / `bottom` | `baseline` | 仅限行内公式。行內垂直對齊方式：`baseline`（預設）使公式數學基線與文字基線對齊；`top` 使公式頂部與行頂對齊；`center` 將公式垂直居中於文字行高；`bottom` 將公式底部對齊文字對齊 |
| `offsetX` | int | `0` | 對齊後額外施加的水平像素偏移（正值為向右） |
| `offsetY` | int | `0` | 對齊後額外施加的垂直像素偏移（正值為向下） |

範例：

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

你可以在 Markdown 文字中直接使用 `$$公式$$`，而無需使用 `<Latex>` 標籤。
所有渲染參數均使用預設值（白色、比例 1.0、無懸停提示、基線對齊）。

- **行內模式**：嵌入段落內的 `$$公式$$` 使用行內渲染。
- **展示模式**：整個段落內容僅為 `$$公式$$`（首尾可有空白）時，渲染為居中的展示式公式塊。

````md
行内简写：$$E=mc^2$$ 和 $$a^2+b^2=c^2$$

行内分数：$$\frac{a+b}{c-d}$$

$$\int_0^\infty e^{-x^2}\,dx = \frac{\sqrt{\pi}}{2}$$

$$\begin{pmatrix} a & b \\ c & d \end{pmatrix}$$
````

注意事項：

- 公式高度以目前行文字高度為基準自動校準。簡單公式與文字等高；含分數、求和、積分等高度較大的公式會自動擴展所在行的行高。
- `valign` 僅對行內公式生效。塊級（展示式）公式始終水平居中；如需垂直方向調整，請使用 `offsetY`。
- `color` 預設為白色（`#FFFFFF`）。如需半透明填充，使用 `#AARRGGBB` 格式。
- `sourceScale` 僅影響渲染清晰度，不會改變顯示大小。低於 `16` 的值會被截斷為 `16`。
- tooltip 優先權為：標籤體富文本 Markdown、`tooltip="..."`、最後才是 `showTooltip={true}` 的原始原始碼回退。
- 標籤體 tooltip 會以普通指南 Markdown 編譯，因此可以包含粗體、列表、連結、物品標籤和巢狀 `<Latex>` 公式。
- `$$公式$$` 簡寫語法始終使用預設參數。如需自訂顏色、比例、對齊或懸停提示，請使用 `<Latex>` 標籤。

### 場景運行時標籤

這些標籤只能在遊戲場景（`<GameScene>` / `<Scene>`）內部運作：

| 标签 | 用途 | 关键属性 |
| --- | --- | --- |
| `<ImportStructure>` | 导入外部 SNBT/NBT 结构资源 | `src`, `x`, `y`, `z`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<ImportStructureLib>` | 透過控制器 id 導入 StructureLib 多方塊 | `controller`, `name`, `piece`, `facing`, `rotation`, `flip`, `channel`, `offsetX`, `offsetY`, `offsetZ`, `formed` |
| `<RemoveBlocks>` | 移除已放置且符合指定方塊匹配器的方塊 | `id` |
| `<BlockAnnotationTemplate>` | 把同一組子註解擴展到場景中所有符合方塊上 | `id` |

关于场景导入/移除行为，请参见 [游戏场景](GameScene)；关于注解模板规则，请参见 [注解](Annotations)。


## 資料圖表

`<ColumnChart>`、`<BarChart>`、`<LineChart>`、`<PieChart>`、`<ScatterChart>` 均为交互式图表块。所有圖表共享下列通用屬性：

| 屬性 | 說明 | 預設 |
| --- | --- | --- |
| `title` | 標題 | 無 |
| `width` / `height` | 顯式尺寸 | 320 / 200 |
| `background` / `border` | 背景與邊框顏色（支援 `#RGB`、`#RRGGBB`、`#AARRGGBB`、`0x...`） | 深灰 |
| `titleColor` / `labelColor` | 標題與資料標籤顏色 | 淺灰 |
| `legend` | 圖例位置：`none`/`top`/`bottom`/`left`/`right` | `top` |
| `labelPosition` | 数据标签位置：`none`/`inside`/`outside`/`above`/`below`/`center` | `none` |
| `cornerLegend` | 圖內角落圖例位置：`none`/`topRight`/`topLeft`/`bottomRight`/`bottomLeft` | `none` |
| `cornerLegendWidth` / `cornerLegendHeight` | 圖內圖例框最大尺寸 | `120` / `64` |
| `cornerLegendBackground` | 圖內圖例背景色 | `#AA111922` |

笛卡尔系图表（柱形/条形/折线/散点）支持坐标轴属性 `xAxisLabel`、`xAxisMin`、`xAxisMax`、`xAxisStep`、`xAxisUnit`、`xAxisTickFormat`，以及对应的`yAxis*`，外加 `showXGrid={true}` 與 `showYGrid={true}` 控制網格線。

子元素：

* `<Series name="..." color="#..." data="10,20,30"/>` — 用於依類別取值的圖表（柱形/長條/類別 X 折線）。
* `<Series name="..." color="#..." points="x:y,x:y,..."/>` — 用於數值 X（折線 `numericX={true}`、散點）。
* `<Slice name="..." value="..." color="#..."/>` — 僅 `<PieChart>` 使用。

未指定 `color` 時依內建 16 色盤循環分配。

`<Series>` 與 `<Slice>` 也接受以下可選圖示/tooltip 屬性：

* `icon="modid:item"`（可带 `@meta` 或互动 NBT JSON，同 `<ItemImage>` 的 `id`）— 使用 `ItemStack` 作为图例色块与悬停图标；悬停时会显示原版物品 tooltip 并在末尾追加图表说明。
* `iconImage="images/foo.png"` — 使用 PNG 資源作為圖例色塊（被 `icon` 覆蓋）。
* `tooltip="..."` — 額外追加到 tooltip 末尾的文字（多行用 `\n`）。

範例：

```mdx
<PieChart title="产出占比">
  <Slice name="铁錠" value="40" icon="minecraft:iron_ingot" tooltip="来自冶炼烉" />
  <Slice name="金錠" value="15" icon="minecraft:gold_ingot" />
</PieChart>
```

### `<ColumnChart>` / `<BarChart>`

附加屬性：`categories`（X 軸/Y 軸類別，逗號分隔）、`barWidthRatio`（預設 0.7）。 `<BarChart>` 類別在 Y 軸、數值在 X 軸。

#### 组合扩展

`<ColumnChart>` 和 `<BarChart>` 額外支援兩類子元素，以便在同一座標系內疊加多種圖形：

- `<LineSeries name="…" data="v1,v2,…" color="#rrggbb" icon="…"/>` — 在柱簇上方再疊加一條折線。每個折點位於對應類別簇中心，與宿主圖共用數值軸；可同時宣告多條 `<LineSeries>`。
- `<PieInset size="60" position="topRight" title="…" startAngleDeg="-90" direction="clockwise" titleColor="#rrggbb">` — 在绘图区四角之一（`topRight`/`topLeft`/`bottomRight`/`bottomLeft`）绘制小型饼图，内部 `<Slice>` 子元素与 `<PieChart>` 一致。

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

僅繪製資料點；`<Series>` 必須使用 `points` 屬性。 X 軸始終為數值軸。

## 函數圖

`<FunctionGraph>` 與單曲線簡寫 `<Function>` 渲染互動式 Desmos 風格面板。同一面板也可透過 ` ```funcgraph ` 圍籬程式碼區塊寫出，詳細範例請見運行時 [Markdown 範例](resourcepack/assets/guidenh/guidenh/_zh_cn/markdown.md)。

面板屬性（容器、簡寫、圍籬首行通用）：

- `width` / `height`（預設 `320` × `220`）
- `title`、`background`、`border`、`axisColor`、`gridColor`
- `showGrid` / `showAxes`（預設 `true`）
- `xRange="a..b"`（或 `xMin` / `xMax` 分寫），`xStep` 控制刻度間距；Y 軸同理
- `xLabel` / `yLabel` 分别在绘图区下方和上方显示类似 Excel 的轴标题，并支持内联 `$$...$$` LaTeX；没有显式 X 范围时，`domain="a..b"` 是兼容旧内容的 `xRange` 别名
- `quadrants="1,2,3,4"` 或 `quadrants="all"` 強制可見象限；不寫則預設僅第一象限，並在取樣發現 `y < 0` 時自動追加第三、第四象限
- `cornerLegend`、`cornerLegendWidth`、`cornerLegendHeight`、`cornerLegendBackground` 可以把帶 `label` 的曲線顯示為繪圖區內部的緊湊圖例

曲線子節點（`<Plot>` / `<Function>`）：

- `expr="..."`：表达式。支持 `+ - * / % ^`、后缀阶乘 `!`（gamma 推广至实数）、`|x|` 绝对值、`√`/`sqrt`、`∛`/`cbrt`、隐式乘法以及常量 `pi`、`tau`、`e`、`phi`。内建调用覆盖常规 trig/log/exp/rounding 函数，并提供双参数 `atan2`、`min`、`max`、`pow`、`hypot`、`mod`。
- `inverse={true}` 將表達式解釋為 `x = f(y)` 並旋轉曲線。
- `domain="a..b"`（x 上下界簡寫）或逗號分隔的比較子句，如 `x>=0, x<5`。
- `color`、`label`。設定非空名稱的曲線會自動出現在面板下方的圖例裡：一個小色塊加曲線名，按從左到右排列，寬度不夠時自動換到下一行。
- `tooltip` 會在計算出的提示訊息後追加純文字。 `showFunction` 和 `showValues` 預設皆為 `true`，設為 `false` 可分別隱藏函數式或即時 `(x, y)` 值。帶有 `expr` 的 `<Plot>` / 內嵌 `<Function>` 可以寫子 Markdown 和 GuideNH 標籤，作為富文本 tooltip 正文。順序總是曲線 `label`、函數式、即時值、`tooltip` 文字、子節點富文本；缺省或關閉的計算行會依此順序跳過。
- `pointEveryX="step"` 以固定 x 間隔在該曲線上產生點。
- `pointEveryY="step"` 在曲線與固定 y 間隔的交點處產生點，內部使用有上限的掃描和二分求解。
- `autoPointLabel="none|x|y|xy"` 控制自動點標籤，預設 `none`。
- `autoPointColor="#..."` 覆蓋自動點顏色；省略時繼承曲線顏色。

標記點（`<Point>`）：

- 明確座標：`x="..."` + `y="..."`。
- 錨定到曲線：`plot="N"` 配合 `atX="v"` 或 `atY="v"`（運行時在該曲線 x 域上二分求解）。
- 可選 `color`、`label`。

互動：滑鼠懸停曲線會高亮此曲線；按住可沿曲線滑動一個標記點。若提供 `label`，提示框最上方先顯示函數名，之後預設顯示函數式和即時 `(x, y)` 值；可用對應開關關閉。提示框預設錨定在該點正上方，頂部空間不足時會自動翻到下方。

## BetterQuesting 相容標籤

`<QuestLink>` 與 `<QuestCard>` 僅在 BetterQuesting 模組已載入時才會被註冊。詳細說明位於 [模組相容](Mod-Compatibility) 頁面，下面給出常用用法摘要。

### `<QuestLink>`

指向 BetterQuesting 任務的行內連結。點選時預設開啟 BetterQuesting 任務 GUI；若該任務 id 出現在某一指南頁的 `quest_ids` 前言中，改為跳到該頁。

| 屬性 | 意義 |
| --- | --- |
| `id` | 必填，BetterQuesting 任務 id；支援標準 UUID 字串和緊湊 Base64 id |
| `text` | 可選，覆蓋顯示文本 |
| `show_tooltip` | 可選布林值，預設 `true`；設為 `false` 可關閉任務描述 tooltip。也接受別名 `showTooltip` |

依玩家進度在編譯時決定外觀：

- 已可見 / 已完成的任務渲染為可點擊連結（已完成的會被染為綠色並追加 `✓`）
- 已鎖定但未隱藏的任務仍會渲染為可點擊任務鏈接，因此仍可打開 BetterQuesting 任務界面或對應的索引指南頁
- 隱藏 / 機密的任務渲染為更深的斜體佔位符，使用 `guidenh.compat.bq.hidden`
- 未知任務 id 渲染為紅色佔位符，使用 `guidenh.compat.bq.missing`

範例：

````md
下一步请参考 <QuestLink id="01234567-89ab-cdef-0123-456789abcdef" />。
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" text="第二阶段任务" />
<QuestLink id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
然后再看 <QuestLink id="AAAAAAAAAAAAAAAAAAAMug==" text="紧凑 quest id 示例" />。
````

### `<QuestCard>`

區塊級任務摘要卡。標題使用與 `<QuestLink>` 相同的狀態相關樣式；當任務對目前玩家可見時，會附帶任務描述作為正文段落。

| 屬性 | 意義 |
| --- | --- |
| `id` | 必填，BetterQuesting 任務 id；支援標準 UUID 字串和緊湊 Base64 id |
| `show_desc` | 可選布爾，預設 `true`；設為 `false` 可隱藏描述正文 |
| `show_tooltip` | 可選布林值，預設 `true`；設為 `false` 可關閉可點選標題上的任務描述 tooltip。也接受別名 `showTooltip` |

卡片邊框顏色隨任務狀態變化：已完成綠色、鎖定 / 隱藏灰色、缺紅色、可見時使用標準連結色。標題在可見、已完成以及鎖定但未隱藏時都會保持可點擊。

範例：

````md
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_desc="false" />
<QuestCard id="01234567-89ab-cdef-0123-456789abcdef" show_tooltip="false" />
<QuestCard id="AAAAAAAAAAAAAAAAAAAMug==" />
````
