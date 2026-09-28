[English](Guide-Page-Format)

# 指南頁格式

GuideNH 的運行時頁面使用 Markdown 文件，目前解析支援：

- 標準 Markdown 區塊級與行內語法
- YAML frontmatter
- GFM 表格
- 刪除線
- `==text==` 行內高亮
- GuideNH 行內下劃線擴展：`++text++`（直下劃線）、`^^text^^`（波浪下劃線）、`::text::`（著重號 / 點狀下劃線）
- `{/* ... */}` 形式的 MDX 註釋
- MDX 風格的自訂標籤

## 已支援的 Markdown

GuideNH 頁面目前支援範例指南裡常用的這些 Markdown 能力：

- 標題
- 段落
- 行內強調、粗體、刪除線、行內程式碼
- 行內高亮（`==text==`）
- 行內底線（`++text++`）、波浪下劃線（`^^text^^`）和著重號（`::text::`）
- 連結與圖片
- 直接寫出的 URL、`www.` 網域和郵件信箱自動鏈接
- 引用式連結與引用式圖片
- 無序列表與有序列表
- 引用區塊
- 分隔線
- 圍籬程式碼區塊
- 縮排程式碼區塊
- GFM 表格
- 純小寫 HTML 片段，如 `<kbd>`、`<sub>`
- 頁面正文中的 MDX 註釋

小寫 HTML 片段範例：<a href="#">連結</a>、<br>、<br />、<sup>上標</sup>、<details><summary>摘要</summary></details>。

可參考 `wiki/resourcepack/assets/guidenh/guidenh/_zh_cn/markdown.md` 查看實際運行時範例。

## 高亮

使用 `==text==` 可以產生行內高亮文字。需要自訂顏色時，可以使用 `<mark color="#8A6A00">text</mark>`。預設高亮色是偏暗的金黃色，用於在白色文字下保持可讀性。

## 程式碼區塊

運行時代碼區塊目前支援：

- 明確指定圍欄語言，例如 `java`、`lua`、`scala`、`csv`、`mermaid`
- 圍籬語言省略時自動推斷語言
- 在程式碼區塊頂部顯示語言標籤
- 在遊戲內右上角提供一鍵複製按鈕
- 對辨識出的語言做輕量運行時語法高亮

範例：

````md
```lua
local value = 42
print(value)
```

```
object Demo extends App {
  println("auto detected scala")
}
```
````

縮排程式碼區塊同樣支援：

````md
    print("indented code")
````

當圍欄程式碼區塊識別為 `mermaid`，並且內容是目前支援的 `mindmap` 語法時，GuideNH 會把它渲染成可互動的運行時思維導圖，而不是普通程式碼區塊。

當圍欄程式碼區塊明確標記為 `csv` 時，GuideNH 會把它渲染成運行時表格，而不是普通程式碼區塊。如果不寫圍籬語言，即使內容看起來像 CSV，也仍然保留為程式碼區塊，只把語言辨識結果用於標籤和高亮。

明確 CSV 表格也可以提供列寬 hint：

````md
```csv widths=120,80
name,value
iron,42
gold,17
```
````

圍欄 `meta` 也支援 `header=false` 和帶引號的寬度清單：

````md
```csv widths="120,80" header=false
name,value
iron,42
gold,17
```
````

普通段落文字裡也支援 GitHub 風格的直接自動連結：

````md
访问 https://example.com/docs、www.example.org 或 guide@example.com
````

## Mermaid 心智圖

GuideNH 目前的 Mermaid 運行時支援聚焦在 `mindmap`：

- 圍欄 ```` ```mermaid ```` 程式碼區塊
- 內容以 `mindmap` 開頭時的自動識別
- 明確 `<Mermaid>...</Mermaid>` 標籤
- 明確 `<Mermaid src="./diagram.mmd" />` 資源導入
- Mermaid 節點文字中的富文本行內 Markdown
- 作為 Mermaid 子元素的 `<NodeContent id="...">...</NodeContent>`，可為指定節點放入任意執行時間區塊內容
- 遊戲內整張圖拖曳平移
- Mermaid 來源文字裡的 `layout: tidy-tree`
- 常見 mindmap 節點形狀，例如方形、圓角、圓形、bang、cloud、hexagon
- `::icon(...)` 與 `:::class` 元資料解析

範例：

````md
```mermaid
mindmap
  root((GuideNH))
    Runtime
      Markdown
      CSV
    Mindmap::icon(fa fa-sitemap)
      Drag to pan
```

<Mermaid src="./markdown-mindmap.mmd" />

<Mermaid width="340" height="240">
mindmap
  root["**GuideNH** [首页](./index.md)"]
    runtime["运行时块"]
    export["站点导出"]

<NodeContent id="runtime">
运行时节点里可以混排普通文本、链接和块内容。

<ItemImage id="minecraft:diamond" />
</NodeContent>

<NodeContent id="export">
![Machine Diagram](./resourcepack/assets/guidenh/guidenh/_zh_cn/test1.png)
</NodeContent>
</Mermaid>
````

目前運行時尚未支援的 Mermaid 圖類型，會繼續以具有 Mermaid 標籤的普通程式碼區塊顯示。

## CSV 表格導入

GuideNH 也支援透過明確標籤在執行時導入 CSV 檔案：

````md
<CsvTable src="./markdown-table.csv" />
````

`src` 路徑會像普通運行時資源連結和場景 `src` 導入一樣，相對目前頁面解析。

導入的 CSV 表格同樣可以提供列寬 hint：

````md
<CsvTable src="./markdown-table.csv" widths="120,80" />
````

也可以直接在 Markdown 裡透過明確 `csv` 圍籬寫內聯表格：

````md
```csv
name,value
iron,42
gold,17
```
````

## Markdown 表格列寬 Hint

普通 GFM Markdown 表格也可以在表格後面緊跟著一行執行時間屬性，提供列寬 hint：

````md
| Name | Value |
| --- | --- |
| Iron | 42 |
| Gold | 17 |
{: widths="120,80" }
````

這樣表格本體仍然保持標準 Markdown，只是在 GuideNH 運行時應用列的首選寬度。

## 任務清單、提示區塊與註腳

GuideNH 運行時也支援幾種常見的 GFM 風格行為：

- 使用 `- [ ]` 和 `- [x]` 的任務列表
- GitHub 风格提示引用块，例如 `[!NOTE]`、`[!TIP]`、`[!IMPORTANT]`、`[!WARNING]`、`[!CAUTION]`
- 註腳引用與定義

範例：

````md
- [x] 已完成
- [ ] 待处理

> [!NOTE]
> 这里是提示内容

脚注引用[^one]

[^one]: 这里是脚注内容
````

腳註引用會在正文中渲染成 tooltip 風格標記，並在頁面下方追加一個緊湊的腳註列表。

## 列表寬度自訂

標準 Markdown 列表本身沒有寬度控制，但 GuideNH 運行時容器可以約束列表寬度：

````md
<Column width="220">
- 较窄的列表项
- 另一条较窄的列表项
</Column>
````

這是目前執行階段自訂清單行寬的建議寫法。

## 引用式連結與圖片

GuideNH 支持 CommonMark 引用定義：

````md
[Guide Ref][doc]
![Machine][img]

[doc]: ./subpage.md#intro
[img]: ./test1.png "Machine Diagram"
````

## 原生 HTML 片段

純小寫 HTML 標籤會以字面量內聯或區塊級 HTML 內容處理，而不會當成 GuideNH 執行時間標籤：

````md
Press <kbd>Shift</kbd> + <sub>1</sub>
````

## MDX 註釋

GuideNH 支持 MDX 註釋寫法，並會在真正編譯 Markdown 前忽略它們：

````md
可見文字。{/* 隱藏的行內註解 */}

{/*
multiline comment
*/}

More visible text.
````

## Frontmatter

GuideNH 會讀取第一個 YAML frontmatter 區塊，並解析這些已知鍵：

| 鍵 | 類型 | 意義 |
| --- | --- | --- |
| `navigation` | map | 將頁面加入導覽樹 |
| `categories` | 字串列表 | 將頁面加入 MediaWiki 風格分類；每一項可選使用 `分類名|排序名` |
| `item_id` | 單一物品篩選表達式 | 單一 NEI 風格物品表達式，讓頁面可被 `<ItemLink>` 發現 |
| `item_ids` | 物品篩選表達式列表 | `item_id` 的列表形式，任一表達式符合即可讓頁面被 `<ItemLink>` 發現 |
| `ore_ids` | 礦辭名列表 | 讓頁面可被礦物物品（如 `ingotIron`、`oreCopper`）索引 |
| `quest_ids` | BetterQuesting 任務 id 列表 | 讓頁面可被 `<QuestLink>` / `<QuestCard>` 以及 BQ 任務 GUI 中的開啟指南快速鍵發現。支援標準 UUID 字串和 BetterQuesting 的緊湊 Base64 形式。僅在 BetterQuesting 載入時生效。參見 [模組相容](Mod-Compatibility) |
| `author` | string | 單一作者名稱。顯示在底部欄。 |
| `authors` | 字串列表或 `{name: ...}` 映射列表 | 多位作者名稱。最多顯示兩位，多餘的用 `...` 取代。與 `author` 同時存在時以 `authors` 為準。 |
| `date` | 字串或 YYYY-MM-DD 日期 | 內容創作日期。顯示在底部欄。 |
| `updated` | 字串或 YYYY-MM-DD 日期 | 最後更新日期。顯示在底部欄。 |
| `zoom` | 正浮點數 | 單頁內容縮放倍數（如 `1.5` 表示 150%）。與 ModConfig 中全域 `contentZoom` 設定相乘。預設 `1.0`。 |
| 其他任意鍵 | 任意 YAML 值 | 儲存在 `additionalProperties` 中，供擴充或工具使用 |

### `navigation`

| 字段 | 必需 | 類型 | 說明 |
| --- | --- | --- | --- |
| `title` | 是 | string | 導覽顯示名稱，也可作為搜尋標題後備值 |
| `keyword` | 否 | string | 一個額外的搜尋關鍵字或別名，支援前綴模糊匹配 |
| `keywords` | 否 | string 列表 | 額外的搜尋關鍵字或別名；多個欄位會合併並去重 |
| `parent` | 否 | page id | 父頁 id；省略時為頂層節點 |
| `position` | 否 | integer | 同級排序順序，預設 `0`，數值越大越前 |
| `priority` | 否 | integer | 同路徑頁覆蓋時的載入優先權；預設 `0`，數值較高者勝出，相同優先權時後處理的資源包條目覆蓋先處理的 |
| `icon` | 否 | item id | 導覽和搜尋中顯示的物品圖示。支援 `modid:name`、`modid:name:meta`、`modid:name:meta:{snbt}`。如需附加自訂名稱等 NBT，建議直接寫在 `icon` 後面的內嵌 `:{snbt}` 尾部。 |
| `icons` | 否 | item id 列表 | 循環輪播的物品圖示清單（每秒切換一次）。每一項語法同 `icon`，支持內聯 `:{snbt}`。存在時優先於 `icon` 使用。 |
| `icon_texture` | 否 | asset path | 紋理圖示路徑，依普通資源路徑解析 |
| `icon_textures` | 否 | asset path 列表 | 循環輪播的紋理圖示清單（每秒切換一次）。存在時優先於 `icon_texture` 使用。 |
| `required_mod` | 否 | 模組 id | 僅在該模組已載入時顯示頁面。 |
| `required_mods` | 否 | 模組 id 列表 | 僅在列出的全部模組均已載入時顯示頁面。 |
| `excluded_mod` | 否 | 模組 id | 此模組載入時隱藏頁面。 |
| `excluded_mods` | 否 | 模組 id 列表 | 其中任一模組載入時隱藏頁面。 |

### Frontmatter 範例

```yaml
item_id: minecraft:potion 16384-16462,!16386
item_ids:
  - ae2:white_paint_ball:*
  - "<minecraft:wool:14>"
navigation:
  title: Root
  parent: index.md
  position: 10
  priority: 0
  icon: minecraft:book:0:{display:{Name:"我的自定义书"}}
  # 使用 meta/损伤值选择特定子类型：
  # icon: minecraft:wool:1       （橙色羊毛，冒号写法）
  # 循环图标列表——每秒切换一次：
  # icons:
  #   - minecraft:wool:1
  #   - minecraft:wool:4:{display:{Name:"自定义绿色羊毛"}}
  #   - minecraft:wool:14

  icon_texture: test1.png
  # 循环纹理列表：
  # icon_textures:
  #   - test1.png
  #   - test2.png
categories:
  - basics
  - examples|Examples Overview
ore_ids:
  - ingotIron
  - oreCopper
quest_ids:
  - 01234567-89ab-cdef-0123-456789abcdef
  - AAAAAAAAAAAAAAAAAAAMug==
author: 示例作者
date: 2024-01-15
updated: 2024-06-01
```

`categories` 既支持普通分類名，也支持 `分类名|排序名` 形式。
可以使用 `<Category name="examples" rows="3" />` 渲染分類清單區塊，
並使用 `<Special name="SpecialPages" rows="3" />` 嵌入自動產生的 MediaWiki 風格特殊頁索引。

對於 BetterQuesting 集成，`quest_ids` 支援兩種格式：

- 標準 UUID 字串，例如 `01234567-89ab-cdef-0123-456789abcdef`
- BetterQuesting 的緊湊 quest id，例如 `AAAAAAAAAAAAAAAAAAAMug==`

不要在同一頁的 `quest_ids` 中同時填入同一個任務的兩種寫法；它們會歸一化成同一個內部 UUID，並被視為重複項。

當 `author`、`authors`、`date` 或 `updated` 中任一項存在時，GuideNH 會在
指南介面底部顯示一個與頂部工具列風格一致的底部欄，靠右對齊顯示形如：
*內容來自 我的模組，作者 範例作者，日期 2024-01-15，更新日期 2024-06-01* 的資訊。

多作者範例：
```yaml
authors:
  - 爱丽丝
  - 鲍勃
  - 查理   # 只显示爱丽丝和鲍勃，后跟 ...
```
或使用結構化寫法：
```yaml
authors:
  - name: 爱丽丝
  - name: 鲍勃
```

### `zoom`

`zoom` 欄位可以單獨放大或縮小某一頁的內容，而不影響其他頁面。其值為正浮點數，作為倍數使用：

| 範例值 | 效果 |
| --- | --- |
| `1.0`（預設） | 正常大小 |
| `1.5` | 150%，內容放大 50% |
| `0.75` | 75%，內容縮小 25% |

單頁 `zoom` 與 ModConfig → GuideNH → UI 中的全域 **contentZoom** 設定相乘生效。
這使模組包可以設定合理的基準縮放，同時允許個別頁面根據內容寬窄自行微調。

範例：將本頁設定為基準縮放的 150%：

```yaml
zoom: 1.5
navigation:
  title: 我的密集页面
```

縮放會以調整後的寬度重新計算頁面佈局，因此無論縮放等級為何，文字換行和所有區塊佈局都能保持正確。

## 連結解析規則

GuideNH 以下列規則解析 id 和路徑：

### 頁面鏈接

| 輸入 | 意義 |
| --- | --- |
| `subpage.md` | 相對目前頁面，並使用目前頁面命名空間 |
| `./subpage.md` | 相對目前頁面，並使用目前頁面命名空間 |
| `/guide.md` | 相對目前頁命名空間根路徑，等價於 `currentmod:guide.md` |
| `gregtech:guide.md` | 明確命名空間；目前指南路徑為 `guidenh` 時會開啟 `gregtech:guidenh` |
| `gregtech:/guide.md` | 明確命名空間加根路徑，會規範化為 `gregtech:guide.md` |
| `subpage.md#anchor` | 頁面加錨點片段 |
| `guidenh:other.md#anchor` | 顯式 `modid:path#anchor` |
| `https://example.com` | 外部 HTTP/HTTPS 鏈接 |

頁面連結依命名空間隔離。例如在 `assets/guidenh/guidenh/_zh_cn/index.md` 中寫
`[Guide](guide.md)` 會解析為 `guidenh:guide.md`；同樣的文字放在
`assets/gregtech/guidenh/_zh_cn/index.md` 中會解析為 `gregtech:guide.md`。如果目前命名空間下找不到目標頁面，
GuideNH 會回報壞鏈，而不會回退到其他模組的同名頁面。

明確 `modid:path` 連結可以跨到另一個模組的資料驅動指南。目標 guide id 會由目標頁命名空間和目前指南路徑推導，
所以從 `guidenh:guidenh` 連結到 `gregtech:guide.md` 時，會開啟 `gregtech:guidenh` 中的
`gregtech:guide.md` 頁面。

錨點片段會使指南捲動到對應標題（標題文字全部小寫、空格替換為連字符，例如 `#crafting-recipe` 對應 `## Crafting Recipe`），或捲動到 `<a name="...">` 命名錨點處。

### 資源鏈接

資源使用與頁面連結相同的解析規則。例如：

- `test1.png` 相對目前頁面檔案解析
- `/assets/example_structure.snbt` 解析到指南資源根目錄
- `guidenh:textures/gui/example.png` 作為明確資源定位子解析

## 物品引用語法

導覽 `icon`、`icons` 以及接受 item id 的標籤使用普通物品引用語法：

```text
modid:name
modid:name:meta
modid:name:meta:{snbt}
```

省略 `meta` 時預設使用 `0`。 SNBT 從第一個 `{` 開始，並會解析為物品 NBT。在支援通配 meta 的位置，
`*` 可與 SNBT 尾部組合使用。

範例：

```text
minecraft:diamond
minecraft:wool:14
minecraft:written_book:0:{title:TestBook,author:GuideNH}
minecraft:written_book:*:{title:TestBook,author:GuideNH}
```

### 物品索引表達式

`item_id` 接受一個 NEI 風格表達式，`item_ids` 接受相同表達式語法的 YAML 列表。 `item_ids` 中每項彼此獨立，任一項配對即可關聯該頁面。空格用於組合條件，`|` 用於組合備選條件，`,` 用於組合一個條件中的規則。

- `minecraft:lava` 會對登錄 id 做不區分大小寫的部分匹配，因此也會匹配 `minecraft:lava_bucket`。
- `<minecraft:wool:14>` 嚴格配對一個物品及其 meta。
- `ae2:white_paint_ball:*`、`:32767` 與 `:ANY` 等大寫 meta 標記是相容的嚴格全 meta 寫法。
- `16384-16462,!16386` 符合 meta 範圍，同時排除 `16386`。
- `!minecraft:portal` 排除符合該登錄 id 的物品。
- `r/^m\\w{6}ft$/` 對登錄 id 使用 Java 正規表示式。

```yaml
item_id: "minecraft:potion 16384-16462,!16386 | ae2:white_paint_ball:*"
item_ids:
  - minecraft:crafting_table
  - appliedenergistics2:item.ItemMultiMaterial:1
  - "minecraft:written_book:*:{title:TestBook,author:GuideNH},!minecraft:written_book:0"
  - "<minecraft:wool:14>"
  - wrench|hammer
  - "minecraft:potion 0-16,20-36,!28"
  - minecraft:diamond#Usage
```

此例中，空格用於組合條件，`,` 用於組合同一項中的規則，`!` 用於排除匹配項，`|` 用於分隔備選表達式。
因此第一個表達式會符合 `16384` 至 `16462` 的藥水 meta（排除 `16386`），或符合任意 meta 的
`ae2:white_paint_ball`。第二個表達式展示了通配物品引用如何與逗號規則和 `!` 反向排除組合使用。
最後兩項展示了多個 meta 範圍的並集並排除 `28`，以及打開 `Usage` 標題錨點的物品映射。

可在末尾追加 `#anchor`，使符合頁面開啟到特定標題錨點。精確物品和顯式 meta 映射優先走直接索引，只有必要時才會計算表達式。

## 錯誤處理

如果頁面解析失敗，GuideNH 會產生錯誤頁，而不是讓整個指南崩潰。無效標籤、id 和屬性會以內嵌指南錯誤文字的形式顯示出來。

## 相關頁面

- [導覽](Navigation)
- [圖片與資源](Images-And-Assets)
- [標籤參考](Tags-Reference)
